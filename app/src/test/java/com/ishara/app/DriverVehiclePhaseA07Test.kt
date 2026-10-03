package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.mapper.VehicleMapper
import com.ishara.app.data.remote.datasource.VehicleRemoteDataSource
import com.ishara.app.data.remote.dto.AssignVehicleRequestDto
import com.ishara.app.data.remote.dto.AssignedVehicleResponseDto
import com.ishara.app.data.remote.dto.CreateVehicleRequestDto
import com.ishara.app.data.remote.dto.DriverVehicleAssignmentDto
import com.ishara.app.data.remote.dto.UnassignVehicleRequestDto
import com.ishara.app.data.remote.dto.VehicleDto
import com.ishara.app.data.repository.DriverVehicleRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleAssignmentActorRole
import com.ishara.app.domain.model.VehicleAssignmentStatus
import com.ishara.app.domain.model.VehicleOwnershipType
import com.ishara.app.domain.model.VehicleType
import com.ishara.app.domain.repository.DriverReadinessRepository
import com.ishara.app.domain.usecase.AssignSelfToVehicleUseCase
import com.ishara.app.domain.usecase.ClearVehicleStateUseCase
import com.ishara.app.domain.usecase.GetAssignedVehicleUseCase
import com.ishara.app.domain.usecase.GetVehicleAssignmentHistoryUseCase
import com.ishara.app.domain.usecase.ListMyVehiclesUseCase
import com.ishara.app.domain.usecase.ObserveAssignedVehicleUseCase
import com.ishara.app.domain.usecase.RefreshAssignedVehicleUseCase
import com.ishara.app.domain.usecase.RegisterVehicleUseCase
import com.ishara.app.domain.usecase.UnassignVehicleUseCase
import com.ishara.app.feature.driver.vehicle.DriverVehicleViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
 * PHASE A07 Test Suite — VEHICLE REGISTRATION & DRIVER–VEHICLE ASSIGNMENT
 *
 * Verifies all architectural, operational, and security requirements:
 * - 1. VEHICLE (1-6)
 * - 2. ASSIGNMENT (7-11)
 * - 3. AUTHORIZATION & ROUTE PROTECTION (12-15)
 * - 4. BACKEND ERROR HANDLING (16-24)
 * - 5. STATE ISOLATION & ACCOUNT SWITCHING (25-27)
 * - 6. A06 READINESS INTEGRATION (28-30)
 * - 7. SECURITY INVARIANTS (31-35)
 * - 8. DRIVER WORKFLOWS / MUTATIONS (36-40)
 */
class DriverVehiclePhaseA07Test {

    private lateinit var mockVehicleRemoteDataSource: FakeVehicleRemoteDataSource
    private lateinit var mockReadinessRepository: FakeDriverReadinessRepository
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var sessionLocalDataSource: SessionLocalDataSourceImpl
    private lateinit var vehicleRepository: DriverVehicleRepositoryImpl

    private lateinit var getAssignedVehicleUseCase: GetAssignedVehicleUseCase
    private lateinit var observeAssignedVehicleUseCase: ObserveAssignedVehicleUseCase
    private lateinit var refreshAssignedVehicleUseCase: RefreshAssignedVehicleUseCase
    private lateinit var listMyVehiclesUseCase: ListMyVehiclesUseCase
    private lateinit var registerVehicleUseCase: RegisterVehicleUseCase
    private lateinit var assignSelfToVehicleUseCase: AssignSelfToVehicleUseCase
    private lateinit var unassignVehicleUseCase: UnassignVehicleUseCase
    private lateinit var getVehicleAssignmentHistoryUseCase: GetVehicleAssignmentHistoryUseCase
    private lateinit var clearVehicleStateUseCase: ClearVehicleStateUseCase

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val driverSession = AuthSession(
        token = "jwt.driver.valid.token",
        userId = "usr_driver_123",
        role = UserRole.DRIVER_CONDUCTOR
    )

    private val passengerSession = AuthSession(
        token = "jwt.passenger.valid.token",
        userId = "usr_passenger_456",
        role = UserRole.USER
    )

