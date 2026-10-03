package com.ishara.app.feature.driver.tracking

import com.ishara.app.core.location.LocationCoordinates

/**
 * Observable operational status of the driver GPS telemetry engine.
 */
sealed interface DriverTrackingStatus {
    /** Engine idle, no active trip or tracking stopped */
    object Idle : DriverTrackingStatus

    /** Actively streaming high-frequency GPS fixes to backend */
    data class Active(
        val tripId: String,
        val lastFixTimestampMillis: Long? = null
    ) : DriverTrackingStatus

    /** Location runtime permission missing or revoked */
    object PermissionRequired : DriverTrackingStatus

    /** Device hardware GPS disabled in system settings */
    object GpsDisabled : DriverTrackingStatus

    /** Cellular network unavailable; maintaining single freshest location fix */
    data class NetworkUnavailable(
        val freshestFix: LocationCoordinates? = null
    ) : DriverTrackingStatus

    /** Terminal or non-retryable error (e.g., 403 Forbidden, 401 Unauthorized) */
    data class Error(val message: String) : DriverTrackingStatus
}
