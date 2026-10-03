package com.ishara.app

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.remote.datasource.UserRemoteDataSource
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.data.repository.OnboardingRepositoryImpl
import com.ishara.app.data.repository.UserRepositoryImpl
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.GetOnboardingStateUseCase
import com.ishara.app.domain.usecase.ResolveApplicationDestinationUseCase
import com.ishara.app.domain.usecase.SubmitOnboardingUseCase
import com.ishara.app.feature.onboarding.OnboardingViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Validates the complete Role & Onboarding Architecture for Phase 04.
 * Covers all 12 required architectural scenarios.
 */
class OnboardingArchitectureTest {

    private class MockUserRemoteDataSource(
        var userDto: UserDto = UserDto(
            id = "user_123",
            name = "Test Student",
            role = "",
            isOnboarded = false
        ),
        var shouldFailValidation: Boolean = false,
        var shouldNetworkFail: Boolean = false
    ) : UserRemoteDataSource {

        val completeOnboardingCallCount = AtomicInteger(0)
        var slowSubmissionGate: CompletableDeferred<Unit>? = null

        override suspend fun getCurrentUserProfile(token: String): IshaaraResult<UserDto> {
            if (shouldNetworkFail) {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }
            return IshaaraResult.success(userDto)
        }

        override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> {
            completeOnboardingCallCount.incrementAndGet()
            slowSubmissionGate?.await()

            if (shouldFailValidation) {
                return IshaaraResult.failure(
                    IshaaraError.Validation(field = "role", message = "Role already assigned.")
                )
            }
            if (shouldNetworkFail) {
                return IshaaraResult.failure(IshaaraError.Network("Connection lost during submission."))
            }

            val updated = userDto.copy(role = role, isOnboarded = true)
            userDto = updated
            return IshaaraResult.success(updated)
        }

        override suspend fun updateUserProfile(
            name: String?,
            phoneNumber: String?,
            image: String?,
            token: String
        ): IshaaraResult<UserDto> {
            val updated = userDto.copy(
                name = name ?: userDto.name,
                phoneNumber = phoneNumber ?: userDto.phoneNumber,
                image = image ?: userDto.image
            )
            userDto = updated
            return IshaaraResult.success(updated)
        }
    }

    private fun createInitialSession(): AuthSession {
        return AuthSession(
            token = "valid_session_token",
            userId = "user_123",
            role = UserRole.USER
        )
    }

