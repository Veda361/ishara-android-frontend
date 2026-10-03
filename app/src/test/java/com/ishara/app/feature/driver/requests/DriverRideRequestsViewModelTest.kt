package com.ishara.app.feature.driver.requests

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.model.DriverRideRequestPage
import com.ishara.app.domain.model.DriverRideRequestStatus
import com.ishara.app.domain.model.DriverRideStatus
import com.ishara.app.domain.repository.DriverRideRequestRepository
import com.ishara.app.domain.usecase.AcceptRideRequestUseCase
import com.ishara.app.domain.usecase.GetDriverRideRequestsUseCase
import com.ishara.app.domain.usecase.ManagePassengerBoardingUseCase
import com.ishara.app.domain.usecase.RejectRideRequestUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriverRideRequestsViewModelTest {

    private val testDispatcher = Dispatchers.Unconfined

    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private lateinit var fakeRepository: FakeDriverRideRequestRepository
    private lateinit var getDriverRideRequestsUseCase: GetDriverRideRequestsUseCase
    private lateinit var acceptRideRequestUseCase: AcceptRideRequestUseCase
    private lateinit var rejectRideRequestUseCase: RejectRideRequestUseCase
    private lateinit var managePassengerBoardingUseCase: ManagePassengerBoardingUseCase
    private lateinit var navigationManager: NavigationManager

    @Before
    fun setup() {
        fakeRepository = FakeDriverRideRequestRepository()
        getDriverRideRequestsUseCase = GetDriverRideRequestsUseCase(fakeRepository)
        acceptRideRequestUseCase = AcceptRideRequestUseCase(fakeRepository)
        rejectRideRequestUseCase = RejectRideRequestUseCase(fakeRepository)
        managePassengerBoardingUseCase = ManagePassengerBoardingUseCase(fakeRepository)
        navigationManager = NavigationManager()
    }

    private fun createViewModel(): DriverRideRequestsViewModel {
        return DriverRideRequestsViewModel(
            getDriverRideRequestsUseCase = getDriverRideRequestsUseCase,
            acceptRideRequestUseCase = acceptRideRequestUseCase,
            rejectRideRequestUseCase = rejectRideRequestUseCase,
            managePassengerBoardingUseCase = managePassengerBoardingUseCase,
            repository = fakeRepository,
            navigationManager = navigationManager,
            dispatchers = dispatchers
        )
    }

    @Test
    fun loadData_populatesPendingRequestsAndActiveRides() = runBlocking {
        val sampleRequest = createSampleRequest("req_101")
        fakeRepository.requestsPageToReturn = IshaaraResult.success(
            DriverRideRequestPage(listOf(sampleRequest), total = 1, page = 1, limit = 20, hasMore = false)
        )

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(DriverRideRequestsStage.Content, state.stage)
        assertEquals(1, state.pendingRequests.size)
        assertEquals("req_101", state.pendingRequests[0].id)
    }

    @Test
    fun acceptRequest_transitionsStateAndRefreshes() = runBlocking {
        val sampleRequest = createSampleRequest("req_101")
        fakeRepository.requestsPageToReturn = IshaaraResult.success(
            DriverRideRequestPage(listOf(sampleRequest), total = 1, page = 1, limit = 20, hasMore = false)
        )
        fakeRepository.acceptResultToReturn = IshaaraResult.success(sampleRequest.copy(status = DriverRideRequestStatus.ACCEPTED))

        val viewModel = createViewModel()

        // When request is accepted, backend no longer lists it as PENDING
        fakeRepository.requestsPageToReturn = IshaaraResult.success(
            DriverRideRequestPage(emptyList(), total = 0, page = 1, limit = 20, hasMore = false)
        )
        viewModel.acceptRequest("req_101")

        val state = viewModel.uiState.value
        assertEquals(DriverActionState.Idle, state.actionState)
        assertTrue(state.pendingRequests.isEmpty())
        assertTrue(state.userNotification?.contains("accepted") == true)
    }

    @Test
    fun rejectRequest_removesRequestFromPending() = runBlocking {
        val sampleRequest = createSampleRequest("req_101")
        fakeRepository.requestsPageToReturn = IshaaraResult.success(
            DriverRideRequestPage(listOf(sampleRequest), total = 1, page = 1, limit = 20, hasMore = false)
        )
        fakeRepository.rejectResultToReturn = IshaaraResult.success(sampleRequest.copy(status = DriverRideRequestStatus.REJECTED))

        val viewModel = createViewModel()
        viewModel.rejectRequest("req_101", "Full vehicle")

        val state = viewModel.uiState.value
        assertEquals(DriverActionState.Idle, state.actionState)
        assertTrue(state.pendingRequests.isEmpty())
        assertTrue(state.userNotification?.contains("rejected") == true)
    }

    @Test
    fun boardingWorkflow_markBoardedUpdatesActiveRide() = runBlocking {
        val activeRide = createSampleRide("ride_202", DriverRideStatus.DRIVER_ARRIVING)
        fakeRepository.ridesToReturn = IshaaraResult.success(listOf(activeRide))
        fakeRepository.boardedResultToReturn = IshaaraResult.success(activeRide.copy(status = DriverRideStatus.PICKED_UP))

        val viewModel = createViewModel()
        viewModel.markBoarded("ride_202")

        val state = viewModel.uiState.value
        assertEquals(DriverActionState.Idle, state.actionState)
        assertEquals(1, state.activeRides.size)
        assertEquals(DriverRideStatus.PICKED_UP, state.activeRides[0].status)
    }

    @Test
    fun realtimeEvent_rideRequestCancelledRemovesRequest() = runBlocking {
        val sampleRequest = createSampleRequest("req_303")
        fakeRepository.requestsPageToReturn = IshaaraResult.success(
            DriverRideRequestPage(listOf(sampleRequest), total = 1, page = 1, limit = 20, hasMore = false)
        )

        val viewModel = createViewModel()
        assertEquals(1, viewModel.uiState.value.pendingRequests.size)

        // Passenger cancels request via realtime event
        fakeRepository.emitRealtimeEvent(RealtimeEvent.RideRequestCancelled("req_303", "Found other transit"))

        val state = viewModel.uiState.value
        assertTrue(state.pendingRequests.isEmpty())
        assertTrue(state.userNotification?.contains("cancelled") == true)
    }

    private fun createSampleRequest(id: String) = DriverRideRequest(
        id = id,
        tripId = "trip_001",
        driverId = "driver_123",
        passengerId = "usr_456",
        pickupAddress = "Gate 1",
        pickupCoordinates = Pair(12.9716, 77.5946),
        destinationAddress = "Library",
        destinationCoordinates = Pair(12.9754, 77.5982),
        status = DriverRideRequestStatus.PENDING,
        requestedAt = "2026-09-25T13:00:00Z",
        expiresAt = "2026-09-25T13:05:00Z",
        respondedAt = null,
        rejectionReason = null,
        cancellationReason = null
    )

    private fun createSampleRide(id: String, status: DriverRideStatus) = DriverPassengerRide(
        id = id,
        rideRequestId = "req_001",
        tripId = "trip_001",
        driverId = "driver_123",
        passengerId = "usr_456",
        pickupAddress = "Gate 1",
        pickupCoordinates = Pair(12.9716, 77.5946),
        destinationAddress = "Library",
        destinationCoordinates = Pair(12.9754, 77.5982),
        status = status,
        pickupTime = null,
        startTime = null,
        completionTime = null,
        cancellationReason = null
    )

    private class FakeDriverRideRequestRepository : DriverRideRequestRepository {
        var requestsPageToReturn: IshaaraResult<DriverRideRequestPage> = IshaaraResult.success(
            DriverRideRequestPage(emptyList(), total = 0, page = 1, limit = 20, hasMore = false)
        )
        var acceptResultToReturn: IshaaraResult<DriverRideRequest> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))
        var rejectResultToReturn: IshaaraResult<DriverRideRequest> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))
        var ridesToReturn: IshaaraResult<List<DriverPassengerRide>> = IshaaraResult.success(emptyList())
        var arrivedResultToReturn: IshaaraResult<DriverPassengerRide> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))
        var boardedResultToReturn: IshaaraResult<DriverPassengerRide> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))
        var startResultToReturn: IshaaraResult<DriverPassengerRide> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))
        var completeResultToReturn: IshaaraResult<DriverPassengerRide> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))
        var cancelResultToReturn: IshaaraResult<DriverPassengerRide> = IshaaraResult.failure(IshaaraError.Validation(message = "Not set"))

        private val _eventsFlow = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 16)
        private val _connectionState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)

        fun emitRealtimeEvent(event: RealtimeEvent) {
            _eventsFlow.tryEmit(event)
        }

        override suspend fun getDriverRideRequests(
            status: String?,
            tripId: String?,
            page: Int,
            limit: Int,
            cursor: String?
        ): IshaaraResult<DriverRideRequestPage> = requestsPageToReturn

        override suspend fun getRideRequestDetails(requestId: String): IshaaraResult<DriverRideRequest> {
            return acceptResultToReturn
        }

        override suspend fun acceptRideRequest(requestId: String): IshaaraResult<DriverRideRequest> = acceptResultToReturn

        override suspend fun rejectRideRequest(requestId: String, reason: String?): IshaaraResult<DriverRideRequest> = rejectResultToReturn

        override suspend fun getDriverRides(status: String?, page: Int, limit: Int): IshaaraResult<List<DriverPassengerRide>> = ridesToReturn

        override suspend fun markDriverArrived(rideId: String): IshaaraResult<DriverPassengerRide> = arrivedResultToReturn

        override suspend fun markPassengerBoarded(rideId: String): IshaaraResult<DriverPassengerRide> = boardedResultToReturn

        override suspend fun startRide(rideId: String): IshaaraResult<DriverPassengerRide> = startResultToReturn

        override suspend fun completeRide(rideId: String): IshaaraResult<DriverPassengerRide> = completeResultToReturn

        override suspend fun cancelRide(rideId: String, reason: String): IshaaraResult<DriverPassengerRide> = cancelResultToReturn

        override fun observeRealtimeEvents(): Flow<RealtimeEvent> = _eventsFlow

        override fun observeRealtimeConnectionState(): StateFlow<RealtimeConnectionState> = _connectionState

        override fun connectRealtime() {}

        override fun disconnectRealtime() {}
    }
}
