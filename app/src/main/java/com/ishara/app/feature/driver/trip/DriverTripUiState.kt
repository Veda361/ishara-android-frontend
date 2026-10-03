package com.ishara.app.feature.driver.trip

import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.model.Vehicle

/**
 * Filter tabs for organizing driver trips.
 */
enum class TripFilterTab {
    ALL,
    ACTIVE,
    UPCOMING, // CREATED, SCHEDULED, ASSIGNED, READY
    PAST;     // COMPLETED, CANCELLED

    fun matches(status: TripStatus): Boolean {
        return when (this) {
            ALL -> true
            ACTIVE -> status == TripStatus.ACTIVE
            UPCOMING -> status == TripStatus.CREATED ||
                    status == TripStatus.SCHEDULED ||
                    status == TripStatus.ASSIGNED ||
                    status == TripStatus.READY
            PAST -> status == TripStatus.COMPLETED || status == TripStatus.CANCELLED
        }
    }
}

/**
 * Exhaustive content state for Driver Trip List Screen.
 */
sealed interface TripListContentState {
    object Loading : TripListContentState
    data class Success(
        val trips: List<Trip>,
        val activeTrip: Trip?,
        val filter: TripFilterTab
    ) : TripListContentState
    data class Empty(val filter: TripFilterTab) : TripListContentState
    data class Error(
        val message: String,
        val isSessionExpired: Boolean = false,
        val isForbidden: Boolean = false,
        val canRetry: Boolean = true
    ) : TripListContentState
}

/**
 * Top-level immutable UI state for Driver Trip List Screen.
 */
data class TripListUiState(
    val contentState: TripListContentState = TripListContentState.Loading,
    val selectedTab: TripFilterTab = TripFilterTab.ALL,
    val isRefreshing: Boolean = false,
    val actionInFlightTripId: String? = null,
    val actionErrorMessage: String? = null,
    val isCreateDialogOpen: Boolean = false,
    val isCreatingTrip: Boolean = false,
    val assignedVehicle: Vehicle? = null
)

/**
 * Exhaustive content state for Driver Trip Detail Screen.
 */
sealed interface TripDetailContentState {
    object Loading : TripDetailContentState
    data class Success(
        val trip: Trip,
        val assignedVehicle: Vehicle? = null
    ) : TripDetailContentState
    data class Error(
        val message: String,
        val isNotFound: Boolean = false,
        val isForbidden: Boolean = false,
        val canRetry: Boolean = true
    ) : TripDetailContentState
}

/**
 * Top-level immutable UI state for Driver Trip Detail Screen.
 */
data class TripDetailUiState(
    val contentState: TripDetailContentState = TripDetailContentState.Loading,
    val isActionLoading: Boolean = false,
    val actionErrorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val isCancelDialogOpen: Boolean = false
)
