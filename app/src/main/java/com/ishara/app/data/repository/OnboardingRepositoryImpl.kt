package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.UserMapper
import com.ishara.app.data.remote.datasource.UserRemoteDataSource
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.OnboardingRepository
import com.ishara.app.domain.repository.UserRepository
import com.ishara.app.domain.usecase.ResolveApplicationDestinationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of OnboardingRepository.
 * Manages the domain OnboardingState state machine, role submission, and synchronization with session storage.
 */
class OnboardingRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
    private val destinationResolver: ResolveApplicationDestinationUseCase = ResolveApplicationDestinationUseCase()
) : OnboardingRepository {

    private val _onboardingStateFlow = MutableStateFlow<OnboardingState>(OnboardingState.Unknown)
    private val submissionMutex = Mutex()

    override fun observeOnboardingState(): Flow<OnboardingState> = _onboardingStateFlow.asStateFlow()

    override suspend fun getOnboardingState(): IshaaraResult<OnboardingState> = submissionMutex.withLock {
        val profileResult = userRepository.getCurrentUserProfile()
        return when (profileResult) {
            is IshaaraResult.Success -> {
                val profile = profileResult.data
                val state = if (profile.hasCompletedOnboarding) {
                    val destination = destinationResolver(profile)
                    OnboardingState.Completed(profile, destination)
                } else {
                    OnboardingState.Required
                }
                _onboardingStateFlow.value = state
                IshaaraResult.success(state)
            }
            is IshaaraResult.Failure -> {
                // If offline, check if session already has an onboarded role
                val session = sessionStore.getSession()
                if (session != null && profileResult.error is IshaaraError.Network) {
                    val offlineProfile = UserProfile(
                        id = session.userId,
                        name = "User",
                        role = session.role,
                        isOnboarded = true
                    )
                    val destination = destinationResolver(offlineProfile)
                    val state = OnboardingState.Completed(offlineProfile, destination)
                    _onboardingStateFlow.value = state
                    IshaaraResult.success(state)
                } else {
                    val state = OnboardingState.Error(profileResult.error)
                    _onboardingStateFlow.value = state
                    IshaaraResult.failure(profileResult.error)
                }
            }
        }
    }

    override suspend fun submitOnboarding(role: UserRole): IshaaraResult<UserProfile> = submissionMutex.withLock {
        _onboardingStateFlow.value = OnboardingState.Submitting(role)

        val session = sessionStore.getSession()
            ?: run {
                val err = IshaaraError.Authentication(message = "No active session available to complete onboarding.")
                _onboardingStateFlow.value = OnboardingState.Error(err, retryRole = role)
                return IshaaraResult.failure(err)
            }

        IshaaraLogger.i(TAG, "Submitting role assignment: ${role.name}")
        val result = remoteDataSource.completeOnboarding(role.name, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val updatedProfile = UserMapper.toDomain(result.data)

                // Update session store so local session contains the authoritative assigned role
                val updatedSession = session.copy(role = role)
                sessionStore.saveSession(updatedSession)

                // Refresh current user in UserRepository so all subscribers observe the updated profile
                userRepository.getCurrentUserProfile()

                val destination = destinationResolver(updatedProfile)
                val completedState = OnboardingState.Completed(updatedProfile, destination)
                _onboardingStateFlow.value = completedState
                IshaaraLogger.i(TAG, "Onboarding successful for user: ${updatedProfile.id}, role: $role -> $destination")
                IshaaraResult.success(updatedProfile)
            }
            is IshaaraResult.Failure -> {
                val error = result.error
                val isConflict = error is IshaaraError.Conflict ||
                    (error as? IshaaraError.Conflict)?.errorCode == "ONBOARDING_ALREADY_COMPLETED" ||
                    error.message.contains("already been completed", ignoreCase = true) ||
                    error.message.contains("already completed", ignoreCase = true) ||
                    error.message.contains("Role cannot be re-assigned", ignoreCase = true)

                if (isConflict) {
                    IshaaraLogger.i(TAG, "Onboarding 409 Conflict: user already completed onboarding. Reconciling with GET /api/v1/users/me...")
                    val profileResult = userRepository.getCurrentUserProfile()
                    if (profileResult is IshaaraResult.Success) {
                        val reconciledProfile = profileResult.data
                        if (reconciledProfile.hasCompletedOnboarding) {
                            val authoritativeRole = reconciledProfile.role ?: role
                            val updatedSession = session.copy(role = authoritativeRole)
                            sessionStore.saveSession(updatedSession)

                            val destination = destinationResolver(reconciledProfile)
                            val completedState = OnboardingState.Completed(reconciledProfile, destination)
                            _onboardingStateFlow.value = completedState
                            IshaaraLogger.i(TAG, "Reconciled onboarding conflict with authoritative profile: role=$authoritativeRole -> $destination")
                            return IshaaraResult.success(reconciledProfile)
                        }
                    }
                }

                IshaaraLogger.w(TAG, "Onboarding submission failed: ${result.error.message}")
                _onboardingStateFlow.value = OnboardingState.Error(result.error, retryRole = role)
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun resetOnboardingState() = submissionMutex.withLock {
        _onboardingStateFlow.value = OnboardingState.Unknown
    }

    companion object {
        private const val TAG = "OnboardingRepository"
    }
}
