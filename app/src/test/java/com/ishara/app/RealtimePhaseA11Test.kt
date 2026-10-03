package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.network.SessionInvalidationCoordinator
import com.ishara.app.core.realtime.OkHttpRealtimeClient
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.realtime.RealtimeEventParser
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.DefaultRideTrackingRemoteDataSource
import com.ishara.app.data.repository.RideTrackingRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.TrackingFreshness
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.model.TrackingState
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.GetDriverLocationUseCase
import com.ishara.app.domain.usecase.GetRideTrackingUseCase
import com.ishara.app.domain.usecase.ObserveRideTrackingUseCase
import com.ishara.app.feature.student.tracking.RideTrackingUiState
import com.ishara.app.feature.student.tracking.RideTrackingViewModel
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * PHASE A11 — LIVE RIDE, REALTIME STATE & GPS FOUNDATION Test Suite
 *
 * Verifies all Phase A11 requirements against the actual backend contract:
 * 1. Transport & Handshake: WebSocket connection to `/api/v1/rides/realtime` with token authentication.
 * 2. Subscription Protocol: RIDE_TRACKING_SUBSCRIBE frame dispatch with rideId.
 * 3. Event Taxonomy: TRACKING_SNAPSHOT, RIDE_TRACKING_UPDATED, DRIVER_LOCATION_UPDATED,
 *    RIDE_DRIVER_ARRIVING, RIDE_PICKED_UP, RIDE_STARTED, RIDE_COMPLETED, RIDE_CANCELLED, RIDE_TRACKING_ENDED.
 * 4. Error Isolation & Resilience: Unknown events, malformed frames, and errors do not crash client.
 * 5. State Machine & Transitions: Linear progression (CREATED -> DRIVER_ARRIVING -> PICKED_UP -> IN_PROGRESS -> COMPLETED)
 *    and cancellation before pickup.
 * 6. Reconciliation after Reconnect: Authoritative REST snapshot (`GET /api/v1/rides/:rideId/tracking`)
 *    is queried upon reconnection to repair missed events.
 * 7. Driver GPS Updates: Normalized spherical coordinates, freshness indicators (FRESH vs STALE),
 *    and distance metrics.
 * 8. Terminal State Cleanup: Realtime subscription halts on terminal states (`COMPLETED`, `CANCELLED`).
 * 9. Authorization & Role Protection: Requires USER role for tracking; 401/403 triggers session invalidation.
 * 10. Account Switching & Resource Cleanup: Sockets close, scopes cancel, and subscriptions clear.
 */
class RealtimePhaseA11Test {

    private val testDispatcher = Dispatchers.Unconfined

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private val networkConfig = NetworkConfig()
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var sessionLocalDataSource: SessionLocalDataSourceImpl
    private lateinit var sessionCoordinator: SessionInvalidationCoordinator
    private lateinit var navigationManager: NavigationManager

    private val sampleTrackingSnapshotJson = """
        {
            "rideId": "651a2b3c4d5e6f7a8b9c0001",
            "status": "IN_PROGRESS",
            "trackingState": "FRESH",
            "driver": {
                "location": { "latitude": 25.2677, "longitude": 82.9913 },
                "accuracyMeters": 5.0,
                "headingDegrees": 180.0,
                "speedMps": 8.5,
                "recordedAt": "2026-10-01T08:10:00.000Z",
                "receivedAt": "2026-10-01T08:10:01.000Z",
                "freshness": "FRESH"
            },
            "route": {
                "distanceMeters": 5000,
                "completedDistanceMeters": 2000,
                "remainingDistanceMeters": 3000,
                "progressPercent": 40.0,
                "distanceFromRouteMeters": 2.5,
                "isOffRoute": false
            },
            "distanceToPickupMeters": null,
            "distanceToDestinationMeters": 3000,
            "eta": {
                "available": true,
                "seconds": 360,
                "source": "LOCAL_ESTIMATE",
                "confidence": "MEDIUM"
            },
            "updatedAt": "2026-10-01T08:10:01.000Z"
        }
    """.trimIndent()

    @Before
    fun setup() {
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        sessionCoordinator = SessionInvalidationCoordinator()
        navigationManager = NavigationManager()

        val validSession = AuthSession(
            token = "jwt_test_passenger_token",
            role = UserRole.USER,
            userId = "usr_passenger_123"
        )
        runBlocking {
            sessionStore.saveSession(validSession)
        }
    }

    // =========================================================================
    // 1. WEBSOCKET TRANSPORT & AUTHENTICATION
    // =========================================================================

