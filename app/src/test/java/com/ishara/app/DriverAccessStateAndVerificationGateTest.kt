package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.AgencyMembershipStatus
import com.ishara.app.domain.model.DriverAccessState
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverTodayStats
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.repository.AgencyMembershipRepository
import com.ishara.app.domain.repository.DriverRepository
import com.ishara.app.domain.usecase.GetDriverOperationalContextUseCase
import com.ishara.app.domain.usecase.ManageDriverTripLifecycleUseCase
import com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase
import com.ishara.app.domain.usecase.ObserveDriverProfileUseCase
import com.ishara.app.domain.usecase.RefreshDriverProfileUseCase
import com.ishara.app.domain.usecase.SetDriverAvailabilityUseCase
import com.ishara.app.feature.driver.DriverHomeStage
import com.ishara.app.feature.driver.DriverViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverAccessStateAndVerificationGateTest {

    private val testDispatcher = Dispatchers.Unconfined
    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val activeAgencyMembership = DriverAgencyMembership(
        id = "mem_123",
        agencyId = "agn_456",
        agencyName = "fdggbv",
        agencyCity = "Jhansi",
        status = AgencyMembershipStatus.APPROVED,
        requestedAt = "2026-10-01T10:00:00Z",
        respondedAt = "2026-10-01T11:00:00Z"
    )

    @Test
    fun `State A - UNDER_REVIEW driver with ACTIVE membership can access app but operations are locked`() {
        val access = DriverAccessState.resolve(
            verificationStatus = DriverVerificationStatus.PENDING,
            isSuspended = false,
            operatingType = "AGENCY",
            membershipState = DriverMembershipState.Approved(activeAgencyMembership)
        )

        assertTrue("App must be accessible for under review driver", access.appAccessible)
        assertFalse("Platform is not verified", access.platformVerified)
        assertTrue("Membership is active", access.membershipActive)
        assertEquals("fdggbv", access.agencyName)
        assertEquals(AgencyMembershipStatus.APPROVED, access.membershipStatus)
        assertFalse("Operational access MUST be locked for under review driver", access.operationalAccess)
        assertNotNull("Lock reason must be explained", access.operationalLockReason)
        assertTrue(access.operationalLockReason!!.contains("verification is complete", ignoreCase = true))
    }

    @Test
    fun `State B - VERIFIED driver with ACTIVE membership has operational access unlocked`() {
        val access = DriverAccessState.resolve(
            verificationStatus = DriverVerificationStatus.VERIFIED,
            isSuspended = false,
            operatingType = "AGENCY",
            membershipState = DriverMembershipState.Approved(activeAgencyMembership)
        )

        assertTrue(access.appAccessible)
        assertTrue(access.platformVerified)
        assertTrue(access.membershipActive)
        assertEquals("fdggbv", access.agencyName)
        assertTrue("Operational access unlocked when verified with active membership", access.operationalAccess)
        assertNull(access.operationalLockReason)
    }

    @Test
    fun `State C - VERIFIED agency driver with NO active membership has operations locked until fleet joined`() {
        val access = DriverAccessState.resolve(
            verificationStatus = DriverVerificationStatus.VERIFIED,
            isSuspended = false,
            operatingType = "AGENCY",
            membershipState = DriverMembershipState.NoMembership
        )

        assertTrue(access.appAccessible)
        assertTrue(access.platformVerified)
        assertFalse(access.membershipActive)
        assertFalse("Operations locked because agency driver has no active fleet", access.operationalAccess)
        assertNotNull(access.operationalLockReason)
        assertTrue(access.operationalLockReason!!.contains("fleet affiliation", ignoreCase = true))
    }

    @Test
    fun `State D - UNDER_REVIEW driver with NO membership has operations locked and fleet required`() {
        val access = DriverAccessState.resolve(
            verificationStatus = DriverVerificationStatus.PENDING,
            isSuspended = false,
            operatingType = "AGENCY",
            membershipState = DriverMembershipState.NoMembership
        )

        assertTrue(access.appAccessible)
        assertFalse(access.platformVerified)
        assertFalse(access.membershipActive)
        assertFalse(access.operationalAccess)
        assertNotNull(access.operationalLockReason)
    }

    @Test
    fun `State E - REJECTED driver has app accessible but operations locked with rejection guidance`() {
        val access = DriverAccessState.resolve(
            verificationStatus = DriverVerificationStatus.REJECTED,
            isSuspended = false,
            operatingType = "AGENCY",
            membershipState = DriverMembershipState.Approved(activeAgencyMembership)
        )

        assertTrue(access.appAccessible)
        assertFalse(access.platformVerified)
        assertFalse(access.operationalAccess)
        assertNotNull(access.operationalLockReason)
        assertTrue(access.operationalLockReason!!.contains("not approved", ignoreCase = true))
    }

    @Test
    fun `State F - Suspended driver has operations locked regardless of verification`() {
        val access = DriverAccessState.resolve(
            verificationStatus = DriverVerificationStatus.VERIFIED,
            isSuspended = true,
            operatingType = "AGENCY",
            membershipState = DriverMembershipState.Approved(activeAgencyMembership)
        )

        assertTrue(access.appAccessible)
        assertFalse(access.platformVerified)
        assertFalse(access.operationalAccess)
        assertNotNull(access.operationalLockReason)
        assertTrue(access.operationalLockReason!!.contains("suspended", ignoreCase = true))
    }

    @Test
    fun `DriverViewModel handles DRIVER_NOT_VERIFIED without crashing or blocking home access`() = runBlocking {
        val profileFlow = MutableStateFlow<DriverProfile?>(
            DriverProfile(
                id = "drv_123",
                userId = "usr_123",
                status = DriverProfileStatus.OFFLINE,
                licenseNumberMasked = "DL••••1234",
                verificationStatus = DriverVerificationStatus.PENDING,
                operatingType = "AGENCY",
                isSuspended = false,
                createdAt = "2026-10-01T10:00:00Z",
                updatedAt = "2026-10-01T10:00:00Z"
            )
        )
        val membershipFlow = MutableStateFlow<DriverMembershipState>(
            DriverMembershipState.Approved(activeAgencyMembership)
        )

        val unverifiedErrorRepo = object : DriverRepository {
            override suspend fun getOperationalContext(timezone: String?): IshaaraResult<DriverOperationalContext> {
                return IshaaraResult.failure(
                    IshaaraError.Forbidden(
                        message = "DRIVER_NOT_VERIFIED: Driver profile is awaiting administrative verification",
                        errorCode = "DRIVER_NOT_VERIFIED"
                    )
                )
            }
            override suspend fun setOnline(): IshaaraResult<DriverIdentity> =
                IshaaraResult.failure(IshaaraError.Forbidden("DRIVER_NOT_VERIFIED", "DRIVER_NOT_VERIFIED"))
            override suspend fun setOffline(): IshaaraResult<DriverIdentity> =
                IshaaraResult.failure(IshaaraError.Forbidden("DRIVER_NOT_VERIFIED", "DRIVER_NOT_VERIFIED"))
            override suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip> =
                IshaaraResult.failure(IshaaraError.Forbidden("DRIVER_NOT_VERIFIED", "DRIVER_NOT_VERIFIED"))
            override suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip> =
                IshaaraResult.failure(IshaaraError.Forbidden("DRIVER_NOT_VERIFIED", "DRIVER_NOT_VERIFIED"))
            override suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip> =
                IshaaraResult.failure(IshaaraError.Forbidden("DRIVER_NOT_VERIFIED", "DRIVER_NOT_VERIFIED"))
            override suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit> =
                IshaaraResult.success(Unit)
            override fun observeDriverProfile(): StateFlow<DriverProfile?> = profileFlow
        }

        val mockAgencyRepo = object : AgencyMembershipRepository {
            override suspend fun getCurrentMembership(): IshaaraResult<DriverAgencyMembership?> =
                IshaaraResult.success(activeAgencyMembership)
            override fun observeCurrentMembership(): StateFlow<DriverAgencyMembership?> =
                MutableStateFlow(activeAgencyMembership)
            override fun observeMembershipState(): StateFlow<DriverMembershipState> = membershipFlow
            override suspend fun listAgencies(search: String?, city: String?): IshaaraResult<List<Agency>> =
                IshaaraResult.success(emptyList())
            override suspend fun getAgency(agencyId: String): IshaaraResult<Agency> =
                IshaaraResult.failure(IshaaraError.NotFound())
            override suspend fun requestMembership(agencyId: String, notes: String?): IshaaraResult<DriverAgencyMembership> =
                IshaaraResult.failure(IshaaraError.Unknown("Not used"))
            override suspend fun cancelMembership(membershipId: String): IshaaraResult<Unit> =
                IshaaraResult.success(Unit)
            override suspend fun refreshMembership(): IshaaraResult<DriverAgencyMembership?> =
                IshaaraResult.success(activeAgencyMembership)
            override fun clearMembershipState() {}
        }

        val getOperationalContextUseCase = GetDriverOperationalContextUseCase(unverifiedErrorRepo)
        val setAvailabilityUseCase = SetDriverAvailabilityUseCase(unverifiedErrorRepo)
        val manageTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(unverifiedErrorRepo)
        val navigationManager = NavigationManager()
        val observeAgencyMembershipStateUseCase = ObserveAgencyMembershipStateUseCase(mockAgencyRepo)
        val observeDriverProfileUseCase = ObserveDriverProfileUseCase(unverifiedErrorRepo)

        val viewModel = DriverViewModel(
            driverName = "Test Driver",
            getDriverOperationalContextUseCase = getOperationalContextUseCase,
            setDriverAvailabilityUseCase = setAvailabilityUseCase,
            manageDriverTripLifecycleUseCase = manageTripLifecycleUseCase,
            navigationManager = navigationManager,
            observeAgencyMembershipStateUseCase = observeAgencyMembershipStateUseCase,
            observeDriverProfileUseCase = observeDriverProfileUseCase,
            dispatchers = testDispatchers
        )

        // Verify stage is accessible (Offline or Content stage with unverified offline context, not an Error stage)
        val state = viewModel.uiState.value
        assertTrue("Driver Home should be in Offline stage, got: ${state.stage}", state.stage is DriverHomeStage.Offline)
        assertFalse("Operational access must be locked", state.accessState.operationalAccess)
        assertEquals("fdggbv", state.accessState.agencyName)
        assertTrue("Membership is active", state.accessState.membershipActive)

        // Attempting to go online must be guarded and rejected with clear error message
        viewModel.goOnline()
        assertNotNull(viewModel.uiState.value.userFacingError)
        assertTrue(viewModel.uiState.value.userFacingError!!.contains("verification", ignoreCase = true))

        // Attempting to start trip must be guarded
        viewModel.startTrip("trip_123")
        assertTrue(viewModel.uiState.value.userFacingError!!.contains("verification", ignoreCase = true))
    }

    @Test
    fun `Verification upgrade dynamically unlocks operational features after refresh`() = runBlocking {
        val profileFlow = MutableStateFlow<DriverProfile?>(
            DriverProfile(
                id = "drv_123",
                userId = "usr_123",
                status = DriverProfileStatus.OFFLINE,
                licenseNumberMasked = "DL••••1234",
                verificationStatus = DriverVerificationStatus.PENDING,
                operatingType = "AGENCY",
                isSuspended = false,
                createdAt = "2026-10-01T10:00:00Z",
                updatedAt = "2026-10-01T10:00:00Z"
            )
        )
        val membershipFlow = MutableStateFlow<DriverMembershipState>(
            DriverMembershipState.Approved(activeAgencyMembership)
        )

        val repo = object : DriverRepository {
            override suspend fun getOperationalContext(timezone: String?): IshaaraResult<DriverOperationalContext> {
                val isVerified = profileFlow.value?.verificationStatus == DriverVerificationStatus.VERIFIED
                return if (isVerified) {
                    IshaaraResult.success(
                        DriverOperationalContext(
                            driver = DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.ONLINE, "DL••••1234", null),
                            vehicle = null,
                            activeTrip = null,
                            activeRidesCount = 0,
                            todayStats = DriverTodayStats(0, true, "2026-10-01", "Asia/Kolkata")
                        )
                    )
                } else {
                    IshaaraResult.failure(IshaaraError.Forbidden("DRIVER_NOT_VERIFIED", "DRIVER_NOT_VERIFIED"))
                }
            }
            override suspend fun setOnline(): IshaaraResult<DriverIdentity> =
                IshaaraResult.success(DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.ONLINE, "DL••••1234", null))
            override suspend fun setOffline(): IshaaraResult<DriverIdentity> =
                IshaaraResult.success(DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.OFFLINE, "DL••••1234", null))
            override suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip> = IshaaraResult.failure(IshaaraError.NotFound())
            override suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip> = IshaaraResult.failure(IshaaraError.NotFound())
            override suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip> = IshaaraResult.failure(IshaaraError.NotFound())
            override suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit> = IshaaraResult.success(Unit)
            override fun observeDriverProfile(): StateFlow<DriverProfile?> = profileFlow
            override suspend fun refreshDriverProfile(): IshaaraResult<DriverProfile> = IshaaraResult.success(profileFlow.value!!)
        }

        val mockAgencyRepo = object : AgencyMembershipRepository {
            override suspend fun getCurrentMembership(): IshaaraResult<DriverAgencyMembership?> =
                IshaaraResult.success(activeAgencyMembership)
            override fun observeCurrentMembership(): StateFlow<DriverAgencyMembership?> =
                MutableStateFlow(activeAgencyMembership)
            override fun observeMembershipState(): StateFlow<DriverMembershipState> = membershipFlow
            override suspend fun listAgencies(search: String?, city: String?): IshaaraResult<List<Agency>> =
                IshaaraResult.success(emptyList())
            override suspend fun getAgency(agencyId: String): IshaaraResult<Agency> =
                IshaaraResult.failure(IshaaraError.NotFound())
            override suspend fun requestMembership(agencyId: String, notes: String?): IshaaraResult<DriverAgencyMembership> =
                IshaaraResult.failure(IshaaraError.Unknown("Not used"))
            override suspend fun cancelMembership(membershipId: String): IshaaraResult<Unit> =
                IshaaraResult.success(Unit)
            override suspend fun refreshMembership(): IshaaraResult<DriverAgencyMembership?> =
                IshaaraResult.success(activeAgencyMembership)
            override fun clearMembershipState() {}
        }

        val getOperationalContextUseCase = GetDriverOperationalContextUseCase(repo)
        val setAvailabilityUseCase = SetDriverAvailabilityUseCase(repo)
        val manageTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(repo)
        val navigationManager = NavigationManager()
        val observeAgencyMembershipStateUseCase = ObserveAgencyMembershipStateUseCase(mockAgencyRepo)
        val observeDriverProfileUseCase = ObserveDriverProfileUseCase(repo)
        val refreshDriverProfileUseCase = RefreshDriverProfileUseCase(repo)

        val viewModel = DriverViewModel(
            driverName = "Test Driver",
            getDriverOperationalContextUseCase = getOperationalContextUseCase,
            setDriverAvailabilityUseCase = setAvailabilityUseCase,
            manageDriverTripLifecycleUseCase = manageTripLifecycleUseCase,
            navigationManager = navigationManager,
            observeAgencyMembershipStateUseCase = observeAgencyMembershipStateUseCase,
            observeDriverProfileUseCase = observeDriverProfileUseCase,
            refreshDriverProfileUseCase = refreshDriverProfileUseCase,
            dispatchers = testDispatchers
        )

        assertFalse(viewModel.uiState.value.accessState.operationalAccess)

        // Administrative verification approval occurs on server
        profileFlow.value = profileFlow.value!!.copy(verificationStatus = DriverVerificationStatus.VERIFIED)
        viewModel.refresh()

        // Operational capabilities are now unlocked without app reinstall or re-login!
        assertTrue(viewModel.uiState.value.accessState.operationalAccess)
        assertTrue(viewModel.uiState.value.accessState.platformVerified)
        assertNull(viewModel.uiState.value.accessState.operationalLockReason)
    }
}
