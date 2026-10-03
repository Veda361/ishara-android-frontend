package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.DriverOperationalReadinessResponseDto
import com.ishara.app.data.remote.dto.DriverReadinessAgencyDto
import com.ishara.app.data.remote.dto.DriverReadinessRequirementsDto
import com.ishara.app.data.remote.dto.DriverReadinessVehicleDto
import com.ishara.app.domain.model.DriverOperationalReadiness
import com.ishara.app.domain.model.DriverReadinessAgency
import com.ishara.app.domain.model.DriverReadinessBlocker
import com.ishara.app.domain.model.DriverReadinessBlockerAction
import com.ishara.app.domain.model.DriverReadinessReason
import com.ishara.app.domain.model.DriverReadinessRequirements
import com.ishara.app.domain.model.DriverReadinessStatus
import com.ishara.app.domain.model.DriverReadinessVehicle

/**
 * Maps Driver Operational Readiness DTOs to Domain models.
 */
object DriverReadinessMapper {

    fun toDomain(dto: DriverOperationalReadinessResponseDto): DriverOperationalReadiness {
        val status = parseStatus(dto.status)
        val reasons = dto.reasons.map { parseReason(it) }
        val requirements = toDomainRequirements(dto.requirements)
        val agency = dto.agency?.let { toDomainAgency(it) }
        val activeVehicle = dto.activeVehicle?.let { toDomainVehicle(it) }

        return DriverOperationalReadiness(
            driverId = dto.driverId,
            userId = dto.userId,
            authorized = dto.authorized,
            status = status,
            reasons = reasons,
            requirements = requirements,
            operatingType = dto.operatingType,
            agency = agency,
            activeVehicle = activeVehicle
        )
    }

    private fun parseStatus(statusStr: String): DriverReadinessStatus {
        return when (statusStr.trim().uppercase()) {
            "READY" -> DriverReadinessStatus.READY
            "SUSPENDED" -> DriverReadinessStatus.SUSPENDED
            else -> DriverReadinessStatus.NOT_READY
        }
    }

    fun parseReason(reasonStr: String): DriverReadinessReason {
        return when (reasonStr.trim().uppercase()) {
            "PLATFORM_VERIFICATION_PENDING" -> DriverReadinessReason.PLATFORM_VERIFICATION_PENDING
            "PLATFORM_VERIFICATION_REJECTED" -> DriverReadinessReason.PLATFORM_VERIFICATION_REJECTED
            "AGENCY_MEMBERSHIP_REQUIRED" -> DriverReadinessReason.AGENCY_MEMBERSHIP_REQUIRED
            "AGENCY_MEMBERSHIP_PENDING" -> DriverReadinessReason.AGENCY_MEMBERSHIP_PENDING
            "AGENCY_MEMBERSHIP_REJECTED" -> DriverReadinessReason.AGENCY_MEMBERSHIP_REJECTED
            "DRIVER_SUSPENDED" -> DriverReadinessReason.DRIVER_SUSPENDED
            "PROFILE_INCOMPLETE" -> DriverReadinessReason.PROFILE_INCOMPLETE
            "VEHICLE_NOT_ASSIGNED" -> DriverReadinessReason.VEHICLE_NOT_ASSIGNED
            else -> DriverReadinessReason.UNKNOWN
        }
    }

    private fun toDomainRequirements(dto: DriverReadinessRequirementsDto): DriverReadinessRequirements {
        return DriverReadinessRequirements(
            platformVerification = dto.platformVerification,
            agencyMembership = dto.agencyMembership,
            profileComplete = dto.profileComplete,
            notSuspended = dto.notSuspended,
            vehicleAssigned = dto.vehicleAssigned
        )
    }

    private fun toDomainAgency(dto: DriverReadinessAgencyDto): DriverReadinessAgency {
        return DriverReadinessAgency(
            membershipStatus = dto.membershipStatus,
            agencyId = dto.agencyId,
            agencyName = dto.agencyName
        )
    }

    private fun toDomainVehicle(dto: DriverReadinessVehicleDto): DriverReadinessVehicle {
        return DriverReadinessVehicle(
            id = dto.id,
            registrationNumber = dto.registrationNumber,
            make = dto.make,
            model = dto.model
        )
    }

    /**
     * Resolves individual authoritative backend reason codes to user-friendly actionable blockers.
     * Only displays backend-supported blockers.
     */
    fun toBlocker(reason: DriverReadinessReason): DriverReadinessBlocker {
        return when (reason) {
            DriverReadinessReason.PLATFORM_VERIFICATION_PENDING -> DriverReadinessBlocker(
                reason = reason,
                title = "Verification Pending",
                description = "Your driver documentation and license are undergoing administrative review.",
                actionType = DriverReadinessBlockerAction.NAVIGATE_VERIFICATION
            )
            DriverReadinessReason.PLATFORM_VERIFICATION_REJECTED -> DriverReadinessBlocker(
                reason = reason,
                title = "Verification Rejected",
                description = "Your documents were not approved. Please review rejection feedback and resubmit.",
                actionType = DriverReadinessBlockerAction.NAVIGATE_VERIFICATION
            )
            DriverReadinessReason.AGENCY_MEMBERSHIP_REQUIRED -> DriverReadinessBlocker(
                reason = reason,
                title = "Agency Affiliation Required",
                description = "Drivers registered under an agency fleet must join an active transport agency.",
                actionType = DriverReadinessBlockerAction.NAVIGATE_AGENCY
            )
            DriverReadinessReason.AGENCY_MEMBERSHIP_PENDING -> DriverReadinessBlocker(
                reason = reason,
                title = "Agency Approval Pending",
                description = "Your membership application is awaiting agency owner approval.",
                actionType = DriverReadinessBlockerAction.NAVIGATE_AGENCY
            )
            DriverReadinessReason.AGENCY_MEMBERSHIP_REJECTED -> DriverReadinessBlocker(
                reason = reason,
                title = "Agency Membership Rejected",
                description = "Your agency affiliation was declined. You can apply to another agency.",
                actionType = DriverReadinessBlockerAction.NAVIGATE_AGENCY
            )
            DriverReadinessReason.DRIVER_SUSPENDED -> DriverReadinessBlocker(
                reason = reason,
                title = "Account Suspended",
                description = "Operational access has been suspended by platform administration.",
                actionType = DriverReadinessBlockerAction.ADMIN_RESTRICTED
            )
            DriverReadinessReason.PROFILE_INCOMPLETE -> DriverReadinessBlocker(
                reason = reason,
                title = "Profile Incomplete",
                description = "Required driver profile information (such as driver's license number) is missing.",
                actionType = DriverReadinessBlockerAction.COMPLETE_PROFILE
            )
            DriverReadinessReason.VEHICLE_NOT_ASSIGNED -> DriverReadinessBlocker(
                reason = reason,
                title = "No Vehicle Assigned",
                description = "An active vehicle is required before dispatching passenger trips (managed in Phase A07).",
                actionType = DriverReadinessBlockerAction.VEHICLE_DEFERRED
            )
            DriverReadinessReason.UNKNOWN -> DriverReadinessBlocker(
                reason = reason,
                title = "Requirement Unmet",
                description = "A platform requirement must be fulfilled before entering operations.",
                actionType = DriverReadinessBlockerAction.NONE
            )
        }
    }
}
