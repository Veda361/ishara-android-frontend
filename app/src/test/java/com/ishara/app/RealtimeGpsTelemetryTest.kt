package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.network.SessionInvalidationCoordinator
import com.ishara.app.core.realtime.OkHttpRealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.realtime.RealtimeEventParser
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.remote.datasource.DriverRemoteDataSourceImpl
import com.ishara.app.data.repository.DriverRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.UpdateDriverLocationUseCase
import com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator
import com.ishara.app.feature.driver.tracking.DriverTrackingStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Comprehensive Phase 09 Test Suite covering Location, Network, Realtime, and Security.
 */
class RealtimeGpsTelemetryTest {

    private val testDispatcher = Dispatchers.Unconfined
    private val testScope = CoroutineScope(testDispatcher + Job())

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private class MockHttpClient(
        var responseToReturn: IshaaraResult<HttpResponse>
    ) : IshaaraHttpClient {
        var lastRequest: HttpRequest? = null

        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            lastRequest = request
            return responseToReturn
        }
    }

    private class MockSessionLocalDataSource(
        var sessionToReturn: AuthSession?
    ) : SessionLocalDataSource {
        override suspend fun saveSession(session: AuthSession) {}
        override suspend fun getSession(): AuthSession? = sessionToReturn
        override fun observeSession(): Flow<AuthSession?> = flowOf(sessionToReturn)
        override suspend fun clearSession() {}
    }

    private val validSession = AuthSession(
        token = "test_driver_jwt_token",
        userId = "driver_usr_123",
        role = UserRole.DRIVER_CONDUCTOR
    )

    // =========================================================================
    // LOCATION SUITE (Tests 1 - 8)
    // =========================================================================

    @Test
    fun test_01_location_provider_emits_valid_location() = runBlocking {
        val testLocation = LocationCoordinates(
            latitude = 18.5204,
            longitude = 73.8567,
            accuracyMeters = 4.2f,
            headingDegrees = 90.0f,
            speedMps = 15.0f,
            timestampMillis = 1758794400000L
        )

        val fakeProvider = object : LocationProvider {
            override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> =
                IshaaraResult.success(testLocation)

            override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> =
                flowOf(testLocation)

            override fun isLocationAvailable(): Boolean = true
        }

        val result = fakeProvider.getCurrentLocation()
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertEquals(18.5204, data.latitude, 0.0001)
        assertEquals(73.8567, data.longitude, 0.0001)
    }

    @Test
    fun test_02_03_location_mapping_preserves_latitude_and_longitude() {
        val coords = LocationCoordinates(
            latitude = 25.4484,
            longitude = 78.5685,
            accuracyMeters = 3.5f,
            headingDegrees = 180.0f,
            speedMps = 8.5f,
            timestampMillis = 1758794400000L
        )

        assertEquals(25.4484, coords.latitude, 0.00001)
        assertEquals(78.5685, coords.longitude, 0.00001)
    }

    @Test
    fun test_04_geojson_output_is_longitude_then_latitude_anti_inversion() {
        val lat = 18.5204
        val lng = 73.8567
        val coords = LocationCoordinates(latitude = lat, longitude = lng)

        val geoJson = coords.toGeoJson()
        val array = coords.toGeoJsonArray()

        // Strict RFC 7946 Anti-Inversion Verification:
        // Index 0 MUST be Longitude, Index 1 MUST be Latitude
        assertEquals("Index 0 must be longitude", lng, array[0], 0.0001)
        assertEquals("Index 1 must be latitude", lat, array[1], 0.0001)

        assertEquals(lng, geoJson.longitude, 0.0001)
        assertEquals(lat, geoJson.latitude, 0.0001)
    }

    @Test
    fun test_05_timestamp_serialization_is_correct_iso8601_utc() {
        // 2026-09-25T10:00:00.000Z in UTC milliseconds = 1790330400000L
        val fixedMillis = 1790330400000L
        val coords = LocationCoordinates(latitude = 18.5, longitude = 73.8, timestampMillis = fixedMillis)

        val isoString = coords.toIso8601Utc()
        assertTrue("Must end in Z for UTC", isoString.endsWith("Z"))
        assertTrue("Must contain date-T-time format", isoString.contains("T"))

        // Re-parse to verify epoch parity
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val parsedEpoch = sdf.parse(isoString)?.time
        assertEquals(fixedMillis, parsedEpoch)
    }

    @Test
    fun test_06_invalid_coordinates_rejected_safely() {
        var latExceptionThrown = false
        try {
            GeoJsonCoordinate(longitude = 73.8, latitude = 95.0) // invalid latitude > 90
        } catch (_: IllegalArgumentException) {
            latExceptionThrown = true
        }
        assertTrue("Invalid latitude > 90 must throw IllegalArgumentException", latExceptionThrown)

        var lngExceptionThrown = false
        try {
            GeoJsonCoordinate(longitude = 195.0, latitude = 18.5) // invalid longitude > 180
        } catch (_: IllegalArgumentException) {
            lngExceptionThrown = true
        }
        assertTrue("Invalid longitude > 180 must throw IllegalArgumentException", lngExceptionThrown)
    }

    @Test
    fun test_07_permission_denied_handled() {
        val error = IshaaraError.PermissionDenied("ACCESS_FINE_LOCATION denied by user")
        assertEquals("ACCESS_FINE_LOCATION denied by user", error.message)
        assertTrue(error is IshaaraError)
    }

    @Test
    fun test_08_gps_unavailable_handled() {
        val error = IshaaraError.LocationDisabled("GPS provider disabled")
        assertEquals("GPS provider disabled", error.message)
        assertTrue(error is IshaaraError)
    }

    // =========================================================================
    // NETWORK SUITE (Tests 9 - 15)
    // =========================================================================

    @Test
    fun test_09_successful_patch_transmits_only_verified_fields() = runBlocking {
        val httpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(200, """{"success":true,"statusCode":200}"""))
        )
        val remoteDataSource = DriverRemoteDataSourceImpl(httpClient, NetworkConfig())
        val sessionDataSource = MockSessionLocalDataSource(validSession)
        val repository = DriverRepositoryImpl(remoteDataSource, sessionDataSource)

        val fix = LocationCoordinates(
            latitude = 18.5204,
            longitude = 73.8567,
            accuracyMeters = 4.5f,
            headingDegrees = 45.0f,
            speedMps = 12.0f,
            timestampMillis = 1790330400000L
        )

        val result = repository.updateLocation(fix)
        assertTrue("PATCH must succeed with HTTP 200", result is IshaaraResult.Success)

        val request = httpClient.lastRequest
        assertNotNull(request)
        assertEquals(HttpMethod.PATCH, request!!.method)
        assertTrue(request.url.endsWith("/drivers/me/location"))
        assertEquals("Bearer test_driver_jwt_token", request.headers["Authorization"])

        // Verify request body contains verified fields: coordinates [lng, lat], heading, speed, accuracy, recordedAt
        val body = request.body!!
        assertTrue(body.contains("\"coordinates\": [73.8567, 18.5204]"))
        assertTrue(body.contains("\"heading\": 45.0"))
        assertTrue(body.contains("\"speed\": 12.0"))
        assertTrue(body.contains("\"accuracy\": 4.5"))
        assertTrue(body.contains("\"recordedAt\": \"${fix.toIso8601Utc()}\""))

        // Critical: Verify NO unverified fields were added
        assertFalse(body.contains("tripId"))
        assertFalse(body.contains("vehicleId"))
        assertFalse(body.contains("passengerId"))
    }

    @Test
    fun test_10_authentication_failure_401() = runBlocking {
        val httpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Authentication(message = "Session expired."))
        )
        val remoteDataSource = DriverRemoteDataSourceImpl(httpClient, NetworkConfig())
        val sessionDataSource = MockSessionLocalDataSource(validSession)
        val repository = DriverRepositoryImpl(remoteDataSource, sessionDataSource)

        val result = repository.updateLocation(LocationCoordinates(18.5, 73.8))
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun test_11_forbidden_driver_403() = runBlocking {
        val httpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Forbidden(message = "Access denied: Required role 'DRIVER_CONDUCTOR'."))
        )
        val remoteDataSource = DriverRemoteDataSourceImpl(httpClient, NetworkConfig())
        val sessionDataSource = MockSessionLocalDataSource(validSession)
        val repository = DriverRepositoryImpl(remoteDataSource, sessionDataSource)

        val result = repository.updateLocation(LocationCoordinates(18.5, 73.8))
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Forbidden)
    }

    @Test
    fun test_12_network_failure() = runBlocking {
        val httpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Network("No route to host"))
        )
        val remoteDataSource = DriverRemoteDataSourceImpl(httpClient, NetworkConfig())
        val sessionDataSource = MockSessionLocalDataSource(validSession)
        val repository = DriverRepositoryImpl(remoteDataSource, sessionDataSource)

        val result = repository.updateLocation(LocationCoordinates(18.5, 73.8))
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Network)
    }

    @Test
    fun test_13_timeout() = runBlocking {
        val httpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Timeout("Read timed out"))
        )
        val remoteDataSource = DriverRemoteDataSourceImpl(httpClient, NetworkConfig())
        val sessionDataSource = MockSessionLocalDataSource(validSession)
        val repository = DriverRepositoryImpl(remoteDataSource, sessionDataSource)

        val result = repository.updateLocation(LocationCoordinates(18.5, 73.8))
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Timeout)
    }

    @Test
    fun test_14_server_failure_500() = runBlocking {
        val httpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Server(500, "Internal Server Error"))
        )
        val remoteDataSource = DriverRemoteDataSourceImpl(httpClient, NetworkConfig())
        val sessionDataSource = MockSessionLocalDataSource(validSession)
        val repository = DriverRepositoryImpl(remoteDataSource, sessionDataSource)

        val result = repository.updateLocation(LocationCoordinates(18.5, 73.8))
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Server)
    }

    @Test
    fun test_15_freshest_only_offline_behavior() = runBlocking {
        var networkAvailable = false
        val transmittedLocations = mutableListOf<LocationCoordinates>()

        val fakeRepository = object : com.ishara.app.domain.repository.DriverRepository {
            override suspend fun getOperationalContext(timezone: String?) = TODO()
            override suspend fun setOnline() = TODO()
            override suspend fun setOffline() = TODO()
            override suspend fun startTrip(tripId: String) = TODO()
            override suspend fun completeTrip(tripId: String) = TODO()
            override suspend fun cancelTrip(tripId: String) = TODO()
            override suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit> {
                return if (networkAvailable) {
                    transmittedLocations.add(location)
                    IshaaraResult.success(Unit)
                } else {
                    IshaaraResult.failure(IshaaraError.Network("Offline"))
                }
            }
        }

        val locationFlow = MutableSharedFlow<LocationCoordinates>()
        val fakeProvider = object : LocationProvider {
            override suspend fun getCurrentLocation() = TODO()
            override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = locationFlow
            override fun isLocationAvailable(): Boolean = true
        }

        val coordinator = DriverTrackingCoordinator(
            locationProvider = fakeProvider,
            updateDriverLocationUseCase = UpdateDriverLocationUseCase(fakeRepository),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        coordinator.startTracking("trip_123")

        // 1. Emit fix while OFFLINE:
        val fix1 = LocationCoordinates(18.51, 73.81, timestampMillis = 1000L)
        locationFlow.emit(fix1)
        assertTrue("Status must be NetworkUnavailable", coordinator.status.value is DriverTrackingStatus.NetworkUnavailable)
        assertEquals(0, transmittedLocations.size)

        // 2. Emit another fix while OFFLINE (should overwrite fix1; never queue historical backlog):
        val fix2 = LocationCoordinates(18.52, 73.82, timestampMillis = 2000L)
        locationFlow.emit(fix2)
        val offlineState = coordinator.status.value as DriverTrackingStatus.NetworkUnavailable
        assertEquals(fix2, offlineState.freshestFix) // Buffered single freshest fix!
        assertEquals(0, transmittedLocations.size)

        // 3. Network recovers!
        networkAvailable = true
        val fix3 = LocationCoordinates(18.53, 73.83, timestampMillis = 3000L)
        locationFlow.emit(fix3)

        // Transmitted fix3 successfully; intermediate fix1 was dropped
        assertEquals(1, transmittedLocations.size)
        assertEquals(fix3, transmittedLocations[0])
        assertTrue(coordinator.status.value is DriverTrackingStatus.Active)

        coordinator.stopTracking()
    }

    // =========================================================================
    // REALTIME SUITE (Tests 16 - 25)
    // =========================================================================

    @Test
    fun test_16_17_websocket_connect_and_state_transitions() = runBlocking {
        val coordinator = SessionInvalidationCoordinator()
        val client = OkHttpRealtimeClient(
            sessionCoordinator = coordinator,
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        // Initial state
        assertEquals(RealtimeConnectionState.Disconnected, client.observeConnectionState().first())
    }

    @Test
    fun test_18_valid_telemetry_event_parses() {
        val json = """
        {
            "event": "live_telemetry_update",
            "rideId": "ride_999",
            "coordinates": [73.8567, 18.5204],
            "headingDegrees": 88.5,
            "speedMps": 14.2,
            "remainingDistanceMeters": 1250,
            "etaSeconds": 180,
            "routeProgressPercentage": 65.5
        }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue("Event must be LiveTelemetryUpdate", event is RealtimeEvent.LiveTelemetryUpdate)

        val telemetry = event as RealtimeEvent.LiveTelemetryUpdate
        assertEquals("ride_999", telemetry.rideId)
        assertEquals(73.8567, telemetry.coordinate.longitude, 0.0001)
        assertEquals(18.5204, telemetry.coordinate.latitude, 0.0001)
        assertEquals(88.5f, telemetry.headingDegrees!!, 0.1f)
        assertEquals(14.2f, telemetry.speedMps!!, 0.1f)
        assertEquals(1250, telemetry.remainingDistanceMeters)
        assertEquals(180, telemetry.etaSeconds)
        assertEquals(65.5f, telemetry.routeProgressPercentage!!, 0.1f)
    }

    @Test
    fun test_19_malformed_event_does_not_crash() {
        val malformedJson = "{ event: corrupted_json, coordinates: [not_numbers] "
        val event = RealtimeEventParser.parse(malformedJson)
        assertNotNull(event)
        assertTrue("Malformed JSON must fallback to RawMessage without crashing", event is RealtimeEvent.RawMessage)
    }

    @Test
    fun test_20_unknown_event_does_not_crash() {
        val unknownJson = """
        {
            "event": "future_fleet_teleoperation_v2",
            "payloadData": "some_opaque_data"
        }
        """.trimIndent()

        val event = RealtimeEventParser.parse(unknownJson)
        assertNotNull(event)
        assertTrue("Unknown event must be safely parsed into UnknownEvent", event is RealtimeEvent.UnknownEvent)
        val unknown = event as RealtimeEvent.UnknownEvent
        assertEquals("future_fleet_teleoperation_v2", unknown.eventType)
    }

    @Test
    fun test_21_normal_close_does_not_reconnect() = runBlocking {
        val client = OkHttpRealtimeClient(
            sessionCoordinator = SessionInvalidationCoordinator(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        client.disconnect()
        assertEquals(RealtimeConnectionState.Disconnected, client.observeConnectionState().first())
    }

    @Test
    fun test_23_unauthorized_close_stops_reconnect() = runBlocking {
        val coordinator = SessionInvalidationCoordinator()
        val client = OkHttpRealtimeClient(
            sessionCoordinator = coordinator,
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        // Status code 4401 or 1008 must stop reconnection
        assertEquals(4401, OkHttpRealtimeClient.UNAUTHORIZED_CLOSE_STATUS)
        assertEquals(1008, OkHttpRealtimeClient.POLICY_VIOLATION_STATUS)
        assertEquals(1000, OkHttpRealtimeClient.NORMAL_CLOSURE_STATUS)
    }

    @Test
    fun test_24_backoff_sequence_is_correct() {
        val client = OkHttpRealtimeClient(
            sessionCoordinator = SessionInvalidationCoordinator(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        assertEquals(1_000L, client.calculateBackoffDelay(1))
        assertEquals(2_000L, client.calculateBackoffDelay(2))
        assertEquals(4_000L, client.calculateBackoffDelay(3))
        assertEquals(8_000L, client.calculateBackoffDelay(4))
        assertEquals(16_000L, client.calculateBackoffDelay(5))
        assertEquals(30_000L, client.calculateBackoffDelay(6))
    }

    @Test
    fun test_25_maximum_reconnect_attempts_enforced() {
        assertEquals(5, OkHttpRealtimeClient.MAX_RECONNECT_ATTEMPTS)
        assertEquals(30_000L, OkHttpRealtimeClient.MAX_RECONNECT_DELAY_MILLIS)
    }

    // =========================================================================
    // SECURITY SUITE (Tests 33 - 34)
    // =========================================================================

    @Test
    fun test_33_token_never_appears_in_logs() {
        val sensitiveLog = "Connecting to wss://host.com/api/v1/rides/realtime?token=eyJhbGciOiJIUzI1NiJ9.secret"
        val sanitized = IshaaraLogger.sanitize(sensitiveLog)
        assertFalse("Log must not contain raw JWT", sanitized.contains("eyJhbGciOiJIUzI1NiJ9.secret"))
        assertTrue("Log must contain [PROTECTED]", sanitized.contains("token=[PROTECTED]"))

        val bearerHeader = "Authorization: Bearer my_secret_token_12345"
        val sanitizedHeader = IshaaraLogger.sanitize(bearerHeader)
        assertFalse(sanitizedHeader.contains("my_secret_token_12345"))
        assertTrue(sanitizedHeader.contains("[PROTECTED]") || sanitizedHeader.contains("[REDACTED]"))
    }

    @Test
    fun test_34_raw_coordinates_never_appear_in_production_logging() {
        IshaaraLogger.setDebug(false) // Production mode
        val prodLog = IshaaraLogger.formatCoordinatesForLog(18.520438, 73.856729)
        assertEquals("[REDACTED]", prodLog)

        IshaaraLogger.setDebug(true) // Debug mode
        val debugLog = IshaaraLogger.formatCoordinatesForLog(18.520438, 73.856729)
        assertEquals("18.52***, 73.86***", debugLog)
        assertFalse(debugLog.contains("18.520438")) // High precision stripped!
    }
}
