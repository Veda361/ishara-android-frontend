package com.ishara.app.domain.usecase

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.repository.RideTrackingRepository

/**
 * Use case to retrieve the authoritative live ride tracking snapshot for a ride.
 */
class GetRideTrackingUseCase(
    private val repository: RideTrackingRepository
) {
    suspend operator fun invoke(
        rideId: String,
        pickupAddress: String? = null,
        destinationAddress: String? = null,
        pickupCoords: LocationCoordinates? = null,
        destinationCoords: LocationCoordinates? = null
    ): IshaaraResult<RideTrackingSnapshot> {
        return repository.getRideTrackingSnapshot(
            rideId = rideId,
            pickupAddress = pickupAddress,
            destinationAddress = destinationAddress,
            pickupCoords = pickupCoords,
            destinationCoords = destinationCoords
        )
    }
}
