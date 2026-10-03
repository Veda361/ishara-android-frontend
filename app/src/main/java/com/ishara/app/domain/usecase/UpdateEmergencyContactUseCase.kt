package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.repository.SafetyRepository

/**
 * Use case to update an existing emergency contact.
 */
class UpdateEmergencyContactUseCase(
    private val safetyRepository: SafetyRepository
) {
    suspend operator fun invoke(
        contactId: String,
        name: String? = null,
        phoneNumber: String? = null,
        relationship: EmergencyContactRelationship? = null,
        isActive: Boolean? = null
    ): IshaaraResult<EmergencyContact> {
        return safetyRepository.updateEmergencyContact(
            contactId = contactId,
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship,
            isActive = isActive
        )
    }
}
