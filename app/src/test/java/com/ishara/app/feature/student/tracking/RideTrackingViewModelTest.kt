package com.ishara.app.feature.student.tracking

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.RideTrackingUpdatedPayloadDto
import com.ishara.app.data.remote.dto.TrackingCoordinatesDto
import com.ishara.app.data.remote.dto.TrackingEtaInfoDto
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingDriverLocation
import com.ishara.app.domain.model.TrackingEta
import com.ishara.app.domain.model.TrackingFreshness
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.model.TrackingRouteProgress
import com.ishara.app.domain.model.TrackingState
import com.ishara.app.domain.repository.RideTrackingRepository
import com.ishara.app.domain.usecase.GetDriverLocationUseCase
import com.ishara.app.domain.usecase.GetRideTrackingUseCase
import com.ishara.app.domain.usecase.ObserveRideTrackingUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RideTrackingViewModelTest {

    private val testDispatcher = Dispatchers.Unconfined

    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private lateinit var fakeRepository: FakeRideTrackingRepository
    private lateinit var getRideTrackingUseCase: GetRideTrackingUseCase
    private lateinit var getDriverLocationUseCase: GetDriverLocationUseCase
    private lateinit var observeRideTrackingUseCase: ObserveRideTrackingUseCase
    private lateinit var navigationManager: NavigationManager

    @Before
    fun setup() {
        fakeRepository = FakeRideTrackingRepository()
        getRideTrackingUseCase = GetRideTrackingUseCase(fakeRepository)
        getDriverLocationUseCase = GetDriverLocationUseCase(fakeRepository)
        observeRideTrackingUseCase = ObserveRideTrackingUseCase(fakeRepository)
        navigationManager = NavigationManager()
    }

    private fun createViewModel(rideId: String = "ride-123"): RideTrackingViewModel {
        return RideTrackingViewModel(
            rideId = rideId,
            getRideTrackingUseCase = getRideTrackingUseCase,
            getDriverLocationUseCase = getDriverLocationUseCase,
            observeRideTrackingUseCase = observeRideTrackingUseCase,
            navigationManager = navigationManager,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `initial load success enters Content state and starts subscription`() = runBlocking {
        fakeRepository.stubSnapshot = createTestSnapshot(
            rideId = "ride-123",
            status = TrackingRideStatus.DRIVER_ARRIVING
        )

        val viewModel = createViewModel("ride-123")
        val state = viewModel.uiState.value

        assertTrue(state is RideTrackingUiState.Content)
        val content = state as RideTrackingUiState.Content
        assertEquals("ride-123", content.snapshot.rideId)
        assertEquals(TrackingRideStatus.DRIVER_ARRIVING, content.snapshot.status)
        assertEquals("Driver is on the way", content.statusMessage)
        assertTrue(fakeRepository.subscribedRides.contains("ride-123"))
    }

    @Test
    fun `initial load with terminal ride status enters Terminal state immediately`() = runBlocking {
        fakeRepository.stubSnapshot = createTestSnapshot(
            rideId = "ride-123",
            status = TrackingRideStatus.COMPLETED
        )

        val viewModel = createViewModel("ride-123")
        val state = viewModel.uiState.value

        assertTrue(state is RideTrackingUiState.Terminal)
        val terminal = state as RideTrackingUiState.Terminal
        assertEquals(TrackingRideStatus.COMPLETED, terminal.status)
        assertFalse(fakeRepository.subscribedRides.contains("ride-123"))
    }

    @Test
    fun `initial load failure enters Error state`() = runBlocking {
        fakeRepository.shouldFailSnapshot = true

        val viewModel = createViewModel("ride-123")
        val state = viewModel.uiState.value

        assertTrue(state is RideTrackingUiState.Error)
        val err = state as RideTrackingUiState.Error
        assertTrue(err.canRetry)
    }

    @Test
    fun `realtime live update reconciles location and route progress in state`() = runBlocking {
        fakeRepository.stubSnapshot = createTestSnapshot(
            rideId = "ride-123",
            status = TrackingRideStatus.IN_PROGRESS
        )

        val viewModel = createViewModel("ride-123")

        val updateEvent = RealtimeEvent.RideTrackingUpdated(
            RideTrackingUpdatedPayloadDto(
                rideId = "ride-123",
                driverLocation = TrackingCoordinatesDto(latitude = 25.4510, longitude = 78.5720),
                freshness = "FRESH",
                distanceToDestinationMeters = 4200.0,
                eta = TrackingEtaInfoDto(available = true, seconds = 240),
                recordedAt = "2026-09-25T14:16:00Z"
            )
        )
        fakeRepository.eventsFlow.emit(updateEvent)

        val state = viewModel.uiState.value
        assertTrue(state is RideTrackingUiState.Content)
        val content = state as RideTrackingUiState.Content
        assertEquals(25.4510, content.snapshot.driverLocation!!.coordinates.latitude, 0.0001)
        assertEquals(4200, content.snapshot.distanceToDestinationMeters)
        assertEquals("4 mins", content.snapshot.eta.formattedEtaMinutes)
    }

    @Test
    fun `realtime ended event transitions to Terminal state and unsubscribes`() = runBlocking {
        fakeRepository.stubSnapshot = createTestSnapshot(
            rideId = "ride-123",
            status = TrackingRideStatus.IN_PROGRESS
        )

        val viewModel = createViewModel("ride-123")

        val endedEvent = RealtimeEvent.RideTrackingEnded(
            rideId = "ride-123",
            status = "COMPLETED",
            reason = "Destination reached",
            timestamp = "2026-09-25T14:20:00Z"
        )
        fakeRepository.eventsFlow.emit(endedEvent)

        val state = viewModel.uiState.value
        assertTrue(state is RideTrackingUiState.Terminal)
        val terminal = state as RideTrackingUiState.Terminal
        assertEquals(TrackingRideStatus.COMPLETED, terminal.status)
        assertEquals("Destination reached", terminal.reason)
        assertTrue(fakeRepository.unsubscribedRides.contains("ride-123"))
    }

    @Test
    fun `realtime disconnect sets degraded state without clearing last known location`() = runBlocking {
        fakeRepository.stubSnapshot = createTestSnapshot(
            rideId = "ride-123",
            status = TrackingRideStatus.DRIVER_ARRIVING
        )

        val viewModel = createViewModel("ride-123")
        fakeRepository.connectionFlow.value = RealtimeConnectionState.Disconnected

        val state = viewModel.uiState.value
        assertTrue(state is RideTrackingUiState.Content)
        val content = state as RideTrackingUiState.Content
        assertTrue(content.isDegraded)
        assertEquals("Live location temporarily unavailable.", content.connectionNotice)
        assertNotNull(content.snapshot.driverLocation)
    }

    @Test
    fun `onNavigateBack unsubscribes from realtime and navigates up`() = runBlocking {
        fakeRepository.stubSnapshot = createTestSnapshot(
            rideId = "ride-123",
            status = TrackingRideStatus.DRIVER_ARRIVING
        )

        val viewModel = createViewModel("ride-123")
        viewModel.onNavigateBack()

        assertTrue(fakeRepository.unsubscribedRides.contains("ride-123"))
    }

    private fun createTestSnapshot(
        rideId: String,
        status: TrackingRideStatus
    ): RideTrackingSnapshot {
        return RideTrackingSnapshot(
            rideId = rideId,
            status = status,
            trackingState = TrackingState.FRESH,
            driverLocation = TrackingDriverLocation(
                coordinates = LocationCoordinates(25.4484, 78.5685),
                accuracyMeters = 5.0f,
                freshness = TrackingFreshness.FRESH
            ),
            routeProgress = TrackingRouteProgress(
                totalDistanceMeters = 8000,
                completedDistanceMeters = 3000,
                remainingDistanceMeters = 5000,
                progressPercent = 37.5f
            ),
            distanceToPickupMeters = null,
            distanceToDestinationMeters = 5000,
            eta = TrackingEta(available = true, seconds = 360),
            updatedAt = "2026-09-25T14:15:00Z"
        )
    }

    private class FakeRideTrackingRepository : RideTrackingRepository {
        var stubSnapshot: RideTrackingSnapshot? = null
        var shouldFailSnapshot = false
        val subscribedRides = mutableListOf<String>()
        val unsubscribedRides = mutableListOf<String>()
        val eventsFlow = MutableSharedFlow<RealtimeEvent>(replay = 1)
        val connectionFlow = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)

        override suspend fun getRideTrackingSnapshot(
            rideId: String,
            pickupAddress: String?,
            destinationAddress: String?,
            pickupCoords: LocationCoordinates?,
            destinationCoords: LocationCoordinates?
        ): IshaaraResult<RideTrackingSnapshot> {
            if (shouldFailSnapshot) {
                return IshaaraResult.failure(IshaaraError.Network("Network error"))
            }
            return stubSnapshot?.let { IshaaraResult.success(it) }
                ?: IshaaraResult.failure(IshaaraError.NotFound(message = "Ride not found"))
        }

        override suspend fun getDriverLocation(rideId: String): IshaaraResult<TrackingDriverLocation> {
            return stubSnapshot?.driverLocation?.let { IshaaraResult.success(it) }
                ?: IshaaraResult.failure(IshaaraError.NotFound(message = "Location not found"))
        }

        override suspend fun subscribeToRideTracking(rideId: String): IshaaraResult<Unit> {
            subscribedRides.add(rideId)
            return IshaaraResult.success(Unit)
        }

        override suspend fun unsubscribeFromRideTracking(rideId: String): IshaaraResult<Unit> {
            unsubscribedRides.add(rideId)
            return IshaaraResult.success(Unit)
        }

        override fun observeRideTrackingEvents(rideId: String): Flow<RealtimeEvent> = eventsFlow

        override fun observeConnectionState(): Flow<RealtimeConnectionState> = connectionFlow
    }
}
