package com.ishara.app.feature.student.tracking

import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingRideStatus

/**
 * Immutable UI State for the Live Passenger Ride Tracking Screen.
 */
sealed interface RideTrackingUiState {

    /**
     * Initial snapshot loading.
     */
    object Loading : RideTrackingUiState

    /**
     * Active ride tracking with authoritative state and live telemetry.
     */
    data class Content(
        val snapshot: RideTrackingSnapshot,
        val connectionState: RealtimeConnectionState = RealtimeConnectionState.Connecting,
        val isRefreshing: Boolean = false,
        val isDegraded: Boolean = false,
        val statusMessage: String = getStatusDescription(snapshot.status),
        val connectionNotice: String? = null,
        val userLocation: com.ishara.app.core.location.LocationCoordinates? = null
    ) : RideTrackingUiState

    /**
     * Terminal state reached: ride is completed or cancelled.
     */
    data class Terminal(
        val status: TrackingRideStatus,
        val reason: String? = null,
        val finalSnapshot: RideTrackingSnapshot? = null
    ) : RideTrackingUiState

    /**
     * Fatal error during initial load or authorization check.
     */
    data class Error(
        val message: String,
        val isUnauthorized: Boolean = false,
        val canRetry: Boolean = true
    ) : RideTrackingUiState

    companion object {
        fun getStatusDescription(status: TrackingRideStatus): String {
            return when (status) {
                TrackingRideStatus.CREATED -> "Driver assigned"
                TrackingRideStatus.DRIVER_ARRIVING -> "Driver is on the way"
                TrackingRideStatus.PICKED_UP -> "You've been picked up"
                TrackingRideStatus.IN_PROGRESS -> "Trip in progress"
                TrackingRideStatus.COMPLETED -> "Trip completed"
                TrackingRideStatus.CANCELLED -> "Trip cancelled"
                TrackingRideStatus.UNKNOWN -> "Ride in progress"
            }
        }
    }
}
