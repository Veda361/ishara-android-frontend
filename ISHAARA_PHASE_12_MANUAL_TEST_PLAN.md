# ISHAARA Phase 12 Manual Device Test Plan: Hands-Free Voice Trip Creation

**Target**: ISHAARA Android Application  
**Phase**: Phase 12 (Voice Trip Creation)  
**Devices**: Physical Android Device / Android Emulator (API 34/35)  
**Prerequisites**: Valid student user account, microphone hardware  

---

### TEST 1 — Voice Permission (First Request & Denial)
- **Objective**: Verify microphone permission is requested just-in-time and handled cleanly.
- **Steps**:
  1. Fresh install ISHAARA Android app.
  2. Log in with student account and land on Student Home.
  3. Observe that no microphone permission prompt appears on startup.
  4. Tap `[ 🎙 Use voice ]`.
  5. Verify system runtime permission dialog appears: *"Allow Ishaara to record audio?"*.
  6. Tap "Don't allow".
- **Expected Result**: App handles denial cleanly, displays human-readable rationale: *"Microphone access needed. Allow microphone access to create a trip using your voice."* No crash or technical exception.

---

### TEST 2 — Start Voice (Granted Permission)
- **Objective**: Verify voice interaction starts promptly upon granting permission.
- **Steps**:
  1. On Student Home, tap `[ 🎙 Use voice ]`.
  2. Grant microphone permission.
  3. Observe Voice Trip screen opens.
- **Expected Result**: Screen displays "Listening…", pulsing microphone indicator activates, and hint text appears: *"Say where you want to go. For example: Take me from Jhansi Railway Station to SRGI College"*.

---

### TEST 3 — Speech Recognition (Real-Time Transcript)
- **Objective**: Verify speech input is transcribed and displayed incrementally.
- **Steps**:
  1. On Voice Trip screen in listening state, speak: *"Take me from Jhansi Railway Station to SRGI College"*.
  2. Observe live transcript area during speech.
- **Expected Result**: Interim partial recognition displays spoken words in real time.

---

### TEST 4 — Empty Speech
- **Objective**: Verify behavior when microphone is active but no speech occurs.
- **Steps**:
  1. Start voice session and remain silent for 5 seconds.
  2. Wait for speech recognition timeout.
- **Expected Result**: App transitions to error state: *"No speech detected. Please tap the microphone and speak clearly."* Displays `[ Try again ]` and `[ Search manually ]` buttons.

---

### TEST 5 — Cancel Speech
- **Objective**: Verify user can abort voice capture at any moment.
- **Steps**:
  1. Start voice session.
  2. While listening or speaking, tap `[ Cancel ]` or top-right `Cancel` button.
- **Expected Result**: Speech recognizer immediately stops, microphone hardware is released, and user returns to Student Home without lag.

---

### TEST 6 — Backend Draft Creation
- **Objective**: Verify backend `POST /api/v1/voice/trip-drafts` receives strict Mode B payload.
- **Steps**:
  1. Speak a valid trip command: *"Take me from Assi Ghat to Lanka"*.
  2. Tap `[ Done speaking ]` or let recognition finalize.
- **Expected Result**: Screen enters "Interpreting trip…" with spinner. Request is transmitted with `inputMode: "DEVICE_TRANSCRIPT"`.

---

### TEST 7 — Correct Origin/Destination Review
- **Objective**: Verify structured draft is displayed accurately for human verification.
- **Steps**:
  1. After backend responds with HTTP 201, observe Review Draft card.
- **Expected Result**:
  - Displays "Is this correct?".
  - "From" section shows resolved origin landmark, title, and formatted address.
  - "To" section shows resolved destination landmark, title, and formatted address.
  - Quote section displays exact transcript: *"Take me from Assi Ghat to Lanka"*.
  - Primary button `[ Confirm & find trips ]` is enabled.

---

### TEST 8 — Ambiguous Location / Clarification
- **Objective**: Verify clarification flow triggers when a location cannot be pinpointed.
- **Steps**:
  1. Speak an ambiguous landmark: *"Take me to railway station"*.
  2. Observe backend responds with unresolved location or not found.
- **Expected Result**: App transitions to clarification view: *"Which location did you mean?"*. Displays list of nearby station candidates. Tapping a candidate updates the draft to the chosen location.

---

### TEST 9 — Edit Draft
- **Objective**: Verify student can revise endpoints before confirming.
- **Steps**:
  1. On Review Draft card, tap `[ Try again ]` or `[ Search manually ]`.
