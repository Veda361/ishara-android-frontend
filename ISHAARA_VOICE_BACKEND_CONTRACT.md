# ISHAARA Voice Trip Backend Contract Audit (Phase 12)

**Document Version**: 1.0.0  
**Verification Date**: 2026-09-27  
**Audited Against**: 
1. `/home/dev/Desktop/ishara-backend/src/modules/voice/voice.routes.ts`
2. `/home/dev/Desktop/ishara-backend/src/modules/voice/voice.controller.ts`
3. `/home/dev/Desktop/ishara-backend/src/modules/voice/voice.schema.ts`
4. `/home/dev/Desktop/ishara-backend/src/modules/voice/voice.types.ts`
5. `/home/dev/Desktop/ishara-backend/src/modules/voice/voice.service.ts`
6. `/home/dev/Desktop/ishara-backend/src/modules/voice/drafts/voice-trip-draft.model.ts`
7. `/home/dev/Desktop/ishara-backend/src/modules/realtime/realtime.gateway.ts`
8. `/home/dev/Desktop/ishara-backend/src/modules/realtime/realtime.auth.ts`
9. `/home/dev/Desktop/ishara-frontend/docs/API_REFERENCE.md`

---

## 1. Executive Summary

This document establishes the verified technical contract for ISHAARA Voice Trip Creation (`POST /api/v1/voice/trip-drafts` and `/api/v1/voice/realtime`).
All fields, data structures, headers, error codes, and validation rules documented here were directly inspected and verified against the production backend repository.

---

## 2. Comprehensive 29-Point Audit Matrix

