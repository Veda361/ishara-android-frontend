package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.repository.RideRepository

/**
 * Use case to retrieve the authenticated student's active ride, if one exists.
 * Returns null safely when no ride is in progress.
 */
class GetActiveRideUseCase(
    private val rideRepository: RideRepository
) {
    suspend operator fun invoke(): IshaaraResult<Ride?> {
        return rideRepository.getActivePassengerRide()
    }
}
