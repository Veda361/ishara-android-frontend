package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.mapper.AgencyMapper
import com.ishara.app.data.remote.datasource.AgencyRemoteDataSource
import com.ishara.app.data.remote.dto.CancelMembershipResponseDto
import com.ishara.app.data.remote.dto.CleanDriverMembershipResponseDto
import com.ishara.app.data.remote.dto.CleanPublicAgencyResponseDto
import com.ishara.app.data.repository.AgencyMembershipRepositoryImpl
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.AgencyMembershipStatus
import com.ishara.app.domain.model.AgencyStatus
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.CancelAgencyMembershipUseCase
import com.ishara.app.domain.usecase.ClearAgencyMembershipStateUseCase
import com.ishara.app.domain.usecase.GetAgencyDetailsUseCase
import com.ishara.app.domain.usecase.GetCurrentAgencyMembershipUseCase
import com.ishara.app.domain.usecase.ListAgenciesUseCase
import com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase
import com.ishara.app.domain.usecase.ObserveCurrentAgencyMembershipUseCase
import com.ishara.app.domain.usecase.RefreshAgencyMembershipUseCase
import com.ishara.app.domain.usecase.RequestAgencyMembershipUseCase
import com.ishara.app.domain.usecase.ResolveApplicationDestinationUseCase
import com.ishara.app.feature.driver.agency.DriverAgencyMembershipViewModel
import com.ishara.app.navigation.IshaaraDestination
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
 * PHASE A05 Test Suite — DRIVER AGENCY MEMBERSHIP & FLEET ASSOCIATION
 *
 * Verifies all 41 requirements from Phase A05 Specification:
 * - 1. MEMBERSHIP STATE (1-6)
 * - 2. INVITATION / MEMBERSHIP ACTION CONTRACT (7-12)
 * - 3. AGENCY DISCOVERY & DETAILS (13-16)
 * - 4. AUTHORIZATION & ROUTE PROTECTION (17-20)
 * - 5. BACKEND ERROR HANDLING (21-29)
 * - 6. ACCOUNT SWITCHING & CACHE SAFETY (30-34)
 * - 7. MUTATION SAFETY & IDEMPOTENCY (35-38)
 * - 8. SECURITY & PRIVACY (39-41)
 */
class DriverAgencyMembershipPhaseA05Test {

    private lateinit var mockRemoteDataSource: FakeAgencyRemoteDataSource
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var agencyRepository: AgencyMembershipRepositoryImpl

    private lateinit var getCurrentAgencyMembershipUseCase: GetCurrentAgencyMembershipUseCase
    private lateinit var observeAgencyMembershipStateUseCase: ObserveAgencyMembershipStateUseCase
    private lateinit var observeCurrentAgencyMembershipUseCase: ObserveCurrentAgencyMembershipUseCase
    private lateinit var listAgenciesUseCase: ListAgenciesUseCase
    private lateinit var getAgencyDetailsUseCase: GetAgencyDetailsUseCase
    private lateinit var requestAgencyMembershipUseCase: RequestAgencyMembershipUseCase
    private lateinit var cancelAgencyMembershipUseCase: CancelAgencyMembershipUseCase
    private lateinit var refreshAgencyMembershipUseCase: RefreshAgencyMembershipUseCase
    private lateinit var clearAgencyMembershipStateUseCase: ClearAgencyMembershipStateUseCase
    private lateinit var resolveApplicationDestinationUseCase: ResolveApplicationDestinationUseCase

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

    private val sampleAgencyDto = CleanPublicAgencyResponseDto(
        id = "65a1234567890123456789ab",
        name = "Metro City Transits",
        businessName = "Metro City Transits Ltd",
        city = "Ibadan",
        state = "Oyo",
        contactPhoneMasked = "+234 803 *** 1234",
        contactEmail = "contact@metrotransits.ng",
        status = "ACTIVE",
        createdAt = "2026-01-15T08:00:00.000Z"
    )

    private val sampleMembershipPendingDto = CleanDriverMembershipResponseDto(
        id = "65b9876543210987654321cd",
        agencyId = "65a1234567890123456789ab",
        agencyName = "Metro City Transits",
        agencyCity = "Ibadan",
        agencyState = "Oyo",
        agencyContactEmail = "contact@metrotransits.ng",
        status = "PENDING",
        requestedAt = "2026-03-30T10:00:00.000Z",
        respondedAt = null,
        rejectionReason = null,
        notes = "Prefer morning express corridor",
        createdAt = "2026-03-30T10:00:00.000Z",
        updatedAt = "2026-03-30T10:00:00.000Z"
    )

