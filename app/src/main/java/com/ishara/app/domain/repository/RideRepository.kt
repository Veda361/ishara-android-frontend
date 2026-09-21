package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.model.RideRequest

/**
 * Domain repository contract for Passenger Ride lifecycle.
 */
interface RideRepository {
    suspend fun createRideRequest(
        tripId: String,
        pickupAddress: String,
        dropoffAddress: String,
        seatsRequested: Int
    ): IshaaraResult<RideRequest>

    suspend fun getActiveRide(rideId: String): IshaaraResult<Ride>

    suspend fun cancelRide(rideId: String, reason: String?): IshaaraResult<Unit>
}
