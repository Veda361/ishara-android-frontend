package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Authentication and Session operations.
 */
interface AuthRepository {
    /**
     * Signs in with Google ID token, returning AuthSession.
     */
    suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSession>

    /**
     * Sends OTP to email for sign-in.
     */
    suspend fun sendEmailOtp(email: String): IshaaraResult<Unit>

    /**
     * Signs in using email and OTP.
     */
    suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSession>

    /**
     * Assigns initial role and profile details during onboarding.
     */
    suspend fun completeOnboarding(
        role: UserRole,
        name: String,
        phoneNumber: String
    ): IshaaraResult<User>

    /**
     * Retrieves the current user's profile (/api/v1/users/me).
     */
    suspend fun getCurrentUser(): IshaaraResult<User>

    /**
     * Updates user profile details.
     */
    suspend fun updateProfile(
        name: String? = null,
        phoneNumber: String? = null,
        image: String? = null
    ): IshaaraResult<User>

    /**
     * Observes the active session reactively.
     */
    fun observeSession(): Flow<AuthSession?>

    /**
     * Validates the current session with the backend.
     */
    suspend fun validateSession(): IshaaraResult<AuthSession>

    /**
     * Checks backend health/liveness.
     */
    suspend fun checkHealth(): IshaaraResult<Unit>

    /**
     * Signs out the active user and clears stored credentials.
     */
    suspend fun signOut(): IshaaraResult<Unit>
}
