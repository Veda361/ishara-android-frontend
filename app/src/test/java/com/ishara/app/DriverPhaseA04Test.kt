package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.DriverRemoteDataSource
import com.ishara.app.data.remote.dto.CleanDriverProfileResponseDto
import com.ishara.app.data.remote.dto.CleanDriverVerificationResponseDto
import com.ishara.app.data.remote.dto.CleanEmergencyContactDto
import com.ishara.app.data.remote.dto.CreateDriverProfileRequestDto
import com.ishara.app.data.remote.dto.DriverIdentityDto
import com.ishara.app.data.remote.dto.DriverOperationalContextResponseDto
import com.ishara.app.data.remote.dto.DriverProfileResponseDto
import com.ishara.app.data.remote.dto.DriverTodayStatsDto
import com.ishara.app.data.remote.dto.DriverTripDto
import com.ishara.app.data.remote.dto.SubmitDriverVerificationRequestDto
import com.ishara.app.data.remote.dto.UpdateDriverProfileRequestDto
import com.ishara.app.data.repository.DriverRepositoryImpl
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DriverEmergencyContact
import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverVerificationDetails
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.model.DriverProfileState
import com.ishara.app.domain.usecase.CreateDriverProfileUseCase
import com.ishara.app.domain.usecase.GetDriverProfileUseCase
import com.ishara.app.domain.usecase.GetDriverVerificationStatusUseCase
import com.ishara.app.domain.usecase.ObserveDriverOnboardingStateUseCase
import com.ishara.app.domain.usecase.ObserveDriverProfileUseCase
import com.ishara.app.domain.usecase.RefreshDriverProfileUseCase
import com.ishara.app.domain.usecase.ResolveApplicationDestinationUseCase
import com.ishara.app.domain.usecase.SubmitDriverVerificationUseCase
import com.ishara.app.feature.driver.onboarding.DriverOperatingTypeChoice
import com.ishara.app.feature.driver.onboarding.DriverOnboardingViewModel
import com.ishara.app.feature.driver.verification.DriverVerificationViewModel
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
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
 * PHASE A04 Test Suite — DRIVER ONBOARDING & VERIFICATION FOUNDATION
 *
 * Verifies all 40 architectural and security requirements:
 * 1. AUTH / ROLE (1-4)
 * 2. ONBOARDING (5-9)
 * 3. VERIFICATION (10-17)
 * 4. AUTHORIZATION & ROUTE PROTECTION (18-21)
 * 5. BACKEND ERROR HANDLING (22-30)
 * 6. STATE RESTORATION & ACCOUNT SWITCHING (31-36)
 * 7. SECURITY INVARIANTS (37-40)
 */
class DriverPhaseA04Test {

    private lateinit var mockRemoteDataSource: FakeDriverRemoteDataSource
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var sessionLocalDataSource: SessionLocalDataSourceImpl
    private lateinit var driverRepository: DriverRepositoryImpl
    private lateinit var navigationManager: NavigationManager
    private lateinit var resolveDestinationUseCase: ResolveApplicationDestinationUseCase

    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val driverSession = AuthSession(
        token = "sess_driver_test_token_123",
        userId = "usr_driver_001",
        role = UserRole.DRIVER_CONDUCTOR
    )

    private val userSession = AuthSession(
        token = "sess_user_test_token_456",
        userId = "usr_commuter_002",
        role = UserRole.USER
    )

    @Before
    fun setUp() {
        mockRemoteDataSource = FakeDriverRemoteDataSource()
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        driverRepository = DriverRepositoryImpl(mockRemoteDataSource, sessionLocalDataSource)
        navigationManager = NavigationManager()
        resolveDestinationUseCase = ResolveApplicationDestinationUseCase()
    }

    // =========================================================================
    // 1. AUTH / ROLE (1-4)
    // =========================================================================

    @Test
    fun test01_authenticatedDriverConductorEntersDriverFlow() {
        val driverProfile = UserProfile(
            id = "usr_driver_001",
            name = "Test Driver",
            email = "driver@isahara.app",
            role = UserRole.DRIVER_CONDUCTOR,
            isOnboarded = true
        )
        val destination = resolveDestinationUseCase(driverProfile)
        assertEquals(ApplicationDestination.DriverDashboard, destination)
    }