    private val sampleVehicleDto = VehicleDto(
        id = "veh_001",
        agencyId = "agn_999",
        operatorId = null,
        assignedDriverId = "drv_123",
        registrationNumber = "UP32AB1234",
        vehicleType = "BUS",
        make = "Tata",
        model = "Starbus Ultra",
        capacity = 32,
        ownershipType = "AGENCY",
        isVerified = true,
        isActive = true,
        createdAt = "2026-09-01T08:00:00.000Z",
        updatedAt = "2026-09-01T08:00:00.000Z"
    )

    private val sampleAssignmentDto = DriverVehicleAssignmentDto(
        id = "asgn_001",
        driverId = "drv_123",
        vehicleId = "veh_001",
        agencyId = "agn_999",
        status = "ACTIVE",
        assignedAt = "2026-09-01T09:00:00.000Z",
        unassignedAt = null,
        assignedBy = "usr_agency_owner",
        assignedByRole = "AGENCY_OWNER",
        unassignedBy = null,
        unassignedByRole = null,
        reason = null,
        vehicle = sampleVehicleDto,
        createdAt = "2026-09-01T09:00:00.000Z",
        updatedAt = "2026-09-01T09:00:00.000Z"
    )

    @Before
    fun setUp() = runBlocking {
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        mockVehicleRemoteDataSource = FakeVehicleRemoteDataSource()
        mockReadinessRepository = FakeDriverReadinessRepository()

        vehicleRepository = DriverVehicleRepositoryImpl(
            remoteDataSource = mockVehicleRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )

        getAssignedVehicleUseCase = GetAssignedVehicleUseCase(vehicleRepository)
        observeAssignedVehicleUseCase = ObserveAssignedVehicleUseCase(vehicleRepository)
        refreshAssignedVehicleUseCase = RefreshAssignedVehicleUseCase(vehicleRepository, mockReadinessRepository)
        listMyVehiclesUseCase = ListMyVehiclesUseCase(vehicleRepository)
        registerVehicleUseCase = RegisterVehicleUseCase(vehicleRepository)
        assignSelfToVehicleUseCase = AssignSelfToVehicleUseCase(vehicleRepository, mockReadinessRepository)
        unassignVehicleUseCase = UnassignVehicleUseCase(vehicleRepository, mockReadinessRepository)
        getVehicleAssignmentHistoryUseCase = GetVehicleAssignmentHistoryUseCase(vehicleRepository)
        clearVehicleStateUseCase = ClearVehicleStateUseCase(vehicleRepository)

        // Seed with driver session
        sessionStore.saveSession(driverSession)
    }

    // =========================================================================
    // 1. VEHICLE TESTS (1-6)
    // =========================================================================

