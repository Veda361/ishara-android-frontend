package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.VoiceTripDraft
import com.ishara.app.domain.repository.VoiceTripRepository

/**
 * Use case to validate and create an authoritative Voice Trip Draft on the backend.
 */
class CreateVoiceTripDraftUseCase(
    private val voiceTripRepository: VoiceTripRepository
) {
    suspend operator fun invoke(
        transcript: String,
        languageHint: String? = null
    ): IshaaraResult<VoiceTripDraft> {
        val trimmed = transcript.trim()
        if (trimmed.length < 3) {
            return IshaaraResult.Failure(
                IshaaraError.Validation(message = "Voice command must contain at least 3 characters.")
            )
        }
        if (trimmed.length > 500) {
            return IshaaraResult.Failure(
                IshaaraError.Validation(message = "Voice command exceeds maximum length of 500 characters.")
            )
        }

        return voiceTripRepository.createTripDraft(trimmed, languageHint)
    }
}
