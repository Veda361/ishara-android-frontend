package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.EmergencyContactResponseDto
import com.ishara.app.data.remote.dto.EmergencyEventResponseDto
import com.ishara.app.data.remote.dto.SafetyLocationSnapshotDto
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyStatus
import com.ishara.app.domain.model.EmergencyType
import com.ishara.app.domain.model.SafetyLocationSnapshot

/**
 * Maps Safety and Emergency DTOs to Domain models.
 * Gracefully tolerates unknown future backend enum statuses without crashing.
 */
object SafetyMapper {

    fun toDomain(dto: EmergencyEventResponseDto): EmergencyEvent {
        return EmergencyEvent(
            id = dto.id,
            eventId = dto.eventId,
            rideId = dto.rideId,
            tripId = dto.tripId,
            triggeredByUserId = dto.triggeredByUserId,
            triggeredByRole = dto.triggeredByRole,
            driverId = dto.driverId,
            passengerUserId = dto.passengerUserId,
            emergencyType = EmergencyType.fromString(dto.emergencyType),
            status = EmergencyStatus.fromString(dto.status),
            locationSnapshot = toDomain(dto.locationSnapshot),
            triggeredAt = dto.triggeredAt,
            acknowledgedAt = dto.acknowledgedAt,
            resolvedAt = dto.resolvedAt,
            cancelledAt = dto.cancelledAt,
            cancellationReason = dto.cancellationReason,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toDomain(dto: SafetyLocationSnapshotDto): SafetyLocationSnapshot {
        return SafetyLocationSnapshot(
            coordinates = dto.coordinates,
            accuracyMeters = dto.accuracyMeters,
            headingDegrees = dto.headingDegrees,
            speedMps = dto.speedMps,
            isStale = dto.isStale,
            capturedAt = dto.capturedAt,
            provider = dto.provider
        )
    }

    fun toDomain(dto: EmergencyContactResponseDto): EmergencyContact {
        return EmergencyContact(
            id = dto.id,
            name = dto.name,
            phoneNumber = dto.phoneNumber,
            relationship = EmergencyContactRelationship.fromString(dto.relationship),
            isVerified = dto.isVerified,
            isActive = dto.isActive,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
}
