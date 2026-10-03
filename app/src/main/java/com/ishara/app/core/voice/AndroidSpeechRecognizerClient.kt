package com.ishara.app.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.ishara.app.core.common.IshaaraLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Production implementation of [VoiceInputClient] using platform Android [SpeechRecognizer].
 * 
 * Safety & Production Guarantees:
 * - Uses applicationContext exclusively to prevent Activity leaks.
 * - Schedules all SpeechRecognizer calls strictly on the Main/UI thread looper.
 * - Explicitly cleans up and destroys SpeechRecognizer instances to release audio hardware.
 * - Ephemeral speech capture: zero audio files persisted to disk.
 * - Handles multilingual Indian context hints ("en-IN", "hi-IN").
 */
class AndroidSpeechRecognizerClient(
    private val appContext: Context
) : VoiceInputClient {

    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    override val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isCurrentlyListening = false

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(appContext)
    }

    override fun startListening(languageHint: String?) {
        mainHandler.post {
            try {
                if (!isAvailable()) {
                    IshaaraLogger.w("AndroidSpeechRecognizerClient", "Speech recognition unavailable on device")
                    _state.value = VoiceRecognitionState.Error(VoiceInputError.SERVICE_UNAVAILABLE)
                    return@post
                }

                // Clean up previous recognizer if any
                cleanupRecognizer()

                val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(createRecognitionListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    // Indian transit context language selection
                    val language = languageHint ?: "en-IN"
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
                }

                _state.value = VoiceRecognitionState.Listening
                isCurrentlyListening = true
                recognizer.startListening(intent)
                IshaaraLogger.d("AndroidSpeechRecognizerClient", "SpeechRecognizer started listening (lang: $languageHint)")
            } catch (e: Exception) {
                IshaaraLogger.e("AndroidSpeechRecognizerClient", "Failed to start speech recognition", e)
                _state.value = VoiceRecognitionState.Error(VoiceInputError.CLIENT_ERROR)
                cleanupRecognizer()
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                if (isCurrentlyListening) {
                    speechRecognizer?.stopListening()
                    isCurrentlyListening = false
                    IshaaraLogger.d("AndroidSpeechRecognizerClient", "SpeechRecognizer stopListening requested")
                }
            } catch (e: Exception) {
                IshaaraLogger.w("AndroidSpeechRecognizerClient", "Error stopping speech recognition", e)
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                IshaaraLogger.w("AndroidSpeechRecognizerClient", "Error cancelling speech recognition", e)
            } finally {
                cleanupRecognizer()
                _state.value = VoiceRecognitionState.Cancelled
                IshaaraLogger.d("AndroidSpeechRecognizerClient", "SpeechRecognizer cancelled and cleaned up")
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            cleanupRecognizer()
            _state.value = VoiceRecognitionState.Idle
            IshaaraLogger.d("AndroidSpeechRecognizerClient", "SpeechRecognizer destroyed")
        }
    }

    private fun cleanupRecognizer() {
        isCurrentlyListening = false
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            IshaaraLogger.w("AndroidSpeechRecognizerClient", "Error destroying speech recognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                IshaaraLogger.d("AndroidSpeechRecognizerClient", "Speech begun")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // RMS sound level changes (kept ephemeral, no logging to protect privacy)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer (ephemeral in memory, never persisted)
            }

            override fun onEndOfSpeech() {
                isCurrentlyListening = false
                IshaaraLogger.d("AndroidSpeechRecognizerClient", "Speech ended")
            }

            override fun onError(error: Int) {
                isCurrentlyListening = false
                val mappedError = mapErrorCode(error)
                IshaaraLogger.w("AndroidSpeechRecognizerClient", "Speech recognition error code: $error -> $mappedError")
                _state.value = VoiceRecognitionState.Error(mappedError, error)
                cleanupRecognizer()
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalTranscript = matches?.firstOrNull()?.trim().orEmpty()

                if (finalTranscript.isNotBlank()) {
                    IshaaraLogger.d("AndroidSpeechRecognizerClient", "Recognition final result length: ${finalTranscript.length}")
                    _state.value = VoiceRecognitionState.FinalResult(finalTranscript)
                } else {
                    _state.value = VoiceRecognitionState.Error(VoiceInputError.NO_SPEECH_DETECTED)
                }
                cleanupRecognizer()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialTranscript = matches?.firstOrNull()?.trim().orEmpty()
                if (partialTranscript.isNotBlank()) {
                    _state.value = VoiceRecognitionState.PartialResult(partialTranscript)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for vendor-specific extensions
            }
        }
    }

    private fun mapErrorCode(errorCode: Int): VoiceInputError {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH -> VoiceInputError.NO_SPEECH_DETECTED
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceInputError.SPEECH_TIMEOUT
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceInputError.NETWORK_ERROR
            SpeechRecognizer.ERROR_AUDIO -> VoiceInputError.AUDIO_RECORDING_ERROR
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputError.PERMISSION_DENIED
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> VoiceInputError.SERVICE_UNAVAILABLE
            SpeechRecognizer.ERROR_CLIENT -> VoiceInputError.CLIENT_ERROR
            else -> VoiceInputError.UNKNOWN
        }
    }
}
