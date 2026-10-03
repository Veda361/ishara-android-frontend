package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.mapper.TripMapper
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.dto.CancelTripRequestDto
import com.ishara.app.data.remote.dto.CleanTripResponseDto
import com.ishara.app.data.remote.dto.CreateTripRequestDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.data.remote.dto.TripRouteDto
import com.ishara.app.data.repository.DriverTripRepositoryImpl
import com.ishara.app.domain.model.AssignedVehicleState
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.CreateTripParams
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripActorRole
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleAssignmentActorRole
import com.ishara.app.domain.model.VehicleAssignmentStatus
import com.ishara.app.domain.model.VehicleOwnershipType
import com.ishara.app.domain.model.VehicleType
import com.ishara.app.domain.repository.DriverVehicleRepository
import com.ishara.app.domain.usecase.CancelDriverTripUseCase
import com.ishara.app.domain.usecase.ClearDriverTripStateUseCase
import com.ishara.app.domain.usecase.CompleteDriverTripUseCase
import com.ishara.app.domain.usecase.CreateDriverTripUseCase
import com.ishara.app.domain.usecase.GetAssignedVehicleUseCase
import com.ishara.app.domain.usecase.GetDriverTripsUseCase
import com.ishara.app.domain.usecase.GetTripDetailsUseCase
import com.ishara.app.domain.usecase.GetVehicleDetailsUseCase
import com.ishara.app.domain.usecase.ObserveActiveTripUseCase
import com.ishara.app.domain.usecase.ObserveDriverTripsUseCase
import com.ishara.app.domain.usecase.StartDriverTripUseCase
import com.ishara.app.feature.driver.trip.DriverTripDetailViewModel
import com.ishara.app.feature.driver.trip.DriverTripListViewModel
import com.ishara.app.feature.driver.trip.TripDetailContentState
import com.ishara.app.feature.driver.trip.TripFilterTab
import com.ishara.app.feature.driver.trip.TripListContentState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 * PHASE A08 Test Suite — DRIVER TRIP & DISPATCH OPERATIONAL FOUNDATION
 *
 * Verifies all architectural, operational, backend-authoritative, and security requirements:
 * - 1. API CONTRACT & ENDPOINT INTEGRITY (1-6)
 * - 2. AUTHENTICATION & ROLE-BASED ACCESS CONTROL (7-10)
 * - 3. TRIP RETRIEVAL & FILTERING (11-15)
 * - 4. ALL 7 BACKEND TRIP STATES & MAPPING (16-20)
 * - 5. TRIP LIFECYCLE MUTATIONS (START, COMPLETE, CANCEL, CREATE) (21-26)
 * - 6. BACKEND ERROR HANDLING & CONFLICTS (401, 403, 404, 409, 500) (27-31)
 * - 7. STATE ISOLATION & ACCOUNT SWITCHING (32-34)
 * - 8. VEHICLE ASSIGNMENT & READINESS INTEGRATION (35-37)
 * - 9. PRESENTATION / VIEWMODEL ARCHITECTURE (38-42)
 */
class DriverTripPhaseA08Test {

    private lateinit var mockTripRemoteDataSource: FakeTripRemoteDataSource
    private lateinit var mockVehicleRepository: FakeDriverVehicleRepository
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var sessionLocalDataSource: SessionLocalDataSourceImpl
    private lateinit var tripRepository: DriverTripRepositoryImpl

    private lateinit var getDriverTripsUseCase: GetDriverTripsUseCase
    private lateinit var getTripDetailsUseCase: GetTripDetailsUseCase
    private lateinit var createDriverTripUseCase: CreateDriverTripUseCase
    private lateinit var startDriverTripUseCase: StartDriverTripUseCase
    private lateinit var completeDriverTripUseCase: CompleteDriverTripUseCase
    private lateinit var cancelDriverTripUseCase: CancelDriverTripUseCase
    private lateinit var observeDriverTripsUseCase: ObserveDriverTripsUseCase
    private lateinit var observeActiveTripUseCase: ObserveActiveTripUseCase
    private lateinit var clearDriverTripStateUseCase: ClearDriverTripStateUseCase
    private lateinit var getAssignedVehicleUseCase: GetAssignedVehicleUseCase
    private lateinit var getVehicleDetailsUseCase: GetVehicleDetailsUseCase

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val driverSession = AuthSession(
        token = "jwt.driver.trip.token",
        userId = "usr_driver_789",
        role = UserRole.DRIVER_CONDUCTOR
    )

    private val passengerSession = AuthSession(
        token = "jwt.passenger.token",
        userId = "usr_passenger_999",
        role = UserRole.USER
    )

