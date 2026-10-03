package com.ishara.app.domain.model

/**
 * Supported vehicle types for the Ishaara mobility platform.
 * Aligned strictly with backend VehicleType enum.
 */
enum class VehicleType {
    AUTO,
    E_RICKSHAW,
    CAB,
    BUS,
    CAR,
    BIKE,
    OTHER;

    companion object {
        fun fromString(value: String): VehicleType {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: OTHER
        }
    }
}

/**
 * Vehicle ownership domain type.
 * Aligned with backend VehicleOwnershipType ("INDIVIDUAL" | "AGENCY" | "OPERATOR").
 */
enum class VehicleOwnershipType {
    INDIVIDUAL,
    AGENCY,
    OPERATOR;

    companion object {
        fun fromString(value: String): VehicleOwnershipType {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: INDIVIDUAL
        }
    }
}

/**
 * Driver-vehicle assignment status.
 * Aligned with backend AssignmentStatus ("ACTIVE" | "ENDED").
 */
enum class VehicleAssignmentStatus {
    ACTIVE,
    ENDED;

    companion object {
        fun fromString(value: String): VehicleAssignmentStatus {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: ENDED
        }
    }
}

/**
 * Actor role who initiated or terminated vehicle assignment.
 * Aligned with backend AssignmentActorRole ("AGENCY_OWNER" | "ADMIN" | "DRIVER").
 */
enum class VehicleAssignmentActorRole {
    AGENCY_OWNER,
    ADMIN,
    DRIVER;

    companion object {
        fun fromString(value: String?): VehicleAssignmentActorRole? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

/**
 * Core Vehicle domain model representing an operational fleet or individual vehicle asset.
 */
data class Vehicle(
    val id: String,
    val agencyId: String?,
    val operatorId: String?,
    val assignedDriverId: String?,
    val registrationNumber: String,
    val vehicleType: VehicleType,
    val make: String,
    val model: String,
    val capacity: Int?,
    val ownershipType: VehicleOwnershipType,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?
) {
    val displayTitle: String
        get() = "$make $model".trim().ifEmpty { registrationNumber }

    val formattedCapacity: String
        get() = capacity?.let { "$it seats" } ?: "N/A"
}

/**
 * Authoritative domain model representing a driver-vehicle assignment record.
 * Distinguishes vehicle asset from driver operational assignment.
 */
data class DriverVehicleAssignment(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val agencyId: String?,
    val status: VehicleAssignmentStatus,
    val assignedAt: String,
    val unassignedAt: String?,
    val assignedBy: String,
    val assignedByRole: VehicleAssignmentActorRole,
    val unassignedBy: String?,
    val unassignedByRole: VehicleAssignmentActorRole?,
    val reason: String?,
    val vehicle: Vehicle?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Authoritative combined state for a driver's currently assigned vehicle and assignment record.
 */
data class AssignedVehicleState(
    val vehicle: Vehicle?,
    val assignment: DriverVehicleAssignment?
) {
    val hasActiveAssignment: Boolean
        get() = assignment?.status == VehicleAssignmentStatus.ACTIVE && vehicle != null && vehicle.isActive

    companion object {
        val EMPTY = AssignedVehicleState(vehicle = null, assignment = null)
    }
}
