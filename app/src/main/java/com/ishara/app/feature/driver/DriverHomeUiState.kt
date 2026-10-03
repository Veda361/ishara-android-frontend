package com.ishara.app.feature.driver

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverOperationalContext

/**
 * High-level lifecycle stage of the Driver Home experience.
 * Explicit states: Loading, Content, Empty, Offline, Error.
 */
sealed interface DriverHomeStage {
    /** Initial or full-screen loading state while fetching operational context */
    object Loading : DriverHomeStage

    /** Active operational context with assigned vehicle and/or active trip */
    data class Content(val context: DriverOperationalContext) : DriverHomeStage

    /** Driver is online and verified, but currently has no active/created trip or assigned vehicle */
    data class Empty(val context: DriverOperationalContext) : DriverHomeStage

    /** Driver is offline and not accepting trips */
    data class Offline(val context: DriverOperationalContext) : DriverHomeStage

    /** Unrecoverable or operational error loading driver context */
    data class Error(val error: IshaaraError, val canRetry: Boolean = true) : DriverHomeStage
}

/**
 * Verified trip lifecycle action types supported by backend contract.
 */
enum class TripActionType {
    START,
    COMPLETE,
    CANCEL
}

/**
 * Explicit trip lifecycle action state (Start, Complete, Cancel).
 */
sealed interface TripActionState {
    object Idle : TripActionState
    data class Submitting(val tripId: String, val actionType: TripActionType) : TripActionState
    data class Success(val trip: DriverActiveTrip, val message: String) : TripActionState
    data class Error(val error: IshaaraError) : TripActionState
}

/**
 * Driver operational status toggle state (Online / Offline).
 */
sealed interface DriverStatusActionState {
    object Idle : DriverStatusActionState
    object Submitting : DriverStatusActionState
    data class Error(val error: IshaaraError) : DriverStatusActionState
}

/**
 * Complete immutable UI state for Driver Home following Unidirectional Data Flow (UDF).
 */
data class DriverHomeUiState(
    val stage: DriverHomeStage = DriverHomeStage.Loading,
    val accessState: com.ishara.app.domain.model.DriverAccessState = com.ishara.app.domain.model.DriverAccessState(),
    val membershipState: com.ishara.app.domain.model.DriverMembershipState = com.ishara.app.domain.model.DriverMembershipState.Loading,
    val driverProfile: com.ishara.app.domain.model.DriverProfile? = null,
    val tripActionState: TripActionState = TripActionState.Idle,
    val statusActionState: DriverStatusActionState = DriverStatusActionState.Idle,
    val telemetryStatus: com.ishara.app.feature.driver.tracking.DriverTrackingStatus = com.ishara.app.feature.driver.tracking.DriverTrackingStatus.Idle,
    val isActionInProgress: Boolean = false,
    val isRefreshing: Boolean = false,
    val userFacingNotification: String? = null,
    val userFacingError: String? = null,
    val ratingSummary: com.ishara.app.domain.model.DriverRatingSummary? = null
) {
    fun getCurrentVehicle(): com.ishara.app.domain.model.DriverAssignedVehicle? {
        return when (stage) {
            is DriverHomeStage.Content -> stage.context.vehicle
            is DriverHomeStage.Empty -> stage.context.vehicle
            is DriverHomeStage.Offline -> stage.context.vehicle
            else -> null
        }
    }

    fun getCurrentContext(): DriverOperationalContext? {
        return when (stage) {
            is DriverHomeStage.Content -> stage.context
            is DriverHomeStage.Empty -> stage.context
            is DriverHomeStage.Offline -> stage.context
            else -> null
        }
    }
}

