# Ishaara Driver / Conductor Architecture Specification

**Document Version**: 1.0.0  
**Status**: ACTIVE / PRODUCTION ARCHITECTURE  
**Phase**: PHASE 08 — DRIVER / CONDUCTOR OPERATIONAL FOUNDATION  
**Role**: `DRIVER_CONDUCTOR`

---

## 1. Scope & Core Objectives

Phase 08 establishes the production driver operational foundation for Ishaara on Android.
The driver experience answers four primary operational questions at a glance:
1. **Who am I?** (Driver identity, license verification status).
2. **What is my availability?** (Online / Offline / On Ride).
3. **What vehicle and trip am I operating?** (Assigned vehicle, active or created route).
4. **What is my next action?** (Go Online, Start Trip, Complete Trip, Cancel Trip).

### Critical Safety Principles
- **Minimal Interaction**: Driver UI is streamlined for safety while stopped or operating.
- **Glanceable Hierarchy**: Large primary CTA (54dp height), high-contrast status chips, and zero decorative or cyberpunk distractions.
- **Single Primary Action**: Obvious next step rendered dynamically based on the authoritative backend state.
- **No Realtime Overreach**: Continuous GPS tracking and WebSockets are deferred to Phase 09.

---

## 2. Architecture & Data Flow

Adheres strictly to Clean Architecture and Unidirectional Data Flow (UDF):

```
┌────────────────────────────────────────────────────────┐
│                   DRIVER UI LAYER                      │
│  DriverHomeScreen • DriverTripCard • ActionButtons     │
└───────────────────────────┬────────────────────────────┘
                            │ observes uiState / sends user intents
                            ▼
┌────────────────────────────────────────────────────────┐
│              DRIVER PRESENTATION LAYER                 │
│  DriverViewModel • DriverHomeUiState (UDF)             │
└───────────────────────────┬────────────────────────────┘
                            │ executes use cases
                            ▼
┌────────────────────────────────────────────────────────┐
│                  DRIVER DOMAIN LAYER                   │
│  GetDriverOperationalContextUseCase                    │
│  SetDriverOnlineUseCase • SetDriverOfflineUseCase      │
│  StartTripUseCase • CompleteTripUseCase • CancelTrip   │
│  DriverRepository • TripRepository                     │
└───────────────────────────┬────────────────────────────┘
                            │ delegates to data layer
                            ▼
┌────────────────────────────────────────────────────────┐
│                   DRIVER DATA LAYER                    │
│  DriverRepositoryImpl • DriverRemoteDataSource         │
│  DriverDtos • DriverMapper • SessionLocalDataSource    │
└───────────────────────────┬────────────────────────────┘
                            │ executes authenticated HTTP
                            ▼
┌────────────────────────────────────────────────────────┐
│                BACKEND REST SERVICES                   │
│  GET  /api/v1/drivers/me/operations/context            │
│  POST /api/v1/drivers/me/status/online                 │
│  POST /api/v1/drivers/me/status/offline                │
│  POST /api/v1/trips/:tripId/start                      │
│  POST /api/v1/trips/:tripId/complete                   │
│  POST /api/v1/trips/:tripId/cancel                     │
└────────────────────────────────────────────────────────┘
```

---

## 3. Domain Models

- **`DriverProfileStatus`**: `OFFLINE`, `ONLINE`, `ON_RIDE`.
- **`DriverVerificationStatus`**: `PENDING`, `VERIFIED`, `REJECTED`.
- **`DriverOperationalContext`**:
  - `driver`: `DriverIdentity` (id, userId, verificationStatus, status, licenseNumberMasked, licenseVerifiedAt).
  - `vehicle`: `DriverAssignedVehicle?` (id, registrationNumber, vehicleType, make, model, isActive, isVerified).
  - `activeTrip`: `DriverActiveTrip?` (id, status, origin, destination, distanceMeters, durationSeconds, startedAt, completedAt, cancelledAt).
  - `activeRidesCount`: count of in-flight passenger rides.
  - `todayStats`: `DriverTodayStats` (completedRidesCount, isOnline, currentDate, timezone).
- **`DriverTripStatus`**: `CREATED`, `ACTIVE`, `COMPLETED`, `CANCELLED`.

---

## 4. State Machine & Unidirectional Data Flow

### 4.1 UI State: `DriverHomeUiState`
```kotlin
data class DriverHomeUiState(
    val stage: DriverHomeStage = DriverHomeStage.Loading,
    val tripActionState: TripActionState = TripActionState.Idle,
    val statusActionState: DriverStatusActionState = DriverStatusActionState.Idle,
    val isActionInProgress: Boolean = false,
    val userFacingNotification: String? = null,
    val userFacingError: String? = null
)

sealed interface DriverHomeStage {
    object Loading : DriverHomeStage
    data class Content(val context: DriverOperationalContext) : DriverHomeStage
    data class Empty(val context: DriverOperationalContext) : DriverHomeStage
    data class Offline(val context: DriverOperationalContext) : DriverHomeStage
    data class Error(val error: IshaaraError, val canRetry: Boolean = true) : DriverHomeStage
}

sealed interface TripActionState {
    object Idle : TripActionState
    data class Submitting(val tripId: String, val actionType: TripActionType) : TripActionState
    data class Success(val trip: DriverActiveTrip, val message: String) : TripActionState
    data class Error(val error: IshaaraError) : TripActionState
}

enum class TripActionType {
    START,
    COMPLETE,
    CANCEL
}

sealed interface DriverStatusActionState {
    object Idle : DriverStatusActionState
    object Submitting : DriverStatusActionState
    data class Error(val error: IshaaraError) : DriverStatusActionState
}
```

