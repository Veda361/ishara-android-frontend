package com.ishara.app.feature.onboarding

import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserRole

/**
 * UI State for the Onboarding and Role Selection feature.
 */
data class OnboardingUiState(
    val selectedRole: UserRole? = null,
    val isSubmitting: Boolean = false,
    val validationError: String? = null,
    val errorMessage: String? = null,
    val onboardingState: OnboardingState = OnboardingState.Unknown
) {
    val canSubmit: Boolean
        get() = !isSubmitting && selectedRole != null

    val isCompleted: Boolean
        get() = onboardingState is OnboardingState.Completed
}
