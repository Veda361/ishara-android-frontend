package com.ishara.app.feature.student.voice

import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.VoiceTripDraft

/**
 * Endpoints eligible for user clarification when location lookup is ambiguous.
 */
enum class ClarificationEndpointType {
    ORIGIN,
    DESTINATION
}

/**
 * Immutable discrete lifecycle stages for the Voice Trip Creation state machine.
 */
sealed interface VoiceTripStage {
    object Idle : VoiceTripStage
    data class Listening(val interimTranscript: String = "") : VoiceTripStage
    data class Submitting(val transcript: String) : VoiceTripStage
    data class ReviewDraft(val draft: VoiceTripDraft) : VoiceTripStage
    data class Clarification(
        val originalTranscript: String,
        val endpointType: ClarificationEndpointType,
        val query: String,
        val candidateLocations: List<SearchResultLocation>,
        val partialDraft: VoiceTripDraft? = null
    ) : VoiceTripStage
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val errorCode: String? = null
    ) : VoiceTripStage
    data class Confirmed(val query: DiscoveryQuery) : VoiceTripStage
}

/**
 * Observable UI state model for Voice Trip Creation screen.
 */
data class VoiceTripUiState(
    val stage: VoiceTripStage = VoiceTripStage.Idle,
    val isMicrophoneActive: Boolean = false,
    val isSubmittingDraft: Boolean = false,
    val isConfirmed: Boolean = false
)
