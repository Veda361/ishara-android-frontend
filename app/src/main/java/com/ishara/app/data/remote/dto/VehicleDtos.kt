package com.ishara.app.data.remote.dto

/**
 * Clean vehicle response DTO matching backend CleanVehicleResponse.
 */
data class VehicleDto(
    val id: String,
    val agencyId: String? = null,
    val operatorId: String? = null,
    val assignedDriverId: String? = null,
    val registrationNumber: String,
    val vehicleType: String,
    val make: String,
    val model: String,
    val capacity: Int? = null,
    val ownershipType: String = "INDIVIDUAL",
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Clean assignment response DTO matching backend CleanAssignmentResponse.
 */
data class DriverVehicleAssignmentDto(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val agencyId: String? = null,
    val status: String = "ACTIVE",
    val assignedAt: String,
    val unassignedAt: String? = null,
    val assignedBy: String = "ADMIN",
    val assignedByRole: String = "DRIVER",
    val unassignedBy: String? = null,
    val unassignedByRole: String? = null,
    val reason: String? = null,
    val vehicle: VehicleDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Authoritative response DTO for GET /api/v1/vehicles/me/assigned and GET /api/v1/drivers/me/vehicle.
 */
data class AssignedVehicleResponseDto(
    val vehicle: VehicleDto? = null,
    val assignment: DriverVehicleAssignmentDto? = null
)

/**
 * Request payload for POST /api/v1/vehicles (individual vehicle registration).
 */
data class CreateVehicleRequestDto(
    val registrationNumber: String,
    val vehicleType: String,
    val make: String,
    val model: String,
    val capacity: Int? = null
)

/**
 * Request payload for POST /api/v1/vehicles/:vehicleId/assignments.
 */
data class AssignVehicleRequestDto(
    val driverId: String
)

/**
 * Request payload for POST /api/v1/vehicles/:vehicleId/unassign.
 */
data class UnassignVehicleRequestDto(
    val reason: String? = null
)
