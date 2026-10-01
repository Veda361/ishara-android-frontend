package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.repository.DriverRepository

class GetDriverProfileUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverProfile> {
        return driverRepository.getDriverProfile()
    }
}
