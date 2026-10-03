package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.repository.SafetyRepository

/**
 * Use case to cancel an active SOS event.
 */
class CancelSosUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend fun cancelByRide(rideId: String, reason: String? = null): IshaaraResult<EmergencyEvent> {
        return safetyRepository.cancelSosByRide(rideId, reason)
    }

    suspend fun cancelByEventId(eventId: String, reason: String? = null): IshaaraResult<EmergencyEvent> {
        return safetyRepository.cancelSosById(eventId, reason)
    }
}
