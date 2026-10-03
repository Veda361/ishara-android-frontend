package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.AssignedVehicleResponseDto
import com.ishara.app.data.remote.dto.DriverVehicleAssignmentDto
import com.ishara.app.data.remote.dto.VehicleDto
import com.ishara.app.domain.model.AssignedVehicleState
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleAssignmentActorRole
import com.ishara.app.domain.model.VehicleAssignmentStatus
import com.ishara.app.domain.model.VehicleOwnershipType
import com.ishara.app.domain.model.VehicleType

/**
 * Maps vehicle DTOs to clean Domain models and vice-versa.
 */
object VehicleMapper {

    fun toDomain(dto: VehicleDto): Vehicle {
        return Vehicle(
            id = dto.id,
            agencyId = dto.agencyId,
            operatorId = dto.operatorId,
            assignedDriverId = dto.assignedDriverId,
            registrationNumber = dto.registrationNumber,
            vehicleType = VehicleType.fromString(dto.vehicleType),
            make = dto.make,
            model = dto.model,
            capacity = dto.capacity,
            ownershipType = VehicleOwnershipType.fromString(dto.ownershipType),
            isVerified = dto.isVerified,
            isActive = dto.isActive,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toDomain(dto: DriverVehicleAssignmentDto): DriverVehicleAssignment {
        return DriverVehicleAssignment(
            id = dto.id,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            agencyId = dto.agencyId,
            status = VehicleAssignmentStatus.fromString(dto.status),
            assignedAt = dto.assignedAt,
            unassignedAt = dto.unassignedAt,
            assignedBy = dto.assignedBy,
            assignedByRole = VehicleAssignmentActorRole.fromString(dto.assignedByRole) ?: VehicleAssignmentActorRole.DRIVER,
            unassignedBy = dto.unassignedBy,
            unassignedByRole = VehicleAssignmentActorRole.fromString(dto.unassignedByRole),
            reason = dto.reason,
            vehicle = dto.vehicle?.let { toDomain(it) },
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toDomain(dto: AssignedVehicleResponseDto): AssignedVehicleState {
        val vehicleDomain = dto.vehicle?.let { toDomain(it) }
        val assignmentDomain = dto.assignment?.let { assignmentDto ->
            // If assignmentDto has no nested vehicle, use the root vehicle
            if (assignmentDto.vehicle == null && dto.vehicle != null) {
                toDomain(assignmentDto.copy(vehicle = dto.vehicle))
            } else {
                toDomain(assignmentDto)
            }
        }

        return AssignedVehicleState(
            vehicle = vehicleDomain,
            assignment = assignmentDomain
        )
    }
}
