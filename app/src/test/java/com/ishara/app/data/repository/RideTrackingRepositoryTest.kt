package com.ishara.app.data.repository

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.remote.datasource.RideTrackingRemoteDataSource
import com.ishara.app.data.remote.dto.RideDriverLocationResponseDto
import com.ishara.app.data.remote.dto.RideTrackingUpdatedPayloadDto
import com.ishara.app.data.remote.dto.TrackingCoordinatesDto
import com.ishara.app.data.remote.dto.TrackingResponseDto
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RideTrackingRepositoryTest {

    private val testDispatcher = Dispatchers.Unconfined

    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private lateinit var fakeRemoteDataSource: FakeRideTrackingRemoteDataSource
    private lateinit var fakeSessionDataSource: FakeSessionLocalDataSource
    private lateinit var fakeRealtimeClient: FakeRealtimeClient
    private lateinit var repository: RideTrackingRepositoryImpl

    @Before
    fun setup() {
        fakeRemoteDataSource = FakeRideTrackingRemoteDataSource()
        fakeSessionDataSource = FakeSessionLocalDataSource()
        fakeRealtimeClient = FakeRealtimeClient()

        repository = RideTrackingRepositoryImpl(
            remoteDataSource = fakeRemoteDataSource,
            sessionLocalDataSource = fakeSessionDataSource,
            networkConfig = NetworkConfig(),
            realtimeClient = fakeRealtimeClient,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `getRideTrackingSnapshot succeeds when user is authenticated with USER role`() = runBlocking {
        fakeSessionDataSource.saveSession(
            AuthSession(token = "jwt-user-token", userId = "user-1", role = UserRole.USER)
        )
        fakeRemoteDataSource.stubSnapshot = TrackingResponseDto(
            rideId = "ride-123",
            status = "DRIVER_ARRIVING",
            trackingState = "FRESH",
            updatedAt = "2026-09-25T14:00:00Z"
        )

        val result = repository.getRideTrackingSnapshot("ride-123")
        assertTrue(result is IshaaraResult.Success)
        val snapshot = (result as IshaaraResult.Success).data
        assertEquals("ride-123", snapshot.rideId)
        assertEquals(TrackingRideStatus.DRIVER_ARRIVING, snapshot.status)
    }

    @Test
    fun `getRideTrackingSnapshot rejects when session role is not USER`() = runBlocking {
        fakeSessionDataSource.saveSession(
            AuthSession(token = "jwt-driver-token", userId = "driver-1", role = UserRole.DRIVER_CONDUCTOR)
        )

        val result = repository.getRideTrackingSnapshot("ride-123")
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
    }

    @Test
    fun `getRideTrackingSnapshot fails when unauthenticated`() = runBlocking {
        fakeSessionDataSource.clearSession()

        val result = repository.getRideTrackingSnapshot("ride-123")
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Authentication)
    }

    @Test
    fun `getDriverLocation returns mapped coordinates`() = runBlocking {
        fakeSessionDataSource.saveSession(
            AuthSession(token = "jwt-user-token", userId = "user-1", role = UserRole.USER)
        )
        fakeRemoteDataSource.stubDriverLocation = RideDriverLocationResponseDto(
            rideId = "ride-123",
            driverId = "driver-456",
            location = TrackingCoordinatesDto(18.5204, 73.8567),
            status = "FRESH"
        )

        val result = repository.getDriverLocation("ride-123")
        assertTrue(result is IshaaraResult.Success)
        val loc = (result as IshaaraResult.Success).data
        assertEquals(18.5204, loc.coordinates.latitude, 0.0001)
    }

    @Test
    fun `subscribeToRideTracking connects and sends RIDE_TRACKING_SUBSCRIBE frame`() = runBlocking {
        fakeSessionDataSource.saveSession(
            AuthSession(token = "jwt-user-token", userId = "user-1", role = UserRole.USER)
        )

        val result = repository.subscribeToRideTracking("ride-999")
        assertTrue(result is IshaaraResult.Success)
        assertTrue(fakeRealtimeClient.lastSentMessage.contains("RIDE_TRACKING_SUBSCRIBE"))
        assertTrue(fakeRealtimeClient.lastSentMessage.contains("ride-999"))
    }

    @Test
    fun `unsubscribeFromRideTracking sends RIDE_TRACKING_UNSUBSCRIBE frame`() = runBlocking {
        val result = repository.unsubscribeFromRideTracking("ride-999")
        assertTrue(result is IshaaraResult.Success)
        assertTrue(fakeRealtimeClient.lastSentMessage.contains("RIDE_TRACKING_UNSUBSCRIBE"))
        assertTrue(fakeRealtimeClient.lastSentMessage.contains("ride-999"))
    }

    @Test
    fun `observeRideTrackingEvents filters events strictly for specified rideId`() = runBlocking {
        val eventRide999 = RealtimeEvent.RideTrackingUpdated(
            RideTrackingUpdatedPayloadDto(rideId = "ride-999")
        )
        val eventRideOther = RealtimeEvent.RideTrackingUpdated(
            RideTrackingUpdatedPayloadDto(rideId = "ride-other")
        )

        val flow = repository.observeRideTrackingEvents("ride-999")
        fakeRealtimeClient.eventsFlow.emit(eventRideOther)
        fakeRealtimeClient.eventsFlow.emit(eventRide999)

        val received = flow.first()
        assertTrue(received is RealtimeEvent.RideTrackingUpdated)
        assertEquals("ride-999", (received as RealtimeEvent.RideTrackingUpdated).update.rideId)
    }

    private class FakeRideTrackingRemoteDataSource : RideTrackingRemoteDataSource {
        var stubSnapshot: TrackingResponseDto? = null
        var stubDriverLocation: RideDriverLocationResponseDto? = null

        override suspend fun getRideTracking(rideId: String, token: String): IshaaraResult<TrackingResponseDto> {
            return stubSnapshot?.let { IshaaraResult.success(it) }
                ?: IshaaraResult.failure(IshaaraError.NotFound(message = "Ride not found"))
        }

        override suspend fun getDriverLocation(rideId: String, token: String): IshaaraResult<RideDriverLocationResponseDto> {
            return stubDriverLocation?.let { IshaaraResult.success(it) }
                ?: IshaaraResult.failure(IshaaraError.NotFound(message = "Location not found"))
        }
    }

    private class FakeSessionLocalDataSource : SessionLocalDataSource {
        private var session: AuthSession? = null

        override suspend fun saveSession(session: AuthSession) {
            this.session = session
        }

        override suspend fun getSession(): AuthSession? = session

        override suspend fun clearSession() {
            session = null
        }

        override fun observeSession(): Flow<AuthSession?> = flowOf(session)
    }

    private class FakeRealtimeClient : RealtimeClient {
        var isConnected = false
        var lastSentMessage = ""
        val eventsFlow = MutableSharedFlow<RealtimeEvent>(replay = 1)
        val connectionFlow = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)

        override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> {
            isConnected = true
            return IshaaraResult.success(Unit)
        }

        override suspend fun disconnect() {
            isConnected = false
        }

        override suspend fun send(message: String): IshaaraResult<Unit> {
            lastSentMessage = message
            return IshaaraResult.success(Unit)
        }

        override fun observeConnectionState(): Flow<RealtimeConnectionState> = connectionFlow

        override fun observeEvents(): Flow<RealtimeEvent> = eventsFlow
    }
}
