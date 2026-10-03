package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for User Profile management.
 * Strictly decoupled from Authentication credentials.
 */
interface UserRepository {
    /**
     * Retrieves the authenticated user's application profile from GET /api/v1/users/me.
     */
    suspend fun getCurrentUserProfile(): IshaaraResult<UserProfile>

    /**
     * Updates user profile fields (name, phone number, image) via PATCH /api/v1/users/me.
     */
    suspend fun updateUserProfile(
        name: String? = null,
        phoneNumber: String? = null,
        image: String? = null
    ): IshaaraResult<UserProfile>

    /**
     * Observes the active user profile reactively.
     */
    fun observeUserProfile(): Flow<UserProfile?>

    /**
     * Clears cached profile data (e.g. on logout).
     */
    suspend fun clearCachedProfile()
}
