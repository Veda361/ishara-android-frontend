package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.repository.SafetyRepository

/**
 * Use case to retrieve the active SOS event for a ride.
 * Used for authoritative state reconciliation across app lifecycle and network resets.
 */
class GetActiveSosUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend operator fun invoke(rideId: String): IshaaraResult<EmergencyEvent?> {
        return safetyRepository.getActiveSosForRide(rideId)
    }
}
