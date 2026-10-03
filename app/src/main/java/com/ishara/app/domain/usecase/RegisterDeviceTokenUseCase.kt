package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DeviceTokenRegistration
import com.ishara.app.domain.repository.DeviceTokenRepository

class RegisterDeviceTokenUseCase(
    private val repository: DeviceTokenRepository
) {
    suspend operator fun invoke(
        token: String,
        platform: String = "ANDROID",
        deviceId: String? = null,
        appVersion: String? = null
    ): IshaaraResult<DeviceTokenRegistration> {
        return repository.registerDeviceToken(token, platform, deviceId, appVersion)
    }
}
