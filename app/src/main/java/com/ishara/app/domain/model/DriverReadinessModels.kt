package com.ishara.app.domain.model

/**
 * Authoritative driver operational readiness state.
 * Strictly aligned with backend DriverReadinessStatus enum.
 */
enum class DriverReadinessStatus {
    READY,
    NOT_READY,
    SUSPENDED
}

/**
 * Authoritative backend driver readiness reason codes.
 * Strictly aligned with backend DriverReadinessReasonCode enum.
 */
enum class DriverReadinessReason {
    PLATFORM_VERIFICATION_PENDING,
    PLATFORM_VERIFICATION_REJECTED,
    AGENCY_MEMBERSHIP_REQUIRED,
    AGENCY_MEMBERSHIP_PENDING,
    AGENCY_MEMBERSHIP_REJECTED,
    DRIVER_SUSPENDED,
    PROFILE_INCOMPLETE,
    VEHICLE_NOT_ASSIGNED,
    UNKNOWN
}

/**
 * Authoritative platform requirements breakdown.
 */
data class DriverReadinessRequirements(
    val platformVerification: Boolean,
    val agencyMembership: Boolean,
    val profileComplete: Boolean,
    val notSuspended: Boolean,
    val vehicleAssigned: Boolean
)

/**
 * Agency association domain representation within readiness context.
 */
data class DriverReadinessAgency(
    val membershipStatus: String?,
    val agencyId: String?,
    val agencyName: String?
)

/**
 * Active vehicle assignment domain representation within readiness context.
 * Exposed as informational telemetry in Phase 07 / Phase 08.
 */
data class DriverReadinessVehicle(
    val id: String,
    val registrationNumber: String,
    val make: String?,
    val model: String?
)

/**
 * Authoritative domain model for Driver Operational Readiness.
 * Decoupled from transport DTOs and network serialization.
 */
data class DriverOperationalReadiness(
    val driverId: String,
    val userId: String,
    val authorized: Boolean,
    val status: DriverReadinessStatus,
    val reasons: List<DriverReadinessReason>,
    val requirements: DriverReadinessRequirements,
    val operatingType: String,
    val agency: DriverReadinessAgency?,
    val activeVehicle: DriverReadinessVehicle?
) {
    val isReady: Boolean get() = status == DriverReadinessStatus.READY && authorized
    val isSuspended: Boolean get() = status == DriverReadinessStatus.SUSPENDED || !requirements.notSuspended
}

/**
 * Action types for actionable readiness blockers.
 */
enum class DriverReadinessBlockerAction {
    NAVIGATE_VERIFICATION,
    NAVIGATE_AGENCY,
    VEHICLE_DEFERRED,
    ADMIN_RESTRICTED,
    COMPLETE_PROFILE,
    NONE
}

/**
 * UI/Domain representation of a specific requirement blocker.
 */
data class DriverReadinessBlocker(
    val reason: DriverReadinessReason,
    val title: String,
    val description: String,
    val actionType: DriverReadinessBlockerAction
)
