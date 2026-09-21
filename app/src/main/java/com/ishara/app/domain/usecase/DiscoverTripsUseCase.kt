package com.ishara.app.domain.usecase

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.repository.TripRepository

/**
 * Use case for discovering running trips matching student/passenger origin and destination.
 */
class DiscoverTripsUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(
        pickup: GeoJsonCoordinate,
        destination: GeoJsonCoordinate,
        passengerCount: Int = 1
    ): IshaaraResult<List<Trip>> {
        if (passengerCount < 1) {
            return IshaaraResult.failure(
                IshaaraError.Validation("passengerCount", "Passenger count must be at least 1.")
            )
        }
        return tripRepository.discoverTrips(pickup, destination, passengerCount)
    }
}
