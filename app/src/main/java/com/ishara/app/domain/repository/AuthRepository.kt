package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract for Authentication and Session operations.
 * Implements centralized session lifecycle, restoration, state machine observation,
 * and secure credential handling.
 */
interface AuthRepository {
    /**
     * Signs in with Google ID token, returning AuthSession.
     */
    suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSession>

    /**
     * Requests a 6-digit email verification OTP for sign-in.
     */
    suspend fun sendEmailOtp(email: String): IshaaraResult<Unit>

    /**
     * Verifies email OTP code and establishes an authenticated session.
     */
    suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSession>

    /**
     * Restores stored session on app launch/restart.
     * Validates local validity and reconciles with backend if online.
     * Offline status preserves a non-expired local session.
     */
    suspend fun restoreSession(): IshaaraResult<AuthSession?>

    /**
     * Retrieves the current in-memory/persisted session if any.
     */
    suspend fun getCurrentSession(): IshaaraResult<AuthSession?>

    /**
     * Observes the application-wide authentication state machine reactively.
     */
    fun observeAuthState(): StateFlow<AuthState>

    /**
     * Assigns initial role during onboarding (USER or DRIVER_CONDUCTOR).
     */
    suspend fun completeOnboarding(role: UserRole): IshaaraResult<User>

    /**
     * Retrieves the current user's profile (/api/v1/users/me).
     */
    suspend fun getCurrentUser(): IshaaraResult<User>

    /**
     * Observes the active session reactively.
     */
    fun observeSession(): Flow<AuthSession?>

    /**
     * Invalidates the active session due to 401 or expiration.
     */
    suspend fun invalidateSession(reason: String = "Your session has expired. Please sign in again.")

    /**
     * Signs out the active user and clears stored credentials.
     * Idempotent: repeated calls will not throw errors or loop.
     */
    suspend fun signOut(): IshaaraResult<Unit>
}
