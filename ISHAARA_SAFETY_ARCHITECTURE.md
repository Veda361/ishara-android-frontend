# ISHAARA Phase 15 — Safety & SOS Architecture

## 1. Executive Summary

Phase 15 implements production-grade Safety, SOS, and Emergency Response for the ISHAARA Android mobility platform. This system is designed under strict safety-critical engineering principles:
- The backend is the sole source of truth.
- Zero invented endpoints, statuses, or automatic dispatch claims.
- The UI never asserts that police, ambulance, or emergency contacts have been notified unless explicitly confirmed by backend response or events.
- Client requests are guarded against accidental taps, duplicate submissions, and network timeout ambiguities.

---

## 2. Safety Architecture Overview

```
                    ACTIVE RIDE
                         │
                         ▼
                      SAFETY
                         │
                         ▼
                    SOS CONFIRM
                         │
                         ▼
                  Safety Use Case
                         │
                         ▼
                   Repository
                         │
                         ▼
                  Backend Safety API
                         │
                  ┌──────┴──────┐
                  ▼             ▼
             REST Result     Realtime
                  │             │
                  └──────┬──────┘
                         ▼
                 Authoritative State
                         │
                         ▼
                         UI
```

### Layered Structure

```
feature/
  safety/
    SafetyScreen.kt          # UI: Calm, accessible, unambiguous emergency controls
    SafetyViewModel.kt       # ViewModel: Coordinates SOS lifecycle, reconciliation, and UI state
    SafetyUiState.kt         # Immutable state representation (Idle, Submitting, ActiveAlert, Error)

domain/
  model/
    EmergencyEvent.kt        # Strongly typed emergency domain model matching backend contract
    EmergencyStatus.kt       # ACTIVE, ACKNOWLEDGED, RESOLVED, CANCELLED, UNKNOWN
    EmergencyType.kt         # SOS, SAFETY_CONCERN, UNKNOWN
    LocationSnapshot.kt      # Domain location snapshot derived server-side
    EmergencyContact.kt      # User's trusted contacts
    EmergencyContactRelationship.kt

  repository/
    SafetyRepository.kt      # Abstract safety contract (SOS trigger, active check, cancel, contacts)

  usecase/
    TriggerSosUseCase.kt     # Orchestrates SOS submission with UUID idempotency key
    GetActiveSosUseCase.kt   # Fetches active SOS state for reconciliation
    CancelSosUseCase.kt      # Cancels caller's active SOS
    GetEmergencyContactsUseCase.kt
    CreateEmergencyContactUseCase.kt
    DeleteEmergencyContactUseCase.kt

data/
  remote/
    dto/SafetyDtos.kt        # Exact serialization/deserialization matching backend Zod strict schemas
    datasource/
      SafetyRemoteDataSource.kt
  repository/
    SafetyRepositoryImpl.kt  # Implementation handling session tokens, network, DTO mapping
  mapper/
    SafetyMapper.kt          # Clean boundary transformations between DTOs and Domain
```

---

## 3. SOS Trigger Flow & Confirmation UX

1. **Discovery & Access**:
   - The user opens the Active Ride Screen (`RideTrackingScreen`).
   - A dedicated, high-contrast, calm "Safety & Emergency Assistance" button is clearly visible.
   - Touching this button opens `SafetyScreen` with context of the current ride.

2. **Intentional Confirmation**:
   - Accidental taps are guarded with a two-step confirmation dialog or hold-to-confirm gesture.
   - Text clearly states:
     - "Send a safety alert for this ride."
     - Explains what the alert does (notifies system operations of an active emergency on your ride).
     - No misleading claims regarding 911/112/police dispatch.

3. **Submission & Idempotency**:
   - A random `UUID` is generated as the `Idempotency-Key` header for the request.
   - Action buttons are immediately disabled while the submission is in-flight (`isSubmitting = true`).
   - If the user re-triggers or network retries occur with the same key, backend returns the existing event cleanly.

4. **Authoritative Response & Realtime Sync**:
   - Upon receiving `201 Created` with `EmergencyEventResponse`, the UI switches to `ActiveAlert` state displaying the event ID (`se_...`) and trigger timestamp.
   - Realtime events (`SOS_CREATED`, `SOS_CANCELLED`) arrive via the established WebSocket connection, updating the state reactively.

---

## 4. Role Boundaries & Ride Eligibility

- **Supported Roles**: Both `USER` (passenger) and `DRIVER_CONDUCTOR` are supported by the backend endpoints if they are participants in the ride. However, for Phase 15 passenger experience, the UI is integrated directly into the student active ride flow.
- **Eligible Ride Statuses**:
  - `CREATED`
  - `DRIVER_ARRIVING`
  - `PICKED_UP`
  - `IN_PROGRESS`
  - Rides in `COMPLETED` or `CANCELLED` statuses are rejected by the backend with HTTP 400 (`SOS_NOT_ELIGIBLE`).

---

## 5. Location Handling & Privacy

- **Server-Derived Snapshots**:
  - The client does **not** transmit client-side coordinates in the POST body (backend schema is `.strict()` and rejects client coordinates).
  - The backend server derives the snapshot:
    - Pre-pickup (`CREATED`, `DRIVER_ARRIVING`): Uses `ride.pickup.coordinates` with provider `"ride_pickup"`.
    - In-transit (`PICKED_UP`, `IN_PROGRESS`): Uses `driverProfile.currentLocation` with provider `"driver_profile"`.
- **Privacy Policy**:
  - Precise raw GPS coordinates are never logged in Android logcat.
  - Logs indicate boolean status: `locationAttached=true` or `hasCoordinates=true`.

---

## 6. Network Failure & Reconciliation

When a network timeout or connection reset occurs during an SOS request:
1. The request may have succeeded on the backend before the socket dropped.
2. The client **must not** blindly re-post without checking status.
3. The client invokes `GetActiveSosUseCase(rideId)`.
4. If an active SOS exists, the client seamlessly reconciles into the `ActiveAlert` state.
5. If no active SOS exists, the client offers a safe retry with a clear, calm message.

---

## 7. Known Backend Limitations

1. **No External Emergency Service Dispatch**: No automated integration with police (112/911) or medical dispatch exists.
2. **SMS Verification Unintegrated**: Emergency contacts have `isVerified = false` hardcoded; no SMS verification gateway is present.
3. **No FCM Safety Push**: Server-side safety notifications are WebSocket-only in Phase 15.
