package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate

/**
 * Trip lifecycle status states.
 * Complete state machine matching backend TripStatus:
 * CREATED / SCHEDULED / ASSIGNED / READY -> ACTIVE -> COMPLETED
 * CREATED / SCHEDULED / ASSIGNED / READY / ACTIVE -> CANCELLED
 */
enum class TripStatus {
    CREATED,
    SCHEDULED,
    ASSIGNED,
    READY,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

/**
 * Actor role performing trip lifecycle operations.
 * Aligned strictly with backend TripActorRole ("DRIVER" | "AGENCY_OWNER" | "ADMIN").
 */
enum class TripActorRole {
    DRIVER,
    AGENCY_OWNER,
    ADMIN
}

/**
 * Normalized trip waypoint location (origin / destination).
 */
data class TripLocation(
    val address: String,
    val coordinate: GeoJsonCoordinate,
    val name: String? = null,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
) {
    val latitude: Double get() = coordinate.latitude
    val longitude: Double get() = coordinate.longitude

    companion object {
        fun of(
            formattedAddress: String,
            latitude: Double,
            longitude: Double,
            name: String? = null,
            googlePlaceId: String? = null,
            serpApiDataId: String? = null
        ): TripLocation = TripLocation(
            address = formattedAddress,
            coordinate = GeoJsonCoordinate(latitude = latitude, longitude = longitude),
            name = name,
            googlePlaceId = googlePlaceId,
            serpApiDataId = serpApiDataId
        )
    }
}

/**
 * Route geometry and navigation metadata.
 */
data class TripRouteGeometry(
    val type: String = "LineString",
    val coordinates: List<Pair<Double, Double>> = emptyList() // [[longitude, latitude], ...]
)

data class TripRoute(
    val geometry: TripRouteGeometry? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null,
    val provider: String? = null
)

/**
 * Authoritative Trip domain model.
 * Exactly reflects backend CleanTripResponse contract with zero client-invented fields.
 */
data class Trip(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val agencyId: String? = null,
    val operatorId: String? = null,
    val origin: TripLocation,
    val destination: TripLocation,
    val route: TripRoute? = null,
    val status: TripStatus,
    val scheduledDepartureAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledByRole: TripActorRole? = null,
    val createdBy: String? = null,
    val createdByRole: TripActorRole? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // Preserved for backward compatibility with legacy student mocks if needed
    val waypoints: List<TripLocation> = emptyList(),
    val totalSeats: Int = 0,
    val availableSeats: Int = 0,
    val baseFarePaise: Int = 0
) {
    val isUnstarted: Boolean
        get() = status == TripStatus.CREATED ||
                status == TripStatus.SCHEDULED ||
                status == TripStatus.ASSIGNED ||
                status == TripStatus.READY

    val isActive: Boolean
        get() = status == TripStatus.ACTIVE

    val isTerminal: Boolean
        get() = status == TripStatus.COMPLETED || status == TripStatus.CANCELLED

    val canBeStarted: Boolean
        get() = isUnstarted

    val canBeCompleted: Boolean
        get() = status == TripStatus.ACTIVE

    val canBeCancelled: Boolean
        get() = !isTerminal
}

/**
 * Domain parameters for driver self-service trip creation (POST /api/v1/trips).
 */
data class CreateTripParams(
    val vehicleId: String,
    val origin: TripLocation,
    val destination: TripLocation,
    val route: TripRoute? = null,
    val scheduledDepartureAt: String? = null
)

/**
 * Filter parameters for querying driver trips (GET /api/v1/drivers/me/trips).
 */
data class DriverTripFilter(
    val status: TripStatus? = null,
    val page: Int = 1,
    val limit: Int = 20
)
