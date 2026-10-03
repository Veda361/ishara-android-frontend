package com.ishara.app.feature.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.usecase.GetActiveRideUseCase
import com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase
import com.ishara.app.domain.usecase.ResolveCurrentLocationUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for Student Home.
 * Owns location state, active ride detection, destination selection, and recent search history.
 * Pure Kotlin logic with zero Android UI framework dependencies for maximum testability.
 */
class StudentHomeViewModel(
    private val userName: String,
    private val resolveCurrentLocationUseCase: ResolveCurrentLocationUseCase,
    private val manageRecentDestinationsUseCase: ManageRecentDestinationsUseCase,
    private val getActiveRideUseCase: GetActiveRideUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        StudentHomeUiState(
            userName = userName,
            greeting = computeTimeBasedGreeting()
        )
    )
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    init {
        observeRecentDestinations()
        refreshLocation()
        checkActiveRide()
    }

    private fun observeRecentDestinations() {
        scope.launch(dispatchers.io) {
            manageRecentDestinationsUseCase.getRecentDestinations().collect { recents ->
                _uiState.update { it.copy(recentDestinations = recents) }
            }
        }
    }

    fun refreshLocation() {
        _uiState.update { it.copy(locationState = LocationUiState.Loading) }
        scope.launch(dispatchers.io) {
            when (val result = resolveCurrentLocationUseCase()) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(locationState = LocationUiState.Available(result.data))
                    }
                }
                is IshaaraResult.Failure -> {
                    val state = when (val error = result.error) {
                        is IshaaraError.PermissionDenied -> LocationUiState.PermissionRequired
                        is IshaaraError.LocationDisabled -> LocationUiState.LocationServicesDisabled
                        is IshaaraError.Network -> LocationUiState.Unavailable(error.message)
                        is IshaaraError.Timeout -> LocationUiState.Unavailable("Location request timed out. Tap to retry.")
                        else -> LocationUiState.Unavailable("Could not detect location. Tap to retry.")
                    }
                    _uiState.update {
                        it.copy(locationState = state)
                    }
                }
            }
        }
    }

    fun onLocationPermissionResult(fineGranted: Boolean, coarseGranted: Boolean) {
        if (fineGranted || coarseGranted) {
            refreshLocation()
        } else {
            _uiState.update { it.copy(locationState = LocationUiState.PermissionDenied) }
        }
    }

    fun checkActiveRide() {
        _uiState.update { it.copy(activeRideState = ActiveRideUiState.Loading) }
        scope.launch(dispatchers.io) {
            when (val result = getActiveRideUseCase()) {
                is IshaaraResult.Success -> {
                    val ride = result.data
                    _uiState.update {
                        it.copy(
                            activeRideState = if (ride != null) {
                                ActiveRideUiState.Active(ride)
                            } else {
                                ActiveRideUiState.None
                            }
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update { it.copy(activeRideState = ActiveRideUiState.None) }
                }
            }
        }
    }

    fun onSearchClicked() {
        navigationManager.navigate(IshaaraDestination.StudentSearch.route)
    }

    fun onVoiceClicked() {
        navigationManager.navigate(IshaaraDestination.StudentVoiceTrip.route)
    }

    fun onBrowseRoutesClicked() {
        navigationManager.navigate(IshaaraDestination.StudentTransitRoutes.route)
    }

    fun onProfileClicked() {
        navigationManager.navigate(IshaaraDestination.StudentProfile.route)
    }

    fun onDestinationSelected(destination: StudentDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
        scope.launch(dispatchers.io) {
            manageRecentDestinationsUseCase.saveDestination(destination)
        }
    }

    fun onClearSelectedDestination() {
        _uiState.update { it.copy(selectedDestination = null) }
    }

    fun onFindTripsClicked() {
        // Enters Phase 06 — Trip Discovery
        navigationManager.navigate(IshaaraDestination.StudentDiscovery.route)
    }

    fun onViewActiveRideClicked(rideId: String) {
        navigationManager.navigate(IshaaraDestination.StudentRideTracking.createRoute(rideId))
    }

    fun onPayRideClicked(rideId: String) {
        navigationManager.navigate(IshaaraDestination.StudentPayment.createRoute(rideId))
    }

    fun onAllowLocationClicked() {
        refreshLocation()
    }

    fun onDismissLocationPermission() {
        _uiState.update { it.copy(locationState = LocationUiState.PermissionDenied) }
    }

    fun onClearRecentDestinations() {
        scope.launch(dispatchers.io) {
            manageRecentDestinationsUseCase.clearRecent()
        }
    }

    private fun computeTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
