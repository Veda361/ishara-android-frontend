package com.ishara.app.domain.usecase

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.DriverRepository

/**
 * Use case for high-frequency driver GPS telemetry updates.
 */
class UpdateDriverLocationUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(location: LocationCoordinates): IshaaraResult<Unit> {
        return driverRepository.updateLocation(location)
    }
}
