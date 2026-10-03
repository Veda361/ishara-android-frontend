package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.SafetyRepository

/**
 * Use case to remove an emergency contact.
 */
class DeleteEmergencyContactUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend operator fun invoke(contactId: String): IshaaraResult<Unit> {
        return safetyRepository.deleteEmergencyContact(contactId)
    }
}
