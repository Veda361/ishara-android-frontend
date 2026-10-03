package com.ishara.app.data.repository

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.VoiceTripDraftMapper
import com.ishara.app.data.remote.datasource.VoiceTripDraftRemoteDataSource
import com.ishara.app.data.remote.dto.CreateVoiceTripDraftRequestDto
import com.ishara.app.domain.model.VoiceTripDraft
import com.ishara.app.domain.repository.VoiceTripRepository
import kotlinx.coroutines.withContext

/**
 * Production implementation of [VoiceTripRepository].
 * Manages token resolution, payload construction, and DTO to domain mapping.
 */
class VoiceTripRepositoryImpl(
    private val remoteDataSource: VoiceTripDraftRemoteDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : VoiceTripRepository {

    override suspend fun createTripDraft(
        transcript: String,
        languageHint: String?
    ): IshaaraResult<VoiceTripDraft> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val request = CreateVoiceTripDraftRequestDto(
            inputMode = "DEVICE_TRANSCRIPT",
            transcript = transcript.trim(),
            languageHint = languageHint
        )

        remoteDataSource.createVoiceTripDraft(request, token).map { dto ->
            VoiceTripDraftMapper.toDomain(dto)
        }
    }

    override suspend fun cancelTripDraft(
        draftId: String
    ): IshaaraResult<Unit> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.cancelVoiceTripDraft(draftId, token)
    }
}
