package com.ishara.app.domain.model

/**
 * Domain model representing an authenticated user's application profile.
 * Sourced directly from GET /api/v1/users/me.
 *
 * Distinct from AuthSession (which represents bearer tokens and auth lifecycle).
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: UserRole?,
    val profileImageUrl: String? = null,
    val isOnboarded: Boolean = false,
    val onboardingCompleted: Boolean = isOnboarded
) {
    /**
     * Determines whether the user has a valid, known role and marked setup complete.
     */
    val hasCompletedOnboarding: Boolean
        get() = (isOnboarded || onboardingCompleted) && role != null
}
