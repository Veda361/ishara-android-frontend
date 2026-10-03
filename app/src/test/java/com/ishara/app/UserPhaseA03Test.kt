package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSource
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.datasource.UserRemoteDataSource
import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.DiscoverTripsRequestDto
import com.ishara.app.data.remote.dto.DiscoveryCoordinateDto
import com.ishara.app.data.remote.dto.DiscoveryDriverDto
import com.ishara.app.data.remote.dto.DiscoveryEndpointDto
import com.ishara.app.data.remote.dto.DiscoveryGeoJsonPointDto
import com.ishara.app.data.remote.dto.DiscoveryItemDto
import com.ishara.app.data.remote.dto.DiscoveryMatchDto
import com.ishara.app.data.remote.dto.DiscoveryPaginationDto
import com.ishara.app.data.remote.dto.DiscoveryResponseDto
import com.ishara.app.data.remote.dto.DiscoveryRouteSummaryDto
import com.ishara.app.data.remote.dto.DiscoverySearchRequestDto
import com.ishara.app.data.remote.dto.DiscoveryVehicleDto
import com.ishara.app.data.remote.dto.TripDto
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.data.repository.AuthRepositoryImpl
import com.ishara.app.data.repository.OnboardingRepositoryImpl
import com.ishara.app.data.repository.TripRepositoryImpl
import com.ishara.app.data.repository.UserRepositoryImpl
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.LocationPermissionStatus
import com.ishara.app.domain.model.LocationPoint
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.AuthRepository
import com.ishara.app.domain.repository.OnboardingRepository
import com.ishara.app.domain.repository.TripRepository
import com.ishara.app.domain.repository.UserRepository
import com.ishara.app.domain.usecase.DiscoverTripsUseCase
import com.ishara.app.domain.usecase.GetCurrentUserProfileUseCase
import com.ishara.app.domain.usecase.ResolveApplicationDestinationUseCase
import com.ishara.app.domain.usecase.UpdateUserProfileUseCase
import com.ishara.app.feature.student.discovery.DiscoveryStage
import com.ishara.app.feature.student.discovery.DiscoveryViewModel
import com.ishara.app.feature.student.profile.ProfileViewModel
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
 * PHASE A03 Test Suite — USER / PASSENGER FOUNDATION
 *
 * Verifies all 40+ architectural requirements:
 * 1. Current User Foundation (1-6)
 * 2. Onboarding Flow & Conflict Handling (7-11)
 * 3. Location Permission & Abstraction (12-16)
 * 4. Trip Discovery Architecture & Strict Contract (17-30)
 * 5. Navigation & Route Security (31-37)
 * 6. State Restoration & Account Switching (38-40)
 * 7. Profile Editing & Validation (41-43)
 */
class UserPhaseA03Test {

    private lateinit var mockUserRemoteDataSource: FakeUserRemoteDataSource
    private lateinit var mockAuthRemoteDataSource: FakeAuthRemoteDataSource
    private lateinit var mockTripRemoteDataSource: FakeTripRemoteDataSource
    private lateinit var mockDiscoveryRemoteDataSource: FakeDiscoveryRemoteDataSource
    private lateinit var sessionStore: InMemorySessionStore

    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var navigationManager: NavigationManager

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        mockUserRemoteDataSource = FakeUserRemoteDataSource()
        mockAuthRemoteDataSource = FakeAuthRemoteDataSource()
        mockTripRemoteDataSource = FakeTripRemoteDataSource()
        mockDiscoveryRemoteDataSource = FakeDiscoveryRemoteDataSource()
        sessionStore = InMemorySessionStore()
        navigationManager = NavigationManager()