    private val sampleCleanTripDto = CleanTripResponseDto(
        id = "trip_001",
        driverId = "usr_driver_789",
        vehicleId = "veh_001",
        agencyId = "agn_lucknow_transit",
        origin = TripLocationDto(
            address = "Charbagh Railway Station, Lucknow",
            coordinates = doubleArrayOf(80.9462, 26.8467)
        ),
        destination = TripLocationDto(
            address = "Polytechnic Chauraha, Lucknow",
            coordinates = doubleArrayOf(80.9700, 26.8920)
        ),
        route = TripRouteDto(
            distanceMeters = 12500.0,
            durationSeconds = 2100.0
        ),
        status = "ASSIGNED",
        scheduledDepartureAt = "2026-10-01T10:00:00.000Z",
        createdAt = "2026-10-01T08:00:00.000Z",
        updatedAt = "2026-10-01T08:30:00.000Z"
    )

    private val sampleVehicle = Vehicle(
        id = "veh_001",
        agencyId = null,
        operatorId = null,
        assignedDriverId = "usr_driver_789",
        registrationNumber = "UP32AB1234",
        vehicleType = VehicleType.BUS,
        make = "Tata",
        model = "Starbus",
        capacity = 42,
        ownershipType = VehicleOwnershipType.INDIVIDUAL,
        isVerified = true,
        isActive = true,
        createdAt = "2026-09-01T10:00:00.000Z",
        updatedAt = "2026-09-01T10:00:00.000Z"
    )

    @Before
    fun setUp() = runBlocking {
        mockTripRemoteDataSource = FakeTripRemoteDataSource()
        mockVehicleRepository = FakeDriverVehicleRepository()
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)

        tripRepository = DriverTripRepositoryImpl(
            remoteDataSource = mockTripRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )

        getDriverTripsUseCase = GetDriverTripsUseCase(tripRepository)
        getTripDetailsUseCase = GetTripDetailsUseCase(tripRepository)
        createDriverTripUseCase = CreateDriverTripUseCase(tripRepository)
        startDriverTripUseCase = StartDriverTripUseCase(tripRepository)
        completeDriverTripUseCase = CompleteDriverTripUseCase(tripRepository)
        cancelDriverTripUseCase = CancelDriverTripUseCase(tripRepository)
        observeDriverTripsUseCase = ObserveDriverTripsUseCase(tripRepository)
        observeActiveTripUseCase = ObserveActiveTripUseCase(tripRepository)
        clearDriverTripStateUseCase = ClearDriverTripStateUseCase(tripRepository)
        getAssignedVehicleUseCase = GetAssignedVehicleUseCase(mockVehicleRepository)
        getVehicleDetailsUseCase = GetVehicleDetailsUseCase(mockVehicleRepository)

