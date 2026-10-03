package com.ishara.app.core.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * Standard error taxonomy for voice input and speech recognition.
 */
enum class VoiceInputError {
    NO_SPEECH_DETECTED,
    SPEECH_TIMEOUT,
    NETWORK_ERROR,
    AUDIO_RECORDING_ERROR,
    PERMISSION_DENIED,
    SERVICE_UNAVAILABLE,
    CLIENT_ERROR,
    UNKNOWN
}

/**
 * Coherent immutable lifecycle states of a Voice Input session.
 */
sealed interface VoiceRecognitionState {
    object Idle : VoiceRecognitionState
    object Listening : VoiceRecognitionState
    data class PartialResult(val partialText: String) : VoiceRecognitionState
    data class FinalResult(val text: String) : VoiceRecognitionState
    data class Error(val error: VoiceInputError, val rawCode: Int? = null) : VoiceRecognitionState
    object Cancelled : VoiceRecognitionState
}

/**
 * Architectural abstraction over platform and provider speech recognition engines.
 * Decouples ViewModels and UI from direct Android SpeechRecognizer dependencies,
 * preventing Activity leaks and enabling 100% deterministic unit testing.
 */
interface VoiceInputClient {
    /**
     * Observable state flow representing current speech recognition session state.
     */
    val state: StateFlow<VoiceRecognitionState>

    /**
     * Checks whether speech recognition services are currently available on this device.
     */
    fun isAvailable(): Boolean

    /**
     * Starts listening for spoken input with optional language hint (e.g. "en-IN", "hi-IN").
     */
    fun startListening(languageHint: String? = null)

    /**
     * Stops listening and requests final transcription of captured audio.
     */
    fun stopListening()

    /**
     * Immediately cancels the active session and releases microphone resources.
     */
    fun cancel()

    /**
     * Cleans up all underlying resources, listeners, and handlers.
     */
    fun destroy()
}