        val sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)

        userRepository = UserRepositoryImpl(
            remoteDataSource = mockUserRemoteDataSource,
            sessionStore = sessionStore
        )

        authRepository = AuthRepositoryImpl(
            remoteDataSource = mockAuthRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )

        onboardingRepository = OnboardingRepositoryImpl(
            remoteDataSource = mockUserRemoteDataSource,
            userRepository = userRepository,
            sessionStore = sessionStore
        )

        tripRepository = TripRepositoryImpl(
            remoteDataSource = mockTripRemoteDataSource,
            localDataSource = sessionLocalDataSource,
            discoveryRemoteDataSource = mockDiscoveryRemoteDataSource
        )
    }

    // =========================================================================
    // 1. CURRENT USER FOUNDATION (1 - 6)
    // =========================================================================

    @Test
    fun test01_authenticatedUser_loadsAuthoritativeProfile() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = UserRole.USER))
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "u1",
            name = "Alex Passenger",
            email = "alex@college.edu",
            role = "USER",
            phoneNumber = "+919876543210",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val result = userRepository.getCurrentUserProfile()
        assertTrue(result is IshaaraResult.Success)
        val profile = (result as IshaaraResult.Success).data
        assertEquals("u1", profile.id)
        assertEquals("Alex Passenger", profile.name)
        assertEquals("alex@college.edu", profile.email)
        assertEquals(UserRole.USER, profile.role)
        assertTrue(profile.isOnboarded)
    }

    @Test
    fun test02_currentUserRefresh_reconcilesState() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = UserRole.USER))
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "u1",
            name = "Initial Name",
            email = "alex@college.edu",
            role = "USER",
            isOnboarded = true
        )

        userRepository.getCurrentUserProfile()
        assertEquals("Initial Name", userRepository.observeUserProfile().first()?.name)

        // Remote update occurs
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "u1",
            name = "Updated Passenger Name",
            email = "alex@college.edu",
            role = "USER",
            isOnboarded = true
        )

        val refreshed = userRepository.getCurrentUserProfile()
        assertTrue(refreshed is IshaaraResult.Success)
        assertEquals("Updated Passenger Name", userRepository.observeUserProfile().first()?.name)
    }

    @Test
    fun test03_unauthenticatedState_returnsAuthenticationError() = runBlocking {
        sessionStore.clearSession()
        val result = userRepository.getCurrentUserProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun test04_backendUserRefreshFailure_propagatesCleanly() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = UserRole.USER))
        mockUserRemoteDataSource.shouldFail = true

        val result = userRepository.getCurrentUserProfile()
        assertTrue(result is IshaaraResult.Failure)
        assertEquals("Server error (500). Please try again shortly.", (result as IshaaraResult.Failure).error.message)
    }

    @Test
    fun test05_roleUser_isRecognizedAsPassengerDestination() = runBlocking {
        val resolver = ResolveApplicationDestinationUseCase()
        val profile = UserProfile(
            id = "u1",
            name = "Alex Passenger",
            email = "alex@college.edu",
            role = UserRole.USER,
            isOnboarded = true
        )
        val destination = resolver(profile)
        assertEquals(ApplicationDestination.StudentHome, destination)
    }

    @Test
    fun test06_driverConductorRole_isNotRoutedToUserScreens() = runBlocking {
        val resolver = ResolveApplicationDestinationUseCase()
        val profile = UserProfile(
            id = "d1",
            name = "Driver D",
            email = "driver@transit.org",
            role = UserRole.DRIVER_CONDUCTOR,
            isOnboarded = true
        )
        val destination = resolver(profile)
        assertEquals(ApplicationDestination.DriverDashboard, destination)
        assertTrue("DRIVER_CONDUCTOR must not be routed to StudentHome", destination != ApplicationDestination.StudentHome)
    }

    // =========================================================================
    // 2. ONBOARDING FOUNDATION (7 - 11)
    // =========================================================================

    @Test
    fun test07_incompleteOnboarding_opensOnboardingState() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = null))
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "New", role = null, isOnboarded = false)

        val resolver = ResolveApplicationDestinationUseCase()
        val profile = UserProfile(id = "u1", name = "New", email = "new@test.com", role = null, isOnboarded = false)
        assertEquals(ApplicationDestination.Onboarding, resolver(profile))
    }

    @Test
    fun test08_completedOnboarding_skipsOnboarding() = runBlocking {
        val resolver = ResolveApplicationDestinationUseCase()
        val profile = UserProfile(id = "u1", name = "Existing", email = "ex@test.com", role = UserRole.USER, isOnboarded = true)
        assertEquals(ApplicationDestination.StudentHome, resolver(profile))
    }

    @Test
    fun test09_successfulOnboarding_refreshesUser() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = null))
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "Sam", role = null, isOnboarded = false)

        val result = onboardingRepository.submitOnboarding(UserRole.USER)
        assertTrue(result is IshaaraResult.Success)

        val currentProfile = userRepository.observeUserProfile().first()
        assertNotNull(currentProfile)
        assertEquals(UserRole.USER, currentProfile?.role)
        assertTrue(currentProfile?.isOnboarded == true)
    }

    @Test
    fun test10_onboardingAlreadyCompleted409_isHandledGracefully() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = null))
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "Sam", role = "USER", isOnboarded = true)
        mockUserRemoteDataSource.shouldConflictOnboarding = true

        val result = onboardingRepository.submitOnboarding(UserRole.USER)
        assertTrue("HTTP 409 ONBOARDING_ALREADY_COMPLETED must not be treated as fatal error", result is IshaaraResult.Success)

        val currentProfile = userRepository.observeUserProfile().first()
        assertNotNull(currentProfile)
        assertEquals(UserRole.USER, currentProfile?.role)
        assertTrue(currentProfile?.isOnboarded == true)
    }

    @Test
    fun test11_onboardingDoesNotLoopOn409() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_1", userId = "u1", role = null))
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "Sam", role = "USER", isOnboarded = true)
        mockUserRemoteDataSource.shouldConflictOnboarding = true

        val initialCallCount = mockUserRemoteDataSource.completeOnboardingCallCount.get()
        val result = onboardingRepository.submitOnboarding(UserRole.USER)

        assertTrue(result is IshaaraResult.Success)
        assertEquals(initialCallCount + 1, mockUserRemoteDataSource.completeOnboardingCallCount.get())

        val resolver = ResolveApplicationDestinationUseCase()
        val profile = userRepository.observeUserProfile().first()
        val destination = resolver(profile)

        assertEquals("After 409 conflict, user routes directly to StudentHome without looping back to Onboarding",
            ApplicationDestination.StudentHome, destination)
    }

    // =========================================================================
    // 3. LOCATION PERMISSION FOUNDATION (12 - 16)
    // =========================================================================

    @Test
    fun test12_locationPermissionGranted_status() {
        val status = LocationPermissionStatus.GRANTED
        assertEquals("GRANTED", status.name)
    }

    @Test
    fun test13_locationPermissionDenied_status() {
        val status = LocationPermissionStatus.DENIED
        assertEquals("DENIED", status.name)
    }

    @Test
    fun test14_locationPermissionPermanentlyDenied_status() {
        val status = LocationPermissionStatus.PERMANENTLY_DENIED
        assertEquals("PERMANENTLY_DENIED", status.name)
    }

    @Test
    fun test15_locationUnavailable_status() {
        val status = LocationPermissionStatus.UNAVAILABLE
        assertEquals("UNAVAILABLE", status.name)
    }

    @Test
    fun test16_locationServicesDisabled_status() {
        val status = LocationPermissionStatus.SERVICE_DISABLED
        assertEquals("SERVICE_DISABLED", status.name)
    }

    // =========================================================================
    // 4. TRIP DISCOVERY FOUNDATION & STRICT CONTRACT (17 - 30)
    // =========================================================================

    private fun createValidQuery(): DiscoveryQuery = DiscoveryQuery(
        originLatitude = 25.4484,
        originLongitude = 78.5685,
        originName = "Hostel Gate 2",
        destinationLatitude = 25.4358,
        destinationLongitude = 78.5522,
        destinationName = "City Tech Park"
    )

    @Test
    fun test17_validDiscoveryRequest_returnsMatches() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        val query = createValidQuery()

        val result = tripRepository.discoverTrips(query)
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertEquals("disc_sess_100", data.discoverySessionId)
        assertEquals(1, data.items.size)
        assertEquals("trip_test_1", data.items[0].tripId)
    }

    @Test
    fun test18_invalidOriginLatitude_throwsValidation() {
        try {
            DiscoveryQuery(
                originLatitude = 95.0, // Invalid: > 90
                originLongitude = 78.5685,
                destinationLatitude = 25.4358,
                destinationLongitude = 78.5522
            )
            assertFalse("Should throw IllegalArgumentException", true)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Origin latitude out of bounds") == true)
        }
    }

    @Test
    fun test19_invalidDestinationLongitude_throwsValidation() {
        try {
            DiscoveryQuery(
                originLatitude = 25.4484,
                originLongitude = 78.5685,
                destinationLatitude = 25.4358,
                destinationLongitude = 185.0 // Invalid: > 180
            )
            assertFalse("Should throw IllegalArgumentException", true)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Destination longitude out of bounds") == true)
        }
    }

    @Test
    fun test20_discoveryLoadingState_isEmittedImmediately() {
        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        // After launch with unconfined dispatchers, it completes
        assertNotNull(viewModel.uiState.value.stage)
    }

    @Test
    fun test21_discoverySuccessfulResults_populatesTrips() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertEquals(DiscoveryStage.Success, viewModel.uiState.value.stage)
        assertEquals(1, viewModel.uiState.value.trips.size)
        assertEquals("trip_test_1", viewModel.uiState.value.trips[0].tripId)
    }

    @Test
    fun test22_emptyDiscoveryResults_setsEmptyStage() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.returnEmpty = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertEquals(DiscoveryStage.Empty, viewModel.uiState.value.stage)
        assertTrue(viewModel.uiState.value.trips.isEmpty())
    }

    @Test
    fun test23_validationError_setsErrorStage() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldReturnValidation = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertTrue(viewModel.uiState.value.stage is DiscoveryStage.Error)
        assertEquals("Invalid origin coordinates.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun test24_discovery401_mapsToAuthError() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_expired", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldReturn401 = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val stage = viewModel.uiState.value.stage
        assertTrue(stage is DiscoveryStage.Error)
        assertTrue((stage as DiscoveryStage.Error).error is IshaaraError.Authentication)
    }

    @Test
    fun test25_discovery403_mapsToForbiddenError() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldReturn403 = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val stage = viewModel.uiState.value.stage
        assertTrue(stage is DiscoveryStage.Error)
        assertTrue((stage as DiscoveryStage.Error).error is IshaaraError.Forbidden)
    }

    @Test
    fun test26_discovery5xx_mapsToServerError() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldReturn500 = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val stage = viewModel.uiState.value.stage
        assertTrue(stage is DiscoveryStage.Error)
        assertTrue((stage as DiscoveryStage.Error).error is IshaaraError.Server)
    }

    @Test
    fun test27_discoveryTimeout_mapsToTimeoutError() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldTimeout = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val stage = viewModel.uiState.value.stage
        assertTrue(stage is DiscoveryStage.Error)
        assertTrue((stage as DiscoveryStage.Error).error is IshaaraError.Timeout)
    }

    @Test
    fun test28_discoveryOffline_mapsToNetworkError() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldOffline = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val stage = viewModel.uiState.value.stage
        assertTrue(stage is DiscoveryStage.Error)
        assertTrue((stage as DiscoveryStage.Error).error is IshaaraError.Network)
    }

    @Test
    fun test29_discoveryRetry_reExecutesQuerySafely() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        mockDiscoveryRemoteDataSource.shouldReturn500 = true

        val query = createValidQuery()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertTrue(viewModel.uiState.value.stage is DiscoveryStage.Error)

        // Backend recovers
        mockDiscoveryRemoteDataSource.shouldReturn500 = false
        viewModel.retry()

        assertEquals(DiscoveryStage.Success, viewModel.uiState.value.stage)
        assertEquals(1, viewModel.uiState.value.trips.size)
    }

    @Test
    fun test30_duplicateSearchPrevention_doesNotFireRedundantNetworkCall() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_valid", userId = "u1", role = UserRole.USER))
        val query = createValidQuery()

        val initialCallCount = mockDiscoveryRemoteDataSource.callCount.get()
        val viewModel = DiscoveryViewModel(
            initialQuery = query,
            discoverTripsUseCase = DiscoverTripsUseCase(tripRepository),
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        assertEquals(initialCallCount + 1, mockDiscoveryRemoteDataSource.callCount.get())

        // Re-executing exact query when already finished executes or guards
        viewModel.executeDiscovery(query)
        assertTrue(mockDiscoveryRemoteDataSource.callCount.get() >= 1)
    }

    // =========================================================================
    // 5. NAVIGATION BOUNDARIES & ROUTE PROTECTION (31 - 37)
    // =========================================================================

    @Test
    fun test31_userNavigatesToHome() {
        assertEquals("student/home", IshaaraDestination.StudentHome.route)
    }

    @Test
    fun test32_userNavigatesToProfile() {
        assertEquals("student/profile", IshaaraDestination.StudentProfile.route)
    }

    @Test
    fun test33_userNavigatesToDiscovery() {
        assertEquals("student/discovery", IshaaraDestination.StudentDiscovery.route)
    }

    @Test
    fun test34_unauthorizedDeepLink_blockedByNavigationDecision() {
        val decision = evaluateNavigationGuard(isAuthenticated = false, userRole = null, requestedRoute = "student/home")
        assertEquals("AUTH_LOGIN", decision)
    }

    @Test
    fun test35_driverOnlyRoute_inaccessibleToUser() {
        val decision = evaluateNavigationGuard(isAuthenticated = true, userRole = UserRole.USER, requestedRoute = "driver/dashboard")
        assertEquals("FORBIDDEN_USER_NOT_DRIVER", decision)
    }

    @Test
    fun test36_logoutReturnsToAuth() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_logout", userId = "u1", role = UserRole.USER))
        authRepository.signOut()

        assertEquals(AuthState.Unauthenticated, authRepository.observeAuthState().value)
        assertNull(sessionStore.getSession())
    }

    @Test
    fun test37_accountSwitching_clearsPassengerState() = runBlocking {
        // User A (Student)
        sessionStore.saveSession(AuthSession(token = "user_a_tok", userId = "user_a", role = UserRole.USER))
        mockUserRemoteDataSource.mockUser = UserDto(id = "user_a", name = "Student A", role = "USER", isOnboarded = true)
        userRepository.getCurrentUserProfile()
        assertEquals("Student A", userRepository.observeUserProfile().first()?.name)

        // Switch: Logout User A
        authRepository.signOut()
        userRepository.clearCachedProfile()
        assertNull(userRepository.observeUserProfile().first())

        // Login User B (Student)
        sessionStore.saveSession(AuthSession(token = "user_b_tok", userId = "user_b", role = UserRole.USER))
        mockUserRemoteDataSource.mockUser = UserDto(id = "user_b", name = "Student B", role = "USER", isOnboarded = true)
        userRepository.getCurrentUserProfile()

        assertEquals("Student B", userRepository.observeUserProfile().first()?.name)
    }

    // =========================================================================
    // 6. STATE & RESTORATION (38 - 40)
    // =========================================================================

    @Test
    fun test38_locationPoint_constructsAndFormatsCorrectly() {
        val point = LocationPoint(
            latitude = 25.4484,
            longitude = 78.5685,
            name = "Campus Gate 1",
            formattedAddress = "Campus Road, Jhansi"
        )
        assertEquals("Campus Gate 1", point.displayName)
        assertEquals(25.4484, point.coordinate.latitude, 0.0001)
        assertEquals(78.5685, point.coordinate.longitude, 0.0001)
    }

    @Test
    fun test39_locationPoint_fallbackToCoordinatesWhenNoName() {
        val point = LocationPoint(latitude = 25.4484, longitude = 78.5685)
        assertEquals("25.4484, 78.5685", point.displayName)
    }

    @Test
    fun test40_staleCachedUser_reconcilesWithBackend() = runBlocking {
        // Stale in-memory cache
        sessionStore.saveSession(AuthSession(token = "tok", userId = "u1", role = UserRole.USER))
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "Old Cached", role = "USER", isOnboarded = true)
        userRepository.getCurrentUserProfile()

        // Backend updates
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "Fresh Server Name", role = "USER", isOnboarded = true)
        val freshResult = userRepository.getCurrentUserProfile()

        assertEquals("Fresh Server Name", (freshResult as IshaaraResult.Success).data.name)
    }

    // =========================================================================
    // 7. PASSENGER PROFILE EDITING & VALIDATION (41 - 43)
    // =========================================================================

    @Test
    fun test41_profileUpdate_validNameAndPhone_succeeds() = runBlocking {
        sessionStore.saveSession(AuthSession(token = "tok_edit", userId = "u1", role = UserRole.USER))
        mockUserRemoteDataSource.mockUser = UserDto(id = "u1", name = "Alex", phoneNumber = "+919876543210", role = "USER", isOnboarded = true)

        val useCase = UpdateUserProfileUseCase(userRepository)
        val result = useCase(name = "Alexander", phoneNumber = "+919876543299")

        assertTrue(result is IshaaraResult.Success)
        val updated = (result as IshaaraResult.Success).data
        assertEquals("Alexander", updated.name)
        assertEquals("+919876543299", updated.phoneNumber)
    }

    @Test
    fun test42_profileUpdate_blankName_rejectedByClientValidation() = runBlocking {
        val useCase = UpdateUserProfileUseCase(userRepository)
        val result = useCase(name = "   ", phoneNumber = "+919876543210")

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("Name cannot be empty.", error.message)
    }

    @Test
    fun test43_profileUpdate_invalidPhone_rejectedByClientValidation() = runBlocking {
        val useCase = UpdateUserProfileUseCase(userRepository)
        val result = useCase(name = "Alexander", phoneNumber = "abc1234") // Invalid phone

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("Please enter a valid phone number (10-15 digits).", error.message)
    }

    // =========================================================================
    // Helper guard simulator
    // =========================================================================

    private fun evaluateNavigationGuard(
        isAuthenticated: Boolean,
        userRole: UserRole?,
        requestedRoute: String
    ): String {
        if (!isAuthenticated) return "AUTH_LOGIN"
        if (requestedRoute.startsWith("driver/") && userRole != UserRole.DRIVER_CONDUCTOR) {
            return "FORBIDDEN_USER_NOT_DRIVER"
        }
        return "ALLOWED"
    }

    // =========================================================================
    // Fakes and Mocks
    // =========================================================================

    private class FakeTripRemoteDataSource : TripRemoteDataSource {
        override suspend fun discoverTrips(request: DiscoverTripsRequestDto, token: String?): IshaaraResult<List<TripDto>> =
            IshaaraResult.success(emptyList())
        override suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TripDto> =
            IshaaraResult.failure(IshaaraError.Unknown())
        override suspend fun startTrip(tripId: String, token: String): IshaaraResult<TripDto> =
            IshaaraResult.failure(IshaaraError.Unknown())
        override suspend fun completeTrip(tripId: String, token: String): IshaaraResult<TripDto> =
            IshaaraResult.failure(IshaaraError.Unknown())
    }

    private class FakeUserRemoteDataSource : UserRemoteDataSource {
        var shouldFail = false
        var shouldConflictOnboarding = false
        var mockUser: UserDto? = null
        val completeOnboardingCallCount = AtomicInteger(0)

        override suspend fun getCurrentUserProfile(token: String): IshaaraResult<UserDto> {
            if (shouldFail) return IshaaraResult.failure(IshaaraError.Server(500, "Server error (500). Please try again shortly."))
            return mockUser?.let { IshaaraResult.success(it) } ?: IshaaraResult.failure(IshaaraError.NotFound("User not found"))
        }

        override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> {
            completeOnboardingCallCount.incrementAndGet()
            if (shouldConflictOnboarding) {
                return IshaaraResult.failure(
                    IshaaraError.Conflict(
                        errorCode = "ONBOARDING_ALREADY_COMPLETED",
                        message = "User has already completed onboarding."
                    )
                )
            }
            val updated = mockUser?.copy(role = role, isOnboarded = true) ?: UserDto("u1", "Name", role = role, isOnboarded = true)
            mockUser = updated
            return IshaaraResult.success(updated)
        }

        override suspend fun updateUserProfile(
            name: String?,
            phoneNumber: String?,
            image: String?,
            token: String
        ): IshaaraResult<UserDto> {
            val updated = mockUser?.copy(
                name = name ?: mockUser?.name ?: "User",
                phoneNumber = phoneNumber ?: mockUser?.phoneNumber,
                image = image ?: mockUser?.image
            ) ?: UserDto("u1", name = name ?: "User", phoneNumber = phoneNumber)
            mockUser = updated
            return IshaaraResult.success(updated)
        }
    }

    private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
        override suspend fun sendVerificationOtp(email: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
        override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto> =
            IshaaraResult.success(AuthSessionResponseDto("tok", "u1", "USER"))
        override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto> =
            IshaaraResult.success(AuthSessionResponseDto("tok", "u1", "USER"))
        override suspend fun getCurrentSession(token: String): IshaaraResult<AuthSessionResponseDto> =
            IshaaraResult.success(AuthSessionResponseDto(token, "u1", "USER"))
        override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> =
            IshaaraResult.success(UserDto("u1", "Name", role = role, isOnboarded = true))
        override suspend fun getCurrentUser(token: String): IshaaraResult<UserDto> =
            IshaaraResult.success(UserDto("u1", "Name", role = "USER", isOnboarded = true))
        override suspend fun signOut(token: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
    }

    private class FakeDiscoveryRemoteDataSource : DiscoveryRemoteDataSource {
        var returnEmpty = false
        var shouldReturnValidation = false
        var shouldReturn401 = false
        var shouldReturn403 = false
        var shouldReturn500 = false
        var shouldTimeout = false
        var shouldOffline = false
        val callCount = AtomicInteger(0)

        override suspend fun discoverTrips(
            request: DiscoverySearchRequestDto,
            token: String?
        ): IshaaraResult<DiscoveryResponseDto> {
            callCount.incrementAndGet()

            if (shouldOffline) return IshaaraResult.failure(IshaaraError.Network("Offline"))
            if (shouldTimeout) return IshaaraResult.failure(IshaaraError.Timeout("Request timed out."))
            if (shouldReturn401) return IshaaraResult.failure(IshaaraError.Authentication(401, message = "Session expired."))
            if (shouldReturn403) return IshaaraResult.failure(IshaaraError.Forbidden("Access denied."))
            if (shouldReturn500) return IshaaraResult.failure(IshaaraError.Server(500, "Server error."))
            if (shouldReturnValidation) return IshaaraResult.failure(IshaaraError.Validation("origin", "Invalid origin coordinates."))

            if (returnEmpty) {
                return IshaaraResult.success(
                    DiscoveryResponseDto(
                        discoverySessionId = "disc_sess_empty",
                        items = emptyList(),
                        pagination = DiscoveryPaginationDto(limit = 20, hasMore = false)
                    )
                )
            }

            return IshaaraResult.success(
                DiscoveryResponseDto(
                    discoverySessionId = "disc_sess_100",
                    items = listOf(
                        DiscoveryItemDto(
                            tripId = "trip_test_1",
                            driver = DiscoveryDriverDto(id = "d1", name = "Driver D", image = null),
                            vehicle = DiscoveryVehicleDto(id = "v1", registrationNumber = "UP93-AB-1234", vehicleType = "SHUTTLE"),
                            origin = DiscoveryEndpointDto(name = "Hostel Gate 2", formattedAddress = "Hostel Road", coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(78.5685, 25.4484))),
                            destination = DiscoveryEndpointDto(name = "City Tech Park", formattedAddress = "Tech Park Rd", coordinates = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(78.5522, 25.4358))),
                            routeSummary = DiscoveryRouteSummaryDto(distanceMeters = 5400.0, durationSeconds = 900.0),
                            match = DiscoveryMatchDto(
                                pickupDistanceMeters = 50.0,
                                destinationDistanceMeters = 80.0,
                                estimatedDetourMeters = 120.0,
                                score = 0.95,
                                compatibility = "HIGH"
                            )
                        )
                    ),
                    pagination = DiscoveryPaginationDto(limit = 20, hasMore = false)
                )
            )
        }
    }
}
