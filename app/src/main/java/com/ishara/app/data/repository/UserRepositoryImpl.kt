package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.UserMapper
import com.ishara.app.data.remote.datasource.UserRemoteDataSource
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of UserRepository.
 * Handles user profile caching, retrieval, and synchronization with the active session token.
 */
class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val sessionStore: SessionStore
) : UserRepository {

    private val _userProfileFlow = MutableStateFlow<UserProfile?>(null)
    private val mutex = Mutex()

    override fun observeUserProfile(): Flow<UserProfile?> = _userProfileFlow.asStateFlow()

    override suspend fun getCurrentUserProfile(): IshaaraResult<UserProfile> = mutex.withLock {
        val session = sessionStore.getSession()
            ?: return IshaaraResult.failure(
                IshaaraError.Authentication(message = "Authentication session required to fetch user profile.")
            )

        val result = remoteDataSource.getCurrentUserProfile(session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val profile = UserMapper.toDomain(result.data)
                _userProfileFlow.value = profile
                IshaaraLogger.d(TAG, "User profile refreshed: id=${profile.id}, role=${profile.role}")
                IshaaraResult.success(profile)
            }
            is IshaaraResult.Failure -> {
                // If network fails but we have a cached profile, return cached
                val cached = _userProfileFlow.value
                if (cached != null && result.error is IshaaraError.Network) {
                    IshaaraLogger.i(TAG, "Offline: returning cached user profile.")
                    IshaaraResult.success(cached)
                } else {
                    IshaaraResult.failure(result.error)
                }
            }
        }
    }

    override suspend fun updateUserProfile(
        name: String?,
        phoneNumber: String?,
        image: String?
    ): IshaaraResult<UserProfile> = mutex.withLock {
        val session = sessionStore.getSession()
            ?: return IshaaraResult.failure(
                IshaaraError.Authentication(message = "Authentication session required to update profile.")
            )

        val result = remoteDataSource.updateUserProfile(name, phoneNumber, image, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val updatedProfile = UserMapper.toDomain(result.data)
                _userProfileFlow.value = updatedProfile
                IshaaraResult.success(updatedProfile)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun clearCachedProfile() = mutex.withLock {
        _userProfileFlow.value = null
        IshaaraLogger.d(TAG, "User profile cache cleared.")
    }

    companion object {
        private const val TAG = "UserRepository"
    }
}
