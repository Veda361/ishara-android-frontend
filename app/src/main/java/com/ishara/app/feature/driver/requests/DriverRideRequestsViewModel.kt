package com.ishara.app.feature.driver.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.navigation.NavigationManager
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.model.DriverRideRequestStatus
import com.ishara.app.domain.repository.DriverRideRequestRepository
import com.ishara.app.domain.usecase.AcceptRideRequestUseCase
import com.ishara.app.domain.usecase.GetDriverRideRequestsUseCase
import com.ishara.app.domain.usecase.ManagePassengerBoardingUseCase
import com.ishara.app.domain.usecase.RejectRideRequestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing Driver Ride Requests and Passenger Boarding lifecycle.
 *
 * Guarantees:
 * 1. Single source of truth via DriverRideRequestsUiState.
 * 2. Duplicate mutation protection: rejects rapid consecutive button clicks.
 * 3. Realtime event collection with automatic reconciliation.
 * 4. Safe offline/error presentation without fake optimistic mutations.
 */
class DriverRideRequestsViewModel(
    private val getDriverRideRequestsUseCase: GetDriverRideRequestsUseCase,
    private val acceptRideRequestUseCase: AcceptRideRequestUseCase,
    private val rejectRideRequestUseCase: RejectRideRequestUseCase,
    private val managePassengerBoardingUseCase: ManagePassengerBoardingUseCase,
    private val repository: DriverRideRequestRepository,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverRideRequestsUiState())
    val uiState: StateFlow<DriverRideRequestsUiState> = _uiState.asStateFlow()

    init {
        loadData(silent = false)
        observeRealtime()
    }

    fun selectTab(tab: DriverRequestsTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedRequest = null, selectedRide = null) }
        loadData(silent = true)
    }

    fun selectRequest(request: DriverRideRequest?) {
        _uiState.update { it.copy(selectedRequest = request) }
    }

    fun selectRide(ride: DriverPassengerRide?) {
        _uiState.update { it.copy(selectedRide = ride) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData(silent = true)
    }

    fun retry() {
        loadData(silent = false)
    }

    fun loadData(silent: Boolean = false) {
        if (!silent) {
            _uiState.update { it.copy(stage = DriverRideRequestsStage.Loading) }
        }

        viewModelScope.launch(dispatchers.io) {
            val requestsResult = getDriverRideRequestsUseCase(status = "PENDING")
            val ridesResult = managePassengerBoardingUseCase.getDriverRides()

            _uiState.update { state ->
                val isRefreshing = false

                if (requestsResult is IshaaraResult.Failure && state.pendingRequests.isEmpty() && !silent) {
                    state.copy(
                        stage = DriverRideRequestsStage.Error(requestsResult.error),
                        isRefreshing = isRefreshing
                    )
                } else {
                    val pendingItems = if (requestsResult is IshaaraResult.Success) {
                        requestsResult.data.items
                    } else {
                        state.pendingRequests
                    }

                    val activeRides = if (ridesResult is IshaaraResult.Success) {
                        ridesResult.data.filter { it.status.name in listOf("CREATED", "DRIVER_ARRIVING", "PICKED_UP", "IN_PROGRESS") }
                    } else {
                        state.activeRides
                    }

                    val totalCount = if (requestsResult is IshaaraResult.Success) requestsResult.data.total else state.totalRequestsCount
                    val hasMore = if (requestsResult is IshaaraResult.Success) requestsResult.data.hasMore else state.hasMore

                    val stage = if (pendingItems.isEmpty() && activeRides.isEmpty()) {
                        DriverRideRequestsStage.Empty
                    } else {
                        DriverRideRequestsStage.Content
                    }

                    state.copy(
                        stage = stage,
                        pendingRequests = pendingItems,
                        activeRides = activeRides,
                        totalRequestsCount = totalCount,
                        hasMore = hasMore,
                        isRefreshing = isRefreshing
                    )
                }
            }
        }
    }

    /**
     * Driver accepts an incoming ride request.
     * Guarded against duplicate calls.
     */
    fun acceptRequest(requestId: String) {
        if (_uiState.value.isActionInProgress) return

        _uiState.update { it.copy(actionState = DriverActionState.Accepting(requestId)) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = acceptRideRequestUseCase(requestId)) {
                is IshaaraResult.Success -> {
                    // Update state with backend authoritative result
                    _uiState.update { state ->
                        val updatedList = state.pendingRequests.filterNot { it.id == requestId }
                        state.copy(
                            actionState = DriverActionState.Idle,
                            pendingRequests = updatedList,
                            selectedRequest = null,
                            userNotification = "Ride request accepted. Passenger ride created.",
                            stage = if (updatedList.isEmpty() && state.activeRides.isEmpty()) DriverRideRequestsStage.Empty else DriverRideRequestsStage.Content
                        )
                    }
                    // Fetch updated rides list to present newly created ride
                    refresh()
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            actionState = DriverActionState.Idle,
                            userNotification = "Failed to accept request: ${result.error.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Driver rejects an incoming ride request.
     * Guarded against duplicate calls.
     */
    fun rejectRequest(requestId: String, reason: String? = null) {
        if (_uiState.value.isActionInProgress) return

        _uiState.update { it.copy(actionState = DriverActionState.Rejecting(requestId)) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = rejectRideRequestUseCase(requestId, reason)) {
                is IshaaraResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = state.pendingRequests.filterNot { it.id == requestId }
                        state.copy(
                            actionState = DriverActionState.Idle,
                            pendingRequests = updatedList,
                            selectedRequest = null,
                            userNotification = "Ride request rejected.",
                            stage = if (updatedList.isEmpty() && state.activeRides.isEmpty()) DriverRideRequestsStage.Empty else DriverRideRequestsStage.Content
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            actionState = DriverActionState.Idle,
                            userNotification = "Failed to reject request: ${result.error.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Driver arrives at pickup location.
     */
    fun markArrived(rideId: String) {
        executeRideAction(rideId, "Arriving at pickup") {
            managePassengerBoardingUseCase.markArrived(rideId)
        }
    }

    /**
     * Passenger Boarding: Driver confirms passenger has boarded.
     */
    fun markBoarded(rideId: String) {
        executeRideAction(rideId, "Confirming passenger boarding") {
            managePassengerBoardingUseCase.markBoarded(rideId)
        }
    }

    /**
     * Start transit towards destination.
     */
    fun startRide(rideId: String) {
        executeRideAction(rideId, "Starting trip to destination") {
            managePassengerBoardingUseCase.startRide(rideId)
        }
    }

    /**
     * Complete ride at destination.
     */
    fun completeRide(rideId: String) {
        executeRideAction(rideId, "Completing ride") {
            managePassengerBoardingUseCase.completeRide(rideId)
        }
    }

    /**
     * Cancel active ride with reason.
     */
    fun cancelRide(rideId: String, reason: String) {
        executeRideAction(rideId, "Cancelling ride") {
            managePassengerBoardingUseCase.cancelRide(rideId, reason)
        }
    }

    private fun executeRideAction(
        rideId: String,
        actionLabel: String,
        action: suspend () -> IshaaraResult<DriverPassengerRide>
    ) {
        if (_uiState.value.isActionInProgress) return

        _uiState.update { it.copy(actionState = DriverActionState.UpdatingRide(rideId, actionLabel)) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = action()) {
                is IshaaraResult.Success -> {
                    val updatedRide = result.data
                    _uiState.update { state ->
                        val updatedList = state.activeRides.map {
                            if (it.id == updatedRide.id) updatedRide else it
                        }.filterNot { it.status.name in listOf("COMPLETED", "CANCELLED") }

                        state.copy(
                            actionState = DriverActionState.Idle,
                            activeRides = updatedList,
                            selectedRide = if (state.selectedRide?.id == updatedRide.id) updatedRide else state.selectedRide,
                            userNotification = "$actionLabel completed successfully."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            actionState = DriverActionState.Idle,
                            userNotification = "Operation failed: ${result.error.message}"
                        )
                    }
                }
            }
        }
    }

    private fun observeRealtime() {
        repository.connectRealtime()

        viewModelScope.launch(dispatchers.io) {
            repository.observeRealtimeConnectionState().collect { state ->
                _uiState.update { it.copy(realtimeState = state) }
            }
        }

        viewModelScope.launch(dispatchers.io) {
            repository.observeRealtimeEvents().collect { event ->
                handleRealtimeEvent(event)
            }
        }
    }

    private fun handleRealtimeEvent(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.RideRequestCreated -> {
                // Refresh list on incoming request
                loadData(silent = true)
                _uiState.update { it.copy(userNotification = "New ride request received!") }
            }
            is RealtimeEvent.RideRequestCancelled -> {
                _uiState.update { state ->
                    val filtered = state.pendingRequests.filterNot { it.id == event.requestId }
                    state.copy(
                        pendingRequests = filtered,
                        userNotification = "A ride request was cancelled by the passenger."
                    )
                }
            }
            is RealtimeEvent.RideRequestExpired -> {
                _uiState.update { state ->
                    val filtered = state.pendingRequests.filterNot { it.id == event.requestId }
                    state.copy(
                        pendingRequests = filtered,
                        userNotification = "A ride request has expired."
                    )
                }
            }
            is RealtimeEvent.RideCreated -> {
                loadData(silent = true)
            }
            else -> {
                // Ignore irrelevant channels
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectRealtime()
    }
}
