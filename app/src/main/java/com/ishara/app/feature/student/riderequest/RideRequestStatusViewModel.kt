package com.ishara.app.feature.student.riderequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.model.isTerminal
import com.ishara.app.domain.usecase.CancelRideRequestUseCase
import com.ishara.app.domain.usecase.GetRideRequestUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing the Passenger Ride Request Status lifecycle.
 *
 * Implements:
 * 1. Authoritative status retrieval from backend (GET /api/v1/ride-requests/:requestId).
 * 2. Controlled, bounded polling for PENDING requests with automatic termination upon final state.
 * 3. Safe passenger cancellation with backend confirmation and conflict detection.
 * 4. Handoff to Phase A11 (StudentRideTracking) when request is ACCEPTED.
 */
class RideRequestStatusViewModel(
    val requestId: String,
    val initialTrip: DiscoveredTrip? = null,
    val initialRequest: RideRequestResult? = null,
    private val getRideRequestUseCase: GetRideRequestUseCase,
    private val cancelRideRequestUseCase: CancelRideRequestUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        RideRequestStatusUiState(
            requestId = requestId,
            trip = initialTrip,
            request = initialRequest,
            status = initialRequest?.status ?: RideRequestStatus.PENDING,
            associatedRideId = initialRequest?.associatedRideId,
            isLoading = initialRequest == null
        )
    )
    val uiState: StateFlow<RideRequestStatusUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var actionJob: Job? = null

    init {
        loadStatus()
        startPollingIfPending()
    }

    /**
     * Fetches current authoritative request state from backend.
     */
    fun refreshStatus() {
        loadStatus()
    }

    private fun loadStatus() {
        actionJob?.cancel()
        actionJob = scope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getRideRequestUseCase.execute(requestId)) {
                is IshaaraResult.Success -> {
                    val request = result.data
                    _uiState.update {
                        it.copy(
                            request = request,
                            status = request.status,
                            isLoading = false,
                            errorMessage = null,
                            associatedRideId = request.associatedRideId ?: it.associatedRideId
                        )
                    }
                    if (request.status.isTerminal) {
                        stopPolling()
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = it.errorMessage ?: result.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Cancels the active pending request on behalf of the passenger.
     */
    fun cancelRequest(reason: String? = null) {
        if (_uiState.value.isCancelling || _uiState.value.status.isTerminal) return

        actionJob?.cancel()
        actionJob = scope.launch(dispatchers.io) {
            _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
            when (val result = cancelRideRequestUseCase.execute(requestId, reason)) {
                is IshaaraResult.Success -> {
                    stopPolling()
                    val updated = result.data
                    _uiState.update {
                        it.copy(
                            request = updated,
                            status = updated.status,
                            isCancelling = false,
                            cancelSuccessMessage = "Ride request cancelled successfully."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            errorMessage = result.error.message
                        )
                    }
                    // If conflict, refresh state to reflect latest server reality
                    loadStatus()
                }
            }
        }
    }

    /**
     * Starts bounded background polling every 5 seconds while state is PENDING.
     */
    private fun startPollingIfPending() {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch(dispatchers.io) {
            while (isActive && !_uiState.value.status.isTerminal) {
                delay(5_000L)
                if (!isActive || _uiState.value.status.isTerminal) break

                val result = getRideRequestUseCase.execute(requestId)
                if (result is IshaaraResult.Success) {
                    val updated = result.data
                    _uiState.update {
                        it.copy(
                            request = updated,
                            status = updated.status,
                            associatedRideId = updated.associatedRideId ?: it.associatedRideId
                        )
                    }
                    if (updated.status.isTerminal) {
                        break
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Navigates toward Phase A11 (StudentRideTracking) when ride is accepted.
     */
    fun onContinueToLiveRide() {
        stopPolling()
        val rideId = _uiState.value.associatedRideId ?: _uiState.value.request?.associatedRideId ?: requestId
        navigationManager.navigate(IshaaraDestination.StudentRideTracking.createRoute(rideId))
    }

    /**
     * Navigates back to Discovery to search for other trips.
     */
    fun onBackToDiscovery() {
        stopPolling()
        navigationManager.navigate(IshaaraDestination.StudentDiscovery.route)
    }

    /**
     * Navigates back to Student Home.
     */
    fun onBackToHome() {
        stopPolling()
        navigationManager.navigate(IshaaraDestination.StudentHome.route)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
