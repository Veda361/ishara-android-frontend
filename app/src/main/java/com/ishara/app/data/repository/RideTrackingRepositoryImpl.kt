package com.ishara.app.data.repository

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.RideTrackingMapper
import com.ishara.app.data.remote.datasource.RideTrackingRemoteDataSource
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingDriverLocation
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.RideTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext

class RideTrackingRepositoryImpl(
    private val remoteDataSource: RideTrackingRemoteDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val networkConfig: NetworkConfig,
    private val realtimeClient: RealtimeClient,
    private val dispatchers: DispatcherProvider
) : RideTrackingRepository {

    private val tag = "RideTrackingRepository"

    override suspend fun getRideTrackingSnapshot(
        rideId: String,
        pickupAddress: String?,
        destinationAddress: String?,
        pickupCoords: LocationCoordinates?,
        destinationCoords: LocationCoordinates?
    ): IshaaraResult<RideTrackingSnapshot> = withContext(dispatchers.io) {
        val session = sessionLocalDataSource.getSession()
            ?: return@withContext IshaaraResult.failure(IshaaraError.Authentication(message = "Unauthorized: Session required"))

        if (session.role != UserRole.USER) {
            return@withContext IshaaraResult.failure(
                IshaaraError.Forbidden("Passenger authorization required for ride tracking")
            )
        }

        when (val result = remoteDataSource.getRideTracking(rideId, session.token)) {
            is IshaaraResult.Success -> {
                val snapshot = RideTrackingMapper.mapTrackingResponse(
                    dto = result.data,
                    pickupAddress = pickupAddress,
                    destinationAddress = destinationAddress,
                    pickupCoords = pickupCoords,
                    destinationCoords = destinationCoords
                )
                IshaaraResult.success(snapshot)
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun getDriverLocation(rideId: String): IshaaraResult<TrackingDriverLocation> =
        withContext(dispatchers.io) {
            val session = sessionLocalDataSource.getSession()
                ?: return@withContext IshaaraResult.failure(IshaaraError.Authentication(message = "Unauthorized: Session required"))

            when (val result = remoteDataSource.getDriverLocation(rideId, session.token)) {
                is IshaaraResult.Success -> {
                    val loc = RideTrackingMapper.mapDriverLocationResponse(result.data)
                    if (loc != null) {
                        IshaaraResult.success(loc)
                    } else {
                        IshaaraResult.failure(IshaaraError.Validation(message = "Driver location coordinates unavailable"))
                    }
                }
                is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
            }
        }

    override suspend fun subscribeToRideTracking(rideId: String): IshaaraResult<Unit> =
        withContext(dispatchers.io) {
            val session = sessionLocalDataSource.getSession()
                ?: return@withContext IshaaraResult.failure(IshaaraError.Authentication(message = "Unauthorized: Session required"))

            val wsEndpoint = "${networkConfig.fullWebSocketBaseUrl}/rides/realtime"
            val connectResult = realtimeClient.connect(wsEndpoint, session.token)
            if (connectResult is IshaaraResult.Failure) {
                IshaaraLogger.w(tag, "WebSocket connect note: ${connectResult.error.message}")
            }

            val subscribeJson = """{"type":"RIDE_TRACKING_SUBSCRIBE","payload":{"rideId":"$rideId"}}"""
            realtimeClient.send(subscribeJson)
        }

    override suspend fun unsubscribeFromRideTracking(rideId: String): IshaaraResult<Unit> =
        withContext(dispatchers.io) {
            val unsubscribeJson = """{"type":"RIDE_TRACKING_UNSUBSCRIBE","payload":{"rideId":"$rideId"}}"""
            realtimeClient.send(unsubscribeJson)
        }

    override fun observeRideTrackingEvents(rideId: String): Flow<RealtimeEvent> {
        return realtimeClient.observeEvents().filter { event ->
            when (event) {
                is RealtimeEvent.LiveTelemetryUpdate -> event.rideId == rideId || event.rideId.isBlank()
                is RealtimeEvent.RideTrackingSubscribed -> event.rideId == rideId
                is RealtimeEvent.TrackingSnapshot -> event.snapshot.rideId == rideId
                is RealtimeEvent.RideTrackingUpdated -> event.update.rideId == rideId
                is RealtimeEvent.RideTrackingEnded -> event.rideId == rideId
                is RealtimeEvent.RideTrackingError -> event.rideId == null || event.rideId == rideId
                is RealtimeEvent.DriverLocationUpdated -> event.rideId == rideId
                is RealtimeEvent.RideDriverArriving -> event.rideId == rideId
                is RealtimeEvent.RidePickedUp -> event.rideId == rideId
                is RealtimeEvent.RideStarted -> event.rideId == rideId
                is RealtimeEvent.RideCompleted -> event.rideId == rideId
                is RealtimeEvent.RideCancelled -> event.rideId == rideId
                else -> false
            }
        }
    }

    override fun observeConnectionState(): Flow<RealtimeConnectionState> {
        return realtimeClient.observeConnectionState()
    }
}
