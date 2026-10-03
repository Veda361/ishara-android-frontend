package com.ishara.app.domain.model

/**
 * Normalized physical waypoint for ride request pickup or destination.
 * Contains only verified fields accepted by backend `locationInputSchema`.
 */
data class RideRequestLocationWaypoint(
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null
) {
    init {
        require(formattedAddress.isNotBlank()) { "formattedAddress cannot be blank" }
        require(latitude in -90.0..90.0) { "latitude out of bounds [-90, 90]" }
        require(longitude in -180.0..180.0) { "longitude out of bounds [-180, 180]" }
    }
}

/**
 * Strict domain input for submitting a ride request on an active trip.
 * Contains ONLY verified fields accepted by backend `createRideRequestSchema.strict()`.
 */
data class RideRequestInput(
    val tripId: String,
    val pickup: RideRequestLocationWaypoint,
    val destination: RideRequestLocationWaypoint,
    val discoverySessionId: String? = null
) {
    init {
        require(tripId.matches(Regex("^[0-9a-fA-F]{24}$"))) {
            "Invalid tripId format: must be a 24-character hexadecimal ObjectId"
        }
    }
}

/**
 * Domain representation of an authoritative ride request lifecycle entity.
 * Contains ONLY fields verified from the backend `RideRequestResponse`.
 */
data class RideRequestResult(
    val id: String,
    val tripId: String,
    val driverId: String,
    val userId: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val status: RideRequestStatus,
    val requestedAt: String,
    val expiresAt: String,
    val respondedAt: String? = null,
    val discoverySessionId: String? = null,
    val rejectionReason: String? = null,
    val cancellationReason: String? = null,
    val pickupCoordinates: com.ishara.app.core.location.GeoJsonCoordinate? = null,
    val destinationCoordinates: com.ishara.app.core.location.GeoJsonCoordinate? = null,
    val associatedRideId: String? = null
)

/**
 * Extension property verifying if the status is immutable/terminal.
 */
val RideRequestStatus.isTerminal: Boolean
    get() = this in setOf(
        RideRequestStatus.ACCEPTED,
        RideRequestStatus.REJECTED,
        RideRequestStatus.CANCELLED,
        RideRequestStatus.EXPIRED
    )

/**
 * Clean user-facing display label for the request status.
 */
val RideRequestStatus.displayName: String
    get() = when (this) {
        RideRequestStatus.PENDING -> "Pending Confirmation"
        RideRequestStatus.ACCEPTED -> "Accepted"
        RideRequestStatus.REJECTED -> "Declined"
        RideRequestStatus.CANCELLED -> "Cancelled"
        RideRequestStatus.EXPIRED -> "Expired"
    }
