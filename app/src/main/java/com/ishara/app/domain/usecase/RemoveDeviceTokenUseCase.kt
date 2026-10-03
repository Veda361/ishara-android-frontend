package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.DeviceTokenRepository

class RemoveDeviceTokenUseCase(
    private val repository: DeviceTokenRepository
) {
    suspend operator fun invoke(token: String): IshaaraResult<Boolean> {
        return repository.removeDeviceToken(token)
    }
}
