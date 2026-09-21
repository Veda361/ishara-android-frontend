package com.ishara.app.domain.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Trip

/**
 * Domain repository contract for Trips and Route Discovery.
 */
interface TripRepository {
    /**
     * Finds active matching trips using origin and destination coordinates.
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
