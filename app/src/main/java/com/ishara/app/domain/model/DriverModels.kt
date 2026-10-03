package com.ishara.app.domain.model

/**
 * Authoritative driver operational availability states.
 * Aligned strictly with backend DriverStatus enum.
 */
enum class DriverProfileStatus {
    OFFLINE,
    ONLINE,
    ON_RIDE
}

/**
 * Driver verification states managed by platform administration.
 * Aligned strictly with backend VerificationStatus enum.
 */
enum class DriverVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}

/**
 * Trip lifecycle status states.
 * Aligned strictly with backend TripStatus enum.
 */
enum class DriverTripStatus {
    CREATED,
    SCHEDULED,
    ASSIGNED,
    READY,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

/**
 * Driver identity and verification details.
 */
data class DriverIdentity(
    val id: String,
    val userId: String,
    val verificationStatus: DriverVerificationStatus,
    val status: DriverProfileStatus,
    val licenseNumberMasked: String?,
    val licenseVerifiedAt: String?
)

/**
 * Vehicle assigned to driver operations.
 */
data class DriverAssignedVehicle(
    val id: String,
    val registrationNumber: String,
    val vehicleType: String,
    val make: String?,
    val model: String?,
    val isActive: Boolean,
    val isVerified: Boolean
)

/**
 * Geographic waypoint for driver trip origin / destination.
 */
data class DriverTripLocation(
    val name: String?,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Current operational trip assigned to or being executed by the driver.
 */
data class DriverActiveTrip(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val origin: DriverTripLocation,
    val destination: DriverTripLocation,
    val distanceMeters: Double?,
    val durationSeconds: Double?,
    val status: DriverTripStatus,
    val startedAt: String?,
    val completedAt: String?,
    val cancelledAt: String?
)

/**
 * Aggregated operational statistics for the current day in driver's timezone.
 */
data class DriverTodayStats(
    val completedRidesCount: Int,
    val isOnline: Boolean,
    val currentDate: String,
    val timezone: String
)

/**
 * Atomic operational snapshot for the authenticated driver.
 * Directly maps from backend GET /api/v1/drivers/me/operations/context.
 */
data class DriverOperationalContext(
    val driver: DriverIdentity,
    val vehicle: DriverAssignedVehicle?,
    val activeTrip: DriverActiveTrip?,
    val activeRidesCount: Int,
    val todayStats: DriverTodayStats
)