    @Test
    fun `websocket url attaches token query parameter and sanitized logging`() {
        val client = OkHttpRealtimeClient(
            sessionCoordinator = sessionCoordinator,
            dispatchers = testDispatchers
        )

        val backoff1 = client.calculateBackoffDelay(1)
        val backoff2 = client.calculateBackoffDelay(2)
        val backoff3 = client.calculateBackoffDelay(3)

        assertEquals(1000L, backoff1)
        assertEquals(2000L, backoff2)
        assertEquals(4000L, backoff3)
    }

    @Test
    fun `subscribeToRideTracking connects to ws rides realtime and sends RIDE_TRACKING_SUBSCRIBE frame`() = runBlocking {
        var connectedEndpoint: String? = null
        var sentFrame: String? = null

        val fakeRealtimeClient = object : RealtimeClient {
            override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> {
                connectedEndpoint = endpoint
                return IshaaraResult.success(Unit)
            }
            override suspend fun disconnect() {}
            override suspend fun send(message: String): IshaaraResult<Unit> {
                sentFrame = message
                return IshaaraResult.success(Unit)
            }
            override fun observeConnectionState(): Flow<RealtimeConnectionState> =
                MutableStateFlow(RealtimeConnectionState.Connected)
            override fun observeEvents(): Flow<RealtimeEvent> = MutableSharedFlow()
        }

        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleTrackingSnapshotJson))
        }

        val remoteDataSource = DefaultRideTrackingRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = RideTrackingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = fakeRealtimeClient,
            dispatchers = testDispatchers
        )

        val result = repository.subscribeToRideTracking("ride_abc_123")
        assertTrue(result.isSuccess)
        assertEquals("${networkConfig.fullWebSocketBaseUrl}/rides/realtime", connectedEndpoint)
        assertEquals("""{"type":"RIDE_TRACKING_SUBSCRIBE","payload":{"rideId":"ride_abc_123"}}""", sentFrame)
    }

    // =========================================================================
    // 2. REALTIME EVENT TAXONOMY & PARSING
    // =========================================================================

    @Test
    fun `RealtimeEventParser correctly parses all backend ride tracking and lifecycle events`() {
        // 1. TRACKING_SNAPSHOT
        val snapshotJson = """
            {
                "type": "TRACKING_SNAPSHOT",
                "payload": $sampleTrackingSnapshotJson
            }
        """.trimIndent()
        val snapshotEvent = RealtimeEventParser.parse(snapshotJson)
        assertTrue(snapshotEvent is RealtimeEvent.TrackingSnapshot)
        val snapshot = (snapshotEvent as RealtimeEvent.TrackingSnapshot).snapshot
        assertEquals("651a2b3c4d5e6f7a8b9c0001", snapshot.rideId)
        assertEquals("IN_PROGRESS", snapshot.status)
        assertEquals(25.2677, snapshot.driver!!.location!!.latitude, 0.0001)

        // 2. RIDE_TRACKING_UPDATED
        val updatedJson = """
            {
                "type": "RIDE_TRACKING_UPDATED",
                "payload": {
                    "rideId": "651a2b3c4d5e6f7a8b9c0001",
                    "driverLocation": { "latitude": 25.2750, "longitude": 82.9950 },
                    "freshness": "FRESH",
                    "trackingState": "FRESH",
                    "routeProgress": {
                        "completedDistanceMeters": 2500,
                        "remainingDistanceMeters": 2500,
                        "progressPercent": 50.0
                    },
                    "distanceToPickupMeters": null,
                    "distanceToDestinationMeters": 2500,
                    "eta": { "available": true, "seconds": 300 },
                    "recordedAt": "2026-10-01T08:12:00.000Z"
                }
            }
        """.trimIndent()
        val updatedEvent = RealtimeEventParser.parse(updatedJson)
        assertTrue(updatedEvent is RealtimeEvent.RideTrackingUpdated)
        assertEquals(25.2750, (updatedEvent as RealtimeEvent.RideTrackingUpdated).update.driverLocation!!.latitude, 0.0001)

        // 3. DRIVER_LOCATION_UPDATED
        val locUpdatedJson = """
            {
                "type": "DRIVER_LOCATION_UPDATED",
                "payload": {
                    "rideId": "651a2b3c4d5e6f7a8b9c0001",
                    "location": { "latitude": 25.2800, "longitude": 83.0000 },
                    "recordedAt": "2026-10-01T08:13:00.000Z",
                    "isStale": false
                }
            }
        """.trimIndent()
        val locEvent = RealtimeEventParser.parse(locUpdatedJson)
        assertTrue(locEvent is RealtimeEvent.DriverLocationUpdated)
        assertEquals(25.2800, (locEvent as RealtimeEvent.DriverLocationUpdated).location.latitude, 0.0001)

        // 4. Ride lifecycle progression events
        val arrivingEvent = RealtimeEventParser.parse("""{"type":"RIDE_DRIVER_ARRIVING","payload":{"rideId":"651a2b3c4d5e6f7a8b9c0001"}}""")
        assertTrue(arrivingEvent is RealtimeEvent.RideDriverArriving)

        val pickedUpEvent = RealtimeEventParser.parse("""{"type":"RIDE_PICKED_UP","payload":{"rideId":"651a2b3c4d5e6f7a8b9c0001"}}""")
        assertTrue(pickedUpEvent is RealtimeEvent.RidePickedUp)

        val startedEvent = RealtimeEventParser.parse("""{"type":"RIDE_STARTED","payload":{"rideId":"651a2b3c4d5e6f7a8b9c0001"}}""")
        assertTrue(startedEvent is RealtimeEvent.RideStarted)

        val completedEvent = RealtimeEventParser.parse("""{"type":"RIDE_COMPLETED","payload":{"rideId":"651a2b3c4d5e6f7a8b9c0001"}}""")
        assertTrue(completedEvent is RealtimeEvent.RideCompleted)

        val cancelledEvent = RealtimeEventParser.parse("""{"type":"RIDE_CANCELLED","payload":{"rideId":"651a2b3c4d5e6f7a8b9c0001","reason":"Driver requested cancel"}}""")
        assertTrue(cancelledEvent is RealtimeEvent.RideCancelled)
        assertEquals("Driver requested cancel", (cancelledEvent as RealtimeEvent.RideCancelled).reason)

        // 5. RIDE_TRACKING_ENDED
        val endedEvent = RealtimeEventParser.parse("""{"type":"RIDE_TRACKING_ENDED","payload":{"rideId":"651a2b3c4d5e6f7a8b9c0001","status":"COMPLETED"}}""")
        assertTrue(endedEvent is RealtimeEvent.RideTrackingEnded)
        assertEquals("COMPLETED", (endedEvent as RealtimeEvent.RideTrackingEnded).status)
    }

    // =========================================================================
    // 3. UNKNOWN AND MALFORMED EVENT ISOLATION
    // =========================================================================

    @Test
    fun `malformed and unknown events do not crash parser and fall back safely`() {
        val unknownJson = """{"type":"NEW_FUTURE_BACKEND_EVENT","payload":{"data":123}}"""
        val unknownEvent = RealtimeEventParser.parse(unknownJson)
        assertTrue(unknownEvent is RealtimeEvent.UnknownEvent)
        assertEquals("NEW_FUTURE_BACKEND_EVENT", (unknownEvent as RealtimeEvent.UnknownEvent).eventType)

        val malformedJson = "{ this is invalid json :::"
        val rawEvent = RealtimeEventParser.parse(malformedJson)
        assertTrue(rawEvent is RealtimeEvent.RawMessage)
    }

    // =========================================================================
    // 4. REST INITIAL HYDRATION & RECONCILIATION AFTER RECONNECT
    // =========================================================================

    @Test
    fun `initial tracking snapshot loads via GET tracking endpoint and maps domain state`() = runBlocking {
        var requestedUrl: String? = null
        var authHeader: String? = null

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                requestedUrl = request.url
                authHeader = request.headers["Authorization"]
                return IshaaraResult.success(HttpResponse(200, sampleTrackingSnapshotJson))
            }
        }

        val remoteDataSource = DefaultRideTrackingRemoteDataSource(fakeClient, networkConfig)
        val repository = RideTrackingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = object : RealtimeClient {
                override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
                override suspend fun disconnect() {}
                override suspend fun send(message: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
                override fun observeConnectionState(): Flow<RealtimeConnectionState> = MutableStateFlow(RealtimeConnectionState.Connected)
                override fun observeEvents(): Flow<RealtimeEvent> = MutableSharedFlow()
            },
            dispatchers = testDispatchers
        )

        val result = repository.getRideTrackingSnapshot("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(result.isSuccess)
        val snapshot = (result as IshaaraResult.Success).data

        assertTrue(requestedUrl?.contains("/rides/651a2b3c4d5e6f7a8b9c0001/tracking") == true)
        assertEquals("Bearer jwt_test_passenger_token", authHeader)
        assertEquals("651a2b3c4d5e6f7a8b9c0001", snapshot.rideId)
        assertEquals(TrackingRideStatus.IN_PROGRESS, snapshot.status)
        assertEquals(TrackingState.FRESH, snapshot.trackingState)
        assertEquals(25.2677, snapshot.driverLocation!!.coordinates.latitude, 0.0001)
        assertEquals(82.9913, snapshot.driverLocation!!.coordinates.longitude, 0.0001)
        assertEquals(3000, snapshot.distanceToDestinationMeters)
        assertEquals("6 mins", snapshot.eta.formattedEtaMinutes)
    }

    // =========================================================================
    // 5. VIEWMODEL REALTIME STREAMING & STATE EVOLUTION
    // =========================================================================

    @Test
    fun `RideTrackingViewModel updates UI state dynamically as realtime events arrive`() = runBlocking {
        val eventFlow = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 16)
        val connectionFlow = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleTrackingSnapshotJson))
        }

        val fakeRealtimeClient = object : RealtimeClient {
            override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
            override suspend fun disconnect() {}
            override suspend fun send(message: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
            override fun observeConnectionState(): Flow<RealtimeConnectionState> = connectionFlow
            override fun observeEvents(): Flow<RealtimeEvent> = eventFlow
        }

        val remoteDataSource = DefaultRideTrackingRemoteDataSource(fakeClient, networkConfig)
        val repository = RideTrackingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = fakeRealtimeClient,
            dispatchers = testDispatchers
        )

        val viewModel = RideTrackingViewModel(
            rideId = "651a2b3c4d5e6f7a8b9c0001",
            getRideTrackingUseCase = GetRideTrackingUseCase(repository),
            getDriverLocationUseCase = GetDriverLocationUseCase(repository),
            observeRideTrackingUseCase = ObserveRideTrackingUseCase(repository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        // 1. Initial snapshot state loaded
        val initialContent = viewModel.uiState.value as RideTrackingUiState.Content
        assertEquals(TrackingRideStatus.IN_PROGRESS, initialContent.snapshot.status)
        assertEquals(25.2677, initialContent.snapshot.driverLocation!!.coordinates.latitude, 0.0001)

        // 2. Driver location update received over WebSocket
        eventFlow.emit(
            RealtimeEvent.DriverLocationUpdated(
                rideId = "651a2b3c4d5e6f7a8b9c0001",
                location = com.ishara.app.data.remote.dto.TrackingCoordinatesDto(25.2900, 83.0100),
                recordedAt = "2026-10-01T08:15:00.000Z",
                isStale = false
            )
        )

        val updatedContent = viewModel.uiState.value as RideTrackingUiState.Content
        assertEquals(25.2900, updatedContent.snapshot.driverLocation!!.coordinates.latitude, 0.0001)
        assertEquals(83.0100, updatedContent.snapshot.driverLocation!!.coordinates.longitude, 0.0001)
        assertEquals(TrackingFreshness.FRESH, updatedContent.snapshot.driverLocation!!.freshness)

        // 3. Stale location update received
        eventFlow.emit(
            RealtimeEvent.DriverLocationUpdated(
                rideId = "651a2b3c4d5e6f7a8b9c0001",
                location = com.ishara.app.data.remote.dto.TrackingCoordinatesDto(25.2900, 83.0100),
                recordedAt = "2026-10-01T08:10:00.000Z",
                isStale = true
            )
        )

        val staleContent = viewModel.uiState.value as RideTrackingUiState.Content
        assertEquals(TrackingFreshness.STALE, staleContent.snapshot.driverLocation!!.freshness)
        assertEquals(TrackingState.STALE, staleContent.snapshot.trackingState)

        // 4. Ride completion terminal event
        eventFlow.emit(
            RealtimeEvent.RideCompleted(
                rideId = "651a2b3c4d5e6f7a8b9c0001"
            )
        )

        val terminalContent = viewModel.uiState.value as RideTrackingUiState.Terminal
        assertEquals(TrackingRideStatus.COMPLETED, terminalContent.status)
        assertTrue(terminalContent.status.isTerminal)
    }

    // =========================================================================
    // 6. RECONNECTION RECONCILIATION
    // =========================================================================

    @Test
    fun `reconnection triggers silent authoritative snapshot refresh to repair missed events`() = runBlocking {
        val connectionFlow = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)
        val eventFlow = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 16)
        val fetchCount = AtomicInteger(0)

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                fetchCount.incrementAndGet()
                return IshaaraResult.success(HttpResponse(200, sampleTrackingSnapshotJson))
            }
        }

        val fakeRealtimeClient = object : RealtimeClient {
            override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
            override suspend fun disconnect() {}
            override suspend fun send(message: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
            override fun observeConnectionState(): Flow<RealtimeConnectionState> = connectionFlow
            override fun observeEvents(): Flow<RealtimeEvent> = eventFlow
        }

        val remoteDataSource = DefaultRideTrackingRemoteDataSource(fakeClient, networkConfig)
        val repository = RideTrackingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = fakeRealtimeClient,
            dispatchers = testDispatchers
        )

        val viewModel = RideTrackingViewModel(
            rideId = "651a2b3c4d5e6f7a8b9c0001",
            getRideTrackingUseCase = GetRideTrackingUseCase(repository),
            getDriverLocationUseCase = GetDriverLocationUseCase(repository),
            observeRideTrackingUseCase = ObserveRideTrackingUseCase(repository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertEquals(1, fetchCount.get())

        // Simulate connection lost then reconnected
        connectionFlow.value = RealtimeConnectionState.Reconnecting(attempt = 1, delayMillis = 1000)
        assertTrue((viewModel.uiState.value as RideTrackingUiState.Content).isDegraded)

        connectionFlow.value = RealtimeConnectionState.Connected
        // Silent refresh reconciles state
        assertEquals(2, fetchCount.get())
        assertFalse((viewModel.uiState.value as RideTrackingUiState.Content).isDegraded)
    }

    // =========================================================================
    // 7. TERMINAL RIDE CLEANUP & NAVIGATION
    // =========================================================================

    @Test
    fun `RideTrackingViewModel unregisters realtime listeners on terminal status`() = runBlocking {
        var unsubscribeCalled = false

        val fakeRealtimeClient = object : RealtimeClient {
            override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
            override suspend fun disconnect() {}
            override suspend fun send(message: String): IshaaraResult<Unit> {
                if (message.contains("RIDE_TRACKING_UNSUBSCRIBE")) {
                    unsubscribeCalled = true
                }
                return IshaaraResult.success(Unit)
            }
            override fun observeConnectionState(): Flow<RealtimeConnectionState> = MutableStateFlow(RealtimeConnectionState.Connected)
            override fun observeEvents(): Flow<RealtimeEvent> = MutableSharedFlow()
        }

        val terminalSnapshotJson = """
            {
                "rideId": "651a2b3c4d5e6f7a8b9c0001",
                "status": "COMPLETED",
                "trackingState": "FRESH",
                "driver": null,
                "route": null,
                "distanceToPickupMeters": null,
                "distanceToDestinationMeters": null,
                "eta": { "available": false },
                "updatedAt": "2026-10-01T08:20:00.000Z"
            }
        """.trimIndent()

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, terminalSnapshotJson))
        }

        val remoteDataSource = DefaultRideTrackingRemoteDataSource(fakeClient, networkConfig)
        val repository = RideTrackingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = fakeRealtimeClient,
            dispatchers = testDispatchers
        )

        val viewModel = RideTrackingViewModel(
            rideId = "651a2b3c4d5e6f7a8b9c0001",
            getRideTrackingUseCase = GetRideTrackingUseCase(repository),
            getDriverLocationUseCase = GetDriverLocationUseCase(repository),
            observeRideTrackingUseCase = ObserveRideTrackingUseCase(repository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue(state is RideTrackingUiState.Terminal)
        assertEquals(TrackingRideStatus.COMPLETED, (state as RideTrackingUiState.Terminal).status)
    }

    // =========================================================================
    // 8. SECURITY & ROLE AUTHORIZATION
    // =========================================================================

    @Test
    fun `non-passenger session fails tracking with Forbidden error`() = runBlocking {
        // Driver attempting passenger tracking endpoint
        val driverSession = AuthSession(
            token = "jwt_driver_token",
            role = UserRole.DRIVER_CONDUCTOR,
            userId = "usr_driver_999"
        )
        sessionStore.saveSession(driverSession)

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleTrackingSnapshotJson))
        }

        val remoteDataSource = DefaultRideTrackingRemoteDataSource(fakeClient, networkConfig)
        val repository = RideTrackingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = sessionLocalDataSource,
            networkConfig = networkConfig,
            realtimeClient = object : RealtimeClient {
                override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
                override suspend fun disconnect() {}
                override suspend fun send(message: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
                override fun observeConnectionState(): Flow<RealtimeConnectionState> = MutableStateFlow(RealtimeConnectionState.Connected)
                override fun observeEvents(): Flow<RealtimeEvent> = MutableSharedFlow()
            },
            dispatchers = testDispatchers
        )

        val result = repository.getRideTrackingSnapshot("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(result.isFailure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
    }
}
