package com.ishara.app.data.remote.dto

/**
 * GeoJSON Point representation matching backend coordinates: [longitude, latitude].
 */
data class GeoJsonPointDto(
    val type: String = "Point",
    val coordinates: DoubleArray = doubleArrayOf(0.0, 0.0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GeoJsonPointDto
        return type == other.type && coordinates.contentEquals(other.coordinates)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        return result
    }
}

/**
 * Waypoint location representation returned by backend CleanTripResponse.
 */
data class TripLocationDto(
    val address: String,
    val coordinates: DoubleArray,
    val name: String? = null,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TripLocationDto
        return address == other.address &&
                coordinates.contentEquals(other.coordinates) &&
                name == other.name &&
                googlePlaceId == other.googlePlaceId &&
                serpApiDataId == other.serpApiDataId
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (googlePlaceId?.hashCode() ?: 0)
        result = 31 * result + (serpApiDataId?.hashCode() ?: 0)
        return result
    }
}

/**
 * Route geometry for navigation mapping.
 */
data class TripRouteGeometryDto(
    val type: String = "LineString",
    val coordinates: List<DoubleArray> = emptyList() // [[longitude, latitude], ...]
)

data class TripRouteDto(
    val geometry: TripRouteGeometryDto? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null,
    val provider: String? = null
)

/**
 * CleanTripResponse DTO strictly mirroring backend CleanTripResponse schema.
 */
data class CleanTripResponseDto(
    val id: String = "",
    val driverId: String = "",
    val vehicleId: String = "",
    val agencyId: String? = null,
    val operatorId: String? = null,
    val origin: TripLocationDto = TripLocationDto(address = "", coordinates = doubleArrayOf(0.0, 0.0)),
    val destination: TripLocationDto = TripLocationDto(address = "", coordinates = doubleArrayOf(0.0, 0.0)),
    val route: TripRouteDto? = null,
    val status: String = "CREATED",
    val scheduledDepartureAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledByRole: String? = null,
    val createdBy: String? = null,
    val createdByRole: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * Location input payload for POST /api/v1/trips.
 */
data class LocationInputDto(
    val name: String? = null,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
)

/**
 * Request payload for POST /api/v1/trips.
 */
data class CreateTripRequestDto(
    val vehicleId: String,
    val origin: LocationInputDto,
    val destination: LocationInputDto,
    val route: TripRouteDto? = null,
    val scheduledDepartureAt: String? = null
)

/**
 * Request payload for POST /api/v1/trips/:tripId/cancel.
 */
data class CancelTripRequestDto(
    val reason: String? = null
)

// =========================================================================
// Legacy DTOs (Preserved for compatibility with existing tests/discovery)
// =========================================================================

data class DiscoverTripsRequestDto(
    val pickup: TripLocationDto,
    val destination: TripLocationDto,
    val passengerCount: Int = 1
)

data class TripDto(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val origin: TripLocationDto,
    val destination: TripLocationDto,
    val waypoints: List<TripLocationDto> = emptyList(),
    val totalSeats: Int = 0,
    val availableSeats: Int = 0,
    val baseFarePaise: Int = 0,
    val status: String,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
