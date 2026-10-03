package com.ishara.app.domain.model


/**
 * Verified statuses for a Ride Request per backend contract.
 */
enum class DriverRideRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    UNKNOWN;

    companion object {
        fun fromBackend(value: String?): DriverRideRequestStatus {
            return when (value?.uppercase()) {
                "PENDING" -> PENDING
                "ACCEPTED" -> ACCEPTED
                "REJECTED" -> REJECTED
                "CANCELLED" -> CANCELLED
                "EXPIRED" -> EXPIRED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Domain model representing a ride request from the driver/conductor's perspective.
 */
data class DriverRideRequest(
    val id: String,
    val tripId: String,
    val driverId: String,
    val passengerId: String,
    val pickupAddress: String,
    val pickupCoordinates: Pair<Double, Double>, // (latitude, longitude)
    val destinationAddress: String,
    val destinationCoordinates: Pair<Double, Double>, // (latitude, longitude)
    val status: DriverRideRequestStatus,
    val requestedAt: String?,
    val expiresAt: String?,
    val respondedAt: String?,
    val rejectionReason: String?,
    val cancellationReason: String?
)

/**
 * Paginated container for driver ride requests.
 */
data class DriverRideRequestPage(
    val items: List<DriverRideRequest>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Verified operational statuses for a passenger Ride (Boarding & Transit lifecycle).
 */
enum class DriverRideStatus {
    CREATED,
    DRIVER_ARRIVING,
    PICKED_UP, // Boarded
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun fromBackend(value: String?): DriverRideStatus {
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
 * Domain model representing an authoritative passenger Ride in the operational workflow.
 */
data class DriverPassengerRide(
    val id: String,
    val rideRequestId: String?,
    val tripId: String,
    val driverId: String,
    val passengerId: String,
    val pickupAddress: String,
    val pickupCoordinates: Pair<Double, Double>,
    val destinationAddress: String,
    val destinationCoordinates: Pair<Double, Double>,
    val status: DriverRideStatus,
    val pickupTime: String?,
    val startTime: String?,
    val completionTime: String?,
    val cancellationReason: String?
)
