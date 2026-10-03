package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
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
import com.ishara.app.domain.usecase.SetDriverAvailabilityUseCase
import com.ishara.app.feature.driver.DriverHomeStage
import com.ishara.app.feature.driver.DriverViewModel
import com.ishara.app.feature.driver.TripActionState
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverViewModelTest {

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

    private val sampleTrip = DriverActiveTrip(
        id = "651a2b3c4d5e6f7a8b9c0d30",
        driverId = "drv_123",
        vehicleId = "veh_123",
        origin = DriverTripLocation("Gate A", "Gate A, University", 25.4484, 78.5685),
        destination = DriverTripLocation("Railway Station", "Railway Station, Jhansi", 25.4520, 78.5780),
        distanceMeters = 4500.0,
        durationSeconds = 900.0,
        status = DriverTripStatus.CREATED,
        startedAt = null,
        completedAt = null,
        cancelledAt = null
    )

    private val sampleStats = DriverTodayStats(
        completedRidesCount = 3,
        isOnline = true,
        currentDate = "2026-09-24",
        timezone = "Asia/Kolkata"
    )

    private val sampleContext = DriverOperationalContext(
        driver = sampleDriver,
        vehicle = sampleVehicle,
        activeTrip = sampleTrip,
        activeRidesCount = 0,
        todayStats = sampleStats
    )

    private class MockDriverRepository : DriverRepository {
        var contextResult: IshaaraResult<DriverOperationalContext> = IshaaraResult.success(
            DriverOperationalContext(
                driver = DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.ONLINE, "DL••••1234", null),
                vehicle = null,
                activeTrip = null,
                activeRidesCount = 0,
                todayStats = DriverTodayStats(0, true, "2026-09-24", "Asia/Kolkata")
            )
        )
        var onlineResult: IshaaraResult<DriverIdentity> = IshaaraResult.success(
            DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.ONLINE, "DL••••1234", null)
        )
        var offlineResult: IshaaraResult<DriverIdentity> = IshaaraResult.success(
            DriverIdentity("drv_123", "usr_123", DriverVerificationStatus.VERIFIED, DriverProfileStatus.OFFLINE, "DL••••1234", null)
        )
        var startTripResult: IshaaraResult<DriverActiveTrip>? = null
        var completeTripResult: IshaaraResult<DriverActiveTrip>? = null
        var cancelTripResult: IshaaraResult<DriverActiveTrip>? = null

        var getContextCallCount = 0
        var setOnlineCallCount = 0
        var setOfflineCallCount = 0
        var startTripCallCount = 0
        var completeTripCallCount = 0
        var cancelTripCallCount = 0

        override suspend fun getOperationalContext(timezone: String?): IshaaraResult<DriverOperationalContext> {
            getContextCallCount++
            return contextResult
        }

        override suspend fun setOnline(): IshaaraResult<DriverIdentity> {
            setOnlineCallCount++
            return onlineResult
        }

        override suspend fun setOffline(): IshaaraResult<DriverIdentity> {
            setOfflineCallCount++
            return offlineResult
        }

        override suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
            startTripCallCount++
            return startTripResult ?: IshaaraResult.failure(IshaaraError.NotFound())
        }

        override suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
            completeTripCallCount++
            return completeTripResult ?: IshaaraResult.failure(IshaaraError.NotFound())
        }

        override suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
            cancelTripCallCount++
            return cancelTripResult ?: IshaaraResult.failure(IshaaraError.NotFound())
        }

        override suspend fun updateLocation(location: com.ishara.app.core.location.LocationCoordinates): IshaaraResult<Unit> {
            return IshaaraResult.success(Unit)
        }
    }

    private fun createViewModel(repository: DriverRepository): DriverViewModel {
        return DriverViewModel(
            driverName = "Rajesh Sharma",
            getDriverOperationalContextUseCase = GetDriverOperationalContextUseCase(repository),
            setDriverAvailabilityUseCase = SetDriverAvailabilityUseCase(repository),
            manageDriverTripLifecycleUseCase = ManageDriverTripLifecycleUseCase(repository),
            navigationManager = NavigationManager(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )
    }

    @Test
    fun initialLoading_transitionsToContent_whenActiveTripPresent() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.success(sampleContext)

        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state.stage is DriverHomeStage.Content)
        val content = (state.stage as DriverHomeStage.Content).context
        assertEquals("drv_123", content.driver.id)
        assertEquals("UP93AT1234", content.vehicle?.registrationNumber)
        assertEquals("651a2b3c4d5e6f7a8b9c0d30", content.activeTrip?.id)
        assertFalse(state.isActionInProgress)
    }

    @Test
    fun initialLoading_transitionsToOffline_whenDriverIsOffline() = runBlocking {
        val repo = MockDriverRepository()
        val offlineContext = sampleContext.copy(
            driver = sampleDriver.copy(status = DriverProfileStatus.OFFLINE)
        )
        repo.contextResult = IshaaraResult.success(offlineContext)

        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state.stage is DriverHomeStage.Offline)
        assertEquals(DriverProfileStatus.OFFLINE, (state.stage as DriverHomeStage.Offline).context.driver.status)
    }

    @Test
    fun initialLoading_transitionsToEmpty_whenNoActiveTrip() = runBlocking {
        val repo = MockDriverRepository()
        val emptyTripContext = sampleContext.copy(
            driver = sampleDriver.copy(status = DriverProfileStatus.ONLINE),
            activeTrip = null
        )
        repo.contextResult = IshaaraResult.success(emptyTripContext)

        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state.stage is DriverHomeStage.Empty)
        assertNull((state.stage as DriverHomeStage.Empty).context.activeTrip)
    }

    @Test
    fun networkFailure_setsErrorStageWithRetry() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.failure(IshaaraError.Network("Connection timeout"))

        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state.stage is DriverHomeStage.Error)
        val errorStage = state.stage as DriverHomeStage.Error
        assertTrue(errorStage.canRetry)
        assertEquals("Connection timeout", errorStage.error.message)
    }

    @Test
    fun unauthorizedOrForbidden_setsErrorStageWithoutRetry() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.failure(
            IshaaraError.Forbidden("Access denied: Required role 'DRIVER_CONDUCTOR'.")
        )

        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state.stage is DriverHomeStage.Error)
        val errorStage = state.stage as DriverHomeStage.Error
        assertFalse(errorStage.canRetry)
    }

    @Test
    fun retry_reloadsOperationalContext() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.failure(IshaaraError.Network("Offline"))

        val viewModel = createViewModel(repo)
        assertTrue(viewModel.uiState.value.stage is DriverHomeStage.Error)

        repo.contextResult = IshaaraResult.success(sampleContext)
        viewModel.retry()

        assertTrue(viewModel.uiState.value.stage is DriverHomeStage.Content)
        assertEquals(2, repo.getContextCallCount)
    }

    @Test
    fun goOnline_success_executesAndRefreshesContext() = runBlocking {
        val repo = MockDriverRepository()
        val offlineContext = sampleContext.copy(
            driver = sampleDriver.copy(status = DriverProfileStatus.OFFLINE)
        )
        repo.contextResult = IshaaraResult.success(offlineContext)

        val viewModel = createViewModel(repo)
        assertTrue(viewModel.uiState.value.stage is DriverHomeStage.Offline)

        // Switch to returning online context after goOnline
        repo.contextResult = IshaaraResult.success(sampleContext)
        viewModel.goOnline()

        assertEquals(1, repo.setOnlineCallCount)
        assertTrue(viewModel.uiState.value.stage is DriverHomeStage.Content)
        assertFalse(viewModel.uiState.value.isActionInProgress)
        assertNotNull(viewModel.uiState.value.userFacingNotification)
    }

    @Test
    fun goOffline_success_executesAndRefreshesContext() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.success(sampleContext)

        val viewModel = createViewModel(repo)
        assertTrue(viewModel.uiState.value.stage is DriverHomeStage.Content)

        // Switch to returning offline context after goOffline
        val offlineContext = sampleContext.copy(
            driver = sampleDriver.copy(status = DriverProfileStatus.OFFLINE)
        )
        repo.contextResult = IshaaraResult.success(offlineContext)
        viewModel.goOffline()

        assertEquals(1, repo.setOfflineCallCount)
        assertTrue(viewModel.uiState.value.stage is DriverHomeStage.Offline)
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    @Test
    fun goOffline_blockedLocally_whenDriverIsOnRide() = runBlocking {
        val repo = MockDriverRepository()
        val onRideContext = sampleContext.copy(
            driver = sampleDriver.copy(status = DriverProfileStatus.ON_RIDE)
        )
        repo.contextResult = IshaaraResult.success(onRideContext)

        val viewModel = createViewModel(repo)

        viewModel.goOffline()

        assertEquals(0, repo.setOfflineCallCount)
        assertEquals("Cannot go offline while operating an active ride.", viewModel.uiState.value.userFacingError)
    }

    @Test
    fun startTrip_success_transitionsThroughSuccessAndRefreshesContext() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.success(sampleContext)
        val activeTrip = sampleTrip.copy(status = DriverTripStatus.ACTIVE)
        repo.startTripResult = IshaaraResult.success(activeTrip)

        val viewModel = createViewModel(repo)

        // After startTrip, repo context will return the updated active trip
        repo.contextResult = IshaaraResult.success(sampleContext.copy(activeTrip = activeTrip))
        viewModel.startTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertEquals(1, repo.startTripCallCount)
        val state = viewModel.uiState.value
        assertTrue(state.tripActionState is TripActionState.Success)
        assertFalse(state.isActionInProgress)
        assertEquals("Trip is now active.", state.userFacingNotification)
    }

    @Test
    fun completeTrip_success_transitionsThroughSuccessAndRefreshesContext() = runBlocking {
        val repo = MockDriverRepository()
        val activeTrip = sampleTrip.copy(status = DriverTripStatus.ACTIVE)
        repo.contextResult = IshaaraResult.success(sampleContext.copy(activeTrip = activeTrip))
        val completedTrip = sampleTrip.copy(status = DriverTripStatus.COMPLETED)
        repo.completeTripResult = IshaaraResult.success(completedTrip)

        val viewModel = createViewModel(repo)

        repo.contextResult = IshaaraResult.success(sampleContext.copy(activeTrip = completedTrip))
        viewModel.completeTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertEquals(1, repo.completeTripCallCount)
        val state = viewModel.uiState.value
        assertTrue(state.tripActionState is TripActionState.Success)
        assertFalse(state.isActionInProgress)
    }

    @Test
    fun cancelTrip_success_transitionsThroughSuccessAndRefreshesContext() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.success(sampleContext)
        val cancelledTrip = sampleTrip.copy(status = DriverTripStatus.CANCELLED)
        repo.cancelTripResult = IshaaraResult.success(cancelledTrip)

        val viewModel = createViewModel(repo)

        repo.contextResult = IshaaraResult.success(sampleContext.copy(activeTrip = cancelledTrip))
        viewModel.cancelTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertEquals(1, repo.cancelTripCallCount)
        val state = viewModel.uiState.value
        assertTrue(state.tripActionState is TripActionState.Success)
        assertFalse(state.isActionInProgress)
    }

    @Test
    fun tripActionFailure_setsTripActionErrorAndSurfacesUserFacingError() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.success(sampleContext)
        repo.startTripResult = IshaaraResult.failure(
            IshaaraError.Conflict("Vehicle is already in use on another active trip.")
        )

        val viewModel = createViewModel(repo)

        viewModel.startTrip("651a2b3c4d5e6f7a8b9c0d30")

        val state = viewModel.uiState.value
        assertTrue(state.tripActionState is TripActionState.Error)
        val errorState = state.tripActionState as TripActionState.Error
        assertEquals("Vehicle is already in use on another active trip.", errorState.error.message)
        assertEquals("Vehicle is already in use on another active trip.", state.userFacingError)
        assertFalse(state.isActionInProgress)
    }

    @Test
    fun dismissNotification_clearsNotificationAndError() = runBlocking {
        val repo = MockDriverRepository()
        repo.contextResult = IshaaraResult.success(sampleContext)
        repo.startTripResult = IshaaraResult.failure(IshaaraError.NotFound("Trip missing"))

        val viewModel = createViewModel(repo)
        viewModel.startTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertNotNull(viewModel.uiState.value.userFacingError)

        viewModel.dismissNotification()

        assertNull(viewModel.uiState.value.userFacingError)
        assertNull(viewModel.uiState.value.userFacingNotification)
    }
}
