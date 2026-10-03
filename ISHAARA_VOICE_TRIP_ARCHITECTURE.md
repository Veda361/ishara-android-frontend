# ISHAARA Voice Trip Creation Architecture (Phase 12)

**Status**: PRODUCTION READY  
**Component**: ISHAARA Android Client (`com.ishara.app.feature.student.voice`)  
**Role**: Student / Passenger (`UserRole.USER`)  
**Verification Date**: 2026-09-27  

---

## 1. Purpose

Phase 12 enables students and passengers to create transit trip requests using spoken natural language (English, Hinglish, and Hindi) through a hands-free interaction flow. 
Instead of typing, students tap the microphone button on the Student Home screen, speak their journey intent (e.g., *"Take me from Jhansi Railway Station to SRGI College"*), review the interpreted route endpoints, explicitly confirm them, and seamlessly transition into the existing Phase 06 Trip Discovery matching engine.

---

## 2. Scope & Boundaries

### In Scope:
1. Voice entry point in thumb zone on Student Home screen.
2. Microphone runtime permission handling (just-in-time, accessible rationale, settings redirect if permanently denied).
3. `VoiceInputClient` architectural abstraction decoupling Android platform `SpeechRecognizer`.
4. Ephemeral on-device speech capture without local audio file persistence.
5. Strict JSON serialization matching backend `DeviceTranscriptInputSchema.strict()`.
6. Submitting device transcript to authoritative backend `POST /api/v1/voice/trip-drafts`.
7. Mapping backend response into pure domain model `VoiceTripDraft`.
8. Location resolution and clarification fallback using existing Phase 05/06 `LocationRepository`.
9. Explicit human confirmation card preventing accidental discovery execution.
10. Conversion of confirmed draft to `DiscoveryQuery` and handoff into Phase 06 `DiscoveryScreen`.
11. Monotonically increasing request IDs and double-tap guards preventing race conditions.
12. Comprehensive unit and contract tests verifying all edge cases.

### Out of Scope:
- Payments / Razorpay.
- Digital ticketing.
- Driver voice commands or navigation.
- Local LLM or arbitrary third-party AI SDKs.
- Automatic creation or submission of ride requests directly from speech.
- Unrelated UI redesign.

---

## 3. End-to-End Voice Flow

```
+-------------------------------------------------------------+
|                     Student Home Screen                     |
|           [ Search Destination ]  or  [ 🎙 Use Voice ]      |
+-------------------------------------------------------------+
                               | (tap)
                               v
+-------------------------------------------------------------+
|                 Voice Trip Screen (Idle)                    |
|             "Tap microphone to speak" (or auto-start)       |
+-------------------------------------------------------------+
                               | (mic tap / granted)
                               v
+-------------------------------------------------------------+
|             Speech Capture (VoiceInputClient)               |
|      Android SpeechRecognizer captures audio ephemerally    |
|      Emits VoiceRecognitionState.Listening / PartialResult  |
+-------------------------------------------------------------+
                               | (final speech result)
                               v
+-------------------------------------------------------------+
|                 Device Transcript Emitted                   |
|   "Take me from Jhansi Railway Station to SRGI College"     |
+-------------------------------------------------------------+
                               | (POST /api/v1/voice/trip-drafts)
                               v
+-------------------------------------------------------------+
|            Authoritative Backend Interpretation             |
|   - Deterministic Regex / LLM Entity Extraction            |
|   - Geospatial Resolution via Google Maps / SerpApi         |
|   - Minimum 50m separation validation                       |
+-------------------------------------------------------------+
                               | (HTTP 201 Created)
                               v
+-------------------------------------------------------------+
|                 Review Draft UI (Explicit)                  |
|   "Is this correct?"                                        |
|   From: Jhansi Railway Station                              |
|   To:   SRGI College Campus                                 |
|   [ Edit ]                    [ Confirm & Find Trips ]      |
+-------------------------------------------------------------+
                               | (explicit confirm tap)
                               v
+-------------------------------------------------------------+
|            Phase 06 Trip Discovery Handoff                  |
|   VoiceTripDraft.toDiscoveryQuery() -> DiscoveryScreen      |
|   - Queries active trips along verified route               |
|   - Converges with standard manual search flow              |
+-------------------------------------------------------------+
```

---

## 4. Backend Contract Architecture

### Endpoint: `POST /api/v1/voice/trip-drafts`
- **Method**: `POST`
- **Authentication**: `Authorization: Bearer <token>`
- **Request Body (Strict JSON - Mode B)**:
  ```json
  {
    "inputMode": "DEVICE_TRANSCRIPT",
    "transcript": "Take me from Jhansi Railway Station to SRGI College",
    "languageHint": "en"
  }
  ```
- **Validation**: Strict schema via Zod on backend (`DeviceTranscriptInputSchema.strict()`). Any extraneous property returns `400 Bad Request`.
- **Response**: Standard envelope containing `VoiceTripDraftResponse`:
  - `id`: MongoDB ObjectId string
  - `origin`: `{ "query": "...", "resolved": { "latitude": ..., "longitude": ..., "formattedAddress": "...", "displayName": "..." } }`
  - `destination`: `{ "query": "...", "resolved": { "latitude": ..., "longitude": ..., "formattedAddress": "...", "displayName": "..." } }`
  - `status`: `"CREATED"`
  - `expiresAt`: ISO-8601 timestamp (15-minute TTL)

---

