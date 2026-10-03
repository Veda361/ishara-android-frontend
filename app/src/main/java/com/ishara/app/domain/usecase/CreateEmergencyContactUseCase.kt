package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.repository.SafetyRepository

/**
 * Use case to register a new emergency contact.
 */
class CreateEmergencyContactUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend operator fun invoke(
        name: String,
        phoneNumber: String,
        relationship: EmergencyContactRelationship
    ): IshaaraResult<EmergencyContact> {
        return safetyRepository.createEmergencyContact(
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship
        )
    }
}