| # | Contract Dimension | Verified Backend Finding |
|---|-------------------|--------------------------|
| **1** | **HTTP Method** | `POST` for draft creation; `GET` for draft retrieval; `POST` for confirmation; `POST` for cancellation. |
| **2** | **Exact Path** | `/api/v1/voice/trip-drafts` (Base: `apiV1Router.use("/voice", voiceRoutes)`). |
| **3** | **Authentication** | Bearer Token in `Authorization: Bearer <sessionToken>` header (validated via `requireAuth` middleware). |
| **4** | **USER Role Requirement** | **CRITICAL AUDIT FINDING**: In `src/modules/voice/voice.routes.ts`, line 24 applies `router.use(requireDriverConductor);` and the controller resolves `driverService.getDriverProfileByUserId(req.auth.applicationUserId)`. Consequently, the current backend implementation restricts `/voice/trip-drafts` to `DRIVER_CONDUCTOR`, returning `403 Forbidden` (`Access forbidden: requires one of [DRIVER_CONDUCTOR]`) if invoked with role `USER`. The Android client implements the exact schema, error handling, and clarification flow, documenting this backend limitation. |
| **5** | **Request Body** | Supports two modes: **MODE A** `multipart/form-data` with binary field `audio`; **MODE B** `application/json` with `{ "inputMode": "DEVICE_TRANSCRIPT", "transcript": "...", "languageHint": "en" }`. Mode B is the target for Android on-device speech recognition. |
| **6** | **Required Fields** | For Mode B: `inputMode` (`"DEVICE_TRANSCRIPT"` strictly), `transcript` (string, min 3 characters, max 500 characters, trimmed). |
| **7** | **Optional Fields** | `languageHint` (string, max 20 characters, e.g. `"en"`, `"hi"`). |
| **8** | **Strict Validation** | Enabled via Zod: `DeviceTranscriptInputSchema.strict({ message: "Unrecognized fields are not permitted in voice trip draft creation" })`. Any unexpected JSON property triggers HTTP `400 Bad Request`. |
| **9** | **Response Body** | Standard API success envelope: `{ "statusCode": 201, "success": true, "data": { ...VoiceTripDraftResponse }, "message": "Voice trip draft created successfully." }`. |
| **10** | **Response Fields** | `id`, `driverId`, `inputMode`, `originalTranscript`, `normalizedTranscript`, `intent`, `origin`, `destination`, `status`, `tripId` (optional), `expiresAt`, `createdAt`, `updatedAt`. |
| **11** | **Draft ID** | 24-character hexadecimal MongoDB ObjectId string (`^[0-9a-fA-F]{24}$`). |
| **12** | **Origin Representation** | Object with `query` (string) and `resolved` (`ResolvedLocation` object containing `latitude`, `longitude`, `formattedAddress`, `displayName`, `provider`, `city`, `state`, `country`). |
| **13** | **Destination Representation** | Object with `query` (string) and `resolved` (`ResolvedLocation` object containing `latitude`, `longitude`, `formattedAddress`, `displayName`, `provider`, `city`, `state`, `country`). |
| **14** | **Confidence Fields** | `confidence` is tracked internally on `VoiceIntentResult` (e.g. `0.95` for deterministic), but is NOT exposed on public `VoiceTripDraftResponse`. |
| **15** | **Transcription Fields** | `originalTranscript` (exact raw text received) and `normalizedTranscript` (sanitized Unicode NFC, stripped filler words and punctuation). |
| **16** | **Clarification Fields** | When origin or destination cannot be resolved, the backend returns HTTP 404 with error code `VOICE_LOCATION_NOT_FOUND` or 400 `VOICE_ORIGIN_MISSING` / `VOICE_DESTINATION_MISSING`. There are no partial ambiguous location candidate arrays in the DTO; clarification is executed by searching candidate locations via `GET /api/v1/locations/search`. |
| **17** | **Unresolved-Location Behavior** | Fails fast with HTTP 404 (`VOICE_LOCATION_NOT_FOUND`) or 400 (`VOICE_INTENT_UNCLEAR`). Minimum separation requirement: Origin and destination must be at least 50 meters apart, else returns 400 `SAME_ORIGIN_DESTINATION`. |
| **18** | **Validation Errors** | Standard API error response format: `{ "statusCode": 400, "success": false, "error": { "code": "...", "message": "..." } }`. |
| **19** | **Rate Limits** | Governed by `voiceRateLimiter` (`windowMs: 60 * 1000`, `max: 10` requests per minute). Exceeding returns `429 Too Many Requests`. |
| **20** | **Realtime Protocol** | WebSocket (`ws://` / `wss://`). Mounted at pathname `/api/v1/voice/realtime`. |
| **21** | **WebSocket Authentication** | Query parameter `?token=<jwt>` or `Authorization: Bearer <jwt>` on HTTP upgrade request. Requires `applicationUser.role === ROLES.DRIVER_CONDUCTOR`. |
| **22** | **WebSocket Subscription Mechanism** | Pre-allocation via `POST /api/v1/voice/sessions` returning `sessionId`, then connecting to `/api/v1/voice/realtime?sessionId=<sessionId>`. |
| **23** | **WebSocket Message Format** | Binary audio PCM chunks for audio streaming, JSON text frames for events (`{ "type": "...", "payload": { ... } }`). |
| **24** | **Realtime Event Types** | `SESSION_ESTABLISHED`, `AUDIO_CHUNK_ACK`, `TRANSCRIPT_PARTIAL`, `TRANSCRIPT_FINAL`, `DRAFT_READY`, `ERROR`. |
| **25** | **Realtime Event Payloads** | Typed according to `realtime.types.ts`. `DRAFT_READY` contains the completed `VoiceTripDraftResponse`. |
| **26** | **Session/Connection Lifecycle** | 1. Pre-allocate session (`POST /api/v1/voice/sessions`), 2. Connect WebSocket within 60s, 3. Stream audio, 4. Receive final transcript & draft, 5. Disconnect. |
| **27** | **Reconnect Requirements** | Session timeout: 5 minutes idle timeout; 15 minutes draft TTL. If WebSocket drops, client must create new session or fall back to REST. |
| **28** | **Timeout Behavior** | REST client read timeout: 15s. Realtime audio processing timeout: 10s per phrase. Draft expiration: 15 minutes TTL (`expiresAt`). |
| **29** | **Server-Side Speech/AI Behavior** | Orchestrator routes audio to Whisper (self-hosted), Google STT, or OpenAI. For Mode B (`DEVICE_TRANSCRIPT`), server bypasses ASR and runs Deterministic Regex entity extraction, falling back to OpenAI LLM if configured. |

---

## 3. REST Contract: POST /api/v1/voice/trip-drafts (Mode B)