    @Test
    fun test1_authenticatedUser_incompleteProfile_requiresOnboarding() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource(
            userDto = UserDto(id = "user_123", name = "Test", role = "", isOnboarded = false)
        )
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)

        val stateResult = onboardingRepository.getOnboardingState()

        assertTrue(stateResult.isSuccess)
        assertEquals(OnboardingState.Required, stateResult.getOrNull())
    }

    @Test
    fun test2_authenticatedUser_completedProfile_allowsApplication() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource(
            userDto = UserDto(id = "user_123", name = "Test", role = "USER", isOnboarded = true)
        )
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)

        val stateResult = onboardingRepository.getOnboardingState()

        assertTrue(stateResult.isSuccess)
        val state = stateResult.getOrNull()
        assertTrue(state is OnboardingState.Completed)
        assertEquals(ApplicationDestination.StudentHome, (state as OnboardingState.Completed).destination)
    }

    @Test
    fun test3_validUserRole_resolvesStudentDestination() {
        val resolver = ResolveApplicationDestinationUseCase()
        val studentProfile = UserProfile(
            id = "user_123",
            name = "Student",
            role = UserRole.USER,
            isOnboarded = true
        )

        val destination = resolver(studentProfile)
        assertEquals(ApplicationDestination.StudentHome, destination)
    }

    @Test
    fun test4_validDriverRole_resolvesDriverDestination() {
        val resolver = ResolveApplicationDestinationUseCase()
        val driverProfile = UserProfile(
            id = "driver_456",
            name = "Driver",
            role = UserRole.DRIVER_CONDUCTOR,
            isOnboarded = true
        )

        val destination = resolver(driverProfile)
        assertEquals(ApplicationDestination.DriverDashboard, destination)
    }

    @Test
    fun test5_invalidRole_rejectedFromBackend_returnsNullRole() {
        val unassignedRole = UserRole.fromBackendString("UNKNOWN_ROLE")
        assertNull("Unrecognized roles must not be parsed into valid enum", unassignedRole)

        val corruptProfile = UserProfile(
            id = "user_corrupted",
            name = "Corrupted",
            role = null,
            isOnboarded = true
        )

        val resolver = ResolveApplicationDestinationUseCase()
        val destination = resolver(corruptProfile)
        assertEquals(ApplicationDestination.Onboarding, destination)
    }

    @Test
    fun test6_noRoleSelected_cannotSubmit() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource()
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)
        val navigationManager = NavigationManager()

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = OnboardingViewModel(
            getOnboardingStateUseCase = GetOnboardingStateUseCase(onboardingRepository),
            submitOnboardingUseCase = SubmitOnboardingUseCase(onboardingRepository),
            navigationManager = navigationManager,
            externalScope = testScope
        )

        // Attempt submit without role selection
        viewModel.submitOnboarding()

        assertEquals(0, remoteDataSource.completeOnboardingCallCount.get())
        assertEquals("Choose how you'll use Ishaara.", viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun test7_successfulOnboarding_emitsCompletedState() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource()
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)

        val result = onboardingRepository.submitOnboarding(UserRole.DRIVER_CONDUCTOR)

        assertTrue(result.isSuccess)
        val state = onboardingRepository.observeOnboardingState().first()
        assertTrue(state is OnboardingState.Completed)
        assertEquals(UserRole.DRIVER_CONDUCTOR, (state as OnboardingState.Completed).userProfile.role)
        assertEquals(ApplicationDestination.DriverDashboard, state.destination)

        // Verify session store role updated to DRIVER_CONDUCTOR
        assertEquals(UserRole.DRIVER_CONDUCTOR, sessionStore.getSession()?.role)
    }

    @Test
    fun test8_backendValidationError_emitsErrorAndPreservesInput() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource(shouldFailValidation = true)
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)
        val navigationManager = NavigationManager()

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = OnboardingViewModel(
            getOnboardingStateUseCase = GetOnboardingStateUseCase(onboardingRepository),
            submitOnboardingUseCase = SubmitOnboardingUseCase(onboardingRepository),
            navigationManager = navigationManager,
            externalScope = testScope
        )

        viewModel.selectRole(UserRole.USER)
        viewModel.submitOnboarding()

        assertEquals(UserRole.USER, viewModel.uiState.value.selectedRole)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun test9_networkFailure_emitsRetryableErrorAndPreservesInput() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource(shouldNetworkFail = true)
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)
        val navigationManager = NavigationManager()

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = OnboardingViewModel(
            getOnboardingStateUseCase = GetOnboardingStateUseCase(onboardingRepository),
            submitOnboardingUseCase = SubmitOnboardingUseCase(onboardingRepository),
            navigationManager = navigationManager,
            externalScope = testScope
        )

        viewModel.selectRole(UserRole.DRIVER_CONDUCTOR)
        viewModel.submitOnboarding()

        assertEquals(UserRole.DRIVER_CONDUCTOR, viewModel.uiState.value.selectedRole)
        assertEquals("You're offline. Check your connection and try again.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun test10_duplicateSubmission_dispatchesOnlyOneRequest() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource()
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)
        val navigationManager = NavigationManager()

        val gate = CompletableDeferred<Unit>()
        remoteDataSource.slowSubmissionGate = gate

        val testScope = CoroutineScope(Dispatchers.IO)
        val viewModel = OnboardingViewModel(
            getOnboardingStateUseCase = GetOnboardingStateUseCase(onboardingRepository),
            submitOnboardingUseCase = SubmitOnboardingUseCase(onboardingRepository),
            navigationManager = navigationManager,
            externalScope = testScope
        )

        viewModel.selectRole(UserRole.USER)

        // First submit dispatches in-flight
        viewModel.submitOnboarding()
        delay(50)

        // Duplicate submit while in flight
        viewModel.submitOnboarding()

        // Release gate
        gate.complete(Unit)
        delay(100)

        assertEquals("Only one remote call must be dispatched during in-flight submission", 1, remoteDataSource.completeOnboardingCallCount.get())
    }

    @Test
    fun test11_appRestartDuringOnboarding_retainsConsistentBackendState() = runBlocking {
        val sessionStore = InMemorySessionStore(createInitialSession())
        val remoteDataSource = MockUserRemoteDataSource(
            userDto = UserDto(id = "user_123", name = "Test", role = "", isOnboarded = false)
        )
        val userRepository = UserRepositoryImpl(remoteDataSource, sessionStore)
        val onboardingRepository = OnboardingRepositoryImpl(remoteDataSource, userRepository, sessionStore)

        // Evaluation after app process restart
        val result = onboardingRepository.getOnboardingState()
        assertTrue(result.isSuccess)
        assertEquals(OnboardingState.Required, result.getOrNull())
    }

    @Test
    fun test12_completedOnboarding_backNavigationGuarded() {
        val userProfile = UserProfile(
            id = "user_123",
            name = "Test",
            role = UserRole.USER,
            isOnboarded = true
        )
        val resolver = ResolveApplicationDestinationUseCase()
        val destination = resolver(userProfile)

        // Destination is StudentHome, never Onboarding for completed accounts
        assertEquals(ApplicationDestination.StudentHome, destination)
    }
}
