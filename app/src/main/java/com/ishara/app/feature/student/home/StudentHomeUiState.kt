package com.ishara.app.feature.student.home

import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.model.StudentDestination

/**
 * Coherent UI state for Student Home screen.
 * Avoids contradictory flags and follows Unidirectional Data Flow (UDF).
 */
data class StudentHomeUiState(
    val userName: String = "Student",
    val greeting: String = "Good morning",
    val locationState: LocationUiState = LocationUiState.Loading,
    val activeRideState: ActiveRideUiState = ActiveRideUiState.None,
    val selectedDestination: StudentDestination? = null,
    val recentDestinations: List<StudentDestination> = emptyList(),
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LocationUiState {
    object Loading : LocationUiState
    data class Available(val location: CurrentLocationDisplay) : LocationUiState
    object PermissionRequired : LocationUiState
    object PermissionDenied : LocationUiState
    object LocationServicesDisabled : LocationUiState
    data class Unavailable(val message: String) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

sealed interface ActiveRideUiState {
    object None : ActiveRideUiState
    object Loading : ActiveRideUiState
    data class Active(val ride: Ride) : ActiveRideUiState
}
