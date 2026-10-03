package com.ishara.app.core.notifications

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.DeviceTokenRepository

class PushTokenManagerImpl(
    private val deviceTokenRepository: DeviceTokenRepository
) : PushTokenManager {

    override suspend fun getDeviceToken(): String? {
        return deviceTokenRepository.getCachedToken()
    }

    override suspend fun registerDeviceToken(token: String): IshaaraResult<Unit> {
        return deviceTokenRepository.registerDeviceToken(token).map { Unit }
    }

    override suspend fun unregisterDeviceToken(token: String): IshaaraResult<Unit> {
        return deviceTokenRepository.removeDeviceToken(token).map { Unit }
    }
}
