package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CreateTripParams
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract for Driver-side Trip and Dispatch operations (Phase A08).
 *
 * Responsibilities:
 * - Query driver-assigned and driver-created trips (GET /api/v1/drivers/me/trips).
 * - Query authoritative trip details (GET /api/v1/trips/:tripId).
 * - Self-service driver trip creation (POST /api/v1/trips).
 * - Atomic lifecycle transitions (start, complete, cancel).
 * - Role-enforced boundaries (DRIVER_CONDUCTOR only).
 * - Authoritative state caching and flow observation.
 * - Anti-leakage state clearance on logout or account switch.
 */
interface DriverTripRepository {
    /**
     * Retrieves paginated trips for the authenticated driver with optional status filter.
     */
    suspend fun getDriverTrips(
        status: TripStatus? = null,
        page: Int = 1,
        limit: Int = 20,
        forceRefresh: Boolean = false
    ): IshaaraResult<List<Trip>>

    /**
     * Force-refreshes the driver's trips from the backend.
     */
    suspend fun refreshDriverTrips(status: TripStatus? = null): IshaaraResult<List<Trip>>

    /**
     * Retrieves detailed information for a specific trip.
     */
    suspend fun getTripDetails(
        tripId: String,
        forceRefresh: Boolean = false
    ): IshaaraResult<Trip>

    /**
     * Creates a new trip owned by the authenticated driver.
     */
    suspend fun createTrip(params: CreateTripParams): IshaaraResult<Trip>

    /**
     * Starts an unstarted trip (CREATED/SCHEDULED/ASSIGNED/READY -> ACTIVE).
     */
    suspend fun startTrip(tripId: String): IshaaraResult<Trip>

    /**
     * Completes an active trip (ACTIVE -> COMPLETED).
     */
    suspend fun completeTrip(tripId: String): IshaaraResult<Trip>

    /**
     * Cancels a trip with an optional reason.
     */
    suspend fun cancelTrip(tripId: String, reason: String? = null): IshaaraResult<Trip>

    /**
     * Observes the in-memory list of driver trips for UI reactivity.
     */
    fun observeDriverTrips(): StateFlow<List<Trip>>

    /**
     * Observes the currently active or in-progress trip, if one exists.
     */
    fun observeActiveTrip(): StateFlow<Trip?>

    /**
     * Clears all in-memory trip caches immediately on sign out or account switch.
     */
    fun clearTripState()
}
