package com.ishara.app.domain.model

/**
 * Top-level application destinations resolved after evaluating authentication, profile, and onboarding state.
 */
sealed interface ApplicationDestination {
    /**
     * Commuter workspace for Students / Passengers.
     */
    object StudentHome : ApplicationDestination

    /**
     * Transit operations workspace for Drivers / Conductors.
     */
    object DriverDashboard : ApplicationDestination

    /**
     * Role selection & onboarding flow for authenticated users with incomplete setup.
     */
    object Onboarding : ApplicationDestination

    /**
     * Unauthenticated sign-in flow.
     */
    object Authentication : ApplicationDestination

    /**
     * Fallback recovery destination when user profile contains unrecognized roles or corrupted state.
     */
    data class SafeRecovery(val message: String = "Unable to determine your account role.") : ApplicationDestination
}
