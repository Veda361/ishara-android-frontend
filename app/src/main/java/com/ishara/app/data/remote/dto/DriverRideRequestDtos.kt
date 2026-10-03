package com.ishara.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Location structure with RFC 7946 GeoJSON [lng, lat] coordinates.
 */
@Serializable
data class LocationDto(
    val formattedAddress: String? = null,
    val coordinates: List<Double> = emptyList()
)

/**
 * Paginated response wrapper for driver ride requests:
 * GET /api/v1/drivers/me/ride-requests
 */
@Serializable
data class DriverRideRequestListResponse(
    val items: List<RideRequestItemDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val hasMore: Boolean = false
)

/**
 * Ride request item returned by driver ride-requests list and detail endpoints.
 */
@Serializable
data class RideRequestItemDto(
    val id: String,
    val tripId: String,
    val driverId: String,
    val userId: String,
    val pickup: LocationDto,
    val destination: LocationDto,
    val status: String,
    val requestedAt: String,
    val expiresAt: String? = null,
    val respondedAt: String? = null,
    val rejectionReason: String? = null,
    val cancellationReason: String? = null
)

/**
 * Payload for rejecting a ride request:
 * POST /api/v1/ride-requests/{requestId}/reject
 */
@Serializable
data class RejectRideRequestRequestDto(
    val reason: String? = null
)

/**
 * Payload for cancelling an active ride:
 * POST /api/v1/rides/{rideId}/cancel
 */
@Serializable
data class CancelRideRequestDto(
    val reason: String
)

/**
 * Authoritative response for Ride lifecycle operations:
 * GET /api/v1/drivers/me/rides
 * POST /api/v1/rides/{rideId}/arrive
 * POST /api/v1/rides/{rideId}/pickup (Boarding)
 * POST /api/v1/rides/{rideId}/start
 * POST /api/v1/rides/{rideId}/complete
 * POST /api/v1/rides/{rideId}/cancel
 */
@Serializable
data class DriverRideResponseDto(
    val id: String,
    val rideRequestId: String? = null,
    val tripId: String,
    val driverId: String,
    val userId: String,
    val pickup: LocationDto,
    val destination: LocationDto,
    val status: String,
    val pickupTime: String? = null,
    val startTime: String? = null,
    val completionTime: String? = null,
    val cancellationReason: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
