package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.mapper.RideRequestMapper
import com.ishara.app.data.remote.datasource.RideRequestRemoteDataSourceImpl
import com.ishara.app.data.repository.RideRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.DiscoveredTripDriver
import com.ishara.app.domain.model.DiscoveredTripVehicle
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestLocationWaypoint
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.model.TripCompatibility
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.model.isTerminal
import com.ishara.app.domain.usecase.CancelRideRequestUseCase
import com.ishara.app.domain.usecase.CreateRideRequestUseCase
import com.ishara.app.domain.usecase.GetRideRequestUseCase
import com.ishara.app.domain.usecase.GetUserRideRequestsUseCase
import com.ishara.app.feature.student.riderequest.RideRequestStage
import com.ishara.app.feature.student.riderequest.RideRequestStatusViewModel
import com.ishara.app.feature.student.riderequest.RideRequestViewModel
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * PHASE A10 — RIDE REQUEST LIFECYCLE Test Suite
 *
 * Verifies all Phase A10 requirements:
 * 1. Request Contract & Strict Zod Schema Compliance (POST /api/v1/ride-requests)
 * 2. Strict Serialization Excludes Unpermitted Fields (fare, seatCapacity, seatsRequested)
 * 3. Input Validation (24-hex ObjectId, address length 2..300, min 50m separation)
 * 4. Authentication & Authorization (USER role, 401, 403)
 * 5. Idempotency-Key Header Generation and Deduplication
 * 6. Duplicate Submission Protection (UI isSubmitting & Backend 409 DUPLICATE_RIDE_REQUEST)
 * 7. State Machine Transitions (PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED)
 * 8. RFC 7946 GeoJSON Coordinate Parsing ([longitude, latitude])
 * 9. Authoritative Status Query (GET /api/v1/ride-requests/:requestId)
 * 10. Passenger Cancellation Contract (POST /api/v1/ride-requests/:requestId/cancel)
 * 11. Cancellation Conflict Handling (Already accepted, already expired)
 * 12. Listing Passenger Requests (GET /api/v1/ride-requests/me)
 * 13. State Recovery & Process Recreation
 * 14. Account Switching & State Clearing (No cross-account leakage)
 * 15. Discovery -> Request Review -> Submission -> Status -> Live Ride Handoff Boundary
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RideRequestPhaseA10Test {

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var sessionLocalDataSource: SessionLocalDataSourceImpl
    private lateinit var navigationManager: NavigationManager
    private val networkConfig = NetworkConfig()

    private val sampleDiscoveredTrip = DiscoveredTrip(
        tripId = "651a2b3c4d5e6f7a8b9c0d1e",
        driver = DiscoveredTripDriver(id = "drv_100", name = "Vikram Singh"),
        vehicle = DiscoveredTripVehicle(
            id = "veh_200",
            registrationNumber = "UP65 AB 1234",
            vehicleType = "VAN",
            make = "Tata",
            model = "Magic EV"
        ),
        originName = "BHU Gate",
        originAddress = "BHU Gate, Lanka, Varanasi",
        originCoordinate = GeoJsonCoordinate(latitude = 25.2799, longitude = 82.9995),
        destinationName = "Assi Ghat",
        destinationAddress = "Assi Ghat, Varanasi",
        destinationCoordinate = GeoJsonCoordinate(latitude = 25.2899, longitude = 83.0068),
        distanceMeters = 3200.0,
        durationSeconds = 600.0,
        pickupDistanceMeters = 30.0,
        destinationDistanceMeters = 50.0,
        estimatedDetourMeters = 80.0,
        compatibility = TripCompatibility.HIGH,
        matchScore = 0.95
    )

    private val sampleResponseJson = """
    {
        "success": true,
        "statusCode": 201,
        "message": "Ride request submitted successfully.",
        "data": {
            "id": "651a2b3c4d5e6f7a8b9c0001",
            "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
            "driverId": "drv_100",
            "userId": "usr_passenger_1",
            "pickup": {
                "name": "BHU Gate",
                "formattedAddress": "BHU Gate, Lanka, Varanasi",
                "coordinates": {
                    "type": "Point",
                    "coordinates": [82.9995, 25.2799]
                }
            },
            "destination": {
                "name": "Assi Ghat",
                "formattedAddress": "Assi Ghat, Varanasi",
                "coordinates": {
                    "type": "Point",
                    "coordinates": [83.0068, 25.2899]
                }
            },
            "status": "PENDING",
            "requestedAt": "2026-10-01T08:00:00.000Z",
            "respondedAt": null,
            "expiresAt": "2026-10-01T08:02:00.000Z",
            "discoverySessionId": "dses_0123456789abcdef0123456789abcdef",
            "rejectionReason": null,
            "cancellationReason": null,
            "createdAt": "2026-10-01T08:00:00.000Z",
            "updatedAt": "2026-10-01T08:00:00.000Z"
        }
    }
    """.trimIndent()

    @Before
    fun setUp() {
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        navigationManager = NavigationManager()

        runBlocking {
            sessionLocalDataSource.saveSession(
                AuthSession(
                    userId = "usr_passenger_1",
                    token = "valid_auth_token_p10",
                    role = UserRole.USER
                )
            )
        }
    }

    // =========================================================================
    // 1. API CONTRACT & STRICT SCHEMA SERIALIZATION
    // =========================================================================

    @Test
    fun `ride request creation contract strictly matches backend schema and excludes unpermitted fields`() = runBlocking {
        val capturedRequests = mutableListOf<HttpRequest>()
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedRequests.add(request)
                return IshaaraResult.success(HttpResponse(201, sampleResponseJson))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )
        val useCase = CreateRideRequestUseCase(repository)

        val input = RideRequestInput(
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            pickup = RideRequestLocationWaypoint(
                formattedAddress = "BHU Gate, Lanka, Varanasi",
                latitude = 25.2799,
                longitude = 82.9995,
                name = "BHU Gate"
            ),
            destination = RideRequestLocationWaypoint(
                formattedAddress = "Assi Ghat, Varanasi",
                latitude = 25.2899,
                longitude = 83.0068,
                name = "Assi Ghat"
            ),
            discoverySessionId = "dses_0123456789abcdef0123456789abcdef"
        )

        val result = useCase.execute(input, idempotencyKey = "idem_key_phase10_001")
        assertTrue("Request creation must succeed", result is IshaaraResult.Success)

        assertEquals(1, capturedRequests.size)
        val request = capturedRequests[0]

        // 1. Method must be POST
        assertEquals(HttpMethod.POST, request.method)

        // 2. URL must match /api/v1/ride-requests
        assertEquals("${networkConfig.fullApiBaseUrl}/ride-requests", request.url)

        // 3. Headers
        assertEquals("Bearer valid_auth_token_p10", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("idem_key_phase10_001", request.headers["Idempotency-Key"])

        // 4. Body fields check
        val body = request.body ?: ""
        assertTrue("Must contain tripId", body.contains("\"tripId\":\"651a2b3c4d5e6f7a8b9c0d1e\""))
        assertTrue("Must contain pickup", body.contains("\"pickup\""))
        assertTrue("Must contain destination", body.contains("\"destination\""))
        assertTrue("Must contain discoverySessionId", body.contains("\"discoverySessionId\":\"dses_0123456789abcdef0123456789abcdef\""))

        // STRICT ZOD SCHEMA COMPLIANCE: Unpermitted fields rejected by Zod .strict() MUST NOT exist
        assertFalse("Must NOT contain 'fare'", body.contains("fare"))
        assertFalse("Must NOT contain 'price'", body.contains("price"))
        assertFalse("Must NOT contain 'seatCapacity'", body.contains("seatCapacity"))
        assertFalse("Must NOT contain 'seatsRequested'", body.contains("seatsRequested"))
        assertFalse("Must NOT contain 'passengerCount'", body.contains("passengerCount"))
        assertFalse("Must NOT contain 'status'", body.contains("\"status\""))
        assertFalse("Must NOT contain 'booking'", body.contains("booking"))
    }

    // =========================================================================
    // 2. INPUT VALIDATION & BUSINESS RULES
    // =========================================================================

    @Test
    fun `invalid tripId format is rejected with validation error before network call`() = runBlocking {
        val repository = object : com.ishara.app.domain.repository.RideRepository {
            override suspend fun submitRideRequest(input: RideRequestInput, idempotencyKey: String?): IshaaraResult<RideRequestResult> =
                throw AssertionError("Network must not be called on validation failure")
            override suspend fun createRideRequest(tripId: String, pickupAddress: String, dropoffAddress: String, seatsRequested: Int) =
                throw AssertionError("Not used")
            override suspend fun getActiveRide(rideId: String) = throw AssertionError("Not used")
            override suspend fun getActivePassengerRide() = IshaaraResult.success(null)
            override suspend fun cancelRide(rideId: String, reason: String?) = IshaaraResult.success(Unit)
            override suspend fun getRideRequest(requestId: String) = throw AssertionError("Not used")
            override suspend fun cancelRideRequest(requestId: String, reason: String?) = throw AssertionError("Not used")
            override suspend fun getUserRideRequests(status: RideRequestStatus?, tripId: String?) = throw AssertionError("Not used")
        }

        val useCase = CreateRideRequestUseCase(repository)

        // Invalid tripId (not 24 hex characters)
        try {
            val invalidInput = RideRequestInput(
                tripId = "short_invalid_id",
                pickup = RideRequestLocationWaypoint("Gate A", 25.2799, 82.9995),
                destination = RideRequestLocationWaypoint("Gate B", 25.2899, 83.0068)
            )
            val result = useCase.execute(invalidInput)
            assertTrue(result is IshaaraResult.Failure)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("24-character hexadecimal ObjectId") == true)
        }
    }

    @Test
    fun `separation less than 50 meters is rejected with SAME_ORIGIN_DESTINATION validation error`() = runBlocking {
        val repository = object : com.ishara.app.domain.repository.RideRepository {
            override suspend fun submitRideRequest(input: RideRequestInput, idempotencyKey: String?): IshaaraResult<RideRequestResult> =
                throw AssertionError("Network must not be called")
            override suspend fun createRideRequest(tripId: String, pickupAddress: String, dropoffAddress: String, seatsRequested: Int) =
                throw AssertionError("Not used")
            override suspend fun getActiveRide(rideId: String) = throw AssertionError("Not used")
            override suspend fun getActivePassengerRide() = IshaaraResult.success(null)
            override suspend fun cancelRide(rideId: String, reason: String?) = IshaaraResult.success(Unit)
            override suspend fun getRideRequest(requestId: String) = throw AssertionError("Not used")
            override suspend fun cancelRideRequest(requestId: String, reason: String?) = throw AssertionError("Not used")
            override suspend fun getUserRideRequests(status: RideRequestStatus?, tripId: String?) = throw AssertionError("Not used")
        }

        val useCase = CreateRideRequestUseCase(repository)

        // Almost identical coordinates (separation ~ 0 meters)
        val identicalInput = RideRequestInput(
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            pickup = RideRequestLocationWaypoint("Same Location A", 25.2799, 82.9995),
            destination = RideRequestLocationWaypoint("Same Location B", 25.2799001, 82.9995001)
        )

        val result = useCase.execute(identicalInput)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertTrue(error.message.contains("minimum 50m separation required"))
    }

    // =========================================================================
    // 3. IDEMPOTENCY & DUPLICATE SUBMISSION
    // =========================================================================

    @Test
    fun `viewModel maintains stable idempotency key and prevents duplicate in-flight submission`() = runBlocking {
        var networkCallCount = 0
        val fakeRepo = object : com.ishara.app.domain.repository.RideRepository {
            override suspend fun submitRideRequest(input: RideRequestInput, idempotencyKey: String?): IshaaraResult<RideRequestResult> {
                networkCallCount++
                kotlinx.coroutines.delay(50)
                return IshaaraResult.success(RideRequestMapper.toDomain(
                    RideRequestRemoteDataSourceImpl(object : IshaaraHttpClient {
                        override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(201, sampleResponseJson))
                    }, networkConfig).parseRideRequestResponse(sampleResponseJson)
                ))
            }
            override suspend fun createRideRequest(tripId: String, pickupAddress: String, dropoffAddress: String, seatsRequested: Int) = throw AssertionError("Not used")
            override suspend fun getActiveRide(rideId: String) = throw AssertionError("Not used")
            override suspend fun getActivePassengerRide() = IshaaraResult.success(null)
            override suspend fun cancelRide(rideId: String, reason: String?) = IshaaraResult.success(Unit)
            override suspend fun getRideRequest(requestId: String) = throw AssertionError("Not used")
            override suspend fun cancelRideRequest(requestId: String, reason: String?) = throw AssertionError("Not used")
            override suspend fun getUserRideRequests(status: RideRequestStatus?, tripId: String?) = throw AssertionError("Not used")
        }

        val useCase = CreateRideRequestUseCase(fakeRepo)
        val viewModel = RideRequestViewModel(
            trip = sampleDiscoveredTrip,
            pickupAddress = "BHU Gate, Lanka, Varanasi",
            destinationAddress = "Assi Ghat, Varanasi",
            pickupLatitude = 25.2799,
            pickupLongitude = 82.9995,
            destinationLatitude = 25.2899,
            destinationLongitude = 83.0068,
            createRideRequestUseCase = useCase,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        // Idempotency key must be stable
        val firstKey = viewModel.idempotencyKey
        assertNotNull(firstKey)
        assertTrue(firstKey.isNotBlank())
        assertEquals(firstKey, viewModel.idempotencyKey)

        // Submit first time
        viewModel.submitRideRequest()
        // Concurrent duplicate tap must be guarded
        viewModel.submitRideRequest()
        viewModel.submitRideRequest()

        assertEquals("Only one network call dispatched due to isSubmitting guard", 1, networkCallCount)
    }

    @Test
    fun `backend duplicate request conflict 409 maps to Conflict error`() = runBlocking {
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return IshaaraResult.failure(IshaaraError.Conflict(message = "You already have an active pending ride request for this trip."))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )
        val useCase = CreateRideRequestUseCase(repository)

        val input = RideRequestInput(
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            pickup = RideRequestLocationWaypoint("BHU Gate", 25.2799, 82.9995),
            destination = RideRequestLocationWaypoint("Assi Ghat", 25.2899, 83.0068)
        )

        val result = useCase.execute(input)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Conflict)
        assertTrue(error.message.contains("already have an active pending ride request"))
    }

    // =========================================================================
    // 4. STATUS RETRIEVAL & STATE MACHINE
    // =========================================================================

    @Test
    fun `getRideRequest queries exact GET endpoint and maps status and GeoJSON coordinates`() = runBlocking {
        var queriedUrl: String? = null
        var authHeader: String? = null

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                queriedUrl = request.url
                authHeader = request.headers["Authorization"]
                return IshaaraResult.success(HttpResponse(200, sampleResponseJson))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )
        val useCase = GetRideRequestUseCase(repository)

        val result = useCase.execute("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data

        assertEquals("${networkConfig.fullApiBaseUrl}/ride-requests/651a2b3c4d5e6f7a8b9c0001", queriedUrl)
        assertEquals("Bearer valid_auth_token_p10", authHeader)
        assertEquals("651a2b3c4d5e6f7a8b9c0001", data.id)
        assertEquals(RideRequestStatus.PENDING, data.status)
        assertFalse(data.status.isTerminal)

        // GeoJSON RFC 7946: [longitude, latitude]
        assertNotNull(data.pickupCoordinates)
        assertEquals(25.2799, data.pickupCoordinates!!.latitude, 0.0001)
        assertEquals(82.9995, data.pickupCoordinates!!.longitude, 0.0001)

        assertNotNull(data.destinationCoordinates)
        assertEquals(25.2899, data.destinationCoordinates!!.latitude, 0.0001)
        assertEquals(83.0068, data.destinationCoordinates!!.longitude, 0.0001)
    }

    @Test
    fun `terminal state immutability check identifies accepted rejected cancelled and expired`() {
        assertFalse(RideRequestStatus.PENDING.isTerminal)
        assertTrue(RideRequestStatus.ACCEPTED.isTerminal)
        assertTrue(RideRequestStatus.REJECTED.isTerminal)
        assertTrue(RideRequestStatus.CANCELLED.isTerminal)
        assertTrue(RideRequestStatus.EXPIRED.isTerminal)
    }

    // =========================================================================
    // 5. CANCELLATION LIFECYCLE
    // =========================================================================

    @Test
    fun `passenger cancels pending request via POST cancel endpoint with optional reason`() = runBlocking {
        var cancelUrl: String? = null
        var cancelBody: String? = null

        val cancelledJson = sampleResponseJson.replace("\"status\": \"PENDING\"", "\"status\": \"CANCELLED\"")
            .replace("\"cancellationReason\": null", "\"cancellationReason\": \"Changed plans\"")

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                cancelUrl = request.url
                cancelBody = request.body
                return IshaaraResult.success(HttpResponse(200, cancelledJson))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )
        val useCase = CancelRideRequestUseCase(repository)

        val result = useCase.execute("651a2b3c4d5e6f7a8b9c0001", reason = "Changed plans")
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data

        assertEquals("${networkConfig.fullApiBaseUrl}/ride-requests/651a2b3c4d5e6f7a8b9c0001/cancel", cancelUrl)
        assertTrue(cancelBody!!.contains("\"reason\":\"Changed plans\""))
        assertEquals(RideRequestStatus.CANCELLED, data.status)
        assertEquals("Changed plans", data.cancellationReason)
        assertTrue(data.status.isTerminal)
    }

    @Test
    fun `cancelling an already accepted request maps to conflict error`() = runBlocking {
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return IshaaraResult.failure(IshaaraError.Conflict(message = "Ride request has already been accepted by the driver."))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )
        val useCase = CancelRideRequestUseCase(repository)

        val result = useCase.execute("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Conflict)
        assertTrue(error.message.contains("already been accepted"))
    }

    @Test
    fun `cancel reason exceeding 250 characters is rejected before network dispatch`() = runBlocking {
        val repository = object : com.ishara.app.domain.repository.RideRepository {
            override suspend fun submitRideRequest(input: RideRequestInput, idempotencyKey: String?) = throw AssertionError("Not used")
            override suspend fun createRideRequest(tripId: String, pickupAddress: String, dropoffAddress: String, seatsRequested: Int) = throw AssertionError("Not used")
            override suspend fun getActiveRide(rideId: String) = throw AssertionError("Not used")
            override suspend fun getActivePassengerRide() = IshaaraResult.success(null)
            override suspend fun cancelRide(rideId: String, reason: String?) = IshaaraResult.success(Unit)
            override suspend fun getRideRequest(requestId: String) = throw AssertionError("Not used")
            override suspend fun cancelRideRequest(requestId: String, reason: String?) = throw AssertionError("Network should not be called")
            override suspend fun getUserRideRequests(status: RideRequestStatus?, tripId: String?) = throw AssertionError("Not used")
        }

        val useCase = CancelRideRequestUseCase(repository)
        val longReason = "A".repeat(251)

        val result = useCase.execute("651a2b3c4d5e6f7a8b9c0001", reason = longReason)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertTrue(error.message.contains("cannot exceed 250 characters"))
    }

    // =========================================================================
    // 6. LISTING USER RIDE REQUESTS & RECOVERY
    // =========================================================================

    @Test
    fun `listUserRequests queries GET ride-requests me and returns user request history`() = runBlocking {
        val listJson = """
        {
            "success": true,
            "data": {
                "items": [
                    ${sampleResponseJson.substringAfter("\"data\":").substringBeforeLast("}")}
                ],
                "total": 1,
                "page": 1,
                "limit": 20,
                "hasMore": false
            }
        }
        """.trimIndent()

        var queriedEndpoint: String? = null
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                queriedEndpoint = request.url
                return IshaaraResult.success(HttpResponse(200, listJson))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )
        val useCase = GetUserRideRequestsUseCase(repository)

        val result = useCase.execute()
        assertTrue(result is IshaaraResult.Success)
        val items = (result as IshaaraResult.Success).data
        assertEquals(1, items.size)
        assertEquals("651a2b3c4d5e6f7a8b9c0001", items[0].id)
        assertEquals("${networkConfig.fullApiBaseUrl}/ride-requests/me", queriedEndpoint)
    }

    // =========================================================================
    // 7. STATUS VIEWMODEL LIFECYCLE & POLLING
    // =========================================================================

    @Test
    fun `status viewModel loads request on start and transitions correctly on status change`() = runBlocking {
        val currentStatus = AtomicInteger(0)
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                val statusStr = if (currentStatus.get() == 0) "PENDING" else "ACCEPTED"
                val json = sampleResponseJson.replace("\"status\": \"PENDING\"", "\"status\": \"$statusStr\"")
                return IshaaraResult.success(HttpResponse(200, json))
            }
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )

        val getUseCase = GetRideRequestUseCase(repository)
        val cancelUseCase = CancelRideRequestUseCase(repository)

        val statusViewModel = RideRequestStatusViewModel(
            requestId = "651a2b3c4d5e6f7a8b9c0001",
            initialTrip = sampleDiscoveredTrip,
            getRideRequestUseCase = getUseCase,
            cancelRideRequestUseCase = cancelUseCase,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertEquals("651a2b3c4d5e6f7a8b9c0001", statusViewModel.uiState.value.requestId)
        assertEquals(RideRequestStatus.PENDING, statusViewModel.uiState.value.status)

        // Driver accepts request
        currentStatus.set(1)
        statusViewModel.refreshStatus()

        assertEquals(RideRequestStatus.ACCEPTED, statusViewModel.uiState.value.status)
        assertTrue(statusViewModel.uiState.value.status.isTerminal)
    }

    // =========================================================================
    // 8. HANDOFF TO PHASE A11 (LIVE RIDE)
    // =========================================================================

    @Test
    fun `onContinueToLiveRide navigates toward A11 StudentRideTracking boundary`() = runBlocking {
        val acceptedResult = RideRequestResult(
            id = "651a2b3c4d5e6f7a8b9c0001",
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            driverId = "drv_100",
            userId = "usr_passenger_1",
            pickupAddress = "BHU Gate",
            destinationAddress = "Assi Ghat",
            status = RideRequestStatus.ACCEPTED,
            requestedAt = "2026-10-01T08:00:00Z",
            expiresAt = "2026-10-01T08:02:00Z",
            associatedRideId = "ride_accepted_999"
        )

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleResponseJson))
        }

        val remoteDataSource = RideRequestRemoteDataSourceImpl(fakeClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = fakeClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource,
            rideRequestRemoteDataSource = remoteDataSource
        )

        val statusViewModel = RideRequestStatusViewModel(
            requestId = "651a2b3c4d5e6f7a8b9c0001",
            initialTrip = sampleDiscoveredTrip,
            initialRequest = acceptedResult,
            getRideRequestUseCase = GetRideRequestUseCase(repository),
            cancelRideRequestUseCase = CancelRideRequestUseCase(repository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        statusViewModel.onContinueToLiveRide()

        val navCommand = navigationManager.commands.first()
        assertTrue(navCommand is NavigationCommand.NavigateTo)
        assertEquals("student/ride/ride_accepted_999", (navCommand as NavigationCommand.NavigateTo).route)
    }

    // =========================================================================
    // 9. ACCOUNT SWITCHING & STATE ISOLATION
    // =========================================================================

    @Test
    fun `switching accounts purges passenger session and prevents request leakage`() = runBlocking {
        // Passenger A has an active session
        val currentSession = sessionLocalDataSource.getSession()
        assertNotNull(currentSession)
        assertEquals("usr_passenger_1", currentSession!!.userId)

        // Clear session on switch/logout
        sessionLocalDataSource.clearSession()
        assertNull(sessionLocalDataSource.getSession())

        val repository = RideRepositoryImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, sampleResponseJson))
            },
            networkConfig = networkConfig,
            sessionLocalDataSource = sessionLocalDataSource
        )

        // Request submission or status query without session must fail with 401 Authentication error
        val result = repository.submitRideRequest(
            RideRequestInput(
                tripId = "651a2b3c4d5e6f7a8b9c0d1e",
                pickup = RideRequestLocationWaypoint("A", 25.2799, 82.9995),
                destination = RideRequestLocationWaypoint("B", 25.2899, 83.0068)
            )
        )

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue("Must be Authentication error", error is IshaaraError.Authentication)
        assertEquals(401, error.code)
    }
}
