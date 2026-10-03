package com.ishara.app.domain.model

import com.ishara.app.core.result.IshaaraError

/**
 * Application-wide domain authentication state machine.
 * Observable reactively across the entire application without scattering auth checks.
 */
sealed interface AuthState {
    /**
     * Initial startup state while checking persistent storage for an active session.
     * Prevents navigation flicker before authentication status is determined.
     */
    object Unknown : AuthState

    /**
     * Valid active session restored or created.
     * @param session Active session credentials and role
     * @param user Associated user profile if fetched
     */
    data class Authenticated(
        val session: AuthSession,
        val user: User? = null
    ) : AuthState

    /**
     * No stored session, or user explicitly logged out.
     */
    object Unauthenticated : AuthState

    /**
     * Session expired or invalidated by server (e.g. 401 Unauthorized).
     * @param message User-facing explanation
     */
    data class SessionExpired(
        val message: String = "Your session has expired. Please sign in again."
    ) : AuthState

    /**
     * Unrecoverable authentication error occurred during sign-in or session restoration.
     */
    data class Error(val error: IshaaraError) : AuthState
}
