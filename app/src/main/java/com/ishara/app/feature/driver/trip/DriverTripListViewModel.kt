package com.ishara.app.feature.driver.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CreateTripParams
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.usecase.CancelDriverTripUseCase
import com.ishara.app.domain.usecase.CompleteDriverTripUseCase
import com.ishara.app.domain.usecase.CreateDriverTripUseCase
import com.ishara.app.domain.usecase.GetAssignedVehicleUseCase
import com.ishara.app.domain.usecase.GetDriverTripsUseCase
import com.ishara.app.domain.usecase.ObserveActiveTripUseCase
import com.ishara.app.domain.usecase.ObserveDriverTripsUseCase
import com.ishara.app.domain.usecase.StartDriverTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing the Driver Trip List screen and operational lifecycle actions.
 */
class DriverTripListViewModel(
    private val getDriverTripsUseCase: GetDriverTripsUseCase,
    private val startDriverTripUseCase: StartDriverTripUseCase,
    private val completeDriverTripUseCase: CompleteDriverTripUseCase,
    private val cancelDriverTripUseCase: CancelDriverTripUseCase,
    private val createDriverTripUseCase: CreateDriverTripUseCase,
    private val observeDriverTripsUseCase: ObserveDriverTripsUseCase,
    private val observeActiveTripUseCase: ObserveActiveTripUseCase,
    private val getAssignedVehicleUseCase: GetAssignedVehicleUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    init {
        // Observe trips and active trip reactively
        viewModelScope.launch(dispatchers.main) {
            observeDriverTripsUseCase().collect { cachedTrips ->
                if (_uiState.value.contentState !is TripListContentState.Error) {
                    val active = cachedTrips.firstOrNull { it.status == TripStatus.ACTIVE }
                        ?: cachedTrips.firstOrNull { it.status == TripStatus.READY || it.status == TripStatus.ASSIGNED }
                    updateContentWithTrips(cachedTrips, active, _uiState.value.selectedTab)
                }
            }
        }

        loadTrips(forceRefresh = false)
        loadAssignedVehicle()
    }

    fun selectTab(tab: TripFilterTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        val currentTrips = observeDriverTripsUseCase().value
        val active = observeActiveTripUseCase().value
        updateContentWithTrips(currentTrips, active, tab)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, actionErrorMessage = null) }
        loadTrips(forceRefresh = true)
        loadAssignedVehicle()
    }

    private fun loadTrips(forceRefresh: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            if (!forceRefresh && _uiState.value.contentState !is TripListContentState.Success) {
                _uiState.update { it.copy(contentState = TripListContentState.Loading) }
            }

            when (val result = getDriverTripsUseCase(forceRefresh = forceRefresh)) {
                is IshaaraResult.Success -> {
                    val trips = result.data
                    val active = trips.firstOrNull { it.status == TripStatus.ACTIVE }
                        ?: trips.firstOrNull { it.status == TripStatus.READY || it.status == TripStatus.ASSIGNED }

                    _uiState.update {
                        it.copy(isRefreshing = false)
                    }
                    updateContentWithTrips(trips, active, _uiState.value.selectedTab)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = false,
                            contentState = mapErrorToContentState(result.error)
                        )
                    }
                }
            }
        }
    }

    private fun loadAssignedVehicle() {
        viewModelScope.launch(dispatchers.io) {
            when (val res = getAssignedVehicleUseCase()) {
                is IshaaraResult.Success -> {
                    _uiState.update { it.copy(assignedVehicle = res.data.vehicle) }
                }
                is IshaaraResult.Failure -> {
                    // Informational only, non-fatal for list
                }
            }
        }
    }

    private fun updateContentWithTrips(trips: List<Trip>, activeTrip: Trip?, tab: TripFilterTab) {
        val filtered = trips.filter { tab.matches(it.status) }
        _uiState.update { state ->
            state.copy(
                contentState = if (filtered.isEmpty()) {
                    TripListContentState.Empty(tab)
                } else {
                    TripListContentState.Success(
                        trips = filtered,
                        activeTrip = activeTrip,
                        filter = tab
                    )
                }
            )
        }
    }

    fun startTrip(tripId: String) {
        if (_uiState.value.actionInFlightTripId != null) return // Duplicate submission prevention
        _uiState.update { it.copy(actionInFlightTripId = tripId, actionErrorMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = startDriverTripUseCase(tripId)) {
                is IshaaraResult.Success -> {
                    _uiState.update { it.copy(actionInFlightTripId = null) }
                    loadTrips(forceRefresh = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            actionInFlightTripId = null,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun completeTrip(tripId: String) {
        if (_uiState.value.actionInFlightTripId != null) return
        _uiState.update { it.copy(actionInFlightTripId = tripId, actionErrorMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = completeDriverTripUseCase(tripId)) {
                is IshaaraResult.Success -> {
                    _uiState.update { it.copy(actionInFlightTripId = null) }
                    loadTrips(forceRefresh = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            actionInFlightTripId = null,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun cancelTrip(tripId: String, reason: String? = null) {
        if (_uiState.value.actionInFlightTripId != null) return
        _uiState.update { it.copy(actionInFlightTripId = tripId, actionErrorMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = cancelDriverTripUseCase(tripId, reason)) {
                is IshaaraResult.Success -> {
                    _uiState.update { it.copy(actionInFlightTripId = null) }
                    loadTrips(forceRefresh = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            actionInFlightTripId = null,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = true, actionErrorMessage = null) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, isCreatingTrip = false) }
    }

    fun createTrip(
        originAddress: String,
        originLat: Double,
        originLng: Double,
        destAddress: String,
        destLat: Double,
        destLng: Double,
        scheduledDepartureAt: String? = null
    ) {
        val vehicle = _uiState.value.assignedVehicle
        if (vehicle == null) {
            _uiState.update { it.copy(actionErrorMessage = "Cannot create trip without an assigned vehicle. Assign a vehicle first.") }
            return
        }

        _uiState.update { it.copy(isCreatingTrip = true, actionErrorMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            val params = CreateTripParams(
                vehicleId = vehicle.id,
                origin = TripLocation.of(formattedAddress = originAddress.trim(), latitude = originLat, longitude = originLng),
                destination = TripLocation.of(formattedAddress = destAddress.trim(), latitude = destLat, longitude = destLng),
                scheduledDepartureAt = scheduledDepartureAt
            )

            when (val result = createDriverTripUseCase(params)) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingTrip = false,
                            isCreateDialogOpen = false
                        )
                    }
                    loadTrips(forceRefresh = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isCreatingTrip = false,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun dismissActionError() {
        _uiState.update { it.copy(actionErrorMessage = null) }
    }

    private fun mapErrorToContentState(error: IshaaraError): TripListContentState.Error {
        return when (error) {
            is IshaaraError.Authentication -> {
                val isExpired = error.message.contains("expired", ignoreCase = true) ||
                                (error as? IshaaraError.Authentication)?.errorCode?.contains("EXPIRED", ignoreCase = true) == true
                TripListContentState.Error(
                    message = error.message.ifBlank { "Authentication required. Please sign in." },
                    isSessionExpired = isExpired
                )
            }
            is IshaaraError.Forbidden -> TripListContentState.Error(
                message = error.message,
                isForbidden = true,
                canRetry = false
            )
            is IshaaraError.Network -> TripListContentState.Error(
                message = "Network unavailable. Please check your connection."
            )
            is IshaaraError.Timeout -> TripListContentState.Error(
                message = "Connection timed out. Tap retry to reload trips."
            )
            is IshaaraError.Server -> TripListContentState.Error(
                message = "Backend service unavailable. Please try again shortly."
            )
            else -> TripListContentState.Error(
                message = error.message
            )
        }
    }
}
