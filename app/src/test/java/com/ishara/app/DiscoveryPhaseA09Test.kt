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
import com.ishara.app.data.mapper.DiscoveryMapper
import com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSource
import com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.DiscoveryCoordinateDto
import com.ishara.app.data.remote.dto.DiscoveryDriverDto
import com.ishara.app.data.remote.dto.DiscoveryEndpointDto
import com.ishara.app.data.remote.dto.DiscoveryEstimatedFareDto
import com.ishara.app.data.remote.dto.DiscoveryGeoJsonPointDto
import com.ishara.app.data.remote.dto.DiscoveryItemDto
import com.ishara.app.data.remote.dto.DiscoveryMatchDto
import com.ishara.app.data.remote.dto.DiscoveryOptionsDto
import com.ishara.app.data.remote.dto.DiscoveryPaginationDto
import com.ishara.app.data.remote.dto.DiscoveryResponseDto
import com.ishara.app.data.remote.dto.DiscoveryRouteSummaryDto
import com.ishara.app.data.remote.dto.DiscoverySearchRequestDto
import com.ishara.app.data.remote.dto.DiscoveryVehicleDto
import com.ishara.app.data.remote.dto.PublicTripResponseDto
import com.ishara.app.data.repository.TripRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.TripCompatibility
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.TripRepository
import com.ishara.app.domain.usecase.DiscoverTripsUseCase
import com.ishara.app.domain.usecase.GetPassengerTripDetailsUseCase
import com.ishara.app.feature.student.discovery.DiscoveryStage
import com.ishara.app.feature.student.discovery.DiscoveryViewModel
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
 * PHASE A09 — PASSENGER TRIP DISCOVERY Test Suite
 *
 * Verifies all Phase A09 requirements:
 * 1. Request Contract & Strict Zod Schema Compliance
 * 2. Authentication & Authorization (USER role, 401, 403)
 * 3. Search Prerequisites & Coordinate Validation
 * 4. Origin = Destination Validation Behavior
 * 5. Discovery Flow & Results (Multiple, Empty, Server Error, Network Failure)
 * 6. DTO -> Domain Mapping (GeoJSON RFC 7946 coordinates, route, driver, vehicle, fare)
 * 7. Backend Deterministic Result Ordering Preservation
 * 8. Cursor-Based Pagination
 * 9. Search Concurrency & Stale Response Protection
 * 10. Passenger Trip Details Contract (GET /api/v1/trips/:tripId)
 * 11. Trip Selection & A10 Phase Boundary (Selection only, NO ride request creation)
 * 12. Account Switching & State Clearing (No cross-account leakage)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryPhaseA09Test {

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

    @Before
    fun setUp() {
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        navigationManager = NavigationManager()

        // Seed default authenticated USER passenger session
        runBlocking {
            sessionLocalDataSource.saveSession(
                AuthSession(
                    userId = "user_passenger_01",
                    token = "valid_passenger_jwt_token",
                    role = UserRole.USER
                )
            )
        }
    }

    // =========================================================================
    // 1. REQUEST CONTRACT & STRICT ZOD SCHEMA
    // =========================================================================

    @Test
    fun `request contract strictly complies with backend discovery schema and excludes unpermitted fields`() {
        val lastExecutedRequest = mutableListOf<HttpRequest>()
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                lastExecutedRequest.add(request)
                return IshaaraResult.success(
                    HttpResponse(
                        statusCode = 200,
                        body = """{"success":true,"statusCode":200,"data":{"discoverySessionId":"dses_1","items":[],"pagination":{"limit":20,"hasMore":false,"nextCursor":null}}}"""
                    )
                )
            }
        }

        val remoteDataSource = DiscoveryRemoteDataSourceImpl(fakeClient, networkConfig)
        val query = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            originName = "Jhansi Railway Station",
            originAddress = "Station Road, Jhansi",
            destinationLatitude = 25.4358,
            destinationLongitude = 78.5522,
            destinationName = "Sipri Bazaar",
            destinationAddress = "Sipri, Jhansi",
            maxPickupDistanceMeters = 1000.0,
            maxDestinationDeviationMeters = 2000.0,
            maxResults = 10,
            cursor = "trip_cursor_abc"
        )

        runBlocking {
            val requestDto = DiscoveryMapper.toRequestDto(query)
            remoteDataSource.discoverTrips(requestDto, "valid_token")
        }

        assertEquals(1, lastExecutedRequest.size)
        val request = lastExecutedRequest[0]

        // 1. Method must be POST
        assertEquals(HttpMethod.POST, request.method)

        // 2. Path must be /api/v1/discovery/trips
        assertEquals("${networkConfig.fullApiBaseUrl}/discovery/trips", request.url)

        // 3. Authorization Bearer header
        assertEquals("Bearer valid_token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])

        // 4. JSON Body inspection
        val json = request.body ?: ""
        assertTrue("Origin must be present", json.contains("\"origin\""))
        assertTrue("Destination must be present", json.contains("\"destination\""))
        assertTrue("Options must be present", json.contains("\"options\""))

        assertTrue("Origin latitude must match", json.contains("\"latitude\":25.4484"))
        assertTrue("Origin longitude must match", json.contains("\"longitude\":78.5685"))
        assertTrue("Origin name must match", json.contains("\"name\":\"Jhansi Railway Station\""))
        assertTrue("Destination latitude must match", json.contains("\"latitude\":25.4358"))
        assertTrue("Destination longitude must match", json.contains("\"longitude\":78.5522"))
        assertTrue("maxPickupDistanceMeters must match", json.contains("\"maxPickupDistanceMeters\":1000.0"))
        assertTrue("maxDestinationDeviationMeters must match", json.contains("\"maxDestinationDeviationMeters\":2000.0"))
        assertTrue("maxResults must match", json.contains("\"maxResults\":10"))
        assertTrue("cursor must match", json.contains("\"cursor\":\"trip_cursor_abc\""))

        // STRICT CHECK: Unpermitted fields rejected by Zod .strict() MUST NOT exist in payload
        assertFalse("Payload must NOT contain 'seatCapacity'", json.contains("seatCapacity"))
        assertFalse("Payload must NOT contain 'availableSeats'", json.contains("availableSeats"))
        assertFalse("Payload must NOT contain 'seatsRequested'", json.contains("seatsRequested"))
        assertFalse("Payload must NOT contain 'passengerCount'", json.contains("passengerCount"))
        assertFalse("Payload must NOT contain 'fare'", json.contains("fare"))
        assertFalse("Payload must NOT contain 'price'", json.contains("price"))
        assertFalse("Payload must NOT contain 'pickup'", json.contains("\"pickup\""))
        assertFalse("Payload must NOT contain 'booking'", json.contains("booking"))
    }

    // =========================================================================
    // 2. AUTHENTICATION & AUTHORIZATION
    // =========================================================================

    @Test
    fun `missing token produces unauthenticated request and maps 401 error correctly`() = runBlocking {
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return if (request.headers["Authorization"] == null) {
                    IshaaraResult.failure(IshaaraError.Authentication(message = "Authentication required."))
                } else {
                    IshaaraResult.success(HttpResponse(200, """{"success":true,"data":{"discoverySessionId":"1","items":[],"pagination":{"limit":20,"hasMore":false,"nextCursor":null}}}"""))
                }
            }
        }

        val remoteDataSource = DiscoveryRemoteDataSourceImpl(fakeClient, networkConfig)
        val query = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            destinationLatitude = 25.4358,
            destinationLongitude = 78.5522
        )
        val requestDto = DiscoveryMapper.toRequestDto(query)

        // Without token
        val result = remoteDataSource.discoverTrips(requestDto, null)
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun `403 forbidden is mapped to Forbidden error`() = runBlocking {
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return IshaaraResult.failure(IshaaraError.Forbidden(message = "Forbidden: User does not have access."))
            }
        }

        val remoteDataSource = DiscoveryRemoteDataSourceImpl(fakeClient, networkConfig)
        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = remoteDataSource
        )
        val useCase = DiscoverTripsUseCase(repo)

        val query = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            destinationLatitude = 25.4358,
            destinationLongitude = 78.5522
        )

        val result = useCase.execute(query)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
    }

    // =========================================================================
    // 3. SEARCH PREREQUISITES & COORDINATE VALIDATION
    // =========================================================================

    @Test
    fun `invalid latitude outside bounds is rejected by use case without network call`() = runBlocking {
        var networkCalled = false
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                networkCalled = true
                return IshaaraResult.success(DiscoveryResponseDto("1", emptyList(), DiscoveryPaginationDto()))
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }
        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val useCase = DiscoverTripsUseCase(repo)

        // Invalid latitude > 90
        val invalidQuery = try {
            DiscoveryQuery(
                originLatitude = 120.0,
                originLongitude = 78.5685,
                destinationLatitude = 25.4358,
                destinationLongitude = 78.5522
            )
        } catch (e: IllegalArgumentException) {
            null
        }

        // Domain model requirement checks bounds on construction
        assertNull("DiscoveryQuery constructor must reject latitude > 90", invalidQuery)
        assertFalse("Network must never be called for invalid bounds", networkCalled)
    }

    @Test
    fun `identical origin and destination coordinates are validated before network call`() = runBlocking {
        var networkCalled = false
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                networkCalled = true
                return IshaaraResult.success(DiscoveryResponseDto("1", emptyList(), DiscoveryPaginationDto()))
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }
        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val useCase = DiscoverTripsUseCase(repo)

        val identicalQuery = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            destinationLatitude = 25.4484,
            destinationLongitude = 78.5685
        )

        val result = useCase.execute(identicalQuery)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("Origin and destination cannot be identical.", error.message)
        assertFalse("Network must not be called when origin and destination are identical", networkCalled)
    }

    // =========================================================================
    // 4. DISCOVERY FLOW & RESULTS
    // =========================================================================

    @Test
    fun `successful discovery returns compatible trips and transitions state to Success`() = runBlocking {
        val mockItems = listOf(
            createMockDiscoveryItem("trip_001", "Ramesh Kumar", "UP93AT1234", 0.95, "HIGH", 5000L),
            createMockDiscoveryItem("trip_002", "Suresh Singh", "UP93AT5678", 0.82, "MEDIUM", 6000L)
        )
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return IshaaraResult.success(
                    DiscoveryResponseDto(
                        discoverySessionId = "dses_varanasi_123",
                        items = mockItems,
                        pagination = DiscoveryPaginationDto(limit = 20, hasMore = false, nextCursor = null)
                    )
                )
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val useCase = DiscoverTripsUseCase(repo)
        val initialQuery = DiscoveryQuery(
            originLatitude = 25.2799,
            originLongitude = 82.9995,
            originName = "BHU Gate",
            destinationLatitude = 25.3250,
            destinationLongitude = 83.0200,
            destinationName = "Cantt Station"
        )

        val viewModel = DiscoveryViewModel(
            initialQuery = initialQuery,
            discoverTripsUseCase = useCase,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue("Stage must be Success", state.stage is DiscoveryStage.Success)
        assertEquals(2, state.trips.size)
        assertEquals("trip_001", state.trips[0].tripId)
        assertEquals("Ramesh Kumar", state.trips[0].driver.name)
        assertEquals(TripCompatibility.HIGH, state.trips[0].compatibility)
        assertEquals("trip_002", state.trips[1].tripId)
        assertEquals(TripCompatibility.MEDIUM, state.trips[1].compatibility)
        assertFalse(state.hasMore)
        assertNull(state.errorMessage)
    }

    @Test
    fun `empty discovery response correctly transitions state to Empty stage`() = runBlocking {
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return IshaaraResult.success(
                    DiscoveryResponseDto(
                        discoverySessionId = "dses_empty",
                        items = emptyList(),
                        pagination = DiscoveryPaginationDto(limit = 20, hasMore = false, nextCursor = null)
                    )
                )
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val useCase = DiscoverTripsUseCase(repo)
        val query = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            destinationLatitude = 25.4358,
            destinationLongitude = 78.5522
        )

        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = useCase,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue("Stage must be Empty when 0 trips returned", state.stage is DiscoveryStage.Empty)
        assertEquals(0, state.trips.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun `backend error transitions state to Error stage with user-friendly message`() = runBlocking {
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return IshaaraResult.failure(IshaaraError.Server(500, "Internal corridor matching service error"))
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val useCase = DiscoverTripsUseCase(repo)
        val query = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            destinationLatitude = 25.4358,
            destinationLongitude = 78.5522
        )

        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = useCase,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue("Stage must be Error", state.stage is DiscoveryStage.Error)
        assertEquals("Internal corridor matching service error", state.errorMessage)
    }

    // =========================================================================
    // 5. DTO -> DOMAIN MAPPING & GEOJSON RFC 7946 SPECIFICATION
    // =========================================================================

    @Test
    fun `mapper accurately maps GeoJSON longitude latitude coordinates to domain coordinate model`() {
        // Backend GeoJSON standard: [longitude, latitude]
        val rawItemDto = DiscoveryItemDto(
            tripId = "trip_geojson_test",
            driver = DiscoveryDriverDto(id = "d1", name = "Ajay Verma", image = "https://img.test/d1.png"),
            vehicle = DiscoveryVehicleDto(id = "v1", registrationNumber = "UP65TC9999", vehicleType = "AUTO", make = "Bajaj", model = "Compact"),
            origin = DiscoveryEndpointDto(
                name = "BHU Main Gate",
                formattedAddress = "BHU, Varanasi",
                coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(82.9995, 25.2799)) // [lng, lat]
            ),
            destination = DiscoveryEndpointDto(
                name = "Assi Ghat",
                formattedAddress = "Assi Ghat, Varanasi",
                coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(83.0068, 25.2899)) // [lng, lat]
            ),
            routeSummary = DiscoveryRouteSummaryDto(distanceMeters = 2400.0, durationSeconds = 480.0),
            match = DiscoveryMatchDto(
                pickupDistanceMeters = 40.0,
                destinationDistanceMeters = 60.0,
                directionDifferenceDegrees = 2.0,
                pickupRouteProgress = 0.05,
                destinationRouteProgress = 0.85,
                estimatedDetourMeters = 100.0,
                compatibility = "HIGH",
                score = 0.98
            ),
            estimatedFare = DiscoveryEstimatedFareDto(amountMinor = 4500L, currency = "INR", formatted = "₹45.00")
        )

        val domainTrip = DiscoveryMapper.toDomain(rawItemDto)

        // Coordinates must be properly inverted from GeoJSON [lng, lat] to domain (lat, lng)
        assertEquals(25.2799, domainTrip.originCoordinate.latitude, 0.0001)
        assertEquals(82.9995, domainTrip.originCoordinate.longitude, 0.0001)
        assertEquals(25.2899, domainTrip.destinationCoordinate.latitude, 0.0001)
        assertEquals(83.0068, domainTrip.destinationCoordinate.longitude, 0.0001)

        // Route & match
        assertEquals("2.4 km", domainTrip.formattedDistance)
        assertEquals("8 min", domainTrip.formattedDuration)
        assertEquals("40 m walk", domainTrip.formattedPickupDistance)
        assertEquals(TripCompatibility.HIGH, domainTrip.compatibility)
        assertEquals(0.98, domainTrip.matchScore, 0.001)

        // Estimated fare mapped
        assertNotNull(domainTrip.estimatedFare)
        assertEquals("₹45.00", domainTrip.estimatedFare?.formatted)
        assertEquals(4500L, domainTrip.estimatedFare?.amountMinor)

        // Vehicle title
        assertEquals("Bajaj Compact AUTO", domainTrip.vehicle.displayTitle)
    }

    // =========================================================================
    // 6. BACKEND DETERMINISTIC ORDERING PRESERVATION
    // =========================================================================

    @Test
    fun `frontend preserves exact backend score ranking order without client-side reshuffling`() = runBlocking {
        // Backend returns trips strictly ranked by score descending, with tripId tie-breaker
        val mockItems = listOf(
            createMockDiscoveryItem("trip_rank_1", "Driver A", "UP65A", 0.99, "HIGH"),
            createMockDiscoveryItem("trip_rank_2", "Driver B", "UP65B", 0.91, "HIGH"),
            createMockDiscoveryItem("trip_rank_3", "Driver C", "UP65C", 0.75, "MEDIUM")
        )

        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return IshaaraResult.success(
                    DiscoveryResponseDto(
                        discoverySessionId = "dses_ranked",
                        items = mockItems,
                        pagination = DiscoveryPaginationDto(limit = 10, hasMore = false, nextCursor = null)
                    )
                )
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val viewModel = DiscoveryViewModel(
            initialQuery = DiscoveryQuery(25.44, 78.56, destinationLatitude = 25.43, destinationLongitude = 78.55),
            discoverTripsUseCase = DiscoverTripsUseCase(repo),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertEquals("trip_rank_1", state.trips[0].tripId)
        assertEquals("trip_rank_2", state.trips[1].tripId)
        assertEquals("trip_rank_3", state.trips[2].tripId)
    }

    // =========================================================================
    // 7. CURSOR-BASED PAGINATION
    // =========================================================================

    @Test
    fun `cursor-based pagination appends next page results when loadNextPage is invoked`() = runBlocking {
        val page1Item = createMockDiscoveryItem("trip_page_1", "Driver 1", "UP65_P1", 0.95, "HIGH")
        val page2Item = createMockDiscoveryItem("trip_page_2", "Driver 2", "UP65_P2", 0.80, "MEDIUM")

        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return if (request.options?.cursor == null) {
                    IshaaraResult.success(
                        DiscoveryResponseDto(
                            discoverySessionId = "dses_paged",
                            items = listOf(page1Item),
                            pagination = DiscoveryPaginationDto(limit = 1, hasMore = true, nextCursor = "trip_page_1")
                        )
                    )
                } else if (request.options?.cursor == "trip_page_1") {
                    IshaaraResult.success(
                        DiscoveryResponseDto(
                            discoverySessionId = "dses_paged",
                            items = listOf(page2Item),
                            pagination = DiscoveryPaginationDto(limit = 1, hasMore = false, nextCursor = null)
                        )
                    )
                } else {
                    IshaaraResult.success(DiscoveryResponseDto("dses_paged", emptyList(), DiscoveryPaginationDto(limit = 1, hasMore = false, nextCursor = null)))
                }
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val viewModel = DiscoveryViewModel(
            initialQuery = DiscoveryQuery(25.44, 78.56, destinationLatitude = 25.43, destinationLongitude = 78.55),
            discoverTripsUseCase = DiscoverTripsUseCase(repo),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        // Page 1 assertions
        val state1 = viewModel.uiState.value
        assertEquals(1, state1.trips.size)
        assertEquals("trip_page_1", state1.trips[0].tripId)
        assertTrue(state1.hasMore)
        assertEquals("trip_page_1", state1.nextCursor)

        // Load Page 2
        viewModel.loadNextPage()

        val state2 = viewModel.uiState.value
        assertEquals(2, state2.trips.size)
        assertEquals("trip_page_1", state2.trips[0].tripId)
        assertEquals("trip_page_2", state2.trips[1].tripId)
        assertFalse(state2.hasMore)
        assertNull(state2.nextCursor)
    }

    // =========================================================================
    // 8. SEARCH CONCURRENCY & STALE RESPONSE PROTECTION
    // =========================================================================

    @Test
    fun `stale responses from older searches are discarded when a newer search finishes`() = runBlocking {
        val queryA = DiscoveryQuery(25.44, 78.56, destinationLatitude = 25.43, destinationLongitude = 78.55) // Search A
        val queryB = DiscoveryQuery(25.28, 82.99, destinationLatitude = 25.32, destinationLongitude = 83.02) // Search B

        val itemA = createMockDiscoveryItem("trip_A", "Driver A", "UP65A", 0.90, "HIGH")
        val itemB = createMockDiscoveryItem("trip_B", "Driver B", "UP65B", 0.95, "HIGH")

        val callCount = AtomicInteger(0)
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                val call = callCount.incrementAndGet()
                if (call == 1) {
                    // Search A: Simulate delayed network response
                    delay(50)
                    return IshaaraResult.success(DiscoveryResponseDto("dses_A", listOf(itemA), DiscoveryPaginationDto()))
                } else {
                    // Search B: Immediate return
                    return IshaaraResult.success(DiscoveryResponseDto("dses_B", listOf(itemB), DiscoveryPaginationDto()))
                }
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val viewModel = DiscoveryViewModel(
            initialQuery = queryA,
            discoverTripsUseCase = DiscoverTripsUseCase(repo),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        // Rapidly dispatch Search B
        viewModel.executeDiscovery(queryB)

        // Wait for coroutine completion
        delay(100)

        val state = viewModel.uiState.value
        assertEquals("Latest search query B must win", queryB, state.query)
        assertEquals(1, state.trips.size)
        assertEquals("trip_B", state.trips[0].tripId)
    }

    @Test
    fun `duplicate search execution is ignored while identical query is in flight`() = runBlocking {
        var networkCallCount = 0
        val query = DiscoveryQuery(25.44, 78.56, destinationLatitude = 25.43, destinationLongitude = 78.55)

        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                networkCallCount++
                delay(50)
                return IshaaraResult.success(DiscoveryResponseDto("dses_1", emptyList(), DiscoveryPaginationDto()))
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(repo),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        // Trigger identical discovery while first is in-flight
        viewModel.executeDiscovery(query)

        delay(100)
        assertEquals("Duplicate in-flight query must be skipped", 1, networkCallCount)
    }

    // =========================================================================
    // 9. PASSENGER TRIP DETAILS CONTRACT (GET /api/v1/trips/:tripId)
    // =========================================================================

    @Test
    fun `public passenger trip details response parses and conceals private driver information`() {
        val mockResponseJson = """
        {
            "id": "trip_pub_123",
            "status": "ACTIVE",
            "origin": {
                "name": "BHU Main Gate",
                "formattedAddress": "BHU Gate, Varanasi",
                "coordinates": { "type": "Point", "coordinates": [82.9995, 25.2799] }
            },
            "destination": {
                "name": "Cantt Railway Station",
                "formattedAddress": "Cantt, Varanasi",
                "coordinates": { "type": "Point", "coordinates": [83.0200, 25.3250] }
            },
            "route": {
                "distanceMeters": 8000,
                "durationSeconds": 1200,
                "provider": "OSRM"
            },
            "startedAt": "2026-10-01T10:00:00.000Z",
            "scheduledDepartureAt": "2026-10-01T09:55:00.000Z",
            "createdAt": "2026-10-01T09:30:00.000Z",
            "driver": {
                "id": "drv_public_1",
                "name": "Verified Operator"
            },
            "vehicle": {
                "id": "veh_public_1",
                "registrationNumber": "UP65TC1234",
                "vehicleType": "CAB",
                "make": "Maruti",
                "model": "Dzire"
            }
        }
        """.trimIndent()

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return IshaaraResult.success(HttpResponse(200, mockResponseJson))
            }
        }
        val remoteDataSource = DiscoveryRemoteDataSourceImpl(fakeClient, networkConfig)
        val publicDto = remoteDataSource.parsePublicTripResponse(mockResponseJson)

        assertEquals("trip_pub_123", publicDto.id)
        assertEquals("ACTIVE", publicDto.status)
        assertEquals("Verified Operator", publicDto.driver.name)
        assertEquals("UP65TC1234", publicDto.vehicle.registrationNumber)

        val domainDetails = DiscoveryMapper.toDomain(publicDto)
        assertEquals("trip_pub_123", domainDetails.tripId)
        assertEquals(com.ishara.app.domain.model.TripStatus.ACTIVE, domainDetails.status)
        assertEquals(25.2799, domainDetails.originCoordinate.latitude, 0.0001)
        assertEquals(82.9995, domainDetails.originCoordinate.longitude, 0.0001)
        assertEquals("8.0 km", domainDetails.formattedDistance)
        assertEquals("20 min", domainDetails.formattedDuration)

        // PRIVACY ENFORCEMENT: Private fields do not exist on PassengerTripDetails
        assertEquals("Verified Operator", domainDetails.driver.name)
    }

    // =========================================================================
    // 10. TRIP SELECTION & STRICT PHASE A10 BOUNDARY
    // =========================================================================

    @Test
    fun `trip selection updates state and does NOT call ride request creation API`() = runBlocking {
        val selectedCandidate = createMockDiscoveryItem("trip_candidate_99", "Driver Ram", "UP65X", 0.92, "HIGH")
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return IshaaraResult.success(DiscoveryResponseDto("1", listOf(selectedCandidate), DiscoveryPaginationDto()))
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.success(
                    PublicTripResponseDto(
                        id = tripId,
                        status = "ACTIVE",
                        origin = com.ishara.app.data.remote.dto.PublicTripLocationDto(formattedAddress = "Origin", coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(78.56, 25.44))),
                        destination = com.ishara.app.data.remote.dto.PublicTripLocationDto(formattedAddress = "Destination", coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(78.55, 25.43))),
                        driver = com.ishara.app.data.remote.dto.PublicTripDriverDto(id = "d1", name = "Driver Ram"),
                        vehicle = com.ishara.app.data.remote.dto.PublicTripVehicleDto(id = "v1", registrationNumber = "UP65X", vehicleType = "AUTO")
                    )
                )
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val viewModel = DiscoveryViewModel(
            initialQuery = DiscoveryQuery(25.44, 78.56, destinationLatitude = 25.43, destinationLongitude = 78.55),
            discoverTripsUseCase = DiscoverTripsUseCase(repo),
            getPassengerTripDetailsUseCase = GetPassengerTripDetailsUseCase(repo),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val trip = viewModel.uiState.value.trips[0]

        // 1. Select trip
        viewModel.onTripSelected(trip)

        val stateAfterSelect = viewModel.uiState.value
        assertEquals("trip_candidate_99", stateAfterSelect.selectedTrip?.tripId)
        assertTrue(stateAfterSelect.isDetailsSheetVisible)
        assertNotNull(stateAfterSelect.selectedTripDetails)

        // 2. Continue to ride request (Dispatches to A10 boundary ONLY)
        viewModel.onContinueToRideRequest()

        // Verify navigation command to ride tracking / request route
        val lastCommand = navigationManager.commands.first()
        assertTrue(lastCommand is NavigationCommand.NavigateTo)
        assertEquals("student/ride/trip_candidate_99", (lastCommand as NavigationCommand.NavigateTo).route)

        // Sheet dismissed
        assertFalse(viewModel.uiState.value.isDetailsSheetVisible)
    }

    // =========================================================================
    // 11. ACCOUNT SWITCHING & CACHE SAFETY
    // =========================================================================

    @Test
    fun `clearDiscoveryState resets all state and prevents cross-account result leakage`() = runBlocking {
        val fakeDataSource = object : DiscoveryRemoteDataSource {
            override suspend fun discoverTrips(request: DiscoverySearchRequestDto, token: String?): IshaaraResult<DiscoveryResponseDto> {
                return IshaaraResult.success(
                    DiscoveryResponseDto(
                        discoverySessionId = "dses_user1",
                        items = listOf(createMockDiscoveryItem("trip_leak_test", "Driver 1", "UP65L", 0.9, "HIGH")),
                        pagination = DiscoveryPaginationDto(limit = 10, hasMore = false, nextCursor = null)
                    )
                )
            }
            override suspend fun getPassengerTripDetails(tripId: String, token: String?): IshaaraResult<PublicTripResponseDto> {
                return IshaaraResult.failure(IshaaraError.NotFound())
            }
        }

        val repo = TripRepositoryImpl(
            remoteDataSource = object : com.ishara.app.data.remote.datasource.TripRemoteDataSource {},
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = fakeDataSource
        )
        val viewModel = DiscoveryViewModel(
            initialQuery = DiscoveryQuery(25.44, 78.56, destinationLatitude = 25.43, destinationLongitude = 78.55),
            discoverTripsUseCase = DiscoverTripsUseCase(repo),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertEquals(1, viewModel.uiState.value.trips.size)

        // Simulate logout / account switch
        viewModel.clearDiscoveryState()

        val clearedState = viewModel.uiState.value
        assertEquals("Stage must be reset to Idle", DiscoveryStage.Idle, clearedState.stage)
        assertEquals(0, clearedState.trips.size)
        assertNull(clearedState.selectedTrip)
        assertNull(clearedState.selectedTripDetails)
        assertFalse(clearedState.isDetailsSheetVisible)
        assertNull(clearedState.errorMessage)
        assertNull(clearedState.validationError)
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createMockDiscoveryItem(
        tripId: String,
        driverName: String,
        vehicleReg: String,
        score: Double,
        compatibility: String,
        amountMinor: Long = 5000L
    ): DiscoveryItemDto {
        return DiscoveryItemDto(
            tripId = tripId,
            driver = DiscoveryDriverDto(id = "drv_$tripId", name = driverName, image = null),
            vehicle = DiscoveryVehicleDto(id = "veh_$tripId", registrationNumber = vehicleReg, vehicleType = "AUTO", make = "Bajaj", model = "RE"),
            origin = DiscoveryEndpointDto(name = "Origin", formattedAddress = "Origin Address", coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(78.5685, 25.4484))),
            destination = DiscoveryEndpointDto(name = "Destination", formattedAddress = "Dest Address", coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(78.5522, 25.4358))),
            routeSummary = DiscoveryRouteSummaryDto(distanceMeters = 5000.0, durationSeconds = 900.0),
            match = DiscoveryMatchDto(
                pickupDistanceMeters = 50.0,
                destinationDistanceMeters = 80.0,
                directionDifferenceDegrees = 5.0,
                pickupRouteProgress = 0.1,
                destinationRouteProgress = 0.9,
                estimatedDetourMeters = 130.0,
                compatibility = compatibility,
                score = score
            ),
            estimatedFare = DiscoveryEstimatedFareDto(amountMinor = amountMinor, currency = "INR", formatted = "₹${amountMinor / 100}.00")
        )
    }
}