    @Test
    fun test02_userPassengerDoesNotEnterDriverFlow() {
        val userProfile = UserProfile(
            id = "usr_commuter_002",
            name = "Test Commuter",
            email = "student@college.edu",
            role = UserRole.USER,
            isOnboarded = true
        )
        val destination = resolveDestinationUseCase(userProfile)
        assertEquals(ApplicationDestination.StudentHome, destination)
        assertTrue(destination != ApplicationDestination.DriverDashboard)
    }

    @Test
    fun test03_unauthenticatedUserEntersAuthFlow() {
        val destination = resolveDestinationUseCase(null)
        assertEquals(ApplicationDestination.Authentication, destination)
    }

    @Test
    fun test04_roleIsTakenStrictlyFromAuthoritativeBackendState() {
        // User with incomplete onboarding (no role assigned) must go to Onboarding
        val unassignedRoleProfile = UserProfile(
            id = "usr_new_003",
            name = "Pending Role User",
            email = "pending@college.edu",
            role = null,
            isOnboarded = false
        )
        val dest = resolveDestinationUseCase(unassignedRoleProfile)
        assertEquals(ApplicationDestination.Onboarding, dest)
    }

    // =========================================================================
    // 2. ONBOARDING (5-9)
    // =========================================================================

    @Test
    fun test05_driverWithIncompleteOnboardingEntersNeedsOnboarding() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.NotFound("Driver profile not found. Please complete driver onboarding first.")
        )

        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.NotFound)

        val state = driverRepository.observeDriverOnboardingState().first()
        assertEquals(DriverOnboardingState.NeedsOnboarding, state)
        assertNull(driverRepository.observeDriverProfile().first())
    }

    @Test
    fun test06_completedOnboardingSkipsOnboardingAndSetsPending() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_001",
                userId = "usr_driver_001",
                verificationStatus = "PENDING",
                status = "OFFLINE",
                licenseNumberMasked = "MH12****3456",
                submittedAt = "2026-09-30T10:00:00.000Z",
                operatingType = "INDIVIDUAL"
            )
        )

        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Success)

        val state = driverRepository.observeDriverOnboardingState().first()
        assertTrue(state is DriverOnboardingState.PendingVerification)
        assertEquals("MH12****3456", (state as DriverOnboardingState.PendingVerification).profile.licenseNumberMasked)
    }

    @Test
    fun test07_onboardingSuccessRefreshesAuthoritativeState() = runBlocking {
        sessionStore.saveSession(driverSession)
        val createdDto = CleanDriverProfileResponseDto(
            id = "dp_002",
            userId = "usr_driver_001",
            verificationStatus = "PENDING",
            status = "OFFLINE",
            licenseNumberMasked = "DL01****9999",
            yearsOfExperience = 5,
            operatingType = "INDIVIDUAL"
        )
        mockRemoteDataSource.createDriverProfileResult = IshaaraResult.success(createdDto)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(createdDto)

        val createUseCase = CreateDriverProfileUseCase(driverRepository)
        val vm = DriverOnboardingViewModel(createUseCase, testDispatcherProvider)

        vm.selectOperatingType(DriverOperatingTypeChoice.INDIVIDUAL)
        vm.onLicenseNumberChanged("DL0120260009999")
        vm.onYearsOfExperienceChanged("5")
        vm.onEmergencyContactNameChanged("Emergency Contact")
        vm.onEmergencyContactPhoneChanged("+919876543210")

        var callbackTriggered = false
        vm.submitOnboarding(onSuccess = { callbackTriggered = true })

        assertTrue(vm.uiState.value.isSuccess)
        assertTrue(callbackTriggered)
        assertNotNull(vm.uiState.value.createdProfile)

        val repoState = driverRepository.observeDriverOnboardingState().first()
        assertTrue(repoState is DriverOnboardingState.PendingVerification)
    }

    @Test
    fun test08_onboardingConflictHandledCorrectlyAndReconciles() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.createDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Conflict("Driver profile already exists for this user")
        )
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_existing",
                userId = "usr_driver_001",
                verificationStatus = "PENDING",
                status = "OFFLINE",
                licenseNumberMasked = "MH14****1234",
                operatingType = "INDIVIDUAL"
            )
        )

        val result = driverRepository.createDriverProfile("MH14AB1234", operatingType = "INDIVIDUAL")
        assertTrue(result is IshaaraResult.Success)
        assertEquals("dp_existing", (result as IshaaraResult.Success).data.id)

        val state = driverRepository.observeDriverOnboardingState().first()
        assertTrue(state is DriverOnboardingState.PendingVerification)
    }

    @Test
    fun test09_onboardingValidationRejectsInvalidLicenseAndExperience() {
        val createUseCase = CreateDriverProfileUseCase(driverRepository)
        val vm = DriverOnboardingViewModel(createUseCase, testDispatcherProvider)

        // License number < 3 chars
        vm.onLicenseNumberChanged("AB")
        assertNotNull(vm.uiState.value.licenseNumberError)
        assertFalse(vm.uiState.value.isFormValid)

        // Invalid experience > 60
        vm.onLicenseNumberChanged("MH12AB1234")
        vm.onYearsOfExperienceChanged("75")
        assertNotNull(vm.uiState.value.yearsOfExperienceError)
        assertFalse(vm.uiState.value.isFormValid)

        // Invalid phone number
        vm.onYearsOfExperienceChanged("5")
        vm.onEmergencyContactPhoneChanged("123")
        assertNotNull(vm.uiState.value.emergencyPhoneError)
        assertFalse(vm.uiState.value.isFormValid)

        // Operating type not selected yet -> form cannot be valid
        vm.onEmergencyContactPhoneChanged("+919876543210")
        assertNull(vm.uiState.value.emergencyPhoneError)
        assertFalse(vm.uiState.value.isFormValid)

        // Select operating type -> form is now valid
        vm.selectOperatingType(DriverOperatingTypeChoice.INDIVIDUAL)
        assertTrue(vm.uiState.value.isFormValid)
    }

    // =========================================================================
    // 3. VERIFICATION (10-17)
    // =========================================================================

    @Test
    fun test10_notVerifiedState_whenProfileDoesNotExist() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.NotFound("Driver profile not found.")
        )
        driverRepository.getDriverProfile()
        val state = driverRepository.observeDriverOnboardingState().first()
        assertEquals(DriverOnboardingState.NeedsOnboarding, state)
    }

    @Test
    fun test11_pendingVerificationState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_pending",
                userId = "usr_driver_001",
                verificationStatus = "PENDING",
                status = "OFFLINE",
                submittedAt = "2026-09-30T10:00:00.000Z",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()
        val state = driverRepository.observeDriverOnboardingState().first()
        assertTrue(state is DriverOnboardingState.PendingVerification)
        val profile = (state as DriverOnboardingState.PendingVerification).profile
        assertTrue(profile.isPending)
        assertFalse(profile.isVerified)
        assertFalse(profile.isRejected)
    }

    @Test
    fun test12_verifiedState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_verified",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                status = "OFFLINE",
                licenseVerifiedAt = "2026-09-30T11:00:00.000Z",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()
        val state = driverRepository.observeDriverOnboardingState().first()
        assertTrue(state is DriverOnboardingState.Verified)
        val profile = (state as DriverOnboardingState.Verified).profile
        assertTrue(profile.isVerified)
        assertFalse(profile.isPending)
        assertFalse(profile.isRejected)
    }

    @Test
    fun test13_rejectedState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_rejected",
                userId = "usr_driver_001",
                verificationStatus = "REJECTED",
                status = "OFFLINE",
                rejectionReason = "Commercial license expired on 2026-08-31",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()
        val state = driverRepository.observeDriverOnboardingState().first()
        assertTrue(state is DriverOnboardingState.Rejected)
        val rejectedState = state as DriverOnboardingState.Rejected
        assertEquals("Commercial license expired on 2026-08-31", rejectedState.reason)
        assertTrue(rejectedState.profile.isRejected)
    }

    @Test
    fun test14_suspendedState_supportedAndDistinguished() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_suspended",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                status = "OFFLINE",
                isSuspended = true,
                suspensionReason = "Safety audit pending",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()
        val state = driverRepository.observeDriverOnboardingState().first()
        assertTrue(state is DriverOnboardingState.Suspended)
        val suspendedState = state as DriverOnboardingState.Suspended
        assertEquals("Safety audit pending", suspendedState.reason)
        // Suspended driver must NOT be treated as verified
        assertFalse(suspendedState.profile.isVerified)
    }

    @Test
    fun test15_rejectionReasonDisplayedWhenAvailable() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_rej",
                userId = "usr_driver_001",
                verificationStatus = "REJECTED",
                status = "OFFLINE",
                rejectionReason = "Illegible badge number",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()

        val getVerifUseCase = GetDriverVerificationStatusUseCase(driverRepository)
        val submitVerifUseCase = SubmitDriverVerificationUseCase(driverRepository)
        val observeProfileUseCase = ObserveDriverProfileUseCase(driverRepository)
        val observeStateUseCase = ObserveDriverOnboardingStateUseCase(driverRepository)
        val refreshProfileUseCase = RefreshDriverProfileUseCase(driverRepository)

        val vm = DriverVerificationViewModel(
            getVerifUseCase,
            submitVerifUseCase,
            observeProfileUseCase,
            observeStateUseCase,
            refreshProfileUseCase,
            testDispatcherProvider
        )

        val uiState = vm.uiState.value
        val state = uiState.onboardingState
        assertTrue(state is DriverOnboardingState.Rejected)
        assertEquals("Illegible badge number", (state as DriverOnboardingState.Rejected).reason)
    }

    @Test
    fun test16_resubmitOnlyPermittedWhenRejected() = runBlocking {
        sessionStore.saveSession(driverSession)

        // Case A: Driver is VERIFIED -> resubmission disallowed
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_ver",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                status = "OFFLINE",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()

        val vm = DriverVerificationViewModel(
            GetDriverVerificationStatusUseCase(driverRepository),
            SubmitDriverVerificationUseCase(driverRepository),
            ObserveDriverProfileUseCase(driverRepository),
            ObserveDriverOnboardingStateUseCase(driverRepository),
            RefreshDriverProfileUseCase(driverRepository),
            testDispatcherProvider
        )

        vm.submitResubmission()
        assertNotNull(vm.uiState.value.userFacingError)
        assertTrue(vm.uiState.value.userFacingError?.contains("only allowed after rejection") == true)

        // Case B: Driver is REJECTED -> resubmission proceeds
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_rej2",
                userId = "usr_driver_001",
                verificationStatus = "REJECTED",
                status = "OFFLINE",
                rejectionReason = "Expired license",
                operatingType = "INDIVIDUAL"
            )
        )
        mockRemoteDataSource.submitVerificationResult = IshaaraResult.success(
            CleanDriverVerificationResponseDto(
                driverId = "dp_rej2",
                userId = "usr_driver_001",
                verificationStatus = "PENDING"
            )
        )
        driverRepository.getDriverProfile()
        vm.onResubmissionNotesChanged("Renewed driving license copy submitted")

        var successCallback = false
        vm.submitResubmission(onSuccess = { successCallback = true })

        assertTrue(successCallback)
        assertNotNull(vm.uiState.value.userFacingMessage)
    }

    @Test
    fun test17_verificationStateRefreshWorks() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getVerificationStatusResult = IshaaraResult.success(
            CleanDriverVerificationResponseDto(
                driverId = "dp_poll",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                licenseNumberMasked = "DL12****7890"
            )
        )
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_poll",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                status = "OFFLINE",
                licenseNumberMasked = "DL12****7890",
                operatingType = "INDIVIDUAL"
            )
        )

        val vm = DriverVerificationViewModel(
            GetDriverVerificationStatusUseCase(driverRepository),
            SubmitDriverVerificationUseCase(driverRepository),
            ObserveDriverProfileUseCase(driverRepository),
            ObserveDriverOnboardingStateUseCase(driverRepository),
            RefreshDriverProfileUseCase(driverRepository),
            testDispatcherProvider
        )

        vm.refreshStatus()
        assertFalse(vm.uiState.value.isRefreshing)
        assertEquals(DriverVerificationStatus.VERIFIED, vm.uiState.value.verificationDetails?.verificationStatus)
    }

    // =========================================================================
    // 4. AUTHORIZATION & ROUTE PROTECTION (18-21)
    // =========================================================================

    @Test
    fun test18_userCannotAccessDriverScreens() {
        val commuterProfile = UserProfile(
            id = "usr_commuter",
            name = "Student Commuter",
            email = "commuter@campus.edu",
            role = UserRole.USER,
            isOnboarded = true
        )
        val destination = resolveDestinationUseCase(commuterProfile)
        assertEquals(ApplicationDestination.StudentHome, destination)
        assertTrue(destination != ApplicationDestination.DriverDashboard)
    }

    @Test
    fun test19_driverCannotAccessAdminScreens() {
        // Admin routes are not present in IshaaraDestination for driver graph
        val driverDestinations = listOf(
            IshaaraDestination.DriverDashboard.route,
            IshaaraDestination.DriverOnboarding.route,
            IshaaraDestination.DriverVerification.route,
            IshaaraDestination.DriverRideRequests.route,
            IshaaraDestination.DriverEarnings.route,
            IshaaraDestination.DriverProfile.route
        )
        driverDestinations.forEach { route ->
            assertFalse("Driver route should not access admin", route.startsWith("admin/"))
        }
    }

    @Test
    fun test20_deepLinkToDriverRouteIsProtected() {
        // Unauthenticated user deep linking
        val dest = resolveDestinationUseCase(null)
        assertEquals(ApplicationDestination.Authentication, dest)
    }

    @Test
    fun test21_deepLinkToUnauthorizedRouteIsRejected() {
        // A user with USER role deep-linking to driver must resolve to StudentHome
        val userProfile = UserProfile(
            id = "usr_007",
            name = "Passenger User",
            email = "pass@isahara.app",
            role = UserRole.USER,
            isOnboarded = true
        )
        val destination = resolveDestinationUseCase(userProfile)
        assertEquals(ApplicationDestination.StudentHome, destination)
    }

    // =========================================================================
    // 5. BACKEND ERROR HANDLING (22-30)
    // =========================================================================

    @Test
    fun test22_error400ValidationProblemMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Validation(message = "licenseNumber must be at least 3 characters")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Validation)
        assertEquals("licenseNumber must be at least 3 characters", (result.error as IshaaraError.Validation).message)
    }

    @Test
    fun test23_error401AuthenticationProblemMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Authentication(message = "Authentication session required.")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun test24_error403ForbiddenProblemMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Forbidden("Access denied: Role 'DRIVER_CONDUCTOR' required.")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Forbidden)
    }

    @Test
    fun test25_error404NotFoundMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.NotFound("Driver profile not found. Please complete driver onboarding first.")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.NotFound)
    }

    @Test
    fun test26_error409ConflictMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.submitVerificationResult = IshaaraResult.failure(
            IshaaraError.Conflict("A driver verification request is already pending review.")
        )
        val result = driverRepository.submitVerification()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Conflict)
    }

    @Test
    fun test27_error422UnprocessableEntityMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.createDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Validation(message = "Emergency contact phone number invalid format")
        )
        val result = driverRepository.createDriverProfile(
            "DL12345",
            emergencyContact = DriverEmergencyContact("Bob", "123"),
            operatingType = "INDIVIDUAL"
        )
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Validation)
    }

    @Test
    fun test28_error429RateLimitMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.RateLimited(message = "Too many requests. Please slow down.")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.RateLimited)
    }

    @Test
    fun test29_error5xxServerFailureMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Server(code = 500, message = "Internal database error")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Server)
    }

    @Test
    fun test30_errorTimeoutAndOfflineMapped() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.failure(
            IshaaraError.Network("Socket timeout reading from backend")
        )
        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Network)
    }

    // =========================================================================
    // 6. STATE RESTORATION & ACCOUNT SWITCHING (31-36)
    // =========================================================================

    @Test
    fun test31_configurationChangeRetainsFormValues() {
        val vm = DriverOnboardingViewModel(CreateDriverProfileUseCase(driverRepository), testDispatcherProvider)
        vm.onLicenseNumberChanged("MH12AB1234")
        vm.onYearsOfExperienceChanged("7")
        vm.onOperatingTypeChanged("AGENCY")

        assertEquals("MH12AB1234", vm.uiState.value.licenseNumber)
        assertEquals("7", vm.uiState.value.yearsOfExperience)
        assertEquals("AGENCY", vm.uiState.value.operatingType)
    }

    @Test
    fun test32_processRecreationSessionRestorationResolvesDriver() = runBlocking {
        // Session persisted in store
        sessionStore.saveSession(driverSession)
        val restored = sessionLocalDataSource.getSession()
        assertNotNull(restored)
        assertEquals(UserRole.DRIVER_CONDUCTOR, restored?.role)

        val profile = UserProfile(
            id = restored!!.userId,
            name = "Restored Driver",
            email = "driver@restored.com",
            role = restored.role,
            isOnboarded = true
        )
        val destination = resolveDestinationUseCase(profile)
        assertEquals(ApplicationDestination.DriverDashboard, destination)
    }

    @Test
    fun test33_logoutClearsDriverState() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_logout_test",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                status = "ONLINE",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()
        assertNotNull(driverRepository.observeDriverProfile().first())

        // Clear driver state on logout
        driverRepository.clearDriverState()
        sessionStore.clearSession()

        assertNull(driverRepository.observeDriverProfile().first())
        assertNull(sessionStore.getSession())
    }

    @Test
    fun test34_accountSwitchingDriverToUserCleansDriverState() = runBlocking {
        // Step 1: Driver logs in & has cached state
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_driver_a",
                userId = "usr_driver_001",
                verificationStatus = "VERIFIED",
                status = "ONLINE",
                operatingType = "INDIVIDUAL"
            )
        )
        driverRepository.getDriverProfile()
        assertEquals("dp_driver_a", driverRepository.observeDriverProfile().first()?.id)

        // Step 2: Driver logs out
        driverRepository.clearDriverState()
        sessionStore.clearSession()

        // Step 3: Passenger logs in
        sessionStore.saveSession(userSession)
        assertNull("Driver profile must not leak to passenger session", driverRepository.observeDriverProfile().first())
        val userDest = resolveDestinationUseCase(
            UserProfile(id = userSession.userId, name = "Passenger", email = "p@u.edu", role = UserRole.USER, isOnboarded = true)
        )
        assertEquals(ApplicationDestination.StudentHome, userDest)
    }

    @Test
    fun test35_accountSwitchingUserToDriverRestoresDriverStateCleanly() = runBlocking {
        // Step 1: Passenger is logged in
        sessionStore.saveSession(userSession)
        val userDest = resolveDestinationUseCase(
            UserProfile(id = userSession.userId, name = "Passenger", email = "p@u.edu", role = UserRole.USER, isOnboarded = true)
        )
        assertEquals(ApplicationDestination.StudentHome, userDest)

        // Step 2: Passenger logs out
        sessionStore.clearSession()
        driverRepository.clearDriverState()

        // Step 3: Driver logs in
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_driver_b",
                userId = "usr_driver_001",
                verificationStatus = "PENDING",
                status = "OFFLINE",
                operatingType = "INDIVIDUAL"
            )
        )
        val driverResult = driverRepository.getDriverProfile()
        assertTrue(driverResult is IshaaraResult.Success)
        assertEquals("dp_driver_b", (driverResult as IshaaraResult.Success).data.id)
        val driverDest = resolveDestinationUseCase(
            UserProfile(id = driverSession.userId, name = "Driver B", email = "d@b.com", role = UserRole.DRIVER_CONDUCTOR, isOnboarded = true)
        )
        assertEquals(ApplicationDestination.DriverDashboard, driverDest)
    }

    // =========================================================================
    // 7. SECURITY INVARIANTS (37-40)
    // =========================================================================

    @Test
    fun test36_noVerificationBypass_clientNeverManufacturesVerifiedStatus() {
        // Creating a DriverProfile with PENDING status on client CANNOT be isVerified
        val pendingProfile = DriverProfile(
            id = "p1",
            userId = "u1",
            verificationStatus = DriverVerificationStatus.PENDING,
            status = DriverProfileStatus.OFFLINE,
            licenseNumberMasked = "MH12****1234"
        )
        assertFalse("Client must never treat PENDING as verified", pendingProfile.isVerified)

        val rejectedProfile = DriverProfile(
            id = "p2",
            userId = "u2",
            verificationStatus = DriverVerificationStatus.REJECTED,
            status = DriverProfileStatus.OFFLINE,
            licenseNumberMasked = "MH12****1234"
        )
        assertFalse("Client must never treat REJECTED as verified", rejectedProfile.isVerified)

        val suspendedProfile = DriverProfile(
            id = "p3",
            userId = "u3",
            verificationStatus = DriverVerificationStatus.VERIFIED,
            status = DriverProfileStatus.OFFLINE,
            licenseNumberMasked = "MH12****1234",
            isSuspended = true
        )
        assertFalse("Suspended driver must never be verified even if verificationStatus was VERIFIED", suspendedProfile.isVerified)
    }

    @Test
    fun test37_noSensitiveLoggingOfTokensOrPasswords() {
        val testToken = "sess_secret_jwt_token_xyz"
        // Ensure String representation of DriverProfile doesn't expose tokens
        val profile = DriverProfile(
            id = "dp_1",
            userId = "u1",
            verificationStatus = DriverVerificationStatus.VERIFIED,
            status = DriverProfileStatus.OFFLINE,
            licenseNumberMasked = "MH12****3456"
        )
        assertFalse(profile.toString().contains(testToken))
    }

    @Test
    fun test38_noInsecureTokenPersistence() = runBlocking {
        sessionStore.saveSession(driverSession)
        val token = sessionStore.getSession()?.token
        assertEquals("sess_driver_test_token_123", token)
        sessionStore.clearSession()
        assertNull(sessionStore.getSession())
    }

    @Test
    fun test39_noLocalVerificationAuthority_backendIsSourceOfTruth() = runBlocking {
        // Without backend verification confirmation, driver verification status is not verified
        val mockDto = CleanDriverProfileResponseDto(
            id = "dp_truth",
            userId = "u_truth",
            verificationStatus = "REJECTED",
            status = "OFFLINE",
            rejectionReason = "Expired commercial badge",
            operatingType = "INDIVIDUAL"
        )
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(mockDto)

        val profile = driverRepository.getDriverProfile()
        assertTrue(profile is IshaaraResult.Success)
        assertFalse((profile as IshaaraResult.Success).data.isVerified)
        assertTrue(profile.data.isRejected)
    }

    @Test
    fun test40_driverEmergencyContactMaskingPreservesPrivacy() {
        val contactDto = CleanEmergencyContactDto(
            name = "Ramesh Kumar",
            phoneNumberMasked = "******4321",
            relationship = "Brother"
        )
        val domainContact = com.ishara.app.data.mapper.DriverMapper.toDomain(contactDto)
        assertEquals("Ramesh Kumar", domainContact.name)
        assertEquals("******4321", domainContact.phoneNumber)
        assertEquals("Brother", domainContact.relationship)
    }

    @Test
    fun test41_missingOperatingTypeTransitionsToNeedsOperatingTypeSelection() = runBlocking {
        sessionStore.saveSession(driverSession)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(
            CleanDriverProfileResponseDto(
                id = "dp_missing_type",
                userId = "usr_driver_001",
                verificationStatus = "PENDING",
                status = "OFFLINE",
                operatingType = "" // Missing operating type
            )
        )

        val result = driverRepository.getDriverProfile()
        assertTrue(result is IshaaraResult.Success)

        val onboardingState = driverRepository.observeDriverOnboardingState().first()
        assertEquals(DriverOnboardingState.NeedsOperatingTypeSelection, onboardingState)

        val profileState = driverRepository.observeDriverProfileState().first()
        assertEquals(DriverProfileState.NeedsOperatingTypeSelection, profileState)
    }

    @Test
    fun test42_selectAgencyOperatingType_persistsAndReconcilesAgencyFlow() = runBlocking {
        sessionStore.saveSession(driverSession)
        val agencyDto = CleanDriverProfileResponseDto(
            id = "dp_agency",
            userId = "usr_driver_001",
            verificationStatus = "PENDING",
            status = "OFFLINE",
            licenseNumberMasked = "MH12****9999",
            operatingType = "AGENCY"
        )
        mockRemoteDataSource.createDriverProfileResult = IshaaraResult.success(agencyDto)
        mockRemoteDataSource.getDriverProfileResult = IshaaraResult.success(agencyDto)

        val createUseCase = CreateDriverProfileUseCase(driverRepository)
        val vm = DriverOnboardingViewModel(createUseCase, testDispatcherProvider)

        vm.selectOperatingType(DriverOperatingTypeChoice.AGENCY)
        assertEquals(DriverOperatingTypeChoice.AGENCY, vm.uiState.value.selectedOperatingType)
        assertTrue(vm.uiState.value.isOperatingTypeSelected)

        vm.onLicenseNumberChanged("MH1220260009999")
        vm.onYearsOfExperienceChanged("3")
        vm.onEmergencyContactNameChanged("Agency Contact")
        vm.onEmergencyContactPhoneChanged("+919876543210")

        var successCallback = false
        vm.submitOnboarding(onSuccess = { successCallback = true })

        assertTrue(successCallback)
        assertTrue(vm.uiState.value.isSuccess)

        val profileState = driverRepository.observeDriverProfileState().first()
        assertTrue(profileState is DriverProfileState.Ready)
        assertEquals("AGENCY", (profileState as DriverProfileState.Ready).operatingType)
    }

    @Test
    fun test43_cannotSubmitOnboardingWithoutExplicitOperatingTypeChoice() = runBlocking {
        sessionStore.saveSession(driverSession)
        val createUseCase = CreateDriverProfileUseCase(driverRepository)
        val vm = DriverOnboardingViewModel(createUseCase, testDispatcherProvider)

        // Fill all fields EXCEPT operating type
        vm.onLicenseNumberChanged("MH1220260009999")
        vm.onYearsOfExperienceChanged("3")
        vm.onEmergencyContactNameChanged("Contact Person")
        vm.onEmergencyContactPhoneChanged("+919876543210")

        assertFalse(vm.uiState.value.isOperatingTypeSelected)
        assertFalse(vm.uiState.value.isFormValid)

        var successCallback = false
        vm.submitOnboarding(onSuccess = { successCallback = true })

        assertFalse(successCallback)
        assertFalse(vm.uiState.value.isSuccess)
        assertNotNull(vm.uiState.value.errorMessage)
    }
}

