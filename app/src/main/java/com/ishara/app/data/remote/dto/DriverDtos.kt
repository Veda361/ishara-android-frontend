package com.ishara.app.data.remote.dto

/**
 * Driver identity payload in operational context or profile responses.
 */
data class DriverIdentityDto(
    val id: String,
    val userId: String,
    val verificationStatus: String,
    val status: String,
    val licenseNumberMasked: String? = null,
    val licenseVerifiedAt: String? = null
)

/**
 * Assigned vehicle details from backend vehicle model.
 */
data class DriverVehicleDto(
    val id: String,
    val operatorId: String? = null,
    val registrationNumber: String,
    val vehicleType: String,
    val make: String? = null,
    val model: String? = null,
    val isVerified: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * GeoJSON Point coordinates strictly adhering to RFC 7946 [longitude, latitude].
 */
data class DriverGeoJsonPointDto(
    val type: String = "Point",
    val coordinates: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DriverGeoJsonPointDto
        return type == other.type && coordinates.contentEquals(other.coordinates)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        return result
    }
}

/**
 * Location waypoint for driver trip origin or destination.
 */
data class DriverTripLocationDto(
    val name: String? = null,
    val formattedAddress: String,
    val coordinates: DriverGeoJsonPointDto
)

/**
 * Route summary details.
 */
data class DriverRouteDto(
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null
)

/**
 * Clean trip response returned by backend trip operations.
 */
data class DriverTripDto(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val operatorId: String? = null,
    val origin: DriverTripLocationDto,
    val destination: DriverTripLocationDto,
    val route: DriverRouteDto? = null,
    val status: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Summary stats for the driver's current day in their configured timezone.
 */
data class DriverTodayStatsDto(
    val completedRidesCount: Int = 0,
    val isOnline: Boolean = false,
    val currentDate: String,
    val timezone: String
)

/**
 * Atomic operational context response wrapper.
 */
data class DriverOperationalContextResponseDto(
    val driver: DriverIdentityDto,
    val vehicle: DriverVehicleDto? = null,
    val activeTrip: DriverTripDto? = null,
    val activeRidesCount: Int = 0,
    val todayStats: DriverTodayStatsDto
)

/**
 * Profile response returned by POST /me/status/online and /offline.
 */
data class DriverProfileResponseDto(
    val id: String,
    val userId: String,
    val verificationStatus: String,
    val status: String,
    val licenseNumberMasked: String? = null,
    val licenseVerifiedAt: String? = null
)

/**
 * Emergency contact payload for driver profile creation and updates.
 * Validates against backend emergencyContactInputSchema.
 */
data class EmergencyContactRequestDto(
    val name: String,
    val phoneNumber: String,
    val relationship: String? = null
)

/**
 * Sanitized emergency contact response from backend driver profile.
 */
data class CleanEmergencyContactDto(
    val name: String,
    val phoneNumberMasked: String? = null,
    val relationship: String? = null
)

/**
 * Request payload for POST /api/v1/drivers/me (create driver profile).
 * Validates against backend createDriverProfileSchema.
 */
data class CreateDriverProfileRequestDto(
    val licenseNumber: String,
    val yearsOfExperience: Int? = null,
    val emergencyContact: EmergencyContactRequestDto? = null,
    val operatingType: String? = null
)

/**
 * Request payload for PATCH /api/v1/drivers/me (update driver profile).
 * Validates against backend updateDriverProfileSchema.
 */
data class UpdateDriverProfileRequestDto(
    val licenseNumber: String? = null,
    val yearsOfExperience: Int? = null,
    val emergencyContact: EmergencyContactRequestDto? = null
)

/**
 * Request payload for POST /api/v1/drivers/me/verification (submit or resubmit verification).
 * Validates against backend submitDriverVerificationSchema.
 */
data class SubmitDriverVerificationRequestDto(
    val notes: String? = null
)

/**
 * Sanitized full driver profile response from GET /api/v1/drivers/me.
 * Aligned strictly with backend CleanDriverProfileResponse.
 */
data class CleanDriverProfileResponseDto(
    val id: String,
    val userId: String,
    val verificationStatus: String,
    val status: String,
    val licenseNumberMasked: String? = null,
    val licenseVerifiedAt: String? = null,
    val submittedAt: String? = null,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val rejectionReason: String? = null,
    val yearsOfExperience: Int? = null,
    val emergencyContact: CleanEmergencyContactDto? = null,
    val operatingType: String? = null,
    val isSuspended: Boolean = false,
    val suspensionReason: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Sanitized driver verification status response from GET /api/v1/drivers/me/verification.
 * Aligned strictly with backend CleanDriverVerificationResponse.
 */
data class CleanDriverVerificationResponseDto(
    val driverId: String,
    val userId: String,
    val verificationStatus: String,
    val submittedAt: String? = null,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val rejectionReason: String? = null,
    val licenseNumberMasked: String? = null,
    val operatingType: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

