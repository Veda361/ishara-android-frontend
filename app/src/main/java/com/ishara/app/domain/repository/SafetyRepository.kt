package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyType

/**
 * Domain repository contract for Safety, SOS, and Emergency Contact operations.
 * Strictly adheres to verified backend endpoints.
 */
interface SafetyRepository {

    /**
     * Triggers an SOS emergency alert for an active ride.
     * Supports optional idempotency key for network retry deduplication.
     */
    suspend fun triggerSos(
        rideId: String,
        emergencyType: EmergencyType = EmergencyType.SOS,
        idempotencyKey: String? = null
    ): IshaaraResult<EmergencyEvent>

    /**
     * Retrieves the currently active SOS event for a ride, or null if none exists.
     */
    suspend fun getActiveSosForRide(rideId: String): IshaaraResult<EmergencyEvent?>

    /**
     * Lists paginated safety event history for a ride.
     */
    suspend fun listEventsForRide(
        rideId: String,
        limit: Int = 20,
        skip: Int = 0
    ): IshaaraResult<List<EmergencyEvent>>

    /**
     * Cancels the active SOS event for the specified ride.
     */
    suspend fun cancelSosByRide(
        rideId: String,
        reason: String? = null
    ): IshaaraResult<EmergencyEvent>

    /**
     * Retrieves a specific emergency event by its human-readable event ID.
     */
    suspend fun getSosById(eventId: String): IshaaraResult<EmergencyEvent>

    /**
     * Cancels an active SOS event by its event ID.
     */
    suspend fun cancelSosById(
        eventId: String,
        reason: String? = null
    ): IshaaraResult<EmergencyEvent>

    /**
     * Lists all registered emergency contacts for the authenticated user (max 5).
     */
    suspend fun listEmergencyContacts(): IshaaraResult<List<EmergencyContact>>

    /**
     * Registers a new emergency contact for the authenticated user.
     */
    suspend fun createEmergencyContact(
        name: String,
        phoneNumber: String,
        relationship: EmergencyContactRelationship
    ): IshaaraResult<EmergencyContact>

    /**
     * Updates an existing emergency contact.
     */
    suspend fun updateEmergencyContact(
        contactId: String,
        name: String? = null,
        phoneNumber: String? = null,
        relationship: EmergencyContactRelationship? = null,
        isActive: Boolean? = null
    ): IshaaraResult<EmergencyContact>

    /**
     * Soft-deletes an emergency contact.
     */
    suspend fun deleteEmergencyContact(contactId: String): IshaaraResult<Unit>
}