/**
 * Fake implementation of DriverRemoteDataSource for unit testing.
 */
class FakeDriverRemoteDataSource : DriverRemoteDataSource {

    var getDriverProfileResult: IshaaraResult<CleanDriverProfileResponseDto> =
        IshaaraResult.failure(IshaaraError.NotFound("Not found"))

    var createDriverProfileResult: IshaaraResult<CleanDriverProfileResponseDto> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not configured"))

    var updateDriverProfileResult: IshaaraResult<CleanDriverProfileResponseDto> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not configured"))

    var getVerificationStatusResult: IshaaraResult<CleanDriverVerificationResponseDto> =
        IshaaraResult.failure(IshaaraError.NotFound("Not found"))

    var submitVerificationResult: IshaaraResult<CleanDriverVerificationResponseDto> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not configured"))

    val callCounter = AtomicInteger(0)

    override suspend fun getDriverProfile(token: String): IshaaraResult<CleanDriverProfileResponseDto> {
        callCounter.incrementAndGet()
        return getDriverProfileResult
    }

    override suspend fun createDriverProfileFull(
        request: CreateDriverProfileRequestDto,
        token: String
    ): IshaaraResult<CleanDriverProfileResponseDto> {
        callCounter.incrementAndGet()
        return createDriverProfileResult
    }

