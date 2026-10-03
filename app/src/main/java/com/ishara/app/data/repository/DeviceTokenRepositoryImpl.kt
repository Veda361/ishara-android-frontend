package com.ishara.app.data.repository

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.DeviceTokenStore
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.DeviceTokenMapper
import com.ishara.app.data.remote.datasource.DeviceTokenRemoteDataSource
import com.ishara.app.data.remote.dto.RegisterPushTokenRequestDto
import com.ishara.app.data.remote.dto.RemovePushTokenRequestDto
import com.ishara.app.domain.model.DeviceTokenRegistration
import com.ishara.app.domain.repository.DeviceTokenRepository
import kotlinx.coroutines.withContext

class DeviceTokenRepositoryImpl(
    private val remoteDataSource: DeviceTokenRemoteDataSource,
    private val tokenStore: DeviceTokenStore,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider
) : DeviceTokenRepository {

    override suspend fun registerDeviceToken(
        token: String,
        platform: String,
        deviceId: String?,
        appVersion: String?
    ): IshaaraResult<DeviceTokenRegistration> = withContext(dispatchers.io) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Validation(field = "token", message = "Device push token cannot be blank.")
            )
        }

        val session = sessionStore.getSession()
        val authToken = session?.token
        if (authToken.isNullOrBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Authentication(message = "Authentication required to register push token.")
            )
        }

        val request = RegisterPushTokenRequestDto(
            token = trimmedToken,
            platform = platform,
            deviceId = deviceId,
            appVersion = appVersion
        )

        remoteDataSource.registerPushToken(request, authToken).map { dto ->
            tokenStore.saveToken(trimmedToken)
            DeviceTokenMapper.toDomain(dto)
        }
    }

    override suspend fun removeDeviceToken(
        token: String
    ): IshaaraResult<Boolean> = withContext(dispatchers.io) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Validation(field = "token", message = "Device push token cannot be blank.")
            )
        }

        val session = sessionStore.getSession()
        val authToken = session?.token
        if (authToken.isNullOrBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Authentication(message = "Authentication required to remove push token.")
            )
        }

        val request = RemovePushTokenRequestDto(token = trimmedToken)
        remoteDataSource.removePushToken(request, authToken).map { dto ->
            if (tokenStore.getToken() == trimmedToken) {
                tokenStore.clearToken()
            }
            dto.removed
        }
    }

    override suspend fun getCachedToken(): String? = withContext(dispatchers.io) {
        tokenStore.getToken()
    }

    override suspend fun saveCachedToken(token: String) = withContext(dispatchers.io) {
        tokenStore.saveToken(token)
    }

    override suspend fun clearCachedToken() = withContext(dispatchers.io) {
        tokenStore.clearToken()
    }
}
