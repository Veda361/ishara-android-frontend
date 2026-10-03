package com.ishara.app.domain.model

import com.ishara.app.core.location.LocationCoordinates

/**
 * Verified ride lifecycle statuses from the backend contract.
 */
enum class TrackingRideStatus {
    CREATED,
    DRIVER_ARRIVING,
    PICKED_UP,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    UNKNOWN;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    companion object {
        fun fromBackend(value: String?): TrackingRideStatus {
            return when (value?.uppercase()) {
                "CREATED" -> CREATED
                "DRIVER_ARRIVING" -> DRIVER_ARRIVING
                "PICKED_UP" -> PICKED_UP
                "IN_PROGRESS" -> IN_PROGRESS
                "COMPLETED" -> COMPLETED
                "CANCELLED" -> CANCELLED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Authoritative hierarchical tracking state evaluated by the backend.
 * Precedence: UNAVAILABLE -> STALE -> OFF_ROUTE -> FRESH.
 */
enum class TrackingState {
    FRESH,
    STALE,
    OFF_ROUTE,
    UNAVAILABLE,
    UNKNOWN;

    companion object {
        fun fromBackend(value: String?): TrackingState {
            return when (value?.uppercase()) {
                "FRESH" -> FRESH
                "STALE" -> STALE
                "OFF_ROUTE" -> OFF_ROUTE
                "UNAVAILABLE" -> UNAVAILABLE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Driver location GPS freshness status.
 */
enum class TrackingFreshness {
    FRESH,
    STALE,
    UNAVAILABLE,
    UNKNOWN;

    companion object {
        fun fromBackend(value: String?): TrackingFreshness {
            return when (value?.uppercase()) {
                "FRESH" -> FRESH
                "STALE" -> STALE
                "UNAVAILABLE" -> UNAVAILABLE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Sanitized operational driver location exposed in tracking views.
 */
data class TrackingDriverLocation(
    val coordinates: LocationCoordinates,
    val accuracyMeters: Float? = null,
    val headingDegrees: Float? = null,
    val speedMps: Float? = null,
    val recordedAt: String? = null,
    val receivedAt: String? = null,
    val freshness: TrackingFreshness = TrackingFreshness.FRESH
)

/**
 * Route-relative progress along the planned Trip geometry.
 */
data class TrackingRouteProgress(
    val totalDistanceMeters: Int,
    val completedDistanceMeters: Int,
    val remainingDistanceMeters: Int,
    val progressPercent: Float,
    val distanceFromRouteMeters: Float? = null,
    val isOffRoute: Boolean = false
)

/**
 * Authoritative ETA information provided by the backend.
 */
data class TrackingEta(
    val available: Boolean,
    val seconds: Int? = null,
    val source: String? = null,
    val confidence: String? = null
) {
    val formattedEtaMinutes: String?
        get() {
            if (!available || seconds == null) return null
            val minutes = (seconds + 59) / 60
            return if (minutes <= 1) "1 min" else "$minutes mins"
        }
}

/**
 * Normalized public Tracking domain model.
 * Returned by initial REST snapshot and updated continuously via realtime.
 */
data class RideTrackingSnapshot(
    val rideId: String,
    val status: TrackingRideStatus,
    val trackingState: TrackingState,
    val driverLocation: TrackingDriverLocation?,
    val routeProgress: TrackingRouteProgress?,
    val distanceToPickupMeters: Int?,
    val distanceToDestinationMeters: Int?,
    val eta: TrackingEta,
    val updatedAt: String,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null,
    val pickupCoordinates: LocationCoordinates? = null,
    val destinationCoordinates: LocationCoordinates? = null
)
