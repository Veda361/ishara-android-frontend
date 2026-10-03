package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.VoiceTripRepository

/**
 * Use case to explicitly cancel an active Voice Trip Draft on the backend.
 */
class CancelVoiceTripDraftUseCase(
    private val voiceTripRepository: VoiceTripRepository
) {
    suspend operator fun invoke(draftId: String): IshaaraResult<Unit> {
        if (draftId.isBlank()) return IshaaraResult.Success(Unit)
        return voiceTripRepository.cancelTripDraft(draftId)
    }
}