### 4.2 Duplicate Action & Lifecycle Safety
1. `isActionInProgress` boolean flag in `DriverHomeUiState` immediately locks buttons and displays an inline progress indicator.
2. `DriverViewModel` validates `isActionInProgress` before launching coroutines.
3. Silent refresh (`loadOperationalContext(silent = true)`) syncs fresh backend state without layout shift or UI flickering.
4. Precondition enforcement:
   - Driver must be `VERIFIED` to go online.
   - Driver cannot transition to `OFFLINE` while operating an active ride (`ON_RIDE`).
   - Starting a trip requires `CREATED` status.
   - Completing a trip requires `ACTIVE` status.
   - Cancelling a trip requires `CREATED` or `ACTIVE` status.
5. Confirmation dialogs protect high-impact actions (`completeTrip`, `cancelTrip`).

---

## 5. Security & Role Enforcement

- **Role Gating in MainActivity**:
  - `ApplicationDestination.DriverDashboard` explicitly validates `obState.userProfile.role == UserRole.DRIVER_CONDUCTOR`.
  - Non-driver accounts (`USER_STUDENT`) are denied and routed to `SafeRecoveryScreen` with `"Access denied: Driver permissions required."`.
  - Drivers attempting to enter `ApplicationDestination.StudentHome` are automatically routed to `DriverHomeScreen`.
- **Backend 403 Forbidden Mapping**:
  - If a non-driver attempts to invoke `/drivers/me/operations/context`, backend returns HTTP 403 Forbidden.
  - `DriverViewModel` catches `IshaaraError.Forbidden` and renders a non-retryable error screen prompting the user to sign out.
- **Session Protection**: Uses centralized `SessionLocalDataSource` to inject `Authorization: Bearer <token>`.
- **No Token Leakage**: Tokens and credentials are never stored in plain text or emitted to logs.

---

## 6. Accessibility & Driver Safety UX

- Primary action button height: **54dp** (`IshaaraButtonSize.Large`).
- Minimum touch target for all interactive elements: **48dp**.
- Glanceable hierarchy:
  1. Top Bar: Greeting & Driver Identity + Masked License (`DL••••••••1234`).
  2. Operational Status Card: Current status (`ONLINE`, `OFFLINE`, `ON_RIDE`) with status toggle button.
  3. Assigned Vehicle Card: Registration number, make/model, verified badge.
  4. Current Trip Card: Origin → Destination, waypoints, route distance/duration, dominant primary action button.
  5. Today's Activity Card: Completed rides, in-flight passenger count.
- High contrast color pairs from `IshaaraTheme.colors`.
- Confirmation dialogs:
  - `IshaaraDialog` for Complete Trip confirmation.
  - `IshaaraDialog` with `isDanger = true` for Cancel Trip confirmation.

---

## 7. Automated Unit Test Verification

- **`DriverRepositoryTest`** (10 tests, 100% pass):
  - `getOperationalContext` endpoint, query parameters, and Bearer token verification.
  - `getOperationalContext` without active session failure mapping.
  - `setOnline` POST to `/drivers/me/status/online`.
  - `setOffline` POST to `/drivers/me/status/offline`.
  - `startTrip` POST to `/trips/:tripId/start`.
  - `completeTrip` POST to `/trips/:tripId/complete`.
  - `cancelTrip` POST to `/trips/:tripId/cancel`.
  - HTTP 403 Forbidden mapping to `IshaaraError.Forbidden`.
  - HTTP 409 Conflict mapping to `IshaaraError.Conflict`.
  - RFC 7946 GeoJSON `[longitude, latitude]` coordinate mapping in `DriverMapper`.
- **`DriverViewModelTest`** (14 tests, 100% pass):
  - Initial loading to `Content` when active trip is present.
  - Initial loading to `Offline` when driver is offline.
  - Initial loading to `Empty` when no active trip is scheduled.
  - Network failure transitions to `Error` with retry.
  - Forbidden 403 transitions to `Error` without retry.
  - `retry()` reloads operational context.
  - `goOnline()` and `goOffline()` availability transitions.
  - `goOffline()` blocked locally when `ON_RIDE`.
  - `startTrip`, `completeTrip`, and `cancelTrip` lifecycle execution and context refresh.
  - Trip action conflict error handling.
  - Duplicate action prevention.
  - Notification dismissal.

---

## 8. Explicitly Deferred Functionality (Phase 09 & Later)

- Live GPS streaming (`PATCH /api/v1/drivers/me/location`): Phase 09.
- Realtime WebSocket telemetry updates: Phase 09.
- Passenger ride-request acceptance & boarding queue: Phase 10.
- Hands-free voice trip creation: Phase 12.
- Payment collections & earnings payout: Phase 13/16.
- SOS emergency broadcast: Phase 15.
- Agency / fleet owner management: Web application only.

