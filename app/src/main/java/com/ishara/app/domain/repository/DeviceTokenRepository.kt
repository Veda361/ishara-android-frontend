package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DeviceTokenRegistration

/**
 * Repository contract for managing device push tokens with the backend.
 * Endpoints:
 * - POST   /api/v1/devices/push-token
 * - DELETE /api/v1/devices/push-token
 */
interface DeviceTokenRepository {

    suspend fun registerDeviceToken(
        token: String,
        platform: String = "android",
        deviceId: String? = null,
        appVersion: String? = null
    ): IshaaraResult<DeviceTokenRegistration>

    suspend fun removeDeviceToken(
        token: String
    ): IshaaraResult<Boolean>

    suspend fun getCachedToken(): String?

    suspend fun saveCachedToken(token: String)

    suspend fun clearCachedToken()
}
