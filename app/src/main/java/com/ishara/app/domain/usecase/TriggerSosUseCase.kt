package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyType
import com.ishara.app.domain.repository.SafetyRepository
import java.util.UUID

/**
 * Use case to trigger an SOS emergency alert.
 * Generates a unique UUID idempotency key to prevent duplicate activations on network retries.
 */
class TriggerSosUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend operator fun invoke(
        rideId: String,
        emergencyType: EmergencyType = EmergencyType.SOS,
        idempotencyKey: String? = null
    ): IshaaraResult<EmergencyEvent> {
        val effectiveKey = idempotencyKey ?: UUID.randomUUID().toString()
        return safetyRepository.triggerSos(
            rideId = rideId,
            emergencyType = emergencyType,
            idempotencyKey = effectiveKey
        )
    }
}