- **Expected Result**: `[ Try again ]` restarts speech recognition; `[ Search manually ]` navigates to manual `LocationSearchScreen`.

---

### TEST 10 — Confirm Draft
- **Objective**: Verify explicit confirmation creates `DiscoveryQuery`.
- **Steps**:
  1. On Review Draft card, tap `[ Confirm & find trips ]`.
- **Expected Result**: Button enters confirmed loading state; draft is converted into `DiscoveryQuery` with exact origin and destination coordinates.

---

### TEST 11 — Discovery Handoff
- **Objective**: Verify seamless transition into Phase 06 Trip Discovery.
- **Steps**:
  1. Complete confirmation in Test 10.
  2. Observe screen transition.
- **Expected Result**: Navigates directly into `DiscoveryScreen`. Active buses/trips matching the confirmed route are queried and rendered without needing to re-enter origin or destination.

---

### TEST 12 — Network Disconnected (Offline Mode)
- **Objective**: Verify graceful failure when internet is unavailable.
- **Steps**:
  1. Enable Airplane Mode.
  2. Tap `[ 🎙 Use voice ]`.
  3. Speak a route.
- **Expected Result**: Clean error message displays: *"You're offline. Connect to the internet to use voice trip creation."* Displays `[ Search manually ]` button.

---

### TEST 13 — Backend Unavailable (5xx Server Error)
- **Objective**: Verify resilience against server errors.
- **Steps**:
  1. Mock server responding with 500 Internal Server Error.
  2. Submit voice trip draft.
- **Expected Result**: Error card displays friendly message: *"Could not create voice trip draft. Please try again or search manually."* No crashes.

---

### TEST 14 — Session Expiration (401 Unauthorized)
- **Objective**: Verify token invalidation handling during voice flow.
- **Steps**:
  1. Invalidate session token while reviewing draft.
  2. Submit draft or confirmation.
- **Expected Result**: Centralized `SessionInvalidationCoordinator` handles 401, clears invalid tokens, and safely directs user to Login screen without back-stack leakage.

---

### TEST 15 — Rapid Repeated Taps (Race Condition Protection)
- **Objective**: Verify double-tap guards on microphone and confirm buttons.
- **Steps**:
  1. Rapidly double-tap `[ 🎙 Use voice ]`.
  2. Rapidly double-tap `[ Confirm & find trips ]`.
- **Expected Result**: Exactly one voice session and one discovery query are triggered. Double-taps are rejected by monotonic session counters.

---

### TEST 16 — Background / Foreground Transition
- **Objective**: Verify microphone hardware is released when app is backgrounded.
- **Steps**:
  1. Start voice listening.
  2. Press device Home button or switch to another app.
  3. Return to ISHAARA app.
- **Expected Result**: Microphone hardware is released immediately upon backgrounding. App returns to stable state upon resume.

---

### TEST 17 — Process Recreation
- **Objective**: Verify state integrity across activity recreation (e.g. screen rotation).
- **Steps**:
  1. On Review Draft screen, rotate device.
- **Expected Result**: UI state restores from ViewModel; reviewed endpoints remain intact.

---

### TEST 18 — Accessibility & Touch Targets
- **Objective**: Verify screen reader compatibility and touch target sizing.
- **Steps**:
  1. Enable Android TalkBack.
  2. Navigate through Voice Trip screen.
  3. Inspect touch target sizes.
- **Expected Result**: Microphone button announces: *"Start voice trip creation, button"*. Confirmation button is at least 56dp tall. High-contrast typography is readable.

---

### TEST 19 — Localization Behavior
- **Objective**: Verify Hindi / Hinglish voice queries work seamlessly.
- **Steps**:
  1. Speak: *"BHU se Assi Ghat jaana hai"*.
  2. Observe entity extraction.
- **Expected Result**: Backend deterministic extractor identifies Origin: BHU, Destination: Assi Ghat and resolves coordinates accordingly.

---

### TEST 20 — Regression Test for Phases 05–11
- **Objective**: Verify Phase 12 does not break existing student or driver features.
- **Steps**:
  1. Run `./gradlew test` across the full repository test suite.
  2. Execute manual search from Student Home.
  3. Inspect Driver Dashboard and tracking screens.
- **Expected Result**: All 191 unit tests pass 100%. Phases 05–11 continue to operate with zero regressions.
