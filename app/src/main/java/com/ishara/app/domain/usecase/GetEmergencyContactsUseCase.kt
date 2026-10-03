package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.repository.SafetyRepository

/**
 * Use case to retrieve all registered emergency contacts for the user.
 */
class GetEmergencyContactsUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend operator fun invoke(): IshaaraResult<List<EmergencyContact>> {
        return safetyRepository.listEmergencyContacts()
    }
}
