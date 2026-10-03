package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate

/**
 * Domain query model for discovering active transit trips matching the student's journey.
 *
 * Coordinates are represented as domain [GeoJsonCoordinate] (latitude, longitude),
 * which are translated strictly into the backend's expected coordinate format.
 */
data class DiscoveryQuery(
    val originLatitude: Double,
    val originLongitude: Double,
    val originName: String? = null,
    val originAddress: String? = null,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val destinationName: String? = null,
    val destinationAddress: String? = null,
    val maxPickupDistanceMeters: Double? = DEFAULT_PICKUP_RADIUS_METERS,
    val maxDestinationDeviationMeters: Double? = DEFAULT_DESTINATION_RADIUS_METERS,
    val maxResults: Int? = null,
    val cursor: String? = null
) {
    companion object {
        const val DEFAULT_PICKUP_RADIUS_METERS = 3000.0
        const val DEFAULT_DESTINATION_RADIUS_METERS = 5000.0
    }
    init {
        require(originLatitude in -90.0..90.0) { "Origin latitude out of bounds [-90, 90]" }
        require(originLongitude in -180.0..180.0) { "Origin longitude out of bounds [-180, 180]" }
        require(destinationLatitude in -90.0..90.0) { "Destination latitude out of bounds [-90, 90]" }
        require(destinationLongitude in -180.0..180.0) { "Destination longitude out of bounds [-180, 180]" }
    }

    val originCoordinate: GeoJsonCoordinate
        get() = GeoJsonCoordinate(latitude = originLatitude, longitude = originLongitude)

    val destinationCoordinate: GeoJsonCoordinate
        get() = GeoJsonCoordinate(latitude = destinationLatitude, longitude = destinationLongitude)
}

/**
 * Factual compatibility rating calculated authoritatively by the backend matching engine.
 */
enum class TripCompatibility {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

/**
 * Verified driver public profile returned during discovery.
 */
data class DiscoveredTripDriver(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)

/**
 * Verified vehicle specifications returned during discovery.
 */
data class DiscoveredTripVehicle(
    val id: String,
    val registrationNumber: String,
    val vehicleType: String,
    val make: String? = null,
    val model: String? = null
) {
    val displayTitle: String
        get() {
            val parts = listOfNotNull(make, model, vehicleType).filter { it.isNotBlank() }
            return if (parts.isNotEmpty()) parts.joinToString(" ") else registrationNumber
        }
}

data class DiscoveredTripFare(
    val amountMinor: Long,
    val currency: String,
    val formatted: String
)

/**
 * Discovered transit trip matching the student journey.
 * Contains ONLY verified fields from the authoritative backend DiscoveryItemDto.
 *
 * NOTE: Seat capacity is NOT returned during discovery by the backend.
 * Estimated fare is included only if returned authoritatively by the backend matching engine.
 */
data class DiscoveredTrip(
    val tripId: String,
    val driver: DiscoveredTripDriver,
    val vehicle: DiscoveredTripVehicle,
    val originName: String?,
    val originAddress: String,
    val originCoordinate: GeoJsonCoordinate,
    val destinationName: String?,
    val destinationAddress: String,
    val destinationCoordinate: GeoJsonCoordinate,
    val distanceMeters: Double?,
    val durationSeconds: Double?,
    val pickupDistanceMeters: Double,
    val destinationDistanceMeters: Double,
    val estimatedDetourMeters: Double,
    val compatibility: TripCompatibility,
    val matchScore: Double,
    val estimatedFare: DiscoveredTripFare? = null
) {
    /**
     * Human-readable formatted distance, e.g. "850 m" or "4.2 km".
     */
    val formattedDistance: String?
        get() = distanceMeters?.let { formatMeters(it) }

    /**
     * Human-readable formatted travel duration, e.g. "15 min" or "1 hr 10 min".
     */
    val formattedDuration: String?
        get() = durationSeconds?.let { formatSeconds(it) }

    /**
     * Human-readable pickup walking distance, e.g. "50 m walk" or "1.2 km walk".
     */
    val formattedPickupDistance: String
        get() = "${formatMeters(pickupDistanceMeters)} walk"

    companion object {
        fun formatMeters(meters: Double): String {
            return if (meters < 1000) {
                "${meters.toInt()} m"
            } else {
                val km = meters / 1000.0
                String.format(java.util.Locale.US, "%.1f km", km)
            }
        }

        fun formatSeconds(seconds: Double): String {
            val totalMinutes = (seconds / 60.0).toInt()
            return if (totalMinutes < 60) {
                "$totalMinutes min"
            } else {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins > 0) "${hours} hr ${mins} min" else "${hours} hr"
            }
        }
    }
}

/**
 * Detailed passenger view for a single trip from GET /api/v1/trips/:tripId.
 * Sanitized for passenger privacy: Driver phone, license number, and internal system IDs are excluded.
 */
data class PassengerTripDetails(
    val tripId: String,
    val status: TripStatus,
    val originName: String?,
    val originAddress: String,
    val originCoordinate: GeoJsonCoordinate,
    val destinationName: String?,
    val destinationAddress: String,
    val destinationCoordinate: GeoJsonCoordinate,
    val distanceMeters: Double?,
    val durationSeconds: Double?,
    val routeCoordinates: List<GeoJsonCoordinate>,
    val scheduledDepartureAt: String?,
    val startedAt: String?,
    val createdAt: String,
    val driver: DiscoveredTripDriver,
    val vehicle: DiscoveredTripVehicle
) {
    val formattedDistance: String?
        get() = distanceMeters?.let { DiscoveredTrip.formatMeters(it) }

    val formattedDuration: String?
        get() = durationSeconds?.let { DiscoveredTrip.formatSeconds(it) }
}

/**
 * Domain result representing a completed discovery query.
 */
data class DiscoveryResult(
    val discoverySessionId: String,
    val items: List<DiscoveredTrip>,
    val limit: Int,
    val hasMore: Boolean,
    val nextCursor: String? = null
)