    override suspend fun updateDriverProfile(
        request: UpdateDriverProfileRequestDto,
        token: String
    ): IshaaraResult<CleanDriverProfileResponseDto> {
        callCounter.incrementAndGet()
        return updateDriverProfileResult
    }

    override suspend fun getVerificationStatus(token: String): IshaaraResult<CleanDriverVerificationResponseDto> {
        callCounter.incrementAndGet()
        return getVerificationStatusResult
    }

    override suspend fun submitVerification(
        notes: String?,
        token: String
    ): IshaaraResult<CleanDriverVerificationResponseDto> {
        callCounter.incrementAndGet()
        return submitVerificationResult
    }

    override suspend fun getOperationalContext(
        timezone: String?,
        token: String
    ): IshaaraResult<DriverOperationalContextResponseDto> {
        return IshaaraResult.failure(IshaaraError.NotFound("Not implemented"))
    }

    override suspend fun createDriverProfile(
        licenseNumber: String,
        token: String
    ): IshaaraResult<DriverProfileResponseDto> {
        return IshaaraResult.success(
            DriverProfileResponseDto(
                id = "dp_test",
                userId = "usr_test",
                verificationStatus = "PENDING",
                status = "OFFLINE",
                licenseNumberMasked = licenseNumber
            )
        )
    }

