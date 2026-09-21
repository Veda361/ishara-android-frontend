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
     * Signs out the active user and clears stored credentials.
     */
    suspend fun signOut(): IshaaraResult<Unit>
}
