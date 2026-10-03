package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.VoiceTripDraft

/**
 * Repository interface for creating, reviewing, and cancelling voice trip drafts.
 * Follows clean architecture, returning pure domain [VoiceTripDraft] entities.
 */
interface VoiceTripRepository {
    suspend fun createTripDraft(
        transcript: String,
        languageHint: String? = null
    ): IshaaraResult<VoiceTripDraft>

    suspend fun cancelTripDraft(
        draftId: String
    ): IshaaraResult<Unit>
}
