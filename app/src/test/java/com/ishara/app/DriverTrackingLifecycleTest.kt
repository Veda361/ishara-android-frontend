package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverAssignedVehicle
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverTodayStats
import com.ishara.app.domain.model.DriverTripLocation
import com.ishara.app.domain.model.DriverTripStatus
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.repository.DriverRepository
import com.ishara.app.domain.usecase.GetDriverOperationalContextUseCase
import com.ishara.app.domain.usecase.ManageDriverTripLifecycleUseCase
import com.ishara.app.domain.usecase.ObserveDriverTrackingStatusUseCase
import com.ishara.app.domain.usecase.SetDriverAvailabilityUseCase
import com.ishara.app.domain.usecase.StartDriverTrackingUseCase
import com.ishara.app.domain.usecase.StopDriverTrackingUseCase
import com.ishara.app.domain.usecase.UpdateDriverLocationUseCase
import com.ishara.app.feature.driver.DriverViewModel
import com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator
import com.ishara.app.feature.driver.tracking.DriverTrackingStatus
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Driver GPS tracking lifecycle transitions (Tests 26 - 32).
 */
class DriverTrackingLifecycleTest {

    private val testDispatcher = Dispatchers.Unconfined
    private val testScope = CoroutineScope(testDispatcher + Job())

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val sampleDriver = DriverIdentity(
        id = "drv_123",
        userId = "usr_123",
        verificationStatus = DriverVerificationStatus.VERIFIED,
        status = DriverProfileStatus.ONLINE,
        licenseNumberMasked = "DL••••••••1234",
        licenseVerifiedAt = "2026-09-01T10:00:00Z"
    )

    private val sampleVehicle = DriverAssignedVehicle(
        id = "veh_123",
        registrationNumber = "UP93AT1234",
        vehicleType = "BUS",
        make = "Tata",
        model = "Starbus",
        isActive = true,
        isVerified = true
    )

    private val sampleTripCreated = DriverActiveTrip(
        id = "651a2b3c4d5e6f7a8b9c0d30",
        driverId = "drv_123",
        vehicleId = "veh_123",
        origin = DriverTripLocation(name = "Gate A", formattedAddress = "Gate A", latitude = 25.4, longitude = 78.5),
        destination = DriverTripLocation(name = "Station", formattedAddress = "Station", latitude = 25.5, longitude = 78.6),
        distanceMeters = 5000.0,
        durationSeconds = 600.0,
        status = DriverTripStatus.CREATED,
        startedAt = null,
        completedAt = null,
        cancelledAt = null
    )

    private val sampleTripActive = sampleTripCreated.copy(status = DriverTripStatus.ACTIVE, startedAt = "2026-09-25T10:00:00Z")
    private val sampleTripCompleted = sampleTripCreated.copy(status = DriverTripStatus.COMPLETED, completedAt = "2026-09-25T10:30:00Z")
    private val sampleTripCancelled = sampleTripCreated.copy(status = DriverTripStatus.CANCELLED, cancelledAt = "2026-09-25T10:10:00Z")

    private class FakeDriverRepository : DriverRepository {
        var contextToReturn: DriverOperationalContext? = null
        var lastUpdatedLocation: LocationCoordinates? = null

        override suspend fun getOperationalContext(timezone: String?): IshaaraResult<DriverOperationalContext> {
            return IshaaraResult.success(contextToReturn!!)
        }

        override suspend fun setOnline(): IshaaraResult<DriverIdentity> {
            return IshaaraResult.success(DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.ONLINE, null, null))
        }