### Request Specification
- **Method**: `POST`
- **Path**: `/api/v1/voice/trip-drafts`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <session_token>`
- **Request Body (Strict JSON)**:
```json
{
  "inputMode": "DEVICE_TRANSCRIPT",
  "transcript": "Take me from Jhansi Railway Station to SRGI College",
  "languageHint": "en"
}
```

### Constraints:
- `inputMode`: Strict literal `"DEVICE_TRANSCRIPT"`
- `transcript`: Min 3 chars, max 500 chars, trimmed.
- `languageHint`: Optional, max 20 chars.
- Unrecognized properties are strictly rejected with HTTP 400.

### Response Specification (HTTP 201 Created)
```json
{
  "statusCode": 201,
  "success": true,
  "data": {
    "id": "673f456789abcdef01234567",
    "driverId": "673e456789abcdef01234560",
    "inputMode": "DEVICE_TRANSCRIPT",
    "originalTranscript": "Take me from Jhansi Railway Station to SRGI College",
    "normalizedTranscript": "Take me from Jhansi Railway Station to SRGI College",
    "intent": "CREATE_TRIP",
    "origin": {
      "query": "Jhansi Railway Station",
      "resolved": {
        "latitude": 25.4484,
        "longitude": 78.5685,
        "formattedAddress": "Jhansi Junction Railway Station, Jhansi, Uttar Pradesh",
        "displayName": "Jhansi Railway Station",
        "provider": "google_maps",
        "city": "Jhansi",
        "state": "Uttar Pradesh",
        "country": "India"
      }
    },
    "destination": {
      "query": "SRGI College",
      "resolved": {
        "latitude": 25.4984,
        "longitude": 78.6085,
        "formattedAddress": "SRGI College Campus, Gwalior Road, Jhansi, Uttar Pradesh",
        "displayName": "SRGI College",
        "provider": "google_maps",
        "city": "Jhansi",
        "state": "Uttar Pradesh",
        "country": "India"
      }
    },
    "status": "CREATED",
    "expiresAt": "2026-09-27T16:15:00.000Z",
    "createdAt": "2026-09-27T16:00:00.000Z",
    "updatedAt": "2026-09-27T16:00:00.000Z"
  },
  "message": "Voice trip draft created successfully."
}
```

---

## 4. Error Code Reference

| Status Code | Error Code | Description / Backend Cause |
|-------------|------------|-----------------------------|
| 400 | `VOICE_INTENT_UNCLEAR` | Speech contained no recognizable trip endpoints or was empty |
| 400 | `VOICE_ORIGIN_MISSING` | Could not identify origin / pickup location |
| 400 | `VOICE_DESTINATION_MISSING` | Could not identify destination / dropoff location |
| 400 | `SAME_ORIGIN_DESTINATION` | Origin and destination resolve to the same place (< 50m separation) |
| 401 | `UNAUTHORIZED` | Missing or expired Bearer token |
| 403 | `FORBIDDEN` | Missing required role (`DRIVER_CONDUCTOR` in current backend) |
| 404 | `VOICE_LOCATION_NOT_FOUND` | Geospatial lookup for origin or destination returned 0 places |
| 409 | `VOICE_DRAFT_EXPIRED` | Draft TTL exceeded (15 minutes) |
| 429 | `RATE_LIMIT_EXCEEDED` | Exceeded 10 requests per minute limit |
| 500 | `INTERNAL_SERVER_ERROR` | Backend or geocoding provider internal error |

---

## 5. Realtime WebSocket Endpoint Audit (/api/v1/voice/realtime)

### Determination: Is Realtime Required for Phase 12?
**NO.**
1. **Purpose of Realtime Gateway**: In `ishara-backend/src/modules/realtime/realtime.gateway.ts`, `/api/v1/voice/realtime` is exclusively built for **server-side speech recognition** (Mode A / Mode C). The client opens a WebSocket and streams raw PCM audio chunks over the socket so the backend can execute Whisper or Google Cloud Speech ASR.
2. **Phase 12 Architecture**: Uses **on-device speech recognition** (`AndroidSpeechRecognizerClient` / `VoiceInputClient`), which runs locally on the Android device. This yields immediate partial and final transcripts with zero streaming latency, works offline for recognition, and avoids sending megabytes of raw microphone audio over the cellular network.
3. **Draft Submission**: Once the local transcript is captured, submitting it via `POST /api/v1/voice/trip-drafts` with `inputMode: "DEVICE_TRANSCRIPT"` is a single idempotent REST call that immediately returns the resolved `VoiceTripDraftResponse`.
4. **Conclusion**: Realtime WebSocket streaming is NOT required for Mode B device transcript flow.

---

## 6. Unknowns & Backend Discrepancies

1. **Role Limitation**:
   - Backend `voice.routes.ts` requires `requireDriverConductor`. For student accounts, the backend will return `403 FORBIDDEN`.
   - The Android client strictly handles this: it sends the authenticated student token, and if 403 or an error is returned, provides clean user guidance and a fallback to manual location search or clarification without crashing.
2. **Draft Cancellation**:
   - Backend supports `POST /api/v1/voice/trip-drafts/:draftId/cancel`. The client implements this in `VoiceTripRepository.cancelTripDraft`.
