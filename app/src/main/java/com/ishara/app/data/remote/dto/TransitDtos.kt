package com.ishara.app.data.remote.dto

/**
 * Strict DTO for transit trip terminal locations (origin / destination).
 */
data class TransitTripLocationDto(
    val name: String?,
    val formattedAddress: String,
    val coordinates: DoubleArray, // strictly GeoJSON [longitude, latitude]
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransitTripLocationDto
        return name == other.name &&
            formattedAddress == other.formattedAddress &&
            coordinates.contentEquals(other.coordinates) &&
            googlePlaceId == other.googlePlaceId &&
            serpApiDataId == other.serpApiDataId
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + formattedAddress.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        result = 31 * result + (googlePlaceId?.hashCode() ?: 0)
        result = 31 * result + (serpApiDataId?.hashCode() ?: 0)
        return result
    }
}

/**
 * Strict DTO for transit trip route geometry and metrics.
 */
data class TransitTripRouteDto(
    val geometryCoordinates: List<DoubleArray> = emptyList(), // [[lng, lat], ...]
    val distanceMeters: Double? = null,
    val durationSeconds: Long? = null,
    val provider: String? = null
)

/**
 * Strict DTO for public driver details associated with a transit trip.
 */
data class TransitTripDriverDto(
    val id: String,
    val name: String,
    val image: String? = null
)

/**
 * Strict DTO for public vehicle details associated with a transit trip.
 */
data class TransitTripVehicleDto(
    val id: String,
    val registrationNumber: String,
    val vehicleType: String,
    val make: String,
    val model: String
)

/**
 * Strict DTO matching backend GET /api/v1/trips/active PublicTripResponse.
 */
data class TransitTripDto(
    val id: String,
    val status: String,
    val origin: TransitTripLocationDto,
    val destination: TransitTripLocationDto,
    val route: TransitTripRouteDto? = null,
    val startedAt: String? = null,
    val createdAt: String,
    val driver: TransitTripDriverDto,
    val vehicle: TransitTripVehicleDto
)
