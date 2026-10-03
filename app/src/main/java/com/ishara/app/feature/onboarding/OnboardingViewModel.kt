package com.ishara.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.GetOnboardingStateUseCase
import com.ishara.app.domain.usecase.SubmitOnboardingUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing role-selection and onboarding state.
 * Strictly decoupled from AuthRepository.
 */
class OnboardingViewModel(
    private val getOnboardingStateUseCase: GetOnboardingStateUseCase,
    private val submitOnboardingUseCase: SubmitOnboardingUseCase,
    private val navigationManager: NavigationManager,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val effectiveScope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        observeOnboardingState()
        loadInitialState()
    }

    private fun observeOnboardingState() {
        effectiveScope.launch {
            getOnboardingStateUseCase.observe().collect { state ->
                _uiState.update { current ->
                    when (state) {
                        is OnboardingState.Completed -> {
                            current.copy(
                                onboardingState = state,
                                isSubmitting = false,
                                errorMessage = null,
                                validationError = null
                            )
                        }
                        is OnboardingState.Error -> {
                            current.copy(
                                onboardingState = state,
                                isSubmitting = false,
                                selectedRole = state.retryRole ?: current.selectedRole,
                                errorMessage = mapErrorToUserMessage(state.error)
                            )
                        }
                        is OnboardingState.Submitting -> {
                            current.copy(
                                onboardingState = state,
                                isSubmitting = true
                            )
                        }
                        is OnboardingState.Required -> {
                            current.copy(
                                onboardingState = state,
                                isSubmitting = false
                            )
                        }
                        is OnboardingState.Unknown -> {
                            current.copy(onboardingState = state)
                        }
                    }
                }
            }
        }
    }

    private fun loadInitialState() {
        effectiveScope.launch {
            IshaaraLogger.d(TAG, "Evaluating initial onboarding state...")
            getOnboardingStateUseCase()
        }
    }

    /**
     * Updates selected role, clearing prior validation messages.
     */
    fun selectRole(role: UserRole) {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                selectedRole = role,
                validationError = null,
                errorMessage = null
            )
        }
    }

    /**
     * Submits role selection with client validation and duplicate tap protection.
     */
    fun submitOnboarding() {
        val current = _uiState.value

        // Guard against duplicate submission while in flight
        if (current.isSubmitting) {
            IshaaraLogger.d(TAG, "Onboarding submission already in flight; duplicate ignored.")
            return
        }

        // Domain validation: role must be explicitly selected
        val role = current.selectedRole
        if (role == null) {
            _uiState.update {
                it.copy(validationError = "Choose how you'll use Ishaara.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isSubmitting = true,
                errorMessage = null,
                validationError = null
            )
        }

        effectiveScope.launch {
            submitOnboardingUseCase(role)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(isSubmitting = false)
                    }
                    navigationManager.navigateForRole(role)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorToUserMessage(error)
                        )
                    }
                }
        }
    }

    fun retry() {
        submitOnboarding()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, validationError = null) }
    }

    private fun mapErrorToUserMessage(error: IshaaraError): String {
        return when (error) {
            is IshaaraError.Network -> "You're offline. Check your connection and try again."
            is IshaaraError.Validation -> error.message.ifBlank { "Please check this information." }
            is IshaaraError.Server -> "Couldn't save your details. Please try again shortly."
            else -> error.message.ifBlank { "Couldn't save your details. Please try again." }
        }
    }

    companion object {
        private const val TAG = "OnboardingViewModel"
    }
}
