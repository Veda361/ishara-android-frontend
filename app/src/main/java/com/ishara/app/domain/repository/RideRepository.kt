package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.model.RideRequest

/**
 * Domain repository contract for Passenger Ride lifecycle.
 */
interface RideRepository {
    /**
     * Submits an authoritative ride request for an active trip.
     */
    suspend fun submitRideRequest(
        input: com.ishara.app.domain.model.RideRequestInput,
        idempotencyKey: String? = null
    ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult>

    suspend fun createRideRequest(
        tripId: String,
        pickupAddress: String,
        dropoffAddress: String,
        seatsRequested: Int
    ): IshaaraResult<RideRequest>

    suspend fun getActiveRide(rideId: String): IshaaraResult<Ride>

    /**
     * Checks if the authenticated passenger currently has an in-flight ride.
     * Returns null if no ride is currently active.
     */
    suspend fun getActivePassengerRide(): IshaaraResult<Ride?>

    suspend fun getRideRequest(
        requestId: String
    ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult> =
        IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.NotFound("Not implemented"))

    suspend fun cancelRideRequest(
        requestId: String,
        reason: String? = null
    ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult> =
        IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.NotFound("Not implemented"))

    suspend fun getUserRideRequests(
        status: com.ishara.app.domain.model.RideRequestStatus? = null,
        tripId: String? = null
    ): IshaaraResult<List<com.ishara.app.domain.model.RideRequestResult>> =
        IshaaraResult.success(emptyList())

    suspend fun cancelRide(rideId: String, reason: String?): IshaaraResult<Unit>
}