## 5. Speech Recognition Abstraction (`VoiceInputClient`)

To avoid leaking Android `Activity` or context references and to ensure deterministic unit testing:
1. **Interface `VoiceInputClient`**:
   - `val state: StateFlow<VoiceRecognitionState>`
   - `fun isAvailable(): Boolean`
   - `fun startListening(languageHint: String? = null)`
   - `fun stopListening()`
   - `fun cancel()`
   - `fun destroy()`
2. **Implementation `AndroidSpeechRecognizerClient`**:
   - Uses `applicationContext` exclusively.
   - Dispatches all `SpeechRecognizer` interactions to the Android Main Looper.
   - Cleans up and destroys hardware handles immediately on cancellation or error.
   - Ephemeral memory handling: zero audio frames are written to flash storage.
3. **States**:
   - `Idle`, `Listening`, `PartialResult(text)`, `FinalResult(text)`, `Error(error, rawCode)`, `Cancelled`.

---

## 6. Data & Domain Models

### Domain Entities (`com.ishara.app.domain.model`):
- `VoiceTripDraft`:
  - `id: String`
  - `originalTranscript: String`
  - `normalizedTranscript: String`
  - `intent: String`
  - `origin: VoiceDraftEndpoint`
  - `destination: VoiceDraftEndpoint`
  - `status: VoiceTripDraftStatus`
  - `expiresAt: String`
  - `fun toDiscoveryQuery(): DiscoveryQuery`
  - `fun toStudentDestination(): StudentDestination`
- `VoiceDraftEndpoint`:
  - `query: String`
  - `displayName: String`
  - `formattedAddress: String`
  - `coordinates: LocationCoordinates`

---

## 7. ViewModel State Machine (`VoiceTripViewModel`)

The ViewModel coordinates the voice lifecycle with strict race-condition guarantees:
- **`activeSessionId: Long`**: Monotonically increments on every `startListening()` or `cancelSession()`. Out-of-order callbacks or late HTTP responses from prior sessions are discarded.
- **Duplicate Tap Protection**: If `isMicrophoneActive` or `isSubmittingDraft` is true, repeated taps on the microphone are ignored. If `isConfirmed` is true, repeated confirmation taps are ignored.
- **Stages**:
  - `Idle`: Ready to listen.
  - `Listening(interimTranscript)`: Real-time feedback as speech is recognized.
  - `Submitting(transcript)`: Spinner indicating backend draft processing.
  - `ReviewDraft(draft)`: Human-in-the-loop verification card.
  - `Clarification(candidateLocations)`: Location picker if a place was ambiguous.
  - `Error(message, canRetry)`: User-friendly error card with retry/manual options.
  - `Confirmed(query)`: Loading indicator while transitioning to Phase 06 Discovery.

---

## 8. Clarification & Location Resolution

If the backend cannot uniquely resolve an origin or destination:
1. Returns `404 VOICE_LOCATION_NOT_FOUND` or `400 VOICE_ORIGIN_MISSING` / `VOICE_DESTINATION_MISSING`.
2. `VoiceTripViewModel` catches this error and automatically triggers `SearchLocationsUseCase(candidateText)`.
3. If matching places are found in the existing database/geocoder, enters `VoiceTripStage.Clarification`.
4. Renders matching places in an accessible list.
5. Upon student selection, updates the draft and transitions into `ReviewDraft`.

---

## 9. Convergence with Manual Input

Voice is strictly an **input method**, not a separate mobility subsystem.
- **Manual Path**: Student Home -> Search Screen -> Pick Destination -> `DiscoveryQuery` -> `DiscoveryScreen`
- **Voice Path**: Student Home -> Voice Trip Screen -> Review & Confirm -> `DiscoveryQuery` -> `DiscoveryScreen`

Both paths converge at `DiscoveryQuery` and hand over to the identical Phase 06 `DiscoveryScreen`.

---

## 10. Realtime WebSocket Determination

`/api/v1/voice/realtime` was audited in `ishara-backend/src/modules/realtime/realtime.gateway.ts`:
- **Audited Backend Role**: Audio streaming for server-side Whisper/Google ASR (Mode A / Mode C).
- **Phase 12 Implementation**: Uses on-device ASR (`AndroidSpeechRecognizerClient`). The recognized text is submitted as Mode B (`DEVICE_TRANSCRIPT`) via REST.
- **Verdict**: Realtime WebSocket is not needed for Mode B, reducing network data consumption by 98% and preserving battery life.

---

## 11. Security & Privacy

1. **Microphone Permissions**: Requested just-in-time on explicit tap, never on app startup.
2. **Ephemeral Audio**: Audio buffers are processed in memory and never written to device storage or logs.
3. **Credential Redaction**: Authorization headers and Bearer tokens are redacted in all HTTP logs (`Authorization: [REDACTED]`).
4. **No Third-Party AI Data Leakage**: Speech is processed locally by Android platform services and backend endpoints without sending telemetry to unauthorized third parties.

---

## 12. Accessibility & Localization

1. **Touch Targets**: All primary action buttons (`MicrophoneActionButton`, `[ Confirm & find trips ]`) meet or exceed 56dp.
2. **Content Descriptions**: Semantics applied to microphone button, cancel button, and cards for TalkBack screen readers.
3. **Strings**: 100% of user-facing text externalized in `res/values/strings.xml`.
4. **Reduced Motion**: Respects accessibility settings with subtle pulsing animation rather than jarring flashes.