        override suspend fun setOffline(): IshaaraResult<DriverIdentity> {
            return IshaaraResult.success(DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.OFFLINE, null, null))
        }

        override suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
            val trip = DriverActiveTrip(tripId, "drv_123", "veh_123", DriverTripLocation(null, "A", 0.0, 0.0), DriverTripLocation(null, "B", 0.0, 0.0), 1000.0, 300.0, status = DriverTripStatus.ACTIVE, null, null, null)
            contextToReturn = contextToReturn?.copy(activeTrip = trip)
            return IshaaraResult.success(trip)
        }

        override suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
            val trip = DriverActiveTrip(tripId, "drv_123", "veh_123", DriverTripLocation(null, "A", 0.0, 0.0), DriverTripLocation(null, "B", 0.0, 0.0), 1000.0, 300.0, status = DriverTripStatus.COMPLETED, null, null, null)
            contextToReturn = contextToReturn?.copy(activeTrip = null)
            return IshaaraResult.success(trip)
        }

        override suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
            val trip = DriverActiveTrip(tripId, "drv_123", "veh_123", DriverTripLocation(null, "A", 0.0, 0.0), DriverTripLocation(null, "B", 0.0, 0.0), 1000.0, 300.0, status = DriverTripStatus.CANCELLED, null, null, null)
            contextToReturn = contextToReturn?.copy(activeTrip = null)
            return IshaaraResult.success(trip)
        }

        override suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit> {
            lastUpdatedLocation = location
            return IshaaraResult.success(Unit)
        }
    }

    private class FakeLocationProvider : LocationProvider {
        val flow = MutableSharedFlow<LocationCoordinates>()
        override suspend fun getCurrentLocation() = TODO()
        override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = flow
        override fun isLocationAvailable(): Boolean = true
    }

    @Test
    fun test_26_active_trip_starts_tracking() = runBlocking {
        val fakeRepo = FakeDriverRepository().apply {
            contextToReturn = DriverOperationalContext(
                driver = sampleDriver,
                vehicle = sampleVehicle,
                activeTrip = sampleTripCreated,
                activeRidesCount = 0,
                todayStats = DriverTodayStats(0, true, "2026-09-25", "Asia/Kolkata")
            )
        }
        val fakeProvider = FakeLocationProvider()
        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)

        val viewModel = DriverViewModel(
            driverName = "John Driver",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(fakeRepo),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(fakeRepo),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(fakeRepo),
            navigationManager = NavigationManager(),
            startDriverTrackingUseCase = StartDriverTrackingUseCase(coordinator),
            stopDriverTrackingUseCase = StopDriverTrackingUseCase(coordinator),
            observeDriverTrackingStatusUseCase = ObserveDriverTrackingStatusUseCase(coordinator),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        assertFalse("Initially tracking must not be active for CREATED trip", coordinator.isTrackingActive())

        viewModel.startTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertTrue("Starting trip must automatically trigger tracking", coordinator.isTrackingActive())
        assertTrue(coordinator.status.value is DriverTrackingStatus.Active)
    }

    @Test
    fun test_27_completed_trip_stops_tracking() = runBlocking {
        val fakeRepo = FakeDriverRepository().apply {
            contextToReturn = DriverOperationalContext(
                driver = sampleDriver.copy(status = DriverProfileStatus.ON_RIDE),
                vehicle = sampleVehicle,
                activeTrip = sampleTripActive,
                activeRidesCount = 1,
                todayStats = DriverTodayStats(0, true, "2026-09-25", "Asia/Kolkata")
            )
        }
        val fakeProvider = FakeLocationProvider()
        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)

        val viewModel = DriverViewModel(
            driverName = "John Driver",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(fakeRepo),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(fakeRepo),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(fakeRepo),
            navigationManager = NavigationManager(),
            startDriverTrackingUseCase = StartDriverTrackingUseCase(coordinator),
            stopDriverTrackingUseCase = StopDriverTrackingUseCase(coordinator),
            observeDriverTrackingStatusUseCase = ObserveDriverTrackingStatusUseCase(coordinator),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        // Relaunch/restore initiated tracking for active trip
        assertTrue("Tracking must be active for ACTIVE trip", coordinator.isTrackingActive())

        // Completing trip
        viewModel.completeTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertFalse("Completing trip must stop tracking", coordinator.isTrackingActive())
        assertEquals(DriverTrackingStatus.Idle, coordinator.status.value)
    }

    @Test
    fun test_28_cancelled_trip_stops_tracking() = runBlocking {
        val fakeRepo = FakeDriverRepository().apply {
            contextToReturn = DriverOperationalContext(
                driver = sampleDriver.copy(status = DriverProfileStatus.ON_RIDE),
                vehicle = sampleVehicle,
                activeTrip = sampleTripActive,
                activeRidesCount = 1,
                todayStats = DriverTodayStats(0, true, "2026-09-25", "Asia/Kolkata")
            )
        }
        val fakeProvider = FakeLocationProvider()
        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)

        val viewModel = DriverViewModel(
            driverName = "John Driver",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(fakeRepo),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(fakeRepo),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(fakeRepo),
            navigationManager = NavigationManager(),
            startDriverTrackingUseCase = StartDriverTrackingUseCase(coordinator),
            stopDriverTrackingUseCase = StopDriverTrackingUseCase(coordinator),
            observeDriverTrackingStatusUseCase = ObserveDriverTrackingStatusUseCase(coordinator),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        assertTrue(coordinator.isTrackingActive())

        // Cancelling trip
        viewModel.cancelTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertFalse("Cancelling trip must stop tracking", coordinator.isTrackingActive())
        assertEquals(DriverTrackingStatus.Idle, coordinator.status.value)
    }

    @Test
    fun test_29_offline_driver_does_not_track() = runBlocking {
        val fakeRepo = FakeDriverRepository().apply {
            contextToReturn = DriverOperationalContext(
                driver = sampleDriver.copy(status = DriverProfileStatus.OFFLINE),
                vehicle = sampleVehicle,
                activeTrip = null,
                activeRidesCount = 0,
                todayStats = DriverTodayStats(0, false, "2026-09-25", "Asia/Kolkata")
            )
        }
        val fakeProvider = FakeLocationProvider()
        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)

        val viewModel = DriverViewModel(
            driverName = "John Driver",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(fakeRepo),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(fakeRepo),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(fakeRepo),
            navigationManager = NavigationManager(),
            startDriverTrackingUseCase = StartDriverTrackingUseCase(coordinator),
            stopDriverTrackingUseCase = StopDriverTrackingUseCase(coordinator),
            observeDriverTrackingStatusUseCase = ObserveDriverTrackingStatusUseCase(coordinator),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        assertFalse("Offline driver must not track", coordinator.isTrackingActive())
    }

    @Test
    fun test_30_going_offline_stops_tracking() = runBlocking {
        val fakeRepo = FakeDriverRepository().apply {
            contextToReturn = DriverOperationalContext(
                driver = sampleDriver.copy(status = DriverProfileStatus.ONLINE),
                vehicle = sampleVehicle,
                activeTrip = null,
                activeRidesCount = 0,
                todayStats = DriverTodayStats(0, true, "2026-09-25", "Asia/Kolkata")
            )
        }
        val fakeProvider = FakeLocationProvider()
        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)

        // Force start tracking
        coordinator.startTracking("trip_manual")
        assertTrue(coordinator.isTrackingActive())

        val viewModel = DriverViewModel(
            driverName = "John Driver",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(fakeRepo),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(fakeRepo),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(fakeRepo),
            navigationManager = NavigationManager(),
            startDriverTrackingUseCase = StartDriverTrackingUseCase(coordinator),
            stopDriverTrackingUseCase = StopDriverTrackingUseCase(coordinator),
            observeDriverTrackingStatusUseCase = ObserveDriverTrackingStatusUseCase(coordinator),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        viewModel.goOffline()

        assertFalse("Going offline must stop tracking", coordinator.isTrackingActive())
    }

    @Test
    fun test_31_permission_revocation_stops_tracking() = runBlocking {
        val fakeRepo = FakeDriverRepository()
        val errorFlow = flow<LocationCoordinates> {
            throw SecurityException("Permission revoked at runtime")
        }
        val fakeProvider = object : LocationProvider {
            override suspend fun getCurrentLocation() = TODO()
            override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = errorFlow
            override fun isLocationAvailable(): Boolean = false
        }

        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)
        coordinator.startTracking("trip_123")

        // Flow catches SecurityException and updates status to PermissionRequired
        assertEquals(DriverTrackingStatus.PermissionRequired, coordinator.status.value)
    }

    @Test
    fun test_32_relaunch_with_active_trip_restores_tracking() = runBlocking {
        val fakeRepo = FakeDriverRepository().apply {
            contextToReturn = DriverOperationalContext(
                driver = sampleDriver.copy(status = DriverProfileStatus.ON_RIDE),
                vehicle = sampleVehicle,
                activeTrip = sampleTripActive,
                activeRidesCount = 1,
                todayStats = DriverTodayStats(2, true, "2026-09-25", "Asia/Kolkata")
            )
        }
        val fakeProvider = FakeLocationProvider()
        val coordinator = DriverTrackingCoordinator(fakeProvider, UpdateDriverLocationUseCase(fakeRepo), testDispatchers, testScope)

        assertFalse("Before ViewModel creation, coordinator is not tracking", coordinator.isTrackingActive())

        // App launches and creates DriverViewModel
        val viewModel = DriverViewModel(
            driverName = "John Driver",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(fakeRepo),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(fakeRepo),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(fakeRepo),
            navigationManager = NavigationManager(),
            startDriverTrackingUseCase = StartDriverTrackingUseCase(coordinator),
            stopDriverTrackingUseCase = StopDriverTrackingUseCase(coordinator),
            observeDriverTrackingStatusUseCase = ObserveDriverTrackingStatusUseCase(coordinator),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        // loadOperationalContext in init detected active trip and restored tracking!
        assertTrue("Relaunching with active trip must restore tracking", coordinator.isTrackingActive())
        val status = coordinator.status.value
        assertTrue("Status must be Active", status is DriverTrackingStatus.Active)
        assertEquals(sampleTripActive.id, (status as DriverTrackingStatus.Active).tripId)
    }
}
