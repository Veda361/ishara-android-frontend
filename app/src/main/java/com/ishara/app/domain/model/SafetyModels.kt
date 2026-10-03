package com.ishara.app.domain.model

/**
 * Emergency event types supported in Phase 15.
 */
enum class EmergencyType {
    SOS,
    SAFETY_CONCERN,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): EmergencyType {
            return when (value?.uppercase()) {
                "SOS" -> SOS
                "SAFETY_CONCERN" -> SAFETY_CONCERN
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Emergency event lifecycle statuses.
 * Terminal statuses: RESOLVED, CANCELLED.
 */
enum class EmergencyStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    CANCELLED,
    UNKNOWN;

    val isTerminal: Boolean
        get() = this == RESOLVED || this == CANCELLED

    companion object {
        fun fromString(value: String?): EmergencyStatus {
            return when (value?.uppercase()) {
                "ACTIVE" -> ACTIVE
                "ACKNOWLEDGED" -> ACKNOWLEDGED
                "RESOLVED" -> RESOLVED
                "CANCELLED" -> CANCELLED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Server-derived location snapshot captured at SOS trigger time.
 */
data class SafetyLocationSnapshot(
    val coordinates: List<Double>?,
    val accuracyMeters: Double?,
    val headingDegrees: Double?,
    val speedMps: Double?,
    val isStale: Boolean,
    val capturedAt: String?,
    val provider: String
) {
    val longitude: Double?
        get() = coordinates?.getOrNull(0)

    val latitude: Double?
        get() = coordinates?.getOrNull(1)

    val hasCoordinates: Boolean
        get() = coordinates != null && coordinates.size >= 2
}

/**
 * Authoritative Emergency Event domain model.
 */
data class EmergencyEvent(
    val id: String,
    val eventId: String,
    val rideId: String,
    val tripId: String?,
    val triggeredByUserId: String,
    val triggeredByRole: String,
    val driverId: String,
    val passengerUserId: String,
    val emergencyType: EmergencyType,
    val status: EmergencyStatus,
    val locationSnapshot: SafetyLocationSnapshot,
    val triggeredAt: String,
    val acknowledgedAt: String?,
    val resolvedAt: String?,
    val cancelledAt: String?,
    val cancellationReason: String?,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Emergency contact relationship labels.
 */
enum class EmergencyContactRelationship {
    PARENT,
    SPOUSE,
    SIBLING,
    FRIEND,
    GUARDIAN,
    OTHER,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): EmergencyContactRelationship {
            return when (value?.uppercase()) {
                "PARENT" -> PARENT
                "SPOUSE" -> SPOUSE
                "SIBLING" -> SIBLING
                "FRIEND" -> FRIEND
                "GUARDIAN" -> GUARDIAN
                "OTHER" -> OTHER
                else -> UNKNOWN
            }
        }
    }
}

/**
 * User's registered emergency contact.
 */
data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: EmergencyContactRelationship,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)
