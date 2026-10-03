package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.model.RideStatus
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.repository.LocationRepository
import com.ishara.app.domain.repository.RideRepository
import com.ishara.app.domain.model.RideRequest
import com.ishara.app.domain.usecase.GetActiveRideUseCase
import com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase
import com.ishara.app.domain.usecase.ResolveCurrentLocationUseCase
import com.ishara.app.feature.student.home.ActiveRideUiState
import com.ishara.app.feature.student.home.LocationUiState
import com.ishara.app.feature.student.home.StudentHomeViewModel
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates StudentHomeViewModel state management, location resolution,
 * active ride surfacing, and navigation triggers.
 */
class StudentHomeViewModelTest {

    private class TestDispatcherProvider(
        private val testDispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private class MockLocationRepository(
        var locationSuccess: Boolean = true,
        var activeRide: Ride? = null
    ) : LocationRepository {
        val recentDestinationsFlow = MutableStateFlow<List<StudentDestination>>(emptyList())

        override suspend fun searchLocations(
            query: String,
            latitude: Double?,
            longitude: Double?,
            radius: Double?,
            limit: Int
        ): IshaaraResult<List<SearchResultLocation>> = IshaaraResult.success(emptyList())

        override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
            return if (locationSuccess) {
                IshaaraResult.success(LocationCoordinates(25.4484, 78.5685))
            } else {
                IshaaraResult.failure(IshaaraError.Unknown("Location provider off"))
            }
        }

        override suspend fun resolveHumanReadableLocation(
            coordinates: LocationCoordinates
        ): IshaaraResult<CurrentLocationDisplay> {
            return IshaaraResult.success(
                CurrentLocationDisplay(
                    title = "Current location",
                    subtitle = "Jhansi, Uttar Pradesh",
                    coordinates = coordinates
                )
            )
        }

        override fun getRecentDestinations(): Flow<List<StudentDestination>> = recentDestinationsFlow

        override suspend fun saveRecentDestination(destination: StudentDestination): IshaaraResult<Unit> {
            val current = recentDestinationsFlow.value.toMutableList()
            current.add(0, destination)
            recentDestinationsFlow.value = current
            return IshaaraResult.success(Unit)
        }

        override suspend fun clearRecentDestinations(): IshaaraResult<Unit> {
            recentDestinationsFlow.value = emptyList()
            return IshaaraResult.success(Unit)
        }
    }

    private class MockRideRepository(
        var activeRide: Ride? = null
    ) : RideRepository {
        override suspend fun createRideRequest(
            tripId: String,
            pickupAddress: String,
            dropoffAddress: String,
            seatsRequested: Int
        ): IshaaraResult<RideRequest> = throw UnsupportedOperationException()

        override suspend fun submitRideRequest(
            input: com.ishara.app.domain.model.RideRequestInput,
            idempotencyKey: String?
        ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult> = throw UnsupportedOperationException()

        override suspend fun getActiveRide(rideId: String): IshaaraResult<Ride> = throw UnsupportedOperationException()

        override suspend fun getActivePassengerRide(): IshaaraResult<Ride?> {
            return IshaaraResult.success(activeRide)
        }

        override suspend fun cancelRide(rideId: String, reason: String?): IshaaraResult<Unit> =
            IshaaraResult.success(Unit)
    }

    @Test
    fun test1_initialState_displaysStudentNameAndGreeting() = runBlocking {
        val locationRepo = MockLocationRepository()
        val rideRepo = MockRideRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            val state = viewModel.uiState.value
            assertEquals("Dev", state.userName)
            assertTrue(state.greeting.isNotBlank())
            assertNull(state.selectedDestination)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test2_locationResolution_success_setsAvailableLocation() = runBlocking {
        val locationRepo = MockLocationRepository(locationSuccess = true)
        val rideRepo = MockRideRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            val state = viewModel.uiState.value
            assertTrue("Location must be Available", state.locationState is LocationUiState.Available)
            val available = state.locationState as LocationUiState.Available
            assertEquals("Jhansi, Uttar Pradesh", available.location.subtitle)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test3_locationResolution_failure_setsUnavailable() = runBlocking {
        val locationRepo = MockLocationRepository(locationSuccess = false)
        val rideRepo = MockRideRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            val state = viewModel.uiState.value
            assertTrue("Location must be Unavailable", state.locationState is LocationUiState.Unavailable)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test4_activeRide_surfacesWithTopPriority() = runBlocking {
        val activeRide = Ride(
            id = "ride_99",
            tripId = "trip_01",
            passengerId = "user_dev",
            driverId = "driver_1",
            vehicleId = "veh_bus",
            pickup = TripLocation("Campus Gate", GeoJsonCoordinate(78.56, 25.44)),
            dropoff = TripLocation("City Center", GeoJsonCoordinate(78.58, 25.45)),
            seats = 1,
            farePaise = 2000,
            status = RideStatus.IN_PROGRESS
        )

        val locationRepo = MockLocationRepository()
        val rideRepo = MockRideRepository(activeRide = activeRide)
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            val state = viewModel.uiState.value
            assertTrue("Active ride must be present in state", state.activeRideState is ActiveRideUiState.Active)
            val activeState = state.activeRideState as ActiveRideUiState.Active
            assertEquals("ride_99", activeState.ride.id)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test5_destinationSelection_updatesStateAndSavesRecent() = runBlocking {
        val locationRepo = MockLocationRepository()
        val rideRepo = MockRideRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            val destination = StudentDestination(
                name = "Sipri Bazaar",
                formattedAddress = "Sipri Bazaar, Jhansi",
                coordinates = LocationCoordinates(25.45, 78.57)
            )

            viewModel.onDestinationSelected(destination)

            val state = viewModel.uiState.value
            assertEquals("Sipri Bazaar", state.selectedDestination?.name)
            assertEquals(1, state.recentDestinations.size)

            viewModel.onClearSelectedDestination()
            assertNull(viewModel.uiState.value.selectedDestination)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test6_navigation_searchClicked_navigatesToSearchRoute() = runBlocking {
        val locationRepo = MockLocationRepository()
        val rideRepo = MockRideRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            viewModel.onSearchClicked()

            val command = navManager.commands.first()
            assertTrue(command is NavigationCommand.NavigateTo)
            assertEquals(IshaaraDestination.StudentSearch.route, (command as NavigationCommand.NavigateTo).route)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test7_navigation_findTripsClicked_navigatesToDiscoveryRoute() = runBlocking {
        val locationRepo = MockLocationRepository()
        val rideRepo = MockRideRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.Unconfined + Job())

        try {
            val viewModel = StudentHomeViewModel(
                userName = "Dev",
                resolveCurrentLocationUseCase = ResolveCurrentLocationUseCase(locationRepo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(locationRepo),
                getActiveRideUseCase = GetActiveRideUseCase(rideRepo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope
            )

            viewModel.onFindTripsClicked()

            val command = navManager.commands.first()
            assertTrue(command is NavigationCommand.NavigateTo)
            assertEquals(IshaaraDestination.StudentDiscovery.route, (command as NavigationCommand.NavigateTo).route)
        } finally {
            testScope.cancel()
        }
    }
}
