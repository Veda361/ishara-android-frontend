package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.TrackingDriverLocation
import com.ishara.app.domain.repository.RideTrackingRepository

/**
 * Use case to retrieve the driver's raw location fix via GET /api/v1/rides/:rideId/driver-location.
 */
class GetDriverLocationUseCase(
    private val repository: RideTrackingRepository
) {
    suspend operator fun invoke(rideId: String): IshaaraResult<TrackingDriverLocation> {
        return repository.getDriverLocation(rideId)
    }
}
