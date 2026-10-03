package com.ishara.app.domain.repository

import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.model.DriverRideRequestPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean domain repository for driver ride requests and operational passenger boarding.
 */
interface DriverRideRequestRepository {

    suspend fun getDriverRideRequests(
        status: String? = null,
        tripId: String? = null,
        page: Int = 1,
        limit: Int = 20,
        cursor: String? = null
    ): IshaaraResult<DriverRideRequestPage>

    suspend fun getRideRequestDetails(requestId: String): IshaaraResult<DriverRideRequest>

    suspend fun acceptRideRequest(requestId: String): IshaaraResult<DriverRideRequest>

    suspend fun rejectRideRequest(requestId: String, reason: String?): IshaaraResult<DriverRideRequest>

    suspend fun getDriverRides(
        status: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): IshaaraResult<List<DriverPassengerRide>>

    suspend fun markDriverArrived(rideId: String): IshaaraResult<DriverPassengerRide>

    suspend fun markPassengerBoarded(rideId: String): IshaaraResult<DriverPassengerRide>

    suspend fun startRide(rideId: String): IshaaraResult<DriverPassengerRide>

    suspend fun completeRide(rideId: String): IshaaraResult<DriverPassengerRide>

    suspend fun cancelRide(rideId: String, reason: String): IshaaraResult<DriverPassengerRide>

    /**
     * Connect to the driver ride requests realtime WebSocket stream.
     */
    fun observeRealtimeEvents(): Flow<RealtimeEvent>

    fun observeRealtimeConnectionState(): StateFlow<RealtimeConnectionState>

    fun connectRealtime()

    fun disconnectRealtime()
}
