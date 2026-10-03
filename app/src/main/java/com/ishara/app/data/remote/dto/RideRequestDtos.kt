package com.ishara.app.data.remote.dto

/**
 * Strict request DTO matching backend `locationInputSchema`:
 * z.object({
 *   name: z.string().trim().max(100).optional(),
 *   formattedAddress: z.string().trim().min(2).max(300),
 *   latitude: z.number().finite().min(-90).max(90),
 *   longitude: z.number().finite().min(-180).max(180),
 *   googlePlaceId: z.string().trim().max(100).optional(),
 *   serpApiDataId: z.string().trim().max(100).optional()
 * }).strict()
 */
data class LocationWaypointDto(
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
)

/**
 * Strict request payload for POST /api/v1/ride-requests matching `createRideRequestSchema.strict()`:
 * z.object({
 *   tripId: z.string().regex(/^[0-9a-fA-F]{24}$/),
 *   pickup: locationInputSchema,
 *   destination: locationInputSchema
 * }).strict()
 *
 * CRITICAL RULE:
 * Must NEVER include client-controlled fields such as status, driverId, fare, seatCapacity, etc.
 */
data class CreateRideRequestDto(
    val tripId: String,
    val pickup: LocationWaypointDto,
    val destination: LocationWaypointDto,
    val discoverySessionId: String? = null
)

/**
 * Request payload for POST /api/v1/ride-requests/:requestId/cancel matching cancelRideRequestSchema:
 * z.object({
 *   reason: z.string().trim().max(250).optional()
 * }).strict()
 */
data class PassengerCancelRideRequestDto(
    val reason: String? = null
)

/**
 * Waypoint structure returned inside backend `RideRequestResponse`.
 * Coordinates follow RFC 7946 GeoJSON format: [longitude, latitude].
 */
data class LocationWaypointResponseDto(
    val formattedAddress: String,
    val coordinates: DiscoveryGeoJsonPointDto,
    val name: String? = null,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
)

/**
 * Verified response DTO matching backend `RideRequestResponse`.
 */
data class RideRequestResponseDto(
    val id: String,
    val tripId: String,
    val driverId: String,
    val userId: String,
    val pickup: LocationWaypointResponseDto,
    val destination: LocationWaypointResponseDto,
    val status: String,
    val requestedAt: String,
    val respondedAt: String? = null,
    val expiresAt: String,
    val discoverySessionId: String? = null,
    val rejectionReason: String? = null,
    val cancellationReason: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Paginated listing response for GET /api/v1/ride-requests/me.
 */
data class ListRideRequestsResponseDto(
    val items: List<RideRequestResponseDto>,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int = 20,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)
