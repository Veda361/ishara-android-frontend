package com.ishara.app.feature.student.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.voice.VoiceInputClient
import com.ishara.app.core.voice.VoiceInputError
import com.ishara.app.core.voice.VoiceRecognitionState
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.VoiceDraftEndpoint
import com.ishara.app.domain.model.VoiceTripDraft
import com.ishara.app.domain.usecase.CancelVoiceTripDraftUseCase
import com.ishara.app.domain.usecase.CreateVoiceTripDraftUseCase
import com.ishara.app.domain.usecase.SearchLocationsUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing Hands-Free Voice Trip Creation for ISHAARA.
 * 
 * Responsibilities:
 * - Speech recognition lifecycle coordination via [VoiceInputClient]
 * - Monotonically increasing session IDs guarding against out-of-order responses & race conditions
 * - Submitting verified device transcript to backend `POST /api/v1/voice/trip-drafts`
 * - Clarification flow integration with existing [SearchLocationsUseCase]
 * - Direct handoff into Phase 06 Trip Discovery via [DiscoveryQuery]
 * - Pure Kotlin architecture with zero Activity or Android UI references
 */
class VoiceTripViewModel(
    private val voiceInputClient: VoiceInputClient,
    private val createVoiceTripDraftUseCase: CreateVoiceTripDraftUseCase,
    private val cancelVoiceTripDraftUseCase: CancelVoiceTripDraftUseCase,
    private val searchLocationsUseCase: SearchLocationsUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(VoiceTripUiState())
    val uiState: StateFlow<VoiceTripUiState> = _uiState.asStateFlow()

    private var activeSessionId = 0L
    private var recognitionObserverJob: Job? = null
    private var draftSubmissionJob: Job? = null

    init {
        observeSpeechRecognition()
    }

    private fun observeSpeechRecognition() {
        recognitionObserverJob?.cancel()
        recognitionObserverJob = scope.launch(dispatchers.main) {
            voiceInputClient.state.collect { recognitionState ->
                val currentSession = activeSessionId
                when (recognitionState) {
                    is VoiceRecognitionState.Listening -> {
                        _uiState.update {
                            it.copy(
                                stage = VoiceTripStage.Listening(),
                                isMicrophoneActive = true
                            )
                        }
                    }
                    is VoiceRecognitionState.PartialResult -> {
                        _uiState.update {
                            it.copy(
                                stage = VoiceTripStage.Listening(recognitionState.partialText),
                                isMicrophoneActive = true
                            )
                        }
                    }
                    is VoiceRecognitionState.FinalResult -> {
                        _uiState.update { it.copy(isMicrophoneActive = false) }
                        submitTranscript(recognitionState.text, currentSession)
                    }
                    is VoiceRecognitionState.Error -> {
                        _uiState.update { it.copy(isMicrophoneActive = false) }
                        handleRecognitionError(recognitionState.error)
                    }
                    is VoiceRecognitionState.Cancelled -> {
                        _uiState.update {
                            it.copy(
                                stage = VoiceTripStage.Idle,
                                isMicrophoneActive = false
                            )
                        }
                    }
                    is VoiceRecognitionState.Idle -> {
                        _uiState.update { it.copy(isMicrophoneActive = false) }
                    }
                }
            }
        }
    }

    /**
     * Starts voice capture session. Guards against duplicate rapid taps.
     */
    fun startListening(languageHint: String? = null) {
        val currentState = _uiState.value
        if (currentState.isMicrophoneActive || currentState.isSubmittingDraft) {
            return
        }

        activeSessionId++
        draftSubmissionJob?.cancel()

        _uiState.update {
            it.copy(
                stage = VoiceTripStage.Listening(),
                isMicrophoneActive = true,
                isSubmittingDraft = false,
                isConfirmed = false
            )
        }

        voiceInputClient.startListening(languageHint)
    }

    /**
     * Stops listening and triggers transcription.
     */
    fun stopListening() {
        voiceInputClient.stopListening()
    }

    /**
     * Explicit cancellation of current voice session. Releases microphone and cancels jobs.
     */
    fun cancelSession() {
        val currentDraft = (_uiState.value.stage as? VoiceTripStage.ReviewDraft)?.draft
        activeSessionId++
        draftSubmissionJob?.cancel()
        voiceInputClient.cancel()

        if (currentDraft != null) {
            scope.launch(dispatchers.io) {
                cancelVoiceTripDraftUseCase(currentDraft.id)
            }
        }

        _uiState.update {
            it.copy(
                stage = VoiceTripStage.Idle,
                isMicrophoneActive = false,
                isSubmittingDraft = false
            )
        }
    }

    /**
     * Submits captured transcript to backend with session freshness validation.
     */
    fun submitTranscript(transcript: String, sessionId: Long) {
        if (sessionId != activeSessionId) return
        if (transcript.isBlank()) {
            _uiState.update {
                it.copy(
                    stage = VoiceTripStage.Error(
                        message = "No speech detected. Please speak clearly.",
                        canRetry = true,
                        errorCode = "VOICE_EMPTY"
                    ),
                    isSubmittingDraft = false
                )
            }
            return
        }

        draftSubmissionJob?.cancel()
        _uiState.update {
            it.copy(
                stage = VoiceTripStage.Submitting(transcript),
                isSubmittingDraft = true
            )
        }

        draftSubmissionJob = scope.launch(dispatchers.io) {
            val result = createVoiceTripDraftUseCase(transcript)

            // Stale response guard
            if (sessionId != activeSessionId) return@launch

            when (result) {
                is IshaaraResult.Success -> {
                    val draft = result.data
                    _uiState.update {
                        it.copy(
                            stage = VoiceTripStage.ReviewDraft(draft),
                            isSubmittingDraft = false
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    handleBackendError(transcript, result.error.message, sessionId)
                }
            }
        }
    }

    /**
     * Explicit user confirmation of the interpreted voice trip draft.
     * Transitions into Phase 06 Trip Discovery flow.
     */
    fun onConfirmDraft(onConfirmed: (DiscoveryQuery) -> Unit) {
        val currentState = _uiState.value
        if (currentState.isConfirmed) return // Duplicate confirmation tap guard

        val draft = (currentState.stage as? VoiceTripStage.ReviewDraft)?.draft ?: return
        val discoveryQuery = draft.toDiscoveryQuery()

        _uiState.update {
            it.copy(
                stage = VoiceTripStage.Confirmed(discoveryQuery),
                isConfirmed = true
            )
        }

        onConfirmed(discoveryQuery)
    }

    /**
     * Fallback and clarification handler when a location could not be pinpointed.
     */
    fun onSelectClarifiedLocation(selectedLocation: SearchResultLocation) {
        val currentStage = _uiState.value.stage as? VoiceTripStage.Clarification ?: return
        val existingDraft = currentStage.partialDraft

        val newEndpoint = VoiceDraftEndpoint(
            query = selectedLocation.name,
            displayName = selectedLocation.name,
            formattedAddress = selectedLocation.formattedAddress,
            coordinates = selectedLocation.coordinates
        )

        if (existingDraft != null) {
            val updatedDraft = if (currentStage.endpointType == ClarificationEndpointType.ORIGIN) {
                existingDraft.copy(origin = newEndpoint)
            } else {
                existingDraft.copy(destination = newEndpoint)
            }
            _uiState.update {
                it.copy(
                    stage = VoiceTripStage.ReviewDraft(updatedDraft)
                )
            }
        } else {
            // If draft failed completely at resolution, retry draft with clarified location text
            val newQuery = if (currentStage.endpointType == ClarificationEndpointType.DESTINATION) {
                "From ${currentStage.query} to ${selectedLocation.name}"
            } else {
                "From ${selectedLocation.name} to ${currentStage.query}"
            }
            submitTranscript(newQuery, activeSessionId)
        }
    }

    fun onRetry() {
        startListening()
    }

    fun onNavigateToManualSearch() {
        cancelSession()
        navigationManager.navigate(IshaaraDestination.StudentSearch.route)
    }

    fun onNavigateBack() {
        cancelSession()
        navigationManager.navigateUp()
    }

    private fun handleRecognitionError(error: VoiceInputError) {
        val message = when (error) {
            VoiceInputError.NO_SPEECH_DETECTED,
            VoiceInputError.SPEECH_TIMEOUT -> "No speech detected. Tap the microphone to try again."
            VoiceInputError.NETWORK_ERROR -> "Speech recognition network error. Please check your connection."
            VoiceInputError.AUDIO_RECORDING_ERROR -> "Microphone error. Please ensure microphone permissions are granted."
            VoiceInputError.PERMISSION_DENIED -> "Microphone permission required for voice trip creation."
            VoiceInputError.SERVICE_UNAVAILABLE -> "Speech service temporarily unavailable on this device."
            VoiceInputError.CLIENT_ERROR,
            VoiceInputError.UNKNOWN -> "Couldn't capture voice input. Please try again or search manually."
        }
        _uiState.update {
            it.copy(
                stage = VoiceTripStage.Error(
                    message = message,
                    canRetry = error != VoiceInputError.PERMISSION_DENIED,
                    errorCode = error.name
                )
            )
        }
    }

    private suspend fun handleBackendError(transcript: String, rawError: String, sessionId: Long) {
        val lower = rawError.lowercase()

        // Check if error implies unresolved or missing destination/origin
        if (lower.contains("location") || lower.contains("not found") || lower.contains("landmark")) {
            // Attempt clarification search via existing LocationRepository
            val searchQuery = extractLikelyQuery(transcript)
            val searchResult = searchLocationsUseCase(searchQuery)
            if (sessionId == activeSessionId && searchResult is IshaaraResult.Success && searchResult.data.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        stage = VoiceTripStage.Clarification(
                            originalTranscript = transcript,
                            endpointType = ClarificationEndpointType.DESTINATION,
                            query = searchQuery,
                            candidateLocations = searchResult.data
                        ),
                        isSubmittingDraft = false
                    )
                }
                return
            }
        }

        val userMessage = when {
            lower.contains("offline") || lower.contains("connection") || lower.contains("unable to resolve host") ->
                "You're offline. Connect to the internet to use voice trip creation."
            lower.contains("unclear") || lower.contains("intent") ->
                "Could not determine trip origin and destination. Please mention both start and end locations."
            lower.contains("forbidden") || lower.contains("access forbidden") ->
                "Voice trip service requires updated backend permissions. You can also search manually."
            else ->
                rawError.ifBlank { "Could not interpret voice command. Please try again." }
        }

        _uiState.update {
            it.copy(
                stage = VoiceTripStage.Error(
                    message = userMessage,
                    canRetry = true,
                    errorCode = "BACKEND_ERROR"
                ),
                isSubmittingDraft = false
            )
        }
    }

    private fun extractLikelyQuery(transcript: String): String {
        val words = transcript.split(" ")
        val toIndex = words.indexOfLast { it.equals("to", ignoreCase = true) || it.equals("tak", ignoreCase = true) }
        return if (toIndex != -1 && toIndex < words.size - 1) {
            words.subList(toIndex + 1, words.size).joinToString(" ")
        } else {
            transcript
        }
    }

    override fun onCleared() {
        super.onCleared()
        recognitionObserverJob?.cancel()
        draftSubmissionJob?.cancel()
        voiceInputClient.destroy()
    }
}