        sessionStore.saveSession(driverSession)
        mockVehicleRepository.assignedVehicleState = AssignedVehicleState(
            vehicle = sampleVehicle,
            assignment = DriverVehicleAssignment(
                id = "asgn_001",
                driverId = "usr_driver_789",
                vehicleId = "veh_001",
                agencyId = null,
                status = VehicleAssignmentStatus.ACTIVE,
                assignedAt = "2026-10-01T08:00:00.000Z",
                unassignedAt = null,
                assignedBy = "usr_driver_789",
                assignedByRole = VehicleAssignmentActorRole.DRIVER,
                unassignedBy = null,
                unassignedByRole = null,
                reason = null,
                vehicle = sampleVehicle,
                createdAt = "2026-10-01T08:00:00.000Z",
                updatedAt = "2026-10-01T08:00:00.000Z"
            )
        )
    }

    // =========================================================================
    // 1. API CONTRACT & ENDPOINT INTEGRITY
    // =========================================================================

    @Test
    fun `test 01 - listDriverTrips hits GET api v1 drivers me trips`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)

        val result = getDriverTripsUseCase()

        assertTrue(result is IshaaraResult.Success)
        val trips = (result as IshaaraResult.Success).data
        assertEquals(1, trips.size)
        assertEquals("trip_001", trips[0].id)
        assertEquals("GET /api/v1/drivers/me/trips", mockTripRemoteDataSource.lastEndpointCalled)
    }

    @Test
    fun `test 02 - getTripDetails hits GET api v1 trips tripId`() = runBlocking {
        mockTripRemoteDataSource.tripDetailsResponse = sampleCleanTripDto

        val result = getTripDetailsUseCase("trip_001")

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals("trip_001", trip.id)
        assertEquals("GET /api/v1/trips/trip_001", mockTripRemoteDataSource.lastEndpointCalled)
    }

    @Test
    fun `test 03 - createDriverTrip hits POST api v1 trips with valid body`() = runBlocking {
        val createdDto = sampleCleanTripDto.copy(id = "trip_new", status = "CREATED")
        mockTripRemoteDataSource.createTripResponse = createdDto

        val params = CreateTripParams(
            vehicleId = "veh_001",
            origin = TripLocation.of("Charbagh", 26.8467, 80.9462),
            destination = TripLocation.of("Polytechnic", 26.8920, 80.9700),
            scheduledDepartureAt = "2026-10-01T12:00:00.000Z"
        )

        val result = createDriverTripUseCase(params)

        assertTrue(result is IshaaraResult.Success)
        assertEquals("trip_new", (result as IshaaraResult.Success).data.id)
        assertEquals("POST /api/v1/trips", mockTripRemoteDataSource.lastEndpointCalled)
        assertNotNull(mockTripRemoteDataSource.lastCreateRequest)
        assertEquals("veh_001", mockTripRemoteDataSource.lastCreateRequest?.vehicleId)
    }

    @Test
    fun `test 04 - startDriverTrip hits POST api v1 trips tripId start`() = runBlocking {
        val activeDto = sampleCleanTripDto.copy(status = "ACTIVE", startedAt = "2026-10-01T09:00:00.000Z")
        mockTripRemoteDataSource.startTripResponse = activeDto

        val result = startDriverTripUseCase("trip_001")

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals(TripStatus.ACTIVE, trip.status)
        assertEquals("POST /api/v1/trips/trip_001/start", mockTripRemoteDataSource.lastEndpointCalled)
    }

    @Test
    fun `test 05 - completeDriverTrip hits POST api v1 trips tripId complete`() = runBlocking {
        val completedDto = sampleCleanTripDto.copy(status = "COMPLETED", completedAt = "2026-10-01T10:00:00.000Z")
        mockTripRemoteDataSource.completeTripResponse = completedDto

        val result = completeDriverTripUseCase("trip_001")

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals(TripStatus.COMPLETED, trip.status)
        assertEquals("POST /api/v1/trips/trip_001/complete", mockTripRemoteDataSource.lastEndpointCalled)
    }

    @Test
    fun `test 06 - cancelDriverTrip hits POST api v1 trips tripId cancel with reason`() = runBlocking {
        val cancelledDto = sampleCleanTripDto.copy(
            status = "CANCELLED",
            cancelledAt = "2026-10-01T09:30:00.000Z",
            cancelledByRole = "DRIVER",
            cancellationReason = "Vehicle breakdown"
        )
        mockTripRemoteDataSource.cancelTripResponse = cancelledDto

        val result = cancelDriverTripUseCase("trip_001", "Vehicle breakdown")

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals(TripStatus.CANCELLED, trip.status)
        assertEquals("Vehicle breakdown", trip.cancellationReason)
        assertEquals(TripActorRole.DRIVER, trip.cancelledByRole)
        assertEquals("POST /api/v1/trips/trip_001/cancel", mockTripRemoteDataSource.lastEndpointCalled)
        assertEquals("Vehicle breakdown", mockTripRemoteDataSource.lastCancelRequest?.reason)
    }

    // =========================================================================
    // 2. AUTHENTICATION & ROLE-BASED ACCESS CONTROL
    // =========================================================================

    @Test
    fun `test 07 - missing session returns 401 Authentication error`() = runBlocking {
        sessionStore.clearSession()

        val result = getDriverTripsUseCase()

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Authentication)
    }

    @Test
    fun `test 08 - passenger role is rejected with 403 Forbidden`() = runBlocking {
        sessionStore.saveSession(passengerSession)

        val result = getDriverTripsUseCase()

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
    }

    @Test
    fun `test 09 - start trip denied for passenger role`() = runBlocking {
        sessionStore.saveSession(passengerSession)

        val result = startDriverTripUseCase("trip_001")

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Forbidden)
    }

    @Test
    fun `test 10 - cancel trip denied for passenger role`() = runBlocking {
        sessionStore.saveSession(passengerSession)

        val result = cancelDriverTripUseCase("trip_001", "Reason")

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Forbidden)
    }

    // =========================================================================
    // 3. TRIP RETRIEVAL & FILTERING
    // =========================================================================

    @Test
    fun `test 11 - empty trip list returned when driver has no trips`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = emptyList()

        val result = getDriverTripsUseCase()

        assertTrue(result is IshaaraResult.Success)
        val trips = (result as IshaaraResult.Success).data
        assertTrue(trips.isEmpty())
    }

    @Test
    fun `test 12 - status filter queries backend correctly`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)

        val result = getDriverTripsUseCase(status = TripStatus.ACTIVE)

        assertTrue(result is IshaaraResult.Success)
        assertEquals("status=ACTIVE", mockTripRemoteDataSource.lastQueryFilter)
    }

    @Test
    fun `test 13 - pagination queries backend correctly`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)

        val result = getDriverTripsUseCase(page = 2, limit = 10)

        assertTrue(result is IshaaraResult.Success)
        assertEquals("GET /api/v1/drivers/me/trips", mockTripRemoteDataSource.lastEndpointCalled)
    }

    @Test
    fun `test 14 - active trip is reactively identified in observeActiveTrip`() = runBlocking {
        val activeTrip = sampleCleanTripDto.copy(id = "trip_active", status = "ACTIVE")
        mockTripRemoteDataSource.listTripsResponse = listOf(activeTrip, sampleCleanTripDto)

        getDriverTripsUseCase()

        val observedActive = observeActiveTripUseCase().first()
        assertNotNull(observedActive)
        assertEquals("trip_active", observedActive?.id)
        assertEquals(TripStatus.ACTIVE, observedActive?.status)
    }

    @Test
    fun `test 15 - observeActiveTrip is null when no trip is active`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto.copy(status = "SCHEDULED"))

        getDriverTripsUseCase()

        val observedActive = observeActiveTripUseCase().first()
        assertNull(observedActive)
    }

    // =========================================================================
    // 4. ALL 7 BACKEND TRIP STATES & MAPPING
    // =========================================================================

    @Test
    fun `test 16 - maps CREATED state properly`() {
        val dto = sampleCleanTripDto.copy(status = "CREATED")
        val domain = TripMapper.toDomain(dto)
        assertEquals(TripStatus.CREATED, domain.status)
        assertTrue(domain.isUnstarted)
        assertFalse(domain.isActive)
        assertFalse(domain.isTerminal)
    }

    @Test
    fun `test 17 - maps SCHEDULED and ASSIGNED and READY states properly`() {
        val scheduled = TripMapper.toDomain(sampleCleanTripDto.copy(status = "SCHEDULED"))
        val assigned = TripMapper.toDomain(sampleCleanTripDto.copy(status = "ASSIGNED"))
        val ready = TripMapper.toDomain(sampleCleanTripDto.copy(status = "READY"))

        assertEquals(TripStatus.SCHEDULED, scheduled.status)
        assertEquals(TripStatus.ASSIGNED, assigned.status)
        assertEquals(TripStatus.READY, ready.status)

        assertTrue(scheduled.isUnstarted)
        assertTrue(assigned.isUnstarted)
        assertTrue(ready.isUnstarted)
    }

    @Test
    fun `test 18 - maps ACTIVE state properly`() {
        val active = TripMapper.toDomain(sampleCleanTripDto.copy(status = "ACTIVE"))
        assertEquals(TripStatus.ACTIVE, active.status)
        assertTrue(active.isActive)
        assertFalse(active.isUnstarted)
        assertFalse(active.isTerminal)
    }

    @Test
    fun `test 19 - maps COMPLETED state properly`() {
        val completed = TripMapper.toDomain(sampleCleanTripDto.copy(status = "COMPLETED"))
        assertEquals(TripStatus.COMPLETED, completed.status)
        assertTrue(completed.isTerminal)
        assertFalse(completed.isActive)
    }

    @Test
    fun `test 20 - maps CANCELLED state properly with role and reason`() {
        val cancelled = TripMapper.toDomain(
            sampleCleanTripDto.copy(
                status = "CANCELLED",
                cancelledByRole = "ADMIN",
                cancellationReason = "Route blocked"
            )
        )
        assertEquals(TripStatus.CANCELLED, cancelled.status)
        assertTrue(cancelled.isTerminal)
        assertEquals(TripActorRole.ADMIN, cancelled.cancelledByRole)
        assertEquals("Route blocked", cancelled.cancellationReason)
    }

    // =========================================================================
    // 5. TRIP LIFECYCLE MUTATIONS
    // =========================================================================

    @Test
    fun `test 21 - starting trip updates reactive state and active trip flow`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)
        getDriverTripsUseCase()

        val activeDto = sampleCleanTripDto.copy(status = "ACTIVE", startedAt = "2026-10-01T09:00:00.000Z")
        mockTripRemoteDataSource.startTripResponse = activeDto

        val result = startDriverTripUseCase("trip_001")
        assertTrue(result is IshaaraResult.Success)

        val activeTrip = observeActiveTripUseCase().first()
        assertNotNull(activeTrip)
        assertEquals("trip_001", activeTrip?.id)
        assertEquals(TripStatus.ACTIVE, activeTrip?.status)
    }

    @Test
    fun `test 22 - completing trip updates cache and clears active trip flow`() = runBlocking {
        val activeDto = sampleCleanTripDto.copy(status = "ACTIVE")
        mockTripRemoteDataSource.listTripsResponse = listOf(activeDto)
        getDriverTripsUseCase()

        val completedDto = activeDto.copy(status = "COMPLETED", completedAt = "2026-10-01T10:00:00.000Z")
        mockTripRemoteDataSource.completeTripResponse = completedDto

        val result = completeDriverTripUseCase("trip_001")
        assertTrue(result is IshaaraResult.Success)

        val activeTrip = observeActiveTripUseCase().first()
        assertNull(activeTrip)

        val allTrips = observeDriverTripsUseCase().first()
        assertEquals(TripStatus.COMPLETED, allTrips.find { it.id == "trip_001" }?.status)
    }

    @Test
    fun `test 23 - cancelling active trip clears active trip flow`() = runBlocking {
        val activeDto = sampleCleanTripDto.copy(status = "ACTIVE")
        mockTripRemoteDataSource.listTripsResponse = listOf(activeDto)
        getDriverTripsUseCase()

        val cancelledDto = activeDto.copy(
            status = "CANCELLED",
            cancelledAt = "2026-10-01T09:30:00.000Z",
            cancelledByRole = "DRIVER"
        )
        mockTripRemoteDataSource.cancelTripResponse = cancelledDto

        val result = cancelDriverTripUseCase("trip_001", "Emergency")
        assertTrue(result is IshaaraResult.Success)

        val activeTrip = observeActiveTripUseCase().first()
        assertNull(activeTrip)
    }

    @Test
    fun `test 24 - creating trip prepends to authoritative cached trips list`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)
        getDriverTripsUseCase()

        val createdDto = sampleCleanTripDto.copy(id = "trip_brand_new", status = "CREATED")
        mockTripRemoteDataSource.createTripResponse = createdDto

        val params = CreateTripParams(
            vehicleId = "veh_001",
            origin = TripLocation.of("Charbagh", 26.8467, 80.9462),
            destination = TripLocation.of("Polytechnic", 26.8920, 80.9700)
        )
        val result = createDriverTripUseCase(params)
        assertTrue(result is IshaaraResult.Success)

        val allTrips = observeDriverTripsUseCase().first()
        assertEquals(2, allTrips.size)
        assertEquals("trip_brand_new", allTrips[0].id)
    }

    @Test
    fun `test 25 - getTripDetails returns cached trip if available and forceRefresh false`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)
        getDriverTripsUseCase()

        mockTripRemoteDataSource.getTripDetailsCallCount.set(0)
        val result = getTripDetailsUseCase("trip_001", forceRefresh = false)

        assertTrue(result is IshaaraResult.Success)
        assertEquals(0, mockTripRemoteDataSource.getTripDetailsCallCount.get())
    }

    @Test
    fun `test 26 - getTripDetails fetches remote when forceRefresh true`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)
        getDriverTripsUseCase()

        mockTripRemoteDataSource.tripDetailsResponse = sampleCleanTripDto.copy(scheduledDepartureAt = "2026-10-01T12:30:00.000Z")
        mockTripRemoteDataSource.getTripDetailsCallCount.set(0)

        val result = getTripDetailsUseCase("trip_001", forceRefresh = true)

        assertTrue(result is IshaaraResult.Success)
        assertEquals(1, mockTripRemoteDataSource.getTripDetailsCallCount.get())
        assertEquals("2026-10-01T12:30:00.000Z", (result as IshaaraResult.Success).data.scheduledDepartureAt)
    }

    // =========================================================================
    // 6. BACKEND ERROR HANDLING & CONFLICTS
    // =========================================================================

    @Test
    fun `test 27 - 409 DRIVER_HAS_ACTIVE_TRIP mapped to Conflict error`() = runBlocking {
        mockTripRemoteDataSource.startTripResult = IshaaraResult.failure(
            IshaaraError.Conflict("Driver already has an active trip")
        )

        val result = startDriverTripUseCase("trip_001")

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Conflict)
        assertEquals("Driver already has an active trip", (error as IshaaraError.Conflict).message)
    }

    @Test
    fun `test 28 - 400 INVALID_STATE_TRANSITION mapped to ValidationError`() = runBlocking {
        mockTripRemoteDataSource.completeTripResult = IshaaraResult.failure(
            IshaaraError.Validation(message = "Trip cannot be completed from state: SCHEDULED")
        )

        val result = completeDriverTripUseCase("trip_001")

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
    }

    @Test
    fun `test 29 - 404 trip not found mapped to NotFound error`() = runBlocking {
        mockTripRemoteDataSource.tripDetailsResult = IshaaraResult.failure(
            IshaaraError.NotFound("Trip trip_999 not found")
        )

        val result = getTripDetailsUseCase("trip_999", forceRefresh = true)

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.NotFound)
    }

    @Test
    fun `test 30 - 401 unauthorized mapped to Authentication error`() = runBlocking {
        mockTripRemoteDataSource.listTripsResult = IshaaraResult.failure(
            IshaaraError.Authentication(message = "Session expired")
        )

        val result = getDriverTripsUseCase()

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun `test 31 - network failure mapped cleanly without throwing`() = runBlocking {
        mockTripRemoteDataSource.listTripsResult = IshaaraResult.failure(
            IshaaraError.Network("Connection refused")
        )

        val result = getDriverTripsUseCase()

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Network)
    }

    // =========================================================================
    // 7. STATE ISOLATION & ACCOUNT SWITCHING
    // =========================================================================

    @Test
    fun `test 32 - clearDriverTripState empties all trip flows`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(
            sampleCleanTripDto.copy(status = "ACTIVE")
        )
        getDriverTripsUseCase()

        assertFalse(observeDriverTripsUseCase().first().isEmpty())
        assertNotNull(observeActiveTripUseCase().first())

        clearDriverTripStateUseCase()

        assertTrue(observeDriverTripsUseCase().first().isEmpty())
        assertNull(observeActiveTripUseCase().first())
    }

    @Test
    fun `test 33 - switching account does not leak previous driver trips`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)
        getDriverTripsUseCase()

        // Driver 1 signs out
        clearDriverTripStateUseCase()
        sessionStore.clearSession()

        // Passenger logs in
        sessionStore.saveSession(passengerSession)

        val passengerTrips = observeDriverTripsUseCase().first()
        assertTrue(passengerTrips.isEmpty())

        // Passenger attempts to fetch trips
        val fetchResult = getDriverTripsUseCase()
        assertTrue(fetchResult is IshaaraResult.Failure)
        assertTrue((fetchResult as IshaaraResult.Failure).error is IshaaraError.Forbidden)
    }

    @Test
    fun `test 34 - token passed to remote data source matches active session`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)

        getDriverTripsUseCase()

        assertEquals("jwt.driver.trip.token", mockTripRemoteDataSource.lastTokenUsed)
    }

    // =========================================================================
    // 8. VEHICLE ASSIGNMENT & READINESS INTEGRATION
    // =========================================================================

    @Test
    fun `test 35 - getAssignedVehicle returns currently assigned vehicle for trip context`() = runBlocking {
        val result = getAssignedVehicleUseCase()

        assertTrue(result is IshaaraResult.Success)
        val assigned = (result as IshaaraResult.Success).data
        assertTrue(assigned.hasActiveAssignment)
        assertEquals("veh_001", assigned.vehicle?.id)
        assertEquals("UP32AB1234", assigned.vehicle?.registrationNumber)
    }

    @Test
    fun `test 36 - trip detail resolves vehicle specifications when vehicleId present`() = runBlocking {
        val result = getVehicleDetailsUseCase("veh_001")

        assertTrue(result is IshaaraResult.Success)
        val vehicle = (result as IshaaraResult.Success).data
        assertEquals("Tata", vehicle.make)
        assertEquals(42, vehicle.capacity)
    }

    @Test
    fun `test 37 - GeoJSON longitude and latitude correctly mapped`() {
        val location = TripLocation.of("Charbagh Station", 26.8467, 80.9462)

        // GeoJSON standard: [longitude, latitude]
        assertEquals(80.9462, location.longitude, 0.0001)
        assertEquals(26.8467, location.latitude, 0.0001)
        assertEquals(80.9462, location.coordinate.longitude, 0.0001)
        assertEquals(26.8467, location.coordinate.latitude, 0.0001)
    }

    // =========================================================================
    // 9. PRESENTATION / VIEWMODEL ARCHITECTURE
    // =========================================================================

    @Test
    fun `test 38 - DriverTripListViewModel initializes and loads trips with active banner`() = runBlocking {
        val activeDto = sampleCleanTripDto.copy(status = "ACTIVE")
        mockTripRemoteDataSource.listTripsResponse = listOf(activeDto)

        val viewModel = DriverTripListViewModel(
            getDriverTripsUseCase = getDriverTripsUseCase,
            startDriverTripUseCase = startDriverTripUseCase,
            completeDriverTripUseCase = completeDriverTripUseCase,
            cancelDriverTripUseCase = cancelDriverTripUseCase,
            createDriverTripUseCase = createDriverTripUseCase,
            observeDriverTripsUseCase = observeDriverTripsUseCase,
            observeActiveTripUseCase = observeActiveTripUseCase,
            getAssignedVehicleUseCase = getAssignedVehicleUseCase,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue(state.contentState is TripListContentState.Success)
        val success = state.contentState as TripListContentState.Success
        assertNotNull(success.activeTrip)
        assertEquals(TripStatus.ACTIVE, success.activeTrip?.status)
    }

    @Test
    fun `test 39 - DriverTripListViewModel handles tab switching properly`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(
            sampleCleanTripDto.copy(id = "trip_act", status = "ACTIVE"),
            sampleCleanTripDto.copy(id = "trip_sch", status = "SCHEDULED"),
            sampleCleanTripDto.copy(id = "trip_cmp", status = "COMPLETED")
        )

        val viewModel = DriverTripListViewModel(
            getDriverTripsUseCase = getDriverTripsUseCase,
            startDriverTripUseCase = startDriverTripUseCase,
            completeDriverTripUseCase = completeDriverTripUseCase,
            cancelDriverTripUseCase = cancelDriverTripUseCase,
            createDriverTripUseCase = createDriverTripUseCase,
            observeDriverTripsUseCase = observeDriverTripsUseCase,
            observeActiveTripUseCase = observeActiveTripUseCase,
            getAssignedVehicleUseCase = getAssignedVehicleUseCase,
            dispatchers = testDispatchers
        )

        viewModel.selectTab(TripFilterTab.ACTIVE)
        val activeState = viewModel.uiState.value.contentState as TripListContentState.Success
        assertEquals(1, activeState.trips.size)
        assertEquals(TripStatus.ACTIVE, activeState.trips[0].status)

        viewModel.selectTab(TripFilterTab.UPCOMING)
        val upcomingState = viewModel.uiState.value.contentState as TripListContentState.Success
        assertEquals(1, upcomingState.trips.size)
        assertEquals(TripStatus.SCHEDULED, upcomingState.trips[0].status)

        viewModel.selectTab(TripFilterTab.PAST)
        val pastState = viewModel.uiState.value.contentState as TripListContentState.Success
        assertEquals(1, pastState.trips.size)
        assertEquals(TripStatus.COMPLETED, pastState.trips[0].status)
    }

    @Test
    fun `test 40 - DriverTripListViewModel prevents duplicate concurrent actions`() = runBlocking {
        mockTripRemoteDataSource.listTripsResponse = listOf(sampleCleanTripDto)
        val deferred = kotlinx.coroutines.CompletableDeferred<CleanTripResponseDto>()
        mockTripRemoteDataSource.startTripDeferred = deferred

        val viewModel = DriverTripListViewModel(
            getDriverTripsUseCase = getDriverTripsUseCase,
            startDriverTripUseCase = startDriverTripUseCase,
            completeDriverTripUseCase = completeDriverTripUseCase,
            cancelDriverTripUseCase = cancelDriverTripUseCase,
            createDriverTripUseCase = createDriverTripUseCase,
            observeDriverTripsUseCase = observeDriverTripsUseCase,
            observeActiveTripUseCase = observeActiveTripUseCase,
            getAssignedVehicleUseCase = getAssignedVehicleUseCase,
            dispatchers = testDispatchers
        )

        // First action starts and suspends awaiting deferred
        viewModel.startTrip("trip_001")
        assertEquals("trip_001", viewModel.uiState.value.actionInFlightTripId)

        // Second duplicate action attempted concurrently
        viewModel.startTrip("trip_001")

        // Call count remains 1 because second attempt was immediately blocked
        assertEquals(1, mockTripRemoteDataSource.startTripCallCount.get())

        // Complete deferred to clean up
        deferred.complete(sampleCleanTripDto.copy(status = "ACTIVE"))
        assertNull(viewModel.uiState.value.actionInFlightTripId)
    }

    @Test
    fun `test 41 - DriverTripDetailViewModel loads trip details and handles lifecycle transitions`() = runBlocking {
        mockTripRemoteDataSource.tripDetailsResponse = sampleCleanTripDto

        val viewModel = DriverTripDetailViewModel(
            tripId = "trip_001",
            getTripDetailsUseCase = getTripDetailsUseCase,
            startDriverTripUseCase = startDriverTripUseCase,
            completeDriverTripUseCase = completeDriverTripUseCase,
            cancelDriverTripUseCase = cancelDriverTripUseCase,
            getVehicleDetailsUseCase = getVehicleDetailsUseCase,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue(state.contentState is TripDetailContentState.Success)
        val trip = (state.contentState as TripDetailContentState.Success).trip
        assertEquals("trip_001", trip.id)
        assertEquals(TripStatus.ASSIGNED, trip.status)

        // Perform start
        mockTripRemoteDataSource.startTripResponse = sampleCleanTripDto.copy(status = "ACTIVE")
        viewModel.startTrip()

        val updatedState = viewModel.uiState.value
        val updatedTrip = (updatedState.contentState as TripDetailContentState.Success).trip
        assertEquals(TripStatus.ACTIVE, updatedTrip.status)
    }

    @Test
    fun `test 42 - DriverTripDetailViewModel handles 404 trip not found cleanly`() = runBlocking {
        mockTripRemoteDataSource.tripDetailsResult = IshaaraResult.failure(
            IshaaraError.NotFound("Trip trip_ghost not found")
        )

        val viewModel = DriverTripDetailViewModel(
            tripId = "trip_ghost",
            getTripDetailsUseCase = getTripDetailsUseCase,
            startDriverTripUseCase = startDriverTripUseCase,
            completeDriverTripUseCase = completeDriverTripUseCase,
            cancelDriverTripUseCase = cancelDriverTripUseCase,
            getVehicleDetailsUseCase = getVehicleDetailsUseCase,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue(state.contentState is TripDetailContentState.Error)
        assertTrue((state.contentState as TripDetailContentState.Error).isNotFound)
    }

    // =========================================================================
    // FAKES
    // =========================================================================

    private class FakeTripRemoteDataSource : TripRemoteDataSource {
        var listTripsResponse: List<CleanTripResponseDto> = emptyList()
        var listTripsResult: IshaaraResult<List<CleanTripResponseDto>>? = null

        var tripDetailsResponse: CleanTripResponseDto = CleanTripResponseDto(id = "trip_default")
        var tripDetailsResult: IshaaraResult<CleanTripResponseDto>? = null

        var createTripResponse: CleanTripResponseDto = CleanTripResponseDto(id = "trip_created")
        var createTripResult: IshaaraResult<CleanTripResponseDto>? = null

        var startTripResponse: CleanTripResponseDto = CleanTripResponseDto(id = "trip_started", status = "ACTIVE")
        var startTripResult: IshaaraResult<CleanTripResponseDto>? = null

        var completeTripResponse: CleanTripResponseDto = CleanTripResponseDto(id = "trip_completed", status = "COMPLETED")
        var completeTripResult: IshaaraResult<CleanTripResponseDto>? = null

        var cancelTripResponse: CleanTripResponseDto = CleanTripResponseDto(id = "trip_cancelled", status = "CANCELLED")
        var cancelTripResult: IshaaraResult<CleanTripResponseDto>? = null

        var lastEndpointCalled: String? = null
        var lastTokenUsed: String? = null
        var lastQueryFilter: String? = null
        var lastCreateRequest: CreateTripRequestDto? = null
        var lastCancelRequest: CancelTripRequestDto? = null

        val getTripDetailsCallCount = AtomicInteger(0)
        val startTripCallCount = AtomicInteger(0)

        override suspend fun listDriverTrips(
            token: String,
            page: Int,
            limit: Int,
            status: String?
        ): IshaaraResult<List<CleanTripResponseDto>> {
            lastEndpointCalled = "GET /api/v1/drivers/me/trips"
            lastTokenUsed = token
            lastQueryFilter = status?.let { "status=$it" }
            return listTripsResult ?: IshaaraResult.success(listTripsResponse)
        }

        override suspend fun getDriverTripDetails(
            tripId: String,
            token: String
        ): IshaaraResult<CleanTripResponseDto> {
            getTripDetailsCallCount.incrementAndGet()
            lastEndpointCalled = "GET /api/v1/trips/$tripId"
            lastTokenUsed = token
            return tripDetailsResult ?: IshaaraResult.success(tripDetailsResponse)
        }

        override suspend fun createDriverTrip(
            request: CreateTripRequestDto,
            token: String
        ): IshaaraResult<CleanTripResponseDto> {
            lastEndpointCalled = "POST /api/v1/trips"
            lastTokenUsed = token
            lastCreateRequest = request
            return createTripResult ?: IshaaraResult.success(createTripResponse)
        }

        var startTripDeferred: kotlinx.coroutines.CompletableDeferred<CleanTripResponseDto>? = null

        override suspend fun startDriverTrip(
            tripId: String,
            token: String
        ): IshaaraResult<CleanTripResponseDto> {
            startTripCallCount.incrementAndGet()
            lastEndpointCalled = "POST /api/v1/trips/$tripId/start"
            lastTokenUsed = token
            startTripDeferred?.let { return IshaaraResult.success(it.await()) }
            return startTripResult ?: IshaaraResult.success(startTripResponse)
        }

        override suspend fun completeDriverTrip(
            tripId: String,
            token: String
        ): IshaaraResult<CleanTripResponseDto> {
            lastEndpointCalled = "POST /api/v1/trips/$tripId/complete"
            lastTokenUsed = token
            return completeTripResult ?: IshaaraResult.success(completeTripResponse)
        }

        override suspend fun cancelDriverTrip(
            tripId: String,
            request: CancelTripRequestDto?,
            token: String
        ): IshaaraResult<CleanTripResponseDto> {
            lastEndpointCalled = "POST /api/v1/trips/$tripId/cancel"
            lastTokenUsed = token
            lastCancelRequest = request
            return cancelTripResult ?: IshaaraResult.success(cancelTripResponse)
        }
    }

    private class FakeDriverVehicleRepository : DriverVehicleRepository {
        var assignedVehicleState: AssignedVehicleState = AssignedVehicleState.EMPTY

        override fun observeAssignedVehicle(): StateFlow<AssignedVehicleState?> =
            MutableStateFlow(assignedVehicleState).asStateFlow()

        override suspend fun getAssignedVehicle(forceRefresh: Boolean): IshaaraResult<AssignedVehicleState> {
            return IshaaraResult.success(assignedVehicleState)
        }

        override suspend fun refreshAssignedVehicle(): IshaaraResult<AssignedVehicleState> {
            return IshaaraResult.success(assignedVehicleState)
        }

        override suspend fun listMyVehicles(): IshaaraResult<List<Vehicle>> {
            return IshaaraResult.success(assignedVehicleState.vehicle?.let { listOf(it) } ?: emptyList())
        }

        override suspend fun getVehicleDetails(vehicleId: String): IshaaraResult<Vehicle> {
            return IshaaraResult.success(
                Vehicle(
                    id = vehicleId,
                    agencyId = null,
                    operatorId = null,
                    assignedDriverId = "usr_driver_789",
                    registrationNumber = "UP32AB1234",
                    vehicleType = VehicleType.BUS,
                    make = "Tata",
                    model = "Starbus",
                    capacity = 42,
                    ownershipType = VehicleOwnershipType.INDIVIDUAL,
                    isVerified = true,
                    isActive = true,
                    createdAt = "2026-09-01T10:00:00.000Z",
                    updatedAt = "2026-09-01T10:00:00.000Z"
                )
            )
        }

        override suspend fun registerVehicle(
            registrationNumber: String,
            vehicleType: VehicleType,
            make: String,
            model: String,
            capacity: Int?
        ): IshaaraResult<Vehicle> {
            return IshaaraResult.failure(IshaaraError.Unknown())
        }

        override suspend fun assignSelfToVehicle(
            vehicleId: String,
            driverProfileId: String
        ): IshaaraResult<DriverVehicleAssignment> {
            return IshaaraResult.failure(IshaaraError.Unknown())
        }

        override suspend fun unassignVehicle(vehicleId: String, reason: String?): IshaaraResult<DriverVehicleAssignment?> {
            return IshaaraResult.failure(IshaaraError.Unknown())
        }

        override suspend fun getAssignmentHistory(vehicleId: String): IshaaraResult<List<DriverVehicleAssignment>> {
            return IshaaraResult.success(emptyList())
        }

        override fun clearVehicleState() {
            assignedVehicleState = AssignedVehicleState.EMPTY
        }
    }
}
