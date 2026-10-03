package com.ishara.app.feature.driver.requests

import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest

/**
 * High-level screen stage for driver ride requests view.
 */
sealed interface DriverRideRequestsStage {
    object Loading : DriverRideRequestsStage
    object Empty : DriverRideRequestsStage
    object Content : DriverRideRequestsStage
    data class Error(val error: IshaaraError, val canRetry: Boolean = true) : DriverRideRequestsStage
}

/**
 * Filter tab selection for the driver requests screen.
 */
enum class DriverRequestsTab {
    INCOMING, // Pending requests awaiting driver response
    ACTIVE_RIDES // Boarding & in-progress passenger rides
}

/**
 * Ongoing mutation action state to prevent duplicate submissions and provide deterministic UI feedback.
 */
sealed interface DriverActionState {
    object Idle : DriverActionState
    data class Accepting(val requestId: String) : DriverActionState
    data class Rejecting(val requestId: String) : DriverActionState
    data class UpdatingRide(val rideId: String, val actionName: String) : DriverActionState
}

/**
 * Comprehensive, production-grade UI State for Driver Ride Requests and Passenger Boarding.
 * Prevents impossible boolean combinations via explicit stages and states.
 */
data class DriverRideRequestsUiState(
    val stage: DriverRideRequestsStage = DriverRideRequestsStage.Loading,
    val selectedTab: DriverRequestsTab = DriverRequestsTab.INCOMING,
    val pendingRequests: List<DriverRideRequest> = emptyList(),
    val activeRides: List<DriverPassengerRide> = emptyList(),
    val selectedRequest: DriverRideRequest? = null,
    val selectedRide: DriverPassengerRide? = null,
    val actionState: DriverActionState = DriverActionState.Idle,
    val isRefreshing: Boolean = false,
    val realtimeState: RealtimeConnectionState = RealtimeConnectionState.Disconnected,
    val userNotification: String? = null,
    val totalRequestsCount: Int = 0,
    val hasMore: Boolean = false
) {
    val isActionInProgress: Boolean
        get() = actionState !is DriverActionState.Idle
}
