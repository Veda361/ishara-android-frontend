package com.ishara.app.domain.usecase

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.DiscoveryResult
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.repository.TripRepository

/**
 * Use case for discovering running trips matching student/passenger origin and destination.
 * Validates coordinate bounds and parameters before delegating to the repository.
 */
class DiscoverTripsUseCase(
    private val tripRepository: TripRepository
) {
    /**
     * Primary Phase 06 entry point: Executes discovery using the strict [DiscoveryQuery].
     */
    suspend fun execute(query: DiscoveryQuery): IshaaraResult<DiscoveryResult> {
        // Validate coordinates
        if (query.originLatitude !in -90.0..90.0 || query.originLongitude !in -180.0..180.0) {
            return IshaaraResult.failure(
                IshaaraError.Validation("origin", "Invalid origin coordinates.")
            )
        }
        if (query.destinationLatitude !in -90.0..90.0 || query.destinationLongitude !in -180.0..180.0) {
            return IshaaraResult.failure(
                IshaaraError.Validation("destination", "Invalid destination coordinates.")
            )
        }

        // Validate origin != destination distance
        if (query.originLatitude == query.destinationLatitude && query.originLongitude == query.destinationLongitude) {
            return IshaaraResult.failure(
                IshaaraError.Validation("destination", "Origin and destination cannot be identical.")
            )
        }

        return tripRepository.discoverTrips(query)
    }

    /**
     * Legacy invocation operator.
     */
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