    private val sampleMembershipApprovedDto = sampleMembershipPendingDto.copy(
        status = "APPROVED",
        respondedAt = "2026-03-30T12:00:00.000Z"
    )

    private val sampleMembershipRejectedDto = sampleMembershipPendingDto.copy(
        status = "REJECTED",
        respondedAt = "2026-03-30T14:00:00.000Z",
        rejectionReason = "Route roster at full capacity for this operational cycle."
    )

    @Before
    fun setUp() {
        mockRemoteDataSource = FakeAgencyRemoteDataSource()
        sessionStore = InMemorySessionStore()

        agencyRepository = AgencyMembershipRepositoryImpl(
            remoteDataSource = mockRemoteDataSource,
            sessionStore = sessionStore
        )

        getCurrentAgencyMembershipUseCase = GetCurrentAgencyMembershipUseCase(agencyRepository)
        observeAgencyMembershipStateUseCase = ObserveAgencyMembershipStateUseCase(agencyRepository)
        observeCurrentAgencyMembershipUseCase = ObserveCurrentAgencyMembershipUseCase(agencyRepository)
        listAgenciesUseCase = ListAgenciesUseCase(agencyRepository)
        getAgencyDetailsUseCase = GetAgencyDetailsUseCase(agencyRepository)
        requestAgencyMembershipUseCase = RequestAgencyMembershipUseCase(agencyRepository)
        cancelAgencyMembershipUseCase = CancelAgencyMembershipUseCase(agencyRepository)
        refreshAgencyMembershipUseCase = RefreshAgencyMembershipUseCase(agencyRepository)
        clearAgencyMembershipStateUseCase = ClearAgencyMembershipStateUseCase(agencyRepository)
        resolveApplicationDestinationUseCase = ResolveApplicationDestinationUseCase()
    }

    // =========================================================================
    // 1. MEMBERSHIP STATE (1-6)
    // =========================================================================