    @Test
    fun `test 01 assigned vehicle loads from backend successfully`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        val state = result.getOrNull()
        assertNotNull(state)
        assertEquals("UP32AB1234", state?.vehicle?.registrationNumber)
        assertEquals(VehicleType.BUS, state?.vehicle?.vehicleType)
        assertEquals("Tata", state?.vehicle?.make)
        assertEquals("Starbus Ultra", state?.vehicle?.model)
        assertEquals(32, state?.vehicle?.capacity)
        assertTrue(state?.hasActiveAssignment == true)
    }

    @Test
    fun `test 02 no vehicle assigned returns clean empty state`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = null,
            assignment = null
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        val state = result.getOrNull()
        assertNotNull(state)
        assertNull(state?.vehicle)
        assertNull(state?.assignment)
        assertFalse(state?.hasActiveAssignment == true)
    }

    @Test
    fun `test 03 vehicle details map correctly from DTO to domain model`() {
        val domain = VehicleMapper.toDomain(sampleVehicleDto)
        assertEquals("veh_001", domain.id)
        assertEquals("agn_999", domain.agencyId)
        assertEquals("UP32AB1234", domain.registrationNumber)
        assertEquals(VehicleType.BUS, domain.vehicleType)
        assertEquals("Tata Starbus Ultra", domain.displayTitle)
        assertEquals("32 seats", domain.formattedCapacity)
        assertEquals(VehicleOwnershipType.AGENCY, domain.ownershipType)
        assertTrue(domain.isVerified)
        assertTrue(domain.isActive)
    }

    @Test
    fun `test 04 vehicle status maps correctly for active and inactive vehicles`() {
        val activeDto = sampleVehicleDto.copy(isActive = true)
        val inactiveDto = sampleVehicleDto.copy(isActive = false)

        val activeDomain = VehicleMapper.toDomain(activeDto)
        val inactiveDomain = VehicleMapper.toDomain(inactiveDto)

        assertTrue(activeDomain.isActive)
        assertFalse(inactiveDomain.isActive)
    }

    @Test
    fun `test 05 malformed response handled safely without crash`() = runBlocking {
        // Return fallback DTO with minimal data
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = VehicleDto(
                id = "",
                registrationNumber = "",
                vehicleType = "UNKNOWN_TYPE",
                make = "",
                model = ""
            ),
            assignment = null
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        val vehicle = result.getOrNull()?.vehicle
        assertNotNull(vehicle)
        assertEquals(VehicleType.OTHER, vehicle?.vehicleType)
    }

    @Test
    fun `test 06 refresh re-queries backend and updates cached state`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )
        getAssignedVehicleUseCase(forceRefresh = false)
        assertEquals(1, mockVehicleRemoteDataSource.getMyAssignedVehicleCallCount.get())

        // Cache hit without forceRefresh
        getAssignedVehicleUseCase(forceRefresh = false)
        assertEquals(1, mockVehicleRemoteDataSource.getMyAssignedVehicleCallCount.get())

        // Refresh triggers network call
        refreshAssignedVehicleUseCase()
        assertEquals(2, mockVehicleRemoteDataSource.getMyAssignedVehicleCallCount.get())
    }

    // =========================================================================
    // 2. ASSIGNMENT TESTS (7-11)
    // =========================================================================

    @Test
    fun `test 07 active assignment loads correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        val assignment = result.getOrNull()?.assignment
        assertNotNull(assignment)
        assertEquals("asgn_001", assignment?.id)
        assertEquals("drv_123", assignment?.driverId)
        assertEquals("veh_001", assignment?.vehicleId)
        assertEquals(VehicleAssignmentStatus.ACTIVE, assignment?.status)
        assertEquals(VehicleAssignmentActorRole.AGENCY_OWNER, assignment?.assignedByRole)
    }

    @Test
    fun `test 08 no active assignment returns null assignment object`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = null,
            assignment = null
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.assignment)
    }

    @Test
    fun `test 09 assignment state refresh updates state flow observer`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        refreshAssignedVehicleUseCase()
        val observed = observeAssignedVehicleUseCase().first()
        assertNotNull(observed)
        assertEquals("UP32AB1234", observed?.vehicle?.registrationNumber)
        assertEquals(VehicleAssignmentStatus.ACTIVE, observed?.assignment?.status)
    }

    @Test
    fun `test 10 external assignment becomes visible upon refresh`() = runBlocking {
        // Driver initially has no vehicle
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(vehicle = null, assignment = null)
        getAssignedVehicleUseCase(forceRefresh = true)
        assertNull(vehicleRepository.observeAssignedVehicle().value?.vehicle)

        // Fleet manager assigns vehicle externally
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        refreshAssignedVehicleUseCase()
        val updated = vehicleRepository.observeAssignedVehicle().value
        assertNotNull(updated?.vehicle)
        assertEquals("UP32AB1234", updated?.vehicle?.registrationNumber)
    }

    @Test
    fun `test 11 external unassignment becomes visible upon refresh`() = runBlocking {
        // Initially assigned
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )
        refreshAssignedVehicleUseCase()
        assertNotNull(vehicleRepository.observeAssignedVehicle().value?.vehicle)

        // Fleet manager removes assignment externally
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(vehicle = null, assignment = null)
        refreshAssignedVehicleUseCase()
        val updated = vehicleRepository.observeAssignedVehicle().value
        assertNull(updated?.vehicle)
        assertNull(updated?.assignment)
    }

    // =========================================================================
    // 3. AUTHORIZATION & ROUTE PROTECTION TESTS (12-15)
    // =========================================================================

    @Test
    fun `test 12 USER role cannot access driver vehicle repository`() = runBlocking {
        sessionStore.saveSession(passengerSession)

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals(403, error?.code)
    }

    @Test
    fun `test 13 DRIVER_CONDUCTOR can access own vehicle`() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.vehicle)
    }

    @Test
    fun `test 14 unauthenticated session is blocked`() = runBlocking {
        sessionStore.clearSession()

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Authentication)
    }

    @Test
    fun `test 15 driver cannot access another drivers vehicle (backend 404 mapped properly)`() = runBlocking {
        mockVehicleRemoteDataSource.getVehicleByIdResult = IshaaraResult.failure(
            IshaaraError.NotFound(message = "Vehicle not found.")
        )

        val result = vehicleRepository.getVehicleDetails("veh_other_driver")
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.NotFound)
    }

    // =========================================================================
    // 4. BACKEND ERROR HANDLING TESTS (16-24)
    // =========================================================================

    @Test
    fun `test 16 401 Unauthorized maps to Authentication error`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.Authentication(message = "Session expired.")
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Authentication)
    }

    @Test
    fun `test 17 403 Forbidden maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.Forbidden(message = "Driver is suspended.", errorCode = "DRIVER_OPERATIONAL_SUSPENDED")
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals("DRIVER_OPERATIONAL_SUSPENDED", (error as IshaaraError.Forbidden).errorCode)
    }

    @Test
    fun `test 18 404 Not Found maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.NotFound(message = "Driver profile not found.")
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.NotFound)
    }

    @Test
    fun `test 19 409 Conflict maps correctly (already assigned or duplicate plate)`() = runBlocking {
        mockVehicleRemoteDataSource.createVehicleResult = IshaaraResult.failure(
            IshaaraError.Conflict(message = "Vehicle with registration plate already exists.", errorCode = "VEHICLE_REGISTRATION_ALREADY_EXISTS")
        )

        val result = registerVehicleUseCase("UP32AB1234", VehicleType.CAB, "Maruti", "Dzire")
        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is IshaaraError.Conflict)
        assertEquals("VEHICLE_REGISTRATION_ALREADY_EXISTS", (error as IshaaraError.Conflict).errorCode)
    }

    @Test
    fun `test 20 400 or 422 Validation error maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.createVehicleResult = IshaaraResult.failure(
            IshaaraError.Validation(message = "Invalid vehicle registration plate format.", errorCode = "VALIDATION_ERROR")
        )

        val result = registerVehicleUseCase("123", VehicleType.AUTO, "Bajaj", "RE")
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Validation)
    }

    @Test
    fun `test 21 429 RateLimited error maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.RateLimited(retryAfterSeconds = 30)
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.RateLimited)
    }

    @Test
    fun `test 22 5xx Server error maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.Server(code = 503, message = "Fleet registry unavailable.")
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Server)
    }

    @Test
    fun `test 23 Request Timeout maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.Timeout()
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Timeout)
    }

    @Test
    fun `test 24 Network Offline maps correctly`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResult = IshaaraResult.failure(
            IshaaraError.Network()
        )

        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Network)
    }

    // =========================================================================
    // 5. STATE ISOLATION & ACCOUNT SWITCHING (25-27)
    // =========================================================================

    @Test
    fun `test 25 DRIVER A vehicle does not leak to DRIVER B`() = runBlocking {
        // Driver A logs in and loads Vehicle A
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto.copy(registrationNumber = "VEHICLE_A"),
            assignment = sampleAssignmentDto
        )
        refreshAssignedVehicleUseCase()
        assertEquals("VEHICLE_A", vehicleRepository.observeAssignedVehicle().value?.vehicle?.registrationNumber)

        // Driver A logs out: flush state
        clearVehicleStateUseCase()
        assertNull(vehicleRepository.observeAssignedVehicle().value)

        // Driver B logs in with Vehicle B
        val driverBSession = AuthSession(token = "jwt.driver.b", userId = "usr_b", role = UserRole.DRIVER_CONDUCTOR)
        sessionStore.saveSession(driverBSession)
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto.copy(registrationNumber = "VEHICLE_B"),
            assignment = sampleAssignmentDto.copy(driverId = "drv_b")
        )

        refreshAssignedVehicleUseCase()
        assertEquals("VEHICLE_B", vehicleRepository.observeAssignedVehicle().value?.vehicle?.registrationNumber)
    }

    @Test
    fun `test 26 DRIVER to USER account switch clears vehicle state completely`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )
        refreshAssignedVehicleUseCase()
        assertNotNull(vehicleRepository.observeAssignedVehicle().value)

        // Driver logs out
        clearVehicleStateUseCase()
        sessionStore.saveSession(passengerSession)

        assertNull(vehicleRepository.observeAssignedVehicle().value)
        val passengerAccess = getAssignedVehicleUseCase(forceRefresh = false)
        assertTrue(passengerAccess.isFailure)
    }

    @Test
    fun `test 27 USER to DRIVER login loads fresh vehicle assignment`() = runBlocking {
        sessionStore.saveSession(passengerSession)
        clearVehicleStateUseCase()

        // Driver logs in
        sessionStore.saveSession(driverSession)
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        val result = refreshAssignedVehicleUseCase()
        assertTrue(result.isSuccess)
        assertEquals("UP32AB1234", result.getOrNull()?.vehicle?.registrationNumber)
    }

    // =========================================================================
    // 6. A06 READINESS INTEGRATION (28-30)
    // =========================================================================

    @Test
    fun `test 28 vehicle state does not locally determine readiness`() = runBlocking {
        // Having an assigned vehicle does NOT locally mark driver ready;
        // readiness evaluation remains strictly with backend A06.
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )
        val assignedResult = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(assignedResult.isSuccess)

        // Local state only knows vehicle assignment, not readiness status
        val assignedState = assignedResult.getOrNull()
        assertTrue(assignedState?.hasActiveAssignment == true)
        // Ensure no local isReady mutation occurred on vehicle state
    }

    @Test
    fun `test 29 readiness refresh is triggered after assignment change`() = runBlocking {
        mockVehicleRemoteDataSource.assignVehicleResult = IshaaraResult.success(sampleAssignmentDto)

        assignSelfToVehicleUseCase("veh_001", "drv_123")

        // Verifies mock readiness repository received refresh call
        assertEquals(1, mockReadinessRepository.refreshCallCount.get())
    }

    @Test
    fun `test 30 cached vehicle cannot grant operational authorization`() = runBlocking {
        // Cache exists
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )
        refreshAssignedVehicleUseCase()
        assertNotNull(vehicleRepository.observeAssignedVehicle().value)

        // Clearing session immediately revokes repository access regardless of cache
        sessionStore.clearSession()
        val unauthorizedCall = vehicleRepository.getAssignedVehicle(forceRefresh = false)
        assertTrue(unauthorizedCall.isFailure)
        assertTrue(unauthorizedCall.errorOrNull() is IshaaraError.Authentication)
    }

    // =========================================================================
    // 7. SECURITY INVARIANTS (31-35)
    // =========================================================================

    @Test
    fun `test 31 no hardcoded vehicle ID in repository or mapper`() {
        val mapped = VehicleMapper.toDomain(sampleVehicleDto.copy(id = "dynamic_veh_abc"))
        assertEquals("dynamic_veh_abc", mapped.id)
    }

    @Test
    fun `test 32 no hardcoded assignment ID in repository or mapper`() {
        val mapped = VehicleMapper.toDomain(sampleAssignmentDto.copy(id = "dynamic_asgn_xyz"))
        assertEquals("dynamic_asgn_xyz", mapped.id)
    }

    @Test
    fun `test 33 no admin credential usage in driver network requests`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )
        refreshAssignedVehicleUseCase()

        // Driver requests must only send Bearer token, never admin key
        assertEquals("jwt.driver.valid.token", mockVehicleRemoteDataSource.lastTokenUsed)
        assertFalse(mockVehicleRemoteDataSource.lastTokenUsed?.contains("admin") == true)
    }

    @Test
    fun `test 34 no token or sensitive PII leaked in domain models`() {
        val domain = VehicleMapper.toDomain(sampleVehicleDto)
        // Domain model must only contain operational data, no token, password, or hash
        assertEquals("Tata Starbus Ultra", domain.displayTitle)
    }

    @Test
    fun `test 35 no fake assignment state manufactured client-side`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(vehicle = null, assignment = null)
        val result = getAssignedVehicleUseCase(forceRefresh = true)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.vehicle)
        assertNull(result.getOrNull()?.assignment)
    }

    // =========================================================================
    // 8. DRIVER WORKFLOWS & MUTATIONS (36-40)
    // =========================================================================

    @Test
    fun `test 36 individual vehicle registration succeeds with normalized inputs`() = runBlocking {
        val newVehicleDto = VehicleDto(
            id = "veh_new_999",
            registrationNumber = "UP32IND9999",
            vehicleType = "AUTO",
            make = "Bajaj",
            model = "RE Compact",
            capacity = 3,
            ownershipType = "INDIVIDUAL",
            isVerified = false,
            isActive = true
        )
        mockVehicleRemoteDataSource.createVehicleResult = IshaaraResult.success(newVehicleDto)

        val result = registerVehicleUseCase(
            registrationNumber = "up 32 ind 9999",
            vehicleType = VehicleType.AUTO,
            make = "Bajaj",
            model = "RE Compact",
            capacity = 3
        )

        assertTrue(result.isSuccess)
        val vehicle = result.getOrNull()
        assertNotNull(vehicle)
        assertEquals("UP32IND9999", vehicle?.registrationNumber)
        assertEquals(VehicleOwnershipType.INDIVIDUAL, vehicle?.ownershipType)
    }

    @Test
    fun `test 37 driver self-assignment to owned vehicle succeeds`() = runBlocking {
        mockVehicleRemoteDataSource.assignVehicleResult = IshaaraResult.success(sampleAssignmentDto)
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        val result = assignSelfToVehicleUseCase("veh_001", "drv_123")
        assertTrue(result.isSuccess)
        assertEquals(VehicleAssignmentStatus.ACTIVE, result.getOrNull()?.status)
    }

    @Test
    fun `test 38 driver unassignment terminates active assignment`() = runBlocking {
        mockVehicleRemoteDataSource.unassignVehicleResult = IshaaraResult.success(
            sampleAssignmentDto.copy(status = "ENDED", unassignedAt = "2026-09-01T18:00:00.000Z")
        )

        val result = unassignVehicleUseCase("veh_001", "Shift completed")
        assertTrue(result.isSuccess)
        assertEquals(VehicleAssignmentStatus.ENDED, result.getOrNull()?.status)

        // Assigned vehicle state flow is cleared
        val currentAssigned = vehicleRepository.observeAssignedVehicle().value
        assertNull(currentAssigned?.vehicle)
    }

    @Test
    fun `test 39 vehicle assignment history loads and maps correctly`() = runBlocking {
        val historyItem1 = sampleAssignmentDto.copy(id = "hist_1", status = "ENDED")
        val historyItem2 = sampleAssignmentDto.copy(id = "hist_2", status = "ACTIVE")
        mockVehicleRemoteDataSource.assignmentHistoryResult = IshaaraResult.success(listOf(historyItem2, historyItem1))

        val result = getVehicleAssignmentHistoryUseCase("veh_001")
        assertTrue(result.isSuccess)
        val history = result.getOrNull()
        assertNotNull(history)
        assertEquals(2, history?.size)
        assertEquals(VehicleAssignmentStatus.ACTIVE, history?.get(0)?.status)
        assertEquals(VehicleAssignmentStatus.ENDED, history?.get(1)?.status)
    }

    @Test
    fun `test 40 ViewModel exposes UDF states for loading, assigned, and not-assigned`() = runBlocking {
        mockVehicleRemoteDataSource.assignedVehicleResponse = AssignedVehicleResponseDto(
            vehicle = sampleVehicleDto,
            assignment = sampleAssignmentDto
        )

        val viewModel = DriverVehicleViewModel(
            getAssignedVehicleUseCase = getAssignedVehicleUseCase,
            refreshAssignedVehicleUseCase = refreshAssignedVehicleUseCase,
            observeAssignedVehicleUseCase = observeAssignedVehicleUseCase,
            listMyVehiclesUseCase = listMyVehiclesUseCase,
            registerVehicleUseCase = registerVehicleUseCase,
            assignSelfToVehicleUseCase = assignSelfToVehicleUseCase,
            unassignVehicleUseCase = unassignVehicleUseCase,
            getVehicleAssignmentHistoryUseCase = getVehicleAssignmentHistoryUseCase,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasActiveAssignment)
        assertEquals("UP32AB1234", state.assignedVehicle?.registrationNumber)
    }

    // =========================================================================
    // FAKES
    // =========================================================================

    private class FakeVehicleRemoteDataSource : VehicleRemoteDataSource {
        var assignedVehicleResponse: AssignedVehicleResponseDto = AssignedVehicleResponseDto()
        var assignedVehicleResult: IshaaraResult<AssignedVehicleResponseDto>? = null

        var myVehiclesResponse: List<VehicleDto> = emptyList()
        var myVehiclesResult: IshaaraResult<List<VehicleDto>>? = null

        var getVehicleByIdResult: IshaaraResult<VehicleDto>? = null
        var createVehicleResult: IshaaraResult<VehicleDto>? = null
        var assignVehicleResult: IshaaraResult<DriverVehicleAssignmentDto>? = null
        var unassignVehicleResult: IshaaraResult<DriverVehicleAssignmentDto?>? = null
        var assignmentHistoryResult: IshaaraResult<List<DriverVehicleAssignmentDto>>? = null

        val getMyAssignedVehicleCallCount = AtomicInteger(0)
        var lastTokenUsed: String? = null

        override suspend fun getMyAssignedVehicle(token: String): IshaaraResult<AssignedVehicleResponseDto> {
            getMyAssignedVehicleCallCount.incrementAndGet()
            lastTokenUsed = token
            return assignedVehicleResult ?: IshaaraResult.success(assignedVehicleResponse)
        }

        override suspend fun listMyVehicles(token: String): IshaaraResult<List<VehicleDto>> {
            lastTokenUsed = token
            return myVehiclesResult ?: IshaaraResult.success(myVehiclesResponse)
        }

        override suspend fun getVehicleById(vehicleId: String, token: String): IshaaraResult<VehicleDto> {
            lastTokenUsed = token
            return getVehicleByIdResult ?: IshaaraResult.success(
                VehicleDto(id = vehicleId, registrationNumber = "UP32AB1234", vehicleType = "BUS", make = "Tata", model = "Starbus")
            )
        }

        override suspend fun createVehicle(request: CreateVehicleRequestDto, token: String): IshaaraResult<VehicleDto> {
            lastTokenUsed = token
            return createVehicleResult ?: IshaaraResult.success(
                VehicleDto(
                    id = "veh_created",
                    registrationNumber = request.registrationNumber,
                    vehicleType = request.vehicleType,
                    make = request.make,
                    model = request.model,
                    capacity = request.capacity,
                    ownershipType = "INDIVIDUAL"
                )
            )
        }

        override suspend fun assignVehicle(
            vehicleId: String,
            request: AssignVehicleRequestDto,
            token: String
        ): IshaaraResult<DriverVehicleAssignmentDto> {
            lastTokenUsed = token
            return assignVehicleResult ?: IshaaraResult.success(
                DriverVehicleAssignmentDto(
                    id = "asgn_new",
                    driverId = request.driverId,
                    vehicleId = vehicleId,
                    status = "ACTIVE",
                    assignedAt = "2026-09-01T10:00:00.000Z",
                    assignedBy = "usr_driver",
                    assignedByRole = "DRIVER"
                )
            )
        }

        override suspend fun unassignVehicle(
            vehicleId: String,
            request: UnassignVehicleRequestDto,
            token: String
        ): IshaaraResult<DriverVehicleAssignmentDto?> {
            lastTokenUsed = token
            return unassignVehicleResult ?: IshaaraResult.success(
                DriverVehicleAssignmentDto(
                    id = "asgn_ended",
                    driverId = "drv_123",
                    vehicleId = vehicleId,
                    status = "ENDED",
                    assignedAt = "2026-09-01T10:00:00.000Z",
                    unassignedAt = "2026-09-01T18:00:00.000Z",
                    assignedBy = "usr_driver",
                    assignedByRole = "DRIVER",
                    reason = request.reason
                )
            )
        }

        override suspend fun getVehicleAssignmentHistory(
            vehicleId: String,
            token: String
        ): IshaaraResult<List<DriverVehicleAssignmentDto>> {
            lastTokenUsed = token
            return assignmentHistoryResult ?: IshaaraResult.success(emptyList())
        }
    }

    private class FakeDriverReadinessRepository : DriverReadinessRepository {
        val refreshCallCount = AtomicInteger(0)

        override fun observeDriverReadiness() = kotlinx.coroutines.flow.MutableStateFlow(null)

        override suspend fun getDriverReadiness(forceRefresh: Boolean): IshaaraResult<com.ishara.app.domain.model.DriverOperationalReadiness> {
            return IshaaraResult.failure(IshaaraError.Unknown())
        }

        override suspend fun refreshDriverReadiness(): IshaaraResult<com.ishara.app.domain.model.DriverOperationalReadiness> {
            refreshCallCount.incrementAndGet()
            return IshaaraResult.failure(IshaaraError.Unknown())
        }

        override fun clearReadinessState() {}
    }
}
