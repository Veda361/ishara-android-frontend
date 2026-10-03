package com.ishara.app.data.remote.dto

/**
 * Strict request coordinate matching backend CoordinateSchema in discovery.schema.ts:
 * z.object({
 *   latitude: z.number().min(-90).max(90),
 *   longitude: z.number().min(-180).max(180),
 *   name: z.string().max(200).optional(),
 *   formattedAddress: z.string().max(500).optional()
 * })
 */
data class DiscoveryCoordinateDto(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val formattedAddress: String? = null
)

/**
 * Optional discovery search configuration options matching backend discovery.schema.ts:
 * z.object({
 *   maxPickupDistanceMeters: z.number().positive().max(5000).optional(),
 *   maxDestinationDeviationMeters: z.number().positive().max(10000).optional(),
 *   maxResults: z.number().int().positive().max(50).optional(),
 *   cursor: z.string().optional()
 * })
 */
data class DiscoveryOptionsDto(
    val maxPickupDistanceMeters: Double? = null,
    val maxDestinationDeviationMeters: Double? = null,
    val maxResults: Int? = null,
    val cursor: String? = null
)

/**
 * Strict request DTO for POST /api/v1/discovery/trips.
 *
 * CRITICAL CONTRACT RULE:
 * The backend schema uses .strict() and will throw HTTP 400 Bad Request
 * if ANY unrecognized field (such as seatCapacity, fare, etc.) is sent.
 */
data class DiscoverySearchRequestDto(
    val origin: DiscoveryCoordinateDto,
    val destination: DiscoveryCoordinateDto,
    val options: DiscoveryOptionsDto? = null
)

/**
 * GeoJSON Point representation used in backend responses.
 * Strictly adheres to RFC 7946: coordinates are [longitude, latitude].
 */
data class DiscoveryGeoJsonPointDto(
    val type: String = "Point",
    val coordinates: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DiscoveryGeoJsonPointDto
        return type == other.type && coordinates.contentEquals(other.coordinates)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        return result
    }
}

data class DiscoveryEndpointDto(
    val name: String? = null,
    val formattedAddress: String,
    val coordinates: DiscoveryGeoJsonPointDto
)

data class DiscoveryDriverDto(
    val id: String,
    val name: String,
    val image: String? = null
)

data class DiscoveryVehicleDto(
    val id: String,
    val registrationNumber: String,
    val vehicleType: String,
    val make: String? = null,
    val model: String? = null
)

data class DiscoveryRouteSummaryDto(
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null
)

data class DiscoveryMatchDto(
    val pickupDistanceMeters: Double = 0.0,
    val destinationDistanceMeters: Double = 0.0,
    val directionDifferenceDegrees: Double = 0.0,
    val pickupRouteProgress: Double = 0.0,
    val destinationRouteProgress: Double = 0.0,
    val estimatedDetourMeters: Double = 0.0,
    val compatibility: String = "UNKNOWN",
    val score: Double = 0.0
)

data class DiscoveryEstimatedFareDto(
    val amountMinor: Long = 0L,
    val currency: String = "INR",
    val formatted: String = ""
)

data class DiscoveryItemDto(
    val tripId: String,
    val driver: DiscoveryDriverDto,
    val vehicle: DiscoveryVehicleDto,
    val origin: DiscoveryEndpointDto,
    val destination: DiscoveryEndpointDto,
    val routeSummary: DiscoveryRouteSummaryDto = DiscoveryRouteSummaryDto(),
    val match: DiscoveryMatchDto = DiscoveryMatchDto(),
    val estimatedFare: DiscoveryEstimatedFareDto? = null
)

data class DiscoveryPaginationDto(
    val limit: Int = 20,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

data class DiscoveryResponseDto(
    val discoverySessionId: String,
    val items: List<DiscoveryItemDto>,
    val pagination: DiscoveryPaginationDto
)

/**
 * Sanitized public trip response DTO matching backend PublicTripResponse schema:
 * GET /api/v1/trips/:tripId
 *
 * Returned for non-driver/passenger requesters. Conceals private driver details
 * (license number, phone, internal user ID) while exposing route, schedule, and vehicle.
 */
data class PublicTripLocationDto(
    val name: String? = null,
    val formattedAddress: String = "",
    val coordinates: DiscoveryGeoJsonPointDto = DiscoveryGeoJsonPointDto(coordinates = doubleArrayOf(0.0, 0.0)),
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
)

data class PublicTripRouteGeometryDto(
    val type: String = "LineString",
    val coordinates: List<DoubleArray> = emptyList() // [[longitude, latitude], ...]
)

data class PublicTripRouteDto(
    val geometry: PublicTripRouteGeometryDto? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null,
    val provider: String? = null
)

data class PublicTripDriverDto(
    val id: String = "",
    val name: String? = null,
    val image: String? = null
)

data class PublicTripVehicleDto(
    val id: String = "",
    val registrationNumber: String = "",
    val vehicleType: String = "",
    val make: String = "",
    val model: String = ""
)

data class PublicTripResponseDto(
    val id: String,
    val status: String = "CREATED",
    val origin: PublicTripLocationDto,
    val destination: PublicTripLocationDto,
    val route: PublicTripRouteDto? = null,
    val startedAt: String? = null,
    val scheduledDepartureAt: String? = null,
    val createdAt: String = "",
    val driver: PublicTripDriverDto,
    val vehicle: PublicTripVehicleDto
)
