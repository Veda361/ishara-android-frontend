package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.repository.DriverRideRequestRepository

/**
 * Use case managing verified passenger boarding and operational ride lifecycle transitions:
 * CREATED -> DRIVER_ARRIVING (markArrived)
 * DRIVER_ARRIVING -> PICKED_UP (markBoarded)
 * PICKED_UP -> IN_PROGRESS (startRide)
 * IN_PROGRESS -> COMPLETED (completeRide)
 * CREATED/DRIVER_ARRIVING -> CANCELLED (cancelRide)
 */
class ManagePassengerBoardingUseCase(
    private val repository: DriverRideRequestRepository
) {
    suspend fun markArrived(rideId: String): IshaaraResult<DriverPassengerRide> {
        if (rideId.isBlank()) return IshaaraResult.failure(IshaaraError.Validation(message = "Ride ID cannot be blank"))
        return repository.markDriverArrived(rideId)
    }

    suspend fun markBoarded(rideId: String): IshaaraResult<DriverPassengerRide> {
        if (rideId.isBlank()) return IshaaraResult.failure(IshaaraError.Validation(message = "Ride ID cannot be blank"))
        return repository.markPassengerBoarded(rideId)
    }

    suspend fun startRide(rideId: String): IshaaraResult<DriverPassengerRide> {
        if (rideId.isBlank()) return IshaaraResult.failure(IshaaraError.Validation(message = "Ride ID cannot be blank"))
        return repository.startRide(rideId)
    }

    suspend fun completeRide(rideId: String): IshaaraResult<DriverPassengerRide> {
        if (rideId.isBlank()) return IshaaraResult.failure(IshaaraError.Validation(message = "Ride ID cannot be blank"))
        return repository.completeRide(rideId)
    }

    suspend fun cancelRide(rideId: String, reason: String): IshaaraResult<DriverPassengerRide> {
        if (rideId.isBlank()) return IshaaraResult.failure(IshaaraError.Validation(message = "Ride ID cannot be blank"))
        if (reason.isBlank()) return IshaaraResult.failure(IshaaraError.Validation(message = "Cancellation reason is required"))
        return repository.cancelRide(rideId, reason.trim())
    }

    suspend fun getDriverRides(status: String? = null): IshaaraResult<List<DriverPassengerRide>> {
        return repository.getDriverRides(status)
    }
}