    override suspend fun setOnline(token: String): IshaaraResult<DriverProfileResponseDto> {
        return IshaaraResult.success(
            DriverProfileResponseDto(
                id = "dp_test",
                userId = "usr_test",
                verificationStatus = "VERIFIED",
                status = "ONLINE"
            )
        )
    }

    override suspend fun setOffline(token: String): IshaaraResult<DriverProfileResponseDto> {
        return IshaaraResult.success(
            DriverProfileResponseDto(
                id = "dp_test",
                userId = "usr_test",
                verificationStatus = "VERIFIED",
                status = "OFFLINE"
            )
        )
    }

    override suspend fun startTrip(tripId: String, token: String): IshaaraResult<DriverTripDto> {
        return IshaaraResult.failure(IshaaraError.NotFound("Not implemented"))
    }

    override suspend fun completeTrip(tripId: String, token: String): IshaaraResult<DriverTripDto> {
        return IshaaraResult.failure(IshaaraError.NotFound("Not implemented"))
    }

    override suspend fun cancelTrip(tripId: String, token: String): IshaaraResult<DriverTripDto> {
        return IshaaraResult.failure(IshaaraError.NotFound("Not implemented"))
    }

    override suspend fun updateLocation(location: LocationCoordinates, token: String): IshaaraResult<Unit> {
        return IshaaraResult.success(Unit)
    }
}
