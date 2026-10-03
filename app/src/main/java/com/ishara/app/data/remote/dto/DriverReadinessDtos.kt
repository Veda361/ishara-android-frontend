package com.ishara.app.data.remote.dto

/**
 * Phase 07: Authoritative Driver Operational Readiness DTO.
 * Directly reflects backend GET /api/v1/drivers/me/readiness response.
 */
data class DriverOperationalReadinessResponseDto(
    val driverId: String,
    val userId: String,
    val authorized: Boolean,
    val status: String, // "READY" | "NOT_READY" | "SUSPENDED"
    val reasons: List<String>,
    val requirements: DriverReadinessRequirementsDto,
    val operatingType: String, // "INDIVIDUAL" | "AGENCY"
    val agency: DriverReadinessAgencyDto?,
    val activeVehicle: DriverReadinessVehicleDto?
)

/**
 * Platform operational requirements checklist.
 */
data class DriverReadinessRequirementsDto(
    val platformVerification: Boolean,
    val agencyMembership: Boolean,
    val profileComplete: Boolean,
    val notSuspended: Boolean,
    val vehicleAssigned: Boolean
)

/**
 * Agency association details returned as part of readiness evaluation.
 */
data class DriverReadinessAgencyDto(
    val membershipStatus: String?,
    val agencyId: String?,
    val agencyName: String?
)

/**
 * Active vehicle assignment details returned as informational telemetry.
 */
data class DriverReadinessVehicleDto(
    val id: String,
    val registrationNumber: String,
    val make: String?,
    val model: String?
)