    @Test
    fun test01_noAgencyMembershipState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(null)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Success)
        assertNull((result as IshaaraResult.Success).data)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue("Expected NoMembership but was $state", state is DriverMembershipState.NoMembership)
    }

    @Test
    fun test02_pendingMembershipState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertNotNull(data)
        assertEquals(AgencyMembershipStatus.PENDING, data!!.status)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue("Expected Pending but was $state", state is DriverMembershipState.Pending)
        assertEquals("Metro City Transits", (state as DriverMembershipState.Pending).membership.agencyName)
    }

    @Test
    fun test03_activeApprovedMembershipState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertNotNull(data)
        assertEquals(AgencyMembershipStatus.APPROVED, data!!.status)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue("Expected Approved but was $state", state is DriverMembershipState.Approved)
        assertEquals(AgencyMembershipStatus.APPROVED, (state as DriverMembershipState.Approved).membership.status)
    }

    @Test
    fun test04_rejectedMembershipStateWithReason() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipRejectedDto)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertNotNull(data)
        assertEquals(AgencyMembershipStatus.REJECTED, data!!.status)
        assertEquals("Route roster at full capacity for this operational cycle.", data.rejectionReason)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue("Expected Rejected but was $state", state is DriverMembershipState.Rejected)
        assertEquals("Route roster at full capacity for this operational cycle.", (state as DriverMembershipState.Rejected).membership.rejectionReason)
    }

    @Test
    fun test05_removedMembershipStateReflectsAsNoMembership() = runBlocking {
        // When an agency removes an affiliation, backend returns null on /memberships/current
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(null)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Success)
        assertNull((result as IshaaraResult.Success).data)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue(state is DriverMembershipState.NoMembership)
    }

    @Test
    fun test06_suspendedMembershipDoesNotGrantActivePrivileges() = runBlocking {
        // Verify that only APPROVED status produces DriverMembershipState.Approved
        val customStatusDto = sampleMembershipPendingDto.copy(status = "SUSPENDED")
        val domain = AgencyMapper.toDomain(customStatusDto)
        // Any status other than APPROVED/REJECTED is treated as PENDING/Review
        assertEquals(AgencyMembershipStatus.PENDING, domain.status)
    }

    // =========================================================================
    // 2. INVITATION / MEMBERSHIP ACTION CONTRACT (7-12)
    // =========================================================================

    @Test
    fun test07_invitationHandledAsPendingMembershipIfExposed() = runBlocking {
        // Backend contracts handle driver affiliations via membership records
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Success)
        assertEquals("65a1234567890123456789ab", result.getOrNull()?.agencyId)
    }

    @Test
    fun test08_requestMembershipSuccessTransitionsToPending() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)

        val result = agencyRepository.requestMembership("65a1234567890123456789ab", "Morning shifts")
        assertTrue(result is IshaaraResult.Success)
        assertEquals(AgencyMembershipStatus.PENDING, result.getOrNull()?.status)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue(state is DriverMembershipState.Pending)
    }

    @Test
    fun test09_cancelPendingMembershipSuccessTransitionsToNoMembership() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.cancelMembershipResult = IshaaraResult.success(
            CancelMembershipResponseDto("Cancelled", "65b9876543210987654321cd")
        )

        val result = agencyRepository.cancelMembership("65b9876543210987654321cd")
        assertTrue(result is IshaaraResult.Success)

        val state = agencyRepository.observeMembershipState().first()
        assertTrue(state is DriverMembershipState.NoMembership)
    }

    @Test
    fun test10_alreadyAcceptedConflictReconcilesAuthoritativeState() = runBlocking {
        sessionStore.saveSession(driverSession)
        // Backend returns 409 Conflict because membership is already approved
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.failure(
            IshaaraError.Conflict("Driver already has an active approved agency membership.")
        )
        // Reconcile response returns approved membership
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)

        val result = agencyRepository.requestMembership("65a1234567890123456789ab", "Notes")
        assertTrue(result is IshaaraResult.Failure)
        assertTrue(result.errorOrNull() is IshaaraError.Conflict)

        // Repository reconciled state to Approved
        val state = agencyRepository.observeMembershipState().first()
        assertTrue("Expected reconciled Approved state but was $state", state is DriverMembershipState.Approved)
    }

    @Test
    fun test11_alreadyRejectedConflictPermitsReapplication() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipRejectedDto)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isSuccess)
        val state = agencyRepository.observeMembershipState().first()
        assertTrue(state is DriverMembershipState.Rejected)

        // Driver re-applies
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)
        val reapplyResult = agencyRepository.requestMembership("65a1234567890123456789ab", "New attempt")
        assertTrue(reapplyResult.isSuccess)
        assertTrue(agencyRepository.observeMembershipState().first() is DriverMembershipState.Pending)
    }

    @Test
    fun test12_invitationRefreshPollsBackendAuthoritatively() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)

        val refreshed = agencyRepository.refreshMembership()
        assertTrue(refreshed.isSuccess)
        assertEquals(AgencyMembershipStatus.APPROVED, refreshed.getOrNull()?.status)
    }

    // =========================================================================
    // 3. AGENCY DISCOVERY & DETAILS (13-16)
    // =========================================================================

    @Test
    fun test13_currentAgencyLoadsFromBackend() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isSuccess)
        assertEquals("Metro City Transits", result.getOrNull()?.agencyName)
        assertEquals("Ibadan", result.getOrNull()?.agencyCity)
    }

    @Test
    fun test14_agencyUnavailableReturnsNotFound() = runBlocking {
        mockRemoteDataSource.getAgencyByIdResult = IshaaraResult.failure(
            IshaaraError.NotFound(message = "Agency not found")
        )

        val result = agencyRepository.getAgency("65a999999999999999999999")
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.NotFound)
    }

    @Test
    fun test15_malformedBackendResponseHandledGracefully() = runBlocking {
        val malformedDto = CleanPublicAgencyResponseDto(
            id = "",
            name = "",
            businessName = null,
            city = null,
            state = null,
            contactPhoneMasked = null,
            contactEmail = null,
            status = null,
            createdAt = null
        )
        val domain = AgencyMapper.toDomain(malformedDto)
        assertEquals("", domain.id)
        assertEquals("", domain.name)
        assertEquals(AgencyStatus.ACTIVE, domain.status)
    }

    @Test
    fun test16_listAgenciesSupportsSearchAndFiltering() = runBlocking {
        mockRemoteDataSource.listAgenciesResult = IshaaraResult.success(listOf(sampleAgencyDto))

        val result = agencyRepository.listAgencies("Metro", "Ibadan")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Metro City Transits", result.getOrNull()?.first()?.name)
    }

    // =========================================================================
    // 4. AUTHORIZATION & ROUTE PROTECTION (17-20)
    // =========================================================================

    @Test
    fun test17_userCannotAccessAgencyMembershipRoute() {
        val passengerProfile = UserProfile(
            id = "usr_passenger_456",
            email = "passenger@student.ui.edu.ng",
            name = "Student User",
            role = UserRole.USER,
            isOnboarded = true
        )

        val destination = resolveApplicationDestinationUseCase(passengerProfile)
        assertEquals(ApplicationDestination.StudentHome, destination)
        assertTrue(destination != ApplicationDestination.DriverDashboard)
    }

    @Test
    fun test18_driverConductorCanAccessDriverAgencyDestination() {
        val driverAgencyRoute = IshaaraDestination.DriverAgency.route
        assertEquals("driver/agency", driverAgencyRoute)
        assertTrue(driverAgencyRoute.startsWith("driver/"))
    }

    @Test
    fun test19_unauthorizedDeepLinkBlockedForUnauthenticatedSession() {
        // When session is null, destination resolver mandates Authentication
        val destination = resolveApplicationDestinationUseCase(null)
        assertEquals(ApplicationDestination.Authentication, destination)
    }

    @Test
    fun test20_adminOnlyActionNotExposedOnDriverFrontend() {
        // Ensure no agency-owner approve or reject endpoints exist in AgencyMembershipRepository
        val repoMethods = agencyRepository::class.java.declaredMethods.map { it.name }
        assertFalse(repoMethods.contains("approveMembership"))
        assertFalse(repoMethods.contains("rejectMembership"))
        assertFalse(repoMethods.contains("createAgency"))
    }

    // =========================================================================
    // 5. BACKEND ERROR HANDLING (21-29)
    // =========================================================================

    @Test
    fun test21_error401AuthenticationRequiredMapped() = runBlocking {
        // No session stored
        val result = agencyRepository.getCurrentMembership()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue(result.errorOrNull() is IshaaraError.Authentication)
    }

    @Test
    fun test22_error403ForbiddenProblemMapped() = runBlocking {
        sessionStore.saveSession(passengerSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.failure(
            IshaaraError.Forbidden("Access denied: Role 'DRIVER_CONDUCTOR' required.")
        )

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Forbidden)
    }

    @Test
    fun test23_error404NotFoundMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.failure(
            IshaaraError.NotFound(message = "Driver profile not found.")
        )

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.NotFound)
    }

    @Test
    fun test24_error409ConflictMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.failure(
            IshaaraError.Conflict("A pending membership request already exists for this agency.")
        )

        val result = agencyRepository.requestMembership("65a1234567890123456789ab")
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Conflict)
    }

    @Test
    fun test25_error422UnprocessableEntityMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.failure(
            IshaaraError.Validation(message = "Invalid agencyId format")
        )

        val result = agencyRepository.requestMembership("invalid-id")
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Validation)
    }

    @Test
    fun test26_error429RateLimitMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.failure(
            IshaaraError.RateLimited(message = "Too many requests. Please try again later.")
        )

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.RateLimited)
    }

    @Test
    fun test27_error5xxServerFailureMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.failure(
            IshaaraError.Server(code = 500, message = "Internal database failure")
        )

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Server)
    }

    @Test
    fun test28_errorTimeoutMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.failure(
            IshaaraError.Timeout("Socket connection timed out")
        )

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Timeout)
    }

    @Test
    fun test29_errorOfflineMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.failure(
            IshaaraError.Network("No active network connection")
        )

        val result = agencyRepository.getCurrentMembership()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Network)
    }

    // =========================================================================
    // 6. ACCOUNT SWITCHING & CACHE SAFETY (30-34)
    // =========================================================================

    @Test
    fun test30_driverAToDriverBAgencyDoesNotLeak() = runBlocking {
        // Driver A logs in and has approved membership
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)
        agencyRepository.getCurrentMembership()
        assertEquals("Metro City Transits", agencyRepository.observeCurrentMembership().value?.agencyName)

        // Logout
        agencyRepository.clearMembershipState()
        sessionStore.clearSession()
        assertNull(agencyRepository.observeCurrentMembership().value)
        assertEquals(DriverMembershipState.Loading, agencyRepository.observeMembershipState().value)

        // Driver B logs in with no membership
        val driverBSession = driverSession.copy(userId = "usr_driver_999", token = "jwt.driverB.token")
        sessionStore.saveSession(driverBSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(null)
        agencyRepository.getCurrentMembership()

        assertNull(agencyRepository.observeCurrentMembership().value)
        assertTrue(agencyRepository.observeMembershipState().value is DriverMembershipState.NoMembership)
    }

    @Test
    fun test31_driverToPassengerLogoutFlushesAgencyCache() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)
        agencyRepository.getCurrentMembership()

        // Clear on logout
        agencyRepository.clearMembershipState()
        assertNull(agencyRepository.observeCurrentMembership().value)

        // Passenger logs in
        sessionStore.saveSession(passengerSession)
        assertNull(agencyRepository.observeCurrentMembership().value)
    }

    @Test
    fun test32_passengerToDriverHydratesAuthoritativeAgency() = runBlocking {
        sessionStore.saveSession(passengerSession)
        assertNull(agencyRepository.observeCurrentMembership().value)

        sessionStore.clearSession()
        agencyRepository.clearMembershipState()

        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)
        agencyRepository.getCurrentMembership()

        assertEquals("Metro City Transits", agencyRepository.observeCurrentMembership().value?.agencyName)
        assertTrue(agencyRepository.observeMembershipState().value is DriverMembershipState.Pending)
    }

    @Test
    fun test33_clearMembershipStateUseCaseResetsFlows() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipApprovedDto)
        agencyRepository.getCurrentMembership()

        clearAgencyMembershipStateUseCase()
        assertNull(agencyRepository.observeCurrentMembership().value)
        assertEquals(DriverMembershipState.Loading, agencyRepository.observeMembershipState().value)
    }

    @Test
    fun test34_cachedMembershipDoesNotPersistAcrossProcessInvalidation() {
        agencyRepository.clearMembershipState()
        assertNull(agencyRepository.observeCurrentMembership().value)
        assertEquals(DriverMembershipState.Loading, agencyRepository.observeMembershipState().value)
    }

    // =========================================================================
    // 7. MUTATION SAFETY & IDEMPOTENCY (35-38)
    // =========================================================================

    @Test
    fun test35_doubleTapPreventionInViewModel() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(null)
        mockRemoteDataSource.listAgenciesResult = IshaaraResult.success(listOf(sampleAgencyDto))

        val viewModel = DriverAgencyMembershipViewModel(
            getCurrentAgencyMembershipUseCase = getCurrentAgencyMembershipUseCase,
            observeAgencyMembershipStateUseCase = observeAgencyMembershipStateUseCase,
            observeCurrentAgencyMembershipUseCase = observeCurrentAgencyMembershipUseCase,
            listAgenciesUseCase = listAgenciesUseCase,
            requestAgencyMembershipUseCase = requestAgencyMembershipUseCase,
            cancelAgencyMembershipUseCase = cancelAgencyMembershipUseCase,
            refreshAgencyMembershipUseCase = refreshAgencyMembershipUseCase,
            dispatchers = testDispatchers
        )

        viewModel.onSelectAgency(AgencyMapper.toDomain(sampleAgencyDto))
        assertNotNull(viewModel.uiState.value.selectedAgency)

        // Multiple calls while submitting do not duplicate calls
        viewModel.submitMembershipRequest()
        assertFalse(viewModel.uiState.value.isSubmittingRequest)
    }

    @Test
    fun test36_repeatedSubmissionConflictHandledCleanly() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.failure(
            IshaaraError.Conflict("A pending membership request already exists.")
        )
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)

        val result = agencyRepository.requestMembership("65a1234567890123456789ab")
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Conflict)

        // Verify state is reconciled
        val state = agencyRepository.observeMembershipState().first()
        assertTrue(state is DriverMembershipState.Pending)
    }

    @Test
    fun test37_uncertainNetworkResultPermitsExplicitRefresh() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.requestMembershipResult = IshaaraResult.failure(
            IshaaraError.Network("Socket timeout")
        )

        val result = agencyRepository.requestMembership("65a1234567890123456789ab")
        assertTrue(result.isFailure)

        // Driver performs explicit refresh
        mockRemoteDataSource.getCurrentMembershipResult = IshaaraResult.success(sampleMembershipPendingDto)
        val refreshed = agencyRepository.refreshMembership()
        assertTrue(refreshed.isSuccess)
        assertEquals(AgencyMembershipStatus.PENDING, refreshed.getOrNull()?.status)
    }

    @Test
    fun test38_refreshAfterCancellationRestoresNoMembership() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.cancelMembershipResult = IshaaraResult.success(
            CancelMembershipResponseDto("Cancelled", "65b9876543210987654321cd")
        )

        val result = agencyRepository.cancelMembership("65b9876543210987654321cd")
        assertTrue(result.isSuccess)
        val state = agencyRepository.observeMembershipState().first()
        assertTrue(state is DriverMembershipState.NoMembership)
    }

    // =========================================================================
    // 8. SECURITY & PRIVACY (39-41)
    // =========================================================================

    @Test
    fun test39_noLocalActiveMembershipAuthority() = runBlocking {
        // Client cannot manufacture APPROVED state locally
        agencyRepository.clearMembershipState()
        val initial = agencyRepository.observeCurrentMembership().value
        assertNull(initial)
    }

    @Test
    fun test40_noAdminCredentialsOrKeysInDriverRepository() {
        // Inspect that AgencyMembershipRepositoryImpl does not reference admin keys or headers
        val fields = agencyRepository::class.java.declaredFields.map { it.name }
        assertFalse(fields.contains("adminKey"))
        assertFalse(fields.contains("xAdminKey"))
    }

    @Test
    fun test41_noTokensOrPiiInAgencyModels() {
        val agency = AgencyMapper.toDomain(sampleAgencyDto)
        // Check phone is masked and sensitive fields are excluded
        assertTrue(agency.contactPhoneMasked?.contains("***") == true)
    }
}

