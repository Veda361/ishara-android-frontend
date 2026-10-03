package com.ishara.app.feature.student.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.RideTrackingMapper
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.usecase.GetDriverLocationUseCase
import com.ishara.app.domain.usecase.GetRideTrackingUseCase
import com.ishara.app.domain.usecase.ObserveRideTrackingUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RideTrackingViewModel(
    val rideId: String,
    private val getRideTrackingUseCase: GetRideTrackingUseCase,
    private val getDriverLocationUseCase: GetDriverLocationUseCase,
    private val observeRideTrackingUseCase: ObserveRideTrackingUseCase,
    private val navigationManager: NavigationManager,
    private val locationRepository: com.ishara.app.domain.repository.LocationRepository? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val tag = "RideTrackingViewModel"

    private val _uiState = MutableStateFlow<RideTrackingUiState>(RideTrackingUiState.Loading)
    val uiState: StateFlow<RideTrackingUiState> = _uiState.asStateFlow()

    private var eventObservationJob: Job? = null
    private var connectionObservationJob: Job? = null
    private var wasConnectedBefore = false

    init {
        loadInitialTracking()
    }

    fun loadInitialTracking() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = RideTrackingUiState.Loading

            when (val result = getRideTrackingUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    val snapshot = result.data

                    if (snapshot.status.isTerminal) {
                        _uiState.value = RideTrackingUiState.Terminal(
                            status = snapshot.status,
                            reason = null,
                            finalSnapshot = snapshot
                        )
                        return@launch
                    }

                    _uiState.value = RideTrackingUiState.Content(
                        snapshot = snapshot,
                        connectionState = RealtimeConnectionState.Connecting,
                        statusMessage = RideTrackingUiState.getStatusDescription(snapshot.status)
                    )

                    resolveUserLocation()
                    startRealtimeSubscription()
                }
                is IshaaraResult.Failure -> {
                    handleError(result.error)
                }
            }
        }
    }

    private fun resolveUserLocation() {
        val repo = locationRepository ?: return
        viewModelScope.launch(dispatchers.io) {
            val locResult = repo.getCurrentLocation()
            if (locResult is IshaaraResult.Success) {
                val current = _uiState.value
                if (current is RideTrackingUiState.Content) {
                    _uiState.value = current.copy(userLocation = locResult.data)
                }
            }
        }
    }

    fun refresh() {
        val currentContent = _uiState.value as? RideTrackingUiState.Content ?: return
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = currentContent.copy(isRefreshing = true)

            when (val result = getRideTrackingUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    val snapshot = result.data
                    if (snapshot.status.isTerminal) {
                        unsubscribeFromRealtime()
                        _uiState.value = RideTrackingUiState.Terminal(
                            status = snapshot.status,
                            finalSnapshot = snapshot
                        )
                    } else {
                        _uiState.value = currentContent.copy(
                            snapshot = snapshot,
                            isRefreshing = false,
                            statusMessage = RideTrackingUiState.getStatusDescription(snapshot.status)
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    // Do not wipe valid content on refresh error; keep last known state
                    _uiState.value = currentContent.copy(
                        isRefreshing = false,
                        isDegraded = true,
                        connectionNotice = "Couldn't refresh tracking. Showing last known state."
                    )
                }
            }
        }
    }

    private fun startRealtimeSubscription() {
        eventObservationJob?.cancel()
        connectionObservationJob?.cancel()

        // 1. Observe connection state
        connectionObservationJob = viewModelScope.launch(dispatchers.main) {
            observeRideTrackingUseCase.observeConnectionState().collect { connState ->
                val current = _uiState.value
                if (current is RideTrackingUiState.Content) {
                    when (connState) {
                        is RealtimeConnectionState.Connected -> {
                            val hadDisconnect = wasConnectedBefore
                            wasConnectedBefore = true
                            _uiState.value = current.copy(
                                connectionState = connState,
                                isDegraded = false,
                                connectionNotice = null
                            )
                            if (hadDisconnect) {
                                // Silent refresh after reconnect to ensure authoritative state alignment
                                refreshAuthoritativeStateSilently()
                            }
                        }
                        is RealtimeConnectionState.Connecting -> {
                            _uiState.value = current.copy(
                                connectionState = connState,
                                connectionNotice = "Updating live location..."
                            )
                        }
                        is RealtimeConnectionState.Reconnecting -> {
                            _uiState.value = current.copy(
                                connectionState = connState,
                                isDegraded = true,
                                connectionNotice = "Reconnecting to live tracking..."
                            )
                        }
                        is RealtimeConnectionState.Disconnected -> {
                            _uiState.value = current.copy(
                                connectionState = connState,
                                isDegraded = true,
                                connectionNotice = "Live location temporarily unavailable."
                            )
                        }
                        is RealtimeConnectionState.Failed -> {
                            _uiState.value = current.copy(
                                connectionState = connState,
                                isDegraded = true,
                                connectionNotice = "Connection lost. Showing last known location."
                            )
                        }
                    }
                }
            }
        }

        // 2. Observe typed realtime events
        eventObservationJob = viewModelScope.launch(dispatchers.main) {
            observeRideTrackingUseCase.observeEvents(rideId).collect { event ->
                handleRealtimeEvent(event)
            }
        }

        // 3. Send subscription frame
        viewModelScope.launch(dispatchers.io) {
            observeRideTrackingUseCase.subscribe(rideId)
        }
    }

    private fun handleRealtimeEvent(event: RealtimeEvent) {
        val currentContent = _uiState.value as? RideTrackingUiState.Content ?: return

        when (event) {
            is RealtimeEvent.LiveTelemetryUpdate -> {
                val loc = com.ishara.app.domain.model.TrackingDriverLocation(
                    coordinates = com.ishara.app.core.location.LocationCoordinates(
                        latitude = event.coordinate.latitude,
                        longitude = event.coordinate.longitude
                    ),
                    headingDegrees = event.headingDegrees,
                    speedMps = event.speedMps,
                    recordedAt = com.ishara.app.core.location.LocationCoordinates.formatIso8601Utc(System.currentTimeMillis()),
                    freshness = com.ishara.app.domain.model.TrackingFreshness.FRESH
                )
                val updatedSnapshot = currentContent.snapshot.copy(
                    driverLocation = loc,
                    distanceToDestinationMeters = event.remainingDistanceMeters ?: currentContent.snapshot.distanceToDestinationMeters,
                    trackingState = com.ishara.app.domain.model.TrackingState.FRESH
                )
                _uiState.value = currentContent.copy(
                    snapshot = updatedSnapshot
                )
                IshaaraLogger.d("ISHAARA_WS", "event=LOCATION_UPDATE tripId=$rideId lat=${loc.coordinates.latitude} lng=${loc.coordinates.longitude}")
            }
            is RealtimeEvent.TrackingSnapshot -> {
                val mapped = RideTrackingMapper.mapTrackingResponse(
                    dto = event.snapshot,
                    pickupAddress = currentContent.snapshot.pickupAddress,
                    destinationAddress = currentContent.snapshot.destinationAddress,
                    pickupCoords = currentContent.snapshot.pickupCoordinates,
                    destinationCoords = currentContent.snapshot.destinationCoordinates
                )
                if (mapped.status.isTerminal) {
                    unsubscribeFromRealtime()
                    _uiState.value = RideTrackingUiState.Terminal(
                        status = mapped.status,
                        finalSnapshot = mapped
                    )
                } else {
                    _uiState.value = currentContent.copy(
                        snapshot = mapped,
                        statusMessage = RideTrackingUiState.getStatusDescription(mapped.status)
                    )
                }
            }
            is RealtimeEvent.RideTrackingUpdated -> {
                val reconciled = RideTrackingMapper.reconcileWithRealtimeUpdate(
                    current = currentContent.snapshot,
                    update = event.update
                )
                _uiState.value = currentContent.copy(
                    snapshot = reconciled,
                    statusMessage = RideTrackingUiState.getStatusDescription(reconciled.status)
                )
            }
            is RealtimeEvent.RideTrackingEnded -> {
                unsubscribeFromRealtime()
                val terminalStatus = TrackingRideStatus.fromBackend(event.status)
                _uiState.value = RideTrackingUiState.Terminal(
                    status = terminalStatus,
                    reason = event.reason,
                    finalSnapshot = currentContent.snapshot.copy(status = terminalStatus)
                )
            }
            is RealtimeEvent.RideDriverArriving -> {
                val updatedSnapshot = currentContent.snapshot.copy(status = TrackingRideStatus.DRIVER_ARRIVING)
                _uiState.value = currentContent.copy(
                    snapshot = updatedSnapshot,
                    statusMessage = RideTrackingUiState.getStatusDescription(TrackingRideStatus.DRIVER_ARRIVING)
                )
            }
            is RealtimeEvent.RidePickedUp -> {
                val updatedSnapshot = currentContent.snapshot.copy(status = TrackingRideStatus.PICKED_UP)
                _uiState.value = currentContent.copy(
                    snapshot = updatedSnapshot,
                    statusMessage = RideTrackingUiState.getStatusDescription(TrackingRideStatus.PICKED_UP)
                )
            }
            is RealtimeEvent.RideStarted -> {
                val updatedSnapshot = currentContent.snapshot.copy(status = TrackingRideStatus.IN_PROGRESS)
                _uiState.value = currentContent.copy(
                    snapshot = updatedSnapshot,
                    statusMessage = RideTrackingUiState.getStatusDescription(TrackingRideStatus.IN_PROGRESS)
                )
            }
            is RealtimeEvent.RideCompleted -> {
                unsubscribeFromRealtime()
                val terminalStatus = TrackingRideStatus.COMPLETED
                _uiState.value = RideTrackingUiState.Terminal(
                    status = terminalStatus,
                    reason = null,
                    finalSnapshot = currentContent.snapshot.copy(status = terminalStatus)
                )
            }
            is RealtimeEvent.RideCancelled -> {
                unsubscribeFromRealtime()
                val terminalStatus = TrackingRideStatus.CANCELLED
                _uiState.value = RideTrackingUiState.Terminal(
                    status = terminalStatus,
                    reason = event.reason,
                    finalSnapshot = currentContent.snapshot.copy(status = terminalStatus)
                )
            }
            is RealtimeEvent.DriverLocationUpdated -> {
                val loc = com.ishara.app.domain.model.TrackingDriverLocation(
                    coordinates = com.ishara.app.core.location.LocationCoordinates(
                        latitude = event.location.latitude,
                        longitude = event.location.longitude
                    ),
                    recordedAt = event.recordedAt,
                    freshness = if (event.isStale) com.ishara.app.domain.model.TrackingFreshness.STALE else com.ishara.app.domain.model.TrackingFreshness.FRESH
                )
                val updatedSnapshot = currentContent.snapshot.copy(
                    driverLocation = loc,
                    trackingState = if (event.isStale) com.ishara.app.domain.model.TrackingState.STALE else com.ishara.app.domain.model.TrackingState.FRESH
                )
                _uiState.value = currentContent.copy(
                    snapshot = updatedSnapshot
                )
            }
            is RealtimeEvent.RideTrackingError -> {
                IshaaraLogger.w(tag, "Ride tracking server error: ${event.code} - ${event.message}")
                if (event.code == "RIDE_NOT_AUTHORIZED" || event.code == "UNAUTHORIZED") {
                    _uiState.value = RideTrackingUiState.Error(
                        message = "You are not authorized to track this ride.",
                        isUnauthorized = true,
                        canRetry = false
                    )
                }
            }
            else -> Unit
        }
    }

    private fun refreshAuthoritativeStateSilently() {
        viewModelScope.launch(dispatchers.io) {
            when (val result = getRideTrackingUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    val current = _uiState.value as? RideTrackingUiState.Content ?: return@launch
                    val snapshot = result.data
                    if (snapshot.status.isTerminal) {
                        unsubscribeFromRealtime()
                        _uiState.value = RideTrackingUiState.Terminal(
                            status = snapshot.status,
                            finalSnapshot = snapshot
                        )
                    } else {
                        _uiState.value = current.copy(
                            snapshot = snapshot,
                            statusMessage = RideTrackingUiState.getStatusDescription(snapshot.status)
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Silent refresh after reconnect failed: ${result.error.message}")
                }
            }
        }
    }

    fun onNavigateBack() {
        unsubscribeFromRealtime()
        navigationManager.navigateUp()
    }

    fun onPayFareClicked() {
        navigationManager.navigate(com.ishara.app.navigation.IshaaraDestination.StudentFareSummary.createRoute(rideId))
    }

    fun onSafetyClicked() {
        navigationManager.navigate(com.ishara.app.navigation.IshaaraDestination.StudentSafety.createRoute(rideId))
    }

    private fun unsubscribeFromRealtime() {
        eventObservationJob?.cancel()
        connectionObservationJob?.cancel()
        viewModelScope.launch(dispatchers.io) {
            observeRideTrackingUseCase.unsubscribe(rideId)
        }
    }

    private fun handleError(error: IshaaraError) {
        val isAuth = error is IshaaraError.Authentication ||
                error is IshaaraError.Forbidden ||
                (error is IshaaraError.Server && error.code in listOf(401, 403))
        val message = when {
            isAuth -> "You do not have permission to view this ride."
            error is IshaaraError.Network -> "Network unavailable. Please check your connection."
            else -> "Ride tracking is currently unavailable."
        }
        _uiState.value = RideTrackingUiState.Error(
            message = message,
            isUnauthorized = isAuth,
            canRetry = !isAuth
        )
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeFromRealtime()
    }
}
