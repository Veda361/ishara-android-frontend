package com.ishara.app.data.remote.dto

/**
 * Request payload for POST /api/v1/rides/:rideId/safety/sos.
 * Note: Zod schema is strictly validated server-side.
 * Never send client location, rideId, or user IDs.
 */
data class CreateSosRequestDto(
    val emergencyType: String = "SOS"
)

/**
 * Request payload for POST /api/v1/rides/:rideId/safety/cancel
 * and POST /api/v1/safety/events/:eventId/cancel.
 */
data class CancelSosRequestDto(
    val reason: String? = null
)

/**
 * Server-derived location snapshot DTO.
 */
data class SafetyLocationSnapshotDto(
    val coordinates: List<Double>? = null,
    val accuracyMeters: Double? = null,
    val headingDegrees: Double? = null,
    val speedMps: Double? = null,
    val isStale: Boolean = false,
    val capturedAt: String? = null,
    val provider: String = "driver_profile"
)

/**
 * Authoritative backend emergency event representation DTO.
 */
data class EmergencyEventResponseDto(
    val id: String,
    val eventId: String,
    val rideId: String,
    val tripId: String? = null,
    val triggeredByUserId: String,
    val triggeredByRole: String,
    val driverId: String,
    val passengerUserId: String,
    val emergencyType: String,
    val status: String,
    val locationSnapshot: SafetyLocationSnapshotDto,
    val triggeredAt: String,
    val acknowledgedAt: String? = null,
    val resolvedAt: String? = null,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Emergency contact representation DTO.
 */
data class EmergencyContactResponseDto(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request payload for POST /api/v1/users/me/emergency-contacts.
 */
data class CreateEmergencyContactRequestDto(
    val name: String,
    val phoneNumber: String,
    val relationship: String
)

/**
 * Request payload for PATCH /api/v1/users/me/emergency-contacts/:contactId.
 */
data class UpdateEmergencyContactRequestDto(
    val name: String? = null,
    val phoneNumber: String? = null,
    val relationship: String? = null,
    val isActive: Boolean? = null
)
