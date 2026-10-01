package com.ishara.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DriverProfileDto(
    val id: String,
    val userId: String,
    val status: String,
    val verificationStatus: String,
    val licenseNumber: String,
    val yearsOfExperience: Int,
    val operatingType: String,
    val emergencyContact: EmergencyContactDto? = null
)

@Serializable
data class EmergencyContactDto(
    val name: String,
    val relationship: String,
    val phoneNumber: String
)

@Serializable
data class CreateDriverProfileRequestDto(
    val licenseNumber: String,
    val yearsOfExperience: Int,
    val emergencyContact: EmergencyContactDto
)

@Serializable
data class UpdateDriverProfileRequestDto(
    val yearsOfExperience: Int? = null,
    val emergencyContact: EmergencyContactDto? = null
)

@Serializable
data class DriverLocationDto(
    val location: PointDto,
    val recordedAt: String,
    val isStale: Boolean
)

@Serializable
data class UpdateDriverLocationRequestDto(
    val latitude: Double,
    val longitude: Double,
    val heading: Double? = null,
    val speed: Double? = null
)

@Serializable
data class DriverVerificationRequestDto(
    val licenseNumber: String,
    val documentUrls: List<String>
)

@Serializable
data class DriverVerificationStatusDto(
    val verificationStatus: String,
    val submittedAt: String,
    val rejectionReason: String? = null
)

@Serializable
data class DriverReadinessDto(
    val isReady: Boolean,
    val requirements: DriverReadinessRequirementsDto
)

@Serializable
data class DriverReadinessRequirementsDto(
    val isVerified: Boolean,
    val hasAssignedVehicle: Boolean,
    val hasActiveTrip: Boolean,
    val isSuspended: Boolean
)

@Serializable
data class DriverEarningsDto(
    val summary: EarningsSummaryDto,
    val items: List<EarningItemDto>
)

@Serializable
data class EarningsSummaryDto(
    val period: String,
    val currency: String,
    val grossAmountMinor: Int,
    val platformFeeMinor: Int,
    val netEarningsMinor: Int,
    val settledAmountMinor: Int,
    val pendingSettlementAmountMinor: Int,
    val completedRidesCount: Int
)

@Serializable
data class EarningItemDto(
    val rideId: String,
    val paymentId: String,
    val grossAmountMinor: Int,
    val platformFeeMinor: Int,
    val netEarningsMinor: Int,
    val settlementStatus: String,
    val currency: String,
    val completedAt: String
)