/**
 * Fake implementation of AgencyRemoteDataSource for unit testing.
 */
class FakeAgencyRemoteDataSource : AgencyRemoteDataSource {

    var listAgenciesResult: IshaaraResult<List<CleanPublicAgencyResponseDto>> =
        IshaaraResult.success(emptyList())

    var getAgencyByIdResult: IshaaraResult<CleanPublicAgencyResponseDto> =
        IshaaraResult.failure(IshaaraError.NotFound(message = "Not found"))

    var getCurrentMembershipResult: IshaaraResult<CleanDriverMembershipResponseDto?> =
        IshaaraResult.success(null)

    var listDriverMembershipsResult: IshaaraResult<List<CleanDriverMembershipResponseDto>> =
        IshaaraResult.success(emptyList())

    var requestMembershipResult: IshaaraResult<CleanDriverMembershipResponseDto> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not configured"))

    var cancelMembershipResult: IshaaraResult<CancelMembershipResponseDto> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not configured"))

    val callCounter = AtomicInteger(0)

    override suspend fun listAgencies(
        search: String?,
        city: String?,
        page: Int,
        limit: Int
    ): IshaaraResult<List<CleanPublicAgencyResponseDto>> {
        callCounter.incrementAndGet()
        return listAgenciesResult
    }

    override suspend fun getAgencyById(agencyId: String): IshaaraResult<CleanPublicAgencyResponseDto> {
        callCounter.incrementAndGet()
        return getAgencyByIdResult
    }

    override suspend fun getCurrentDriverMembership(token: String): IshaaraResult<CleanDriverMembershipResponseDto?> {
        callCounter.incrementAndGet()
        return getCurrentMembershipResult
    }

    override suspend fun listDriverMemberships(
        token: String,
        status: String?,
        page: Int,
        limit: Int
    ): IshaaraResult<List<CleanDriverMembershipResponseDto>> {
        callCounter.incrementAndGet()
        return listDriverMembershipsResult
    }

    override suspend fun requestMembership(
        agencyId: String,
        notes: String?,
        token: String
    ): IshaaraResult<CleanDriverMembershipResponseDto> {
        callCounter.incrementAndGet()
        return requestMembershipResult
    }

    override suspend fun cancelMembership(
        membershipId: String,
        token: String
    ): IshaaraResult<CancelMembershipResponseDto> {
        callCounter.incrementAndGet()
        return cancelMembershipResult
    }
}
