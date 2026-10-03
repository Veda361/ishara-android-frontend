package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.repository.DriverRepository

/**
 * Use case to transition the driver between ONLINE and OFFLINE operational availability.
 */
class SetDriverAvailabilityUseCase(
    private val driverRepository: DriverRepository
) {
    suspend fun goOnline(): IshaaraResult<DriverIdentity> {
        return driverRepository.setOnline()
    }

    suspend fun goOffline(): IshaaraResult<DriverIdentity> {
        return driverRepository.setOffline()
    }
}
