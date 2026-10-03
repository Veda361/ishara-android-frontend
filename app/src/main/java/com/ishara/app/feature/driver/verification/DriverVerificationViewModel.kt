package com.ishara.app.feature.driver.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.usecase.GetDriverVerificationStatusUseCase
import com.ishara.app.domain.usecase.ObserveDriverOnboardingStateUseCase
import com.ishara.app.domain.usecase.ObserveDriverProfileUseCase
import com.ishara.app.domain.usecase.RefreshDriverProfileUseCase
import com.ishara.app.domain.usecase.SubmitDriverVerificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing Driver Verification review, status polling, and resubmissions.
 * Enforces strict separation between verification and operational readiness.
 */
class DriverVerificationViewModel(
    private val getDriverVerificationStatusUseCase: GetDriverVerificationStatusUseCase,
    private val submitDriverVerificationUseCase: SubmitDriverVerificationUseCase,
    private val observeDriverProfileUseCase: ObserveDriverProfileUseCase,
    private val observeDriverOnboardingStateUseCase: ObserveDriverOnboardingStateUseCase,
    private val refreshDriverProfileUseCase: RefreshDriverProfileUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _uiState = MutableStateFlow(DriverVerificationUiState())
    val uiState: StateFlow<DriverVerificationUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeOnboardingState()
        refreshStatus()
    }

    private fun observeProfile() {
        scope.launch(dispatchers.main) {
            observeDriverProfileUseCase().collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    private fun observeOnboardingState() {
        scope.launch(dispatchers.main) {
            observeDriverOnboardingStateUseCase().collect { state ->
                _uiState.update { it.copy(onboardingState = state) }
            }
        }
    }

    fun onResubmissionNotesChanged(notes: String) {
        val limited = if (notes.length > 500) notes.take(500) else notes
        _uiState.update { it.copy(resubmissionNotes = limited, userFacingError = null) }
    }

    fun refreshStatus() {
        _uiState.update { it.copy(isRefreshing = true, userFacingError = null) }
        scope.launch(dispatchers.io) {
            // First fetch verification details
            val verifResult = getDriverVerificationStatusUseCase()
            if (verifResult is IshaaraResult.Success) {
                _uiState.update { it.copy(verificationDetails = verifResult.data) }
            }

            // Then refresh driver profile
            val profileResult = refreshDriverProfileUseCase()
            _uiState.update { current ->
                val errorMsg = if (profileResult is IshaaraResult.Failure) {
                    mapErrorToMessage(profileResult.error)
                } else null
                current.copy(
                    isRefreshing = false,
                    userFacingError = errorMsg
                )
            }
        }
    }

    fun submitResubmission(onSuccess: (() -> Unit)? = null) {
        val currentProfile = _uiState.value.profile
        if (currentProfile?.verificationStatus != DriverVerificationStatus.REJECTED) {
            _uiState.update {
                it.copy(userFacingError = "Verification resubmission is only allowed after rejection.")
            }
            return
        }

        val notes = _uiState.value.resubmissionNotes.trim().ifEmpty { null }
        _uiState.update { it.copy(isSubmitting = true, userFacingError = null, userFacingMessage = null) }

        scope.launch(dispatchers.io) {
            val result = submitDriverVerificationUseCase(notes)
            when (result) {
                is IshaaraResult.Success -> {
                    IshaaraLogger.i(TAG, "Verification resubmission succeeded: ${result.data.verificationStatus}")
                    // Refresh profile to reflect pending state
                    refreshDriverProfileUseCase()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            verificationDetails = result.data,
                            resubmissionNotes = "",
                            userFacingMessage = "Verification request resubmitted. Awaiting administrator review."
                        )
                    }
                    onSuccess?.invoke()
                }
                is IshaaraResult.Failure -> {
                    val message = mapErrorToMessage(result.error)
                    IshaaraLogger.w(TAG, "Verification resubmission failed: $message")
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            userFacingError = message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userFacingMessage = null, userFacingError = null) }
    }

    private fun mapErrorToMessage(error: IshaaraError): String {
        return when (error) {
            is IshaaraError.Conflict -> "A verification request is already pending or has already been approved."
            is IshaaraError.Validation -> error.message ?: "Invalid verification submission."
            is IshaaraError.Authentication -> "Authentication required. Please sign in again."
            is IshaaraError.Forbidden -> "Access denied: Driver permissions required."
            is IshaaraError.NotFound -> "Driver profile not found. Please complete onboarding first."
            is IshaaraError.RateLimited -> "Too many requests. Please wait a moment before trying again."
            is IshaaraError.Network -> "Network unavailable. Please check your internet connection."
            is IshaaraError.Server -> "Server encountered an error. Please try again later."
            else -> error.message ?: "Unable to complete request."
        }
    }

    companion object {
        private const val TAG = "DriverVerificationViewModel"
    }
}
