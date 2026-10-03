package com.ishara.app.feature.student.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.usecase.DiscoverTripsUseCase
import com.ishara.app.domain.usecase.GetPassengerTripDetailsUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing the Passenger Trip Discovery lifecycle (Phase A09).
 *
 * Responsibilities:
 * 1. Maintains origin and destination query state.
 * 2. Validates discovery prerequisites (coordinate bounds, identical origin/destination).
 * 3. Prevents duplicate searches while a query is in-flight.
 * 4. Protects against stale out-of-order network responses using monotonically increasing request IDs.
 * 5. Preserves authoritative backend candidate ranking order.
 * 6. Supports cursor-based pagination (loadNextPage).
 * 7. Fetches public sanitized passenger trip details on trip selection.
 * 8. Maintains a strict architectural boundary: Selection candidate ONLY (NO ride request execution).
 * 9. Supports clean state clearing to prevent cross-account data leakage.
 */
class DiscoveryViewModel(
    initialQuery: DiscoveryQuery,
    private val discoverTripsUseCase: DiscoverTripsUseCase,
    private val getPassengerTripDetailsUseCase: GetPassengerTripDetailsUseCase? = null,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(DiscoveryUiState(query = initialQuery))
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var detailsJob: Job? = null
    private var currentRequestId = 0L

    init {
        executeDiscovery(initialQuery)
    }

    /**
     * Validates prerequisites and executes trip discovery for the specified [query].
     * Cancels any ongoing search and protects against stale responses.
     */
    fun executeDiscovery(query: DiscoveryQuery) {
        // Prevent duplicate execution if already loading the exact same query
        val currentState = _uiState.value
        if (discoveryJob?.isActive == true && currentState.query == query && currentState.stage is DiscoveryStage.Loading) {
            return
        }

        // Search prerequisites validation
        if (query.originLatitude !in -90.0..90.0 || query.originLongitude !in -180.0..180.0) {
            _uiState.update {
                it.copy(
                    query = query,
                    stage = DiscoveryStage.ValidationError("Invalid origin coordinates: latitude must be between -90 and 90, longitude between -180 and 180."),
                    validationError = "Invalid origin coordinates.",
                    trips = emptyList()
                )
            }
            return
        }

        if (query.destinationLatitude !in -90.0..90.0 || query.destinationLongitude !in -180.0..180.0) {
            _uiState.update {
                it.copy(
                    query = query,
                    stage = DiscoveryStage.ValidationError("Invalid destination coordinates: latitude must be between -90 and 90, longitude between -180 and 180."),
                    validationError = "Invalid destination coordinates.",
                    trips = emptyList()
                )
            }
            return
        }

        if (query.originLatitude == query.destinationLatitude && query.originLongitude == query.destinationLongitude) {
            _uiState.update {
                it.copy(
                    query = query,
                    stage = DiscoveryStage.ValidationError("Origin and destination cannot be the same location."),
                    validationError = "Origin and destination cannot be identical.",
                    trips = emptyList()
                )
            }
            return
        }

        discoveryJob?.cancel()

        val requestId = ++currentRequestId

        _uiState.update {
            it.copy(
                query = query,
                stage = DiscoveryStage.Loading,
                errorMessage = null,
                validationError = null
            )
        }

        discoveryJob = scope.launch(dispatchers.io) {
            val result = discoverTripsUseCase.execute(query)

            // Stale response guard: Ignore result if a newer request has started
            if (requestId != currentRequestId) {
                return@launch
            }

            when (result) {
                is IshaaraResult.Success -> {
                    val discoveryResult = result.data
                    val items = discoveryResult.items
                    _uiState.update {
                        it.copy(
                            trips = items,
                            stage = if (items.isEmpty()) DiscoveryStage.Empty else DiscoveryStage.Success,
                            hasMore = discoveryResult.hasMore,
                            nextCursor = discoveryResult.nextCursor,
                            errorMessage = null,
                            validationError = null
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            stage = DiscoveryStage.Error(result.error),
                            errorMessage = result.error.message,
                            hasMore = false,
                            nextCursor = null
                        )
                    }
                }
            }
        }
    }

    /**
     * Loads the next page of results using cursor-based pagination.
     */
    fun loadNextPage() {
        val state = _uiState.value
        val cursor = state.nextCursor
        if (!state.hasMore || cursor.isNullOrBlank() || state.stage is DiscoveryStage.Loading || state.stage is DiscoveryStage.LoadingMore) {
            return
        }

        val paginatedQuery = state.query.copy(cursor = cursor)
        val requestId = currentRequestId

        _uiState.update { it.copy(stage = DiscoveryStage.LoadingMore) }

        discoveryJob = scope.launch(dispatchers.io) {
            val result = discoverTripsUseCase.execute(paginatedQuery)

            if (requestId != currentRequestId) {
                return@launch
            }

            when (result) {
                is IshaaraResult.Success -> {
                    val discoveryResult = result.data
                    val combinedItems = state.trips + discoveryResult.items
                    _uiState.update {
                        it.copy(
                            trips = combinedItems,
                            stage = DiscoveryStage.Success,
                            hasMore = discoveryResult.hasMore,
                            nextCursor = discoveryResult.nextCursor
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    // On pagination failure, keep existing items and revert stage to Success
                    _uiState.update {
                        it.copy(
                            stage = DiscoveryStage.Success,
                            errorMessage = "Failed to load more trips: ${result.error.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Re-runs the discovery operation using the current query parameters.
     */
    fun retry() {
        executeDiscovery(_uiState.value.query)
    }

    /**
     * Selects a trip to inspect its verified details.
     * Optionally fetches fresh public passenger trip details.
     */
    fun onTripSelected(trip: DiscoveredTrip) {
        _uiState.update {
            it.copy(
                selectedTrip = trip,
                selectedTripDetails = null,
                isLoadingDetails = getPassengerTripDetailsUseCase != null,
                isDetailsSheetVisible = true
            )
        }

        if (getPassengerTripDetailsUseCase != null) {
            detailsJob?.cancel()
            detailsJob = scope.launch(dispatchers.io) {
                val detailsResult = getPassengerTripDetailsUseCase(trip.tripId)
                if (detailsResult is IshaaraResult.Success) {
                    _uiState.update {
                        if (it.selectedTrip?.tripId == trip.tripId) {
                            it.copy(
                                selectedTripDetails = detailsResult.data,
                                isLoadingDetails = false
                            )
                        } else {
                            it
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoadingDetails = false) }
                }
            }
        }
    }

    /**
     * Dismisses the trip details sheet.
     */
    fun onDismissTripDetails() {
        detailsJob?.cancel()
        _uiState.update {
            it.copy(
                isDetailsSheetVisible = false,
                selectedTripDetails = null,
                isLoadingDetails = false
            )
        }
    }

    /**
     * Continues to Phase A10 (Ride Request) with the verified selected trip candidate.
     *
     * BOUNDARY ENFORCEMENT:
     * Phase A09 MUST NOT call POST /api/v1/ride-requests.
     * This handler simply dispatches navigation to the Phase A10 boundary.
     */
    fun onContinueToRideRequest() {
        val trip = _uiState.value.selectedTrip ?: return
        _uiState.update { it.copy(isDetailsSheetVisible = false) }
        navigationManager.navigate(IshaaraDestination.StudentRideTracking.createRoute(trip.tripId))
    }

    /**
     * Navigates back or opens search to change journey parameters.
     */
    fun onChangeSearchClicked() {
        navigationManager.navigateUp()
    }

    /**
     * Clears all state to prevent cross-account leakage during account switching or logout.
     */
    fun clearDiscoveryState() {
        discoveryJob?.cancel()
        detailsJob?.cancel()
        _uiState.update {
            DiscoveryUiState(
                query = DiscoveryQuery(
                    originLatitude = 0.0,
                    originLongitude = 0.0,
                    destinationLatitude = 0.0,
                    destinationLongitude = 0.0
                ),
                stage = DiscoveryStage.Idle,
                trips = emptyList(),
                selectedTrip = null,
                selectedTripDetails = null,
                isDetailsSheetVisible = false,
                hasMore = false,
                nextCursor = null,
                errorMessage = null,
                validationError = null
            )
        }
    }
}

