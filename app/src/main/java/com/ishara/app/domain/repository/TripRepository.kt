package com.ishara.app.domain.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Trip

/**
 * Domain repository contract for Trips and Route Discovery.
 */
interface TripRepository {
    /**
     * Executes trip discovery using the strict verified DiscoveryQuery.
     */
    suspend fun discoverTrips(query: com.ishara.app.domain.model.DiscoveryQuery): IshaaraResult<com.ishara.app.domain.model.DiscoveryResult>

    /**
     * Retrieves sanitized public passenger trip details by its identifier.
     * GET /api/v1/trips/:tripId
     */
    suspend fun getPassengerTripDetails(tripId: String): IshaaraResult<com.ishara.app.domain.model.PassengerTripDetails>

    /**
     * Legacy helper for finding trips.
     */
    suspend fun discoverTrips(
        pickup: GeoJsonCoordinate,
        destination: GeoJsonCoordinate,
        passengerCount: Int = 1
    ): IshaaraResult<List<Trip>>

    /**
     * Retrieves details for a specific trip by its identifier.
     */
    suspend fun getTripById(tripId: String): IshaaraResult<Trip>

    /**
     * Driver operation: Starts an active trip for route tracking.
     */
    suspend fun startTrip(tripId: String): IshaaraResult<Trip>

    /**
     * Driver operation: Completes an active trip.
     */
    suspend fun completeTrip(tripId: String): IshaaraResult<Trip>
}
