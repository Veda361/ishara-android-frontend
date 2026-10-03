package com.ishara.app.domain.model

import com.ishara.app.core.result.IshaaraError

/**
 * Controlled domain state machine for application onboarding.
 */
sealed interface OnboardingState {
    /** Initial state while evaluating user profile. */
    object Unknown : OnboardingState

    /** Authenticated user has not completed onboarding; role selection required. */
    object Required : OnboardingState

    /** In-flight submission to POST /api/v1/users/me/onboarding to prevent duplicate requests. */
    data class Submitting(val selectedRole: UserRole) : OnboardingState

    /** Onboarding successfully completed on backend; target destination resolved. */
    data class Completed(
        val userProfile: UserProfile,
        val destination: ApplicationDestination
    ) : OnboardingState

    /** Submission failed; retains selected role to preserve form input. */
    data class Error(
        val error: IshaaraError,
        val retryRole: UserRole? = null
    ) : OnboardingState
}
