package com.ishara.app.domain.repository

import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingDriverLocation
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository abstraction for Live Passenger Ride Tracking.
 * Bridges authoritative REST snapshots and live WebSocket stream updates.
 */
interface RideTrackingRepository {

    /**
     * Fetches authoritative initial tracking snapshot via GET /api/v1/rides/:rideId/tracking.
     */
    suspend fun getRideTrackingSnapshot(
        rideId: String,
        pickupAddress: String? = null,
        destinationAddress: String? = null,
        pickupCoords: com.ishara.app.core.location.LocationCoordinates? = null,
        destinationCoords: com.ishara.app.core.location.LocationCoordinates? = null
    ): IshaaraResult<RideTrackingSnapshot>

    /**
     * Secondary fallback for driver location via GET /api/v1/rides/:rideId/driver-location.
     */
    suspend fun getDriverLocation(rideId: String): IshaaraResult<TrackingDriverLocation>

    /**
     * Initiates WebSocket subscription for live tracking of a specific ride.
     */
    suspend fun subscribeToRideTracking(rideId: String): IshaaraResult<Unit>

    /**
     * Unsubscribes from live tracking of a specific ride.
     */
    suspend fun unsubscribeFromRideTracking(rideId: String): IshaaraResult<Unit>

    /**
     * Streams incoming typed realtime events (snapshots, updates, terminal notices).
     */
    fun observeRideTrackingEvents(rideId: String): Flow<RealtimeEvent>

    /**
     * Observes underlying WebSocket connection states (Connecting, Connected, Disconnected, Failed).
     */
    fun observeConnectionState(): Flow<RealtimeConnectionState>
}
