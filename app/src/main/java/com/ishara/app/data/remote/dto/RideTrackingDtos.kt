package com.ishara.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrackingCoordinatesDto(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class TrackingDriverInfoDto(
    val location: TrackingCoordinatesDto? = null,
    val accuracyMeters: Double? = null,
    val headingDegrees: Double? = null,
    val speedMps: Double? = null,
    val recordedAt: String? = null,
    val receivedAt: String? = null,
    val freshness: String? = null
)

@Serializable
data class TrackingRouteInfoDto(
    val distanceMeters: Double = 0.0,
    val completedDistanceMeters: Double = 0.0,
    val remainingDistanceMeters: Double = 0.0,
    val progressPercent: Double = 0.0,
    val distanceFromRouteMeters: Double = 0.0,
    val isOffRoute: Boolean = false
)

@Serializable
data class TrackingRouteProgressSummaryDto(
    val completedDistanceMeters: Double = 0.0,
    val remainingDistanceMeters: Double = 0.0,
    val progressPercent: Double = 0.0
)

@Serializable
data class TrackingEtaInfoDto(
    val available: Boolean = false,
    val seconds: Int? = null,
    val source: String? = null,
    val confidence: String? = null
)

/**
 * Public response from GET /api/v1/rides/:rideId/tracking and WebSocket TRACKING_SNAPSHOT
 */
@Serializable
data class TrackingResponseDto(
    val rideId: String,
    val status: String,
    val trackingState: String,
    val driver: TrackingDriverInfoDto? = null,
    val route: TrackingRouteInfoDto? = null,
    val distanceToPickupMeters: Double? = null,
    val distanceToDestinationMeters: Double? = null,
    val eta: TrackingEtaInfoDto? = null,
    val updatedAt: String
)

/**
 * Response from GET /api/v1/rides/:rideId/driver-location
 */
@Serializable
data class RideDriverLocationResponseDto(
    val rideId: String,
    val driverId: String,
    val location: TrackingCoordinatesDto? = null,
    val accuracyMeters: Double? = null,
    val headingDegrees: Double? = null,
    val speedMps: Double? = null,
    val altitudeMeters: Double? = null,
    val recordedAt: String? = null,
    val receivedAt: String? = null,
    val isStale: Boolean = false,
    val status: String = "UNAVAILABLE"
)

/**
 * Realtime broadcast payload for RIDE_TRACKING_UPDATED
 */
@Serializable
data class RideTrackingUpdatedPayloadDto(
    val rideId: String,
    val driverLocation: TrackingCoordinatesDto? = null,
    val freshness: String? = null,
    val trackingState: String? = null,
    val routeProgress: TrackingRouteProgressSummaryDto? = null,
    val distanceToPickupMeters: Double? = null,
    val distanceToDestinationMeters: Double? = null,
    val eta: TrackingEtaInfoDto? = null,
    val recordedAt: String? = null
)

/**
 * Realtime broadcast payload for RIDE_TRACKING_ENDED
 */
@Serializable
data class RideTrackingEndedPayloadDto(
    val rideId: String,
    val status: String,
    val reason: String? = null,
    val timestamp: String? = null
)
