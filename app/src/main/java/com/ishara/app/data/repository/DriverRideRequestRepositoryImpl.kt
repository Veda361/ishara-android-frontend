package com.ishara.app.data.repository

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.realtime.OkHttpRealtimeClient
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.DriverRideRequestMapper
import com.ishara.app.data.remote.datasource.DriverRideRequestRemoteDataSource
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.model.DriverRideRequestPage
import com.ishara.app.domain.repository.DriverRideRequestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DriverRideRequestRepositoryImpl(
    private val remoteDataSource: DriverRideRequestRemoteDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val networkConfig: NetworkConfig,
    private val realtimeClient: RealtimeClient = OkHttpRealtimeClient(),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatchers.io)
) : DriverRideRequestRepository {

    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Disconnected)

    init {
        coroutineScope.launch {
            realtimeClient.observeConnectionState().collect { state ->
                _connectionState.value = state
            }
        }
    }

    override suspend fun getDriverRideRequests(
        status: String?,
        tripId: String?,
        page: Int,
        limit: Int,
        cursor: String?
    ): IshaaraResult<DriverRideRequestPage> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.getDriverRideRequests(
            status = status,
            tripId = tripId,
            page = page,
            limit = limit,
            cursor = cursor,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapPage(dto)
        }
    }

    override suspend fun getRideRequestDetails(requestId: String): IshaaraResult<DriverRideRequest> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.getRideRequestDetails(
            requestId = requestId,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapItem(dto)
        }
    }

    override suspend fun acceptRideRequest(requestId: String): IshaaraResult<DriverRideRequest> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.acceptRideRequest(
            requestId = requestId,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapItem(dto)
        }
    }

    override suspend fun rejectRideRequest(
        requestId: String,
        reason: String?
    ): IshaaraResult<DriverRideRequest> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.rejectRideRequest(
            requestId = requestId,
            reason = reason,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapItem(dto)
        }
    }

    override suspend fun getDriverRides(
        status: String?,
        page: Int,
        limit: Int
    ): IshaaraResult<List<DriverPassengerRide>> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.getDriverRides(
            status = status,
            page = page,
            limit = limit,
            token = session.token
        ).map { dtoList ->
            dtoList.map { DriverRideRequestMapper.mapRide(it) }
        }
    }

    override suspend fun markDriverArrived(rideId: String): IshaaraResult<DriverPassengerRide> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.markDriverArrived(
            rideId = rideId,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapRide(dto)
        }
    }

    override suspend fun markPassengerBoarded(rideId: String): IshaaraResult<DriverPassengerRide> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.markPassengerBoarded(
            rideId = rideId,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapRide(dto)
        }
    }

    override suspend fun startRide(rideId: String): IshaaraResult<DriverPassengerRide> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.startRide(
            rideId = rideId,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapRide(dto)
        }
    }

    override suspend fun completeRide(rideId: String): IshaaraResult<DriverPassengerRide> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.completeRide(
            rideId = rideId,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapRide(dto)
        }
    }

    override suspend fun cancelRide(
        rideId: String,
        reason: String
    ): IshaaraResult<DriverPassengerRide> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.cancelRide(
            rideId = rideId,
            reason = reason,
            token = session.token
        ).map { dto ->
            DriverRideRequestMapper.mapRide(dto)
        }
    }

    override fun observeRealtimeEvents(): Flow<RealtimeEvent> {
        return realtimeClient.observeEvents()
    }

    override fun observeRealtimeConnectionState(): StateFlow<RealtimeConnectionState> {
        return _connectionState.asStateFlow()
    }

    override fun connectRealtime() {
        val wsEndpoint = "${networkConfig.fullWebSocketBaseUrl}/ride-requests/realtime"
        coroutineScope.launch {
            val session = sessionLocalDataSource.getSession() ?: return@launch
            realtimeClient.connect(wsEndpoint, session.token)
        }
    }

    override fun disconnectRealtime() {
        coroutineScope.launch {
            realtimeClient.disconnect()
        }
    }
}
