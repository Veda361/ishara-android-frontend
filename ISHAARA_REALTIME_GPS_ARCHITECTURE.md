# Ishaara Realtime GPS & Telemetry Architecture Specification

**Document Version**: 2.0.0  
**Phase**: PHASE 09 — GPS + REALTIME DRIVER TELEMETRY  
**Status**: VERIFIED & FULLY IMPLEMENTED  
**Target Roles**: `DRIVER_CONDUCTOR`, `USER` (Passenger Observer)  

---

## 1. Architectural Overview & Boundaries

The Ishaara Driver Telemetry and Realtime subsystem is engineered according to Clean Architecture and Unidirectional Data Flow (UDF). It strictly separates device hardware acquisition, foreground operating system services, network transport layers, domain use cases, and presentation state machines.

### 1.1 Complete Layer Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                          DRIVER PRESENTATION                           │
│  DriverHomeScreen • DriverTripCard • StatusChips                       │
│  ↑                                                                     │
│  DriverHomeUiState (stage, tripActionState, telemetryStatus)           │
│  ↑                                                                     │
│  DriverViewModel                                                       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ triggers / observes
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           USE CASE LAYER                               │
│  StartDriverTrackingUseCase • StopDriverTrackingUseCase                │
│  ObserveDriverTrackingStatusUseCase • UpdateDriverLocationUseCase      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ orchestrates
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          COORDINATOR & REPO                            │
│  DriverTrackingCoordinator (Monotonic enforcement + freshest-only)     │
│  DriverRepository (Delegates authenticated location payload)           │
└───────────────────┬───────────────────────┬────────────────────────────┘
                    │                       │
      ┌─────────────┴─────────────┐         │
      ▼                           ▼         ▼
┌───────────────────┐   ┌───────────────────┐   ┌────────────────────────┐
│ ANDROID LOCATION  │   │   REST LOCATION   │   │   WEBSOCKET REALTIME   │
│    DATA SOURCE    │   │    DATA SOURCE    │   │      DATA SOURCE       │
│                   │   │                   │   │                        │
│ FusedLocation-    │   │ DriverRemote-     │   │ OkHttpRealtimeClient   │
│ Provider          │   │ DataSourceImpl    │   │ (OkHttp WebSocket)     │
│ (callbackFlow)    │   │ (PATCH /location) │   │ (wss://rides/realtime) │
└─────────┬─────────┘   └───────────────────┘   └────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       ANDROID PLATFORM LAYER                           │
│  DriverLocationService (Foreground Service: FOREGROUND_SERVICE_LOCATION)│
│  Ongoing Notification ("Ishaara Driver Tracking Active")               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Production Code Inventory

### 2.1 Classes Created (11 New Files)
1. `com.ishara.app.core.location.FusedLocationProvider`: Production implementation of `LocationProvider` wrapping `FusedLocationProviderClient` into Kotlin Coroutines `callbackFlow`.
2. `com.ishara.app.core.realtime.OkHttpRealtimeClient`: Resilient WebSocket client implementing `RealtimeClient` with audited exponential backoff (1s, 2s, 4s, 8s, 16s, max 30s), auth-failure detection (1008, 4401, HTTP 401/403), and zero token logging.
3. `com.ishara.app.core.realtime.RealtimeEventParser`: Pure Kotlin JSON parser safely handling snake_case and camelCase telemetry payloads with zero crashes on malformed frames.
4. `com.ishara.app.feature.driver.tracking.DriverTrackingStatus`: Sealed interface representing observable telemetry states (`Idle`, `Active`, `PermissionRequired`, `GpsDisabled`, `NetworkUnavailable`, `Error`).
5. `com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator`: Production coordinator enforcing monotonic timestamps and freshest-only offline buffering.
6. `com.ishara.app.feature.driver.tracking.DriverLocationService`: Android 14+ Foreground Service adapter (`foregroundServiceType="location"`) posting persistent ongoing notification.
7. `com.ishara.app.domain.usecase.StartDriverTrackingUseCase`: Domain use case to initiate high-frequency telemetry streaming.
8. `com.ishara.app.domain.usecase.StopDriverTrackingUseCase`: Domain use case to halt telemetry streaming.
9. `com.ishara.app.domain.usecase.ObserveDriverTrackingStatusUseCase`: Domain use case observing live `DriverTrackingStatus`.
10. `com.ishara.app.RealtimeGpsTelemetryTest`: 26 comprehensive unit tests covering Location (1-8), Network (9-15), Realtime (16-25), and Security (33-34).
11. `com.ishara.app.DriverTrackingLifecycleTest`: 7 unit tests covering trip lifecycle transitions, offline stops, and app relaunch tracking restoration (26-32).

### 2.2 Classes Modified (12 Files)
1. `LocationCoordinates.kt`: Added `toIso8601Utc()` UTC serialization.
2. `RealtimeEvent.kt`: Added `routeProgressPercentage` to `LiveTelemetryUpdate` and added `UnknownEvent` variant.
3. `IshaaraError.kt`: Added `PermissionDenied` and `LocationDisabled` domain error types.
4. `IshaaraLogger.kt`: Added token query parameter sanitization (`?token=[PROTECTED]`) and `formatCoordinatesForLog(lat, lng)` coarsening coordinates in debug and redacting in production.
5. `DriverRemoteDataSource.kt`: Added mandatory `recordedAt` ISO-8601 timestamp to `PATCH /api/v1/drivers/me/location` request body.
6. `DriverHomeUiState.kt`: Added `telemetryStatus: DriverTrackingStatus = DriverTrackingStatus.Idle`.
7. `DriverViewModel.kt`: Integrated `startDriverTrackingUseCase`, `stopDriverTrackingUseCase`, and `observeDriverTrackingStatusUseCase`. Hooked tracking start on trip start, tracking stop on trip complete/cancel/offline, and auto-restore on relaunch with active trip.
8. `DriverHomeScreen.kt`: Added contextual runtime permission launcher and rendered natural language status badges ("Location active", "Turn on location to continue", "Connection lost. Trying again…").
9. `AppContainer.kt`: Wired production `FusedLocationProvider`, `OkHttpRealtimeClient`, and `DriverTrackingCoordinator`.
10. `MainActivity.kt`: Passed tracking use cases to `DriverViewModel`.
11. `AndroidManifest.xml`: Declared `DriverLocationService` with `android:foregroundServiceType="location"`.
12. `libs.versions.toml` & `app/build.gradle.kts`: Added `play-services-location:21.3.0` and `okhttp:4.12.0`.

---

## 3. Location Synchronization Pipeline (Driver -> Backend)

```
[Hardware GNSS / Network Fix]
       │
       ▼
FusedLocationProviderClient
       │ (converts Android Location to LocationCoordinates via callbackFlow)
       ▼
DriverTrackingCoordinator
       │ 1. Coordinate check: lat in [-90, 90], lng in [-180, 180]
       │ 2. Monotonic check: timestampMillis > lastSentMonotonicTime
       ▼
UpdateDriverLocationUseCase
       │
       ▼
DriverRepositoryImpl
       │ (attaches session bearer token)
       ▼
DriverRemoteDataSourceImpl (PATCH /api/v1/drivers/me/location)
       │
   [HTTP 200] ──► Success: updates lastSentMonotonicTime; clears offline buffer
       │
   [HTTP 401] ──► SessionInvalidationCoordinator: alerts auth repository & halts tracking
       │
   [HTTP 403] ──► Forbidden: halts tracking & notifies UI (unverified driver)
       │
   [Network Error] ──► Buffers single freshest fix; updates status to NetworkUnavailable
```

---

## 4. Realtime Streaming Pipeline (Server -> Passenger / Observers)

```
Backend Ingestion Engine (processes PATCH /api/v1/drivers/me/location)
       │
       ▼ Broadcasts
wss://<host>/api/v1/rides/realtime?token=<bearer_token>
       │
       ▼
OkHttp WebSocket Connection (pingInterval = 30s)
       │
       ▼
OkHttpRealtimeClient.onMessage(text)
       │
       ▼
RealtimeEventParser.parse(text)
       │ (maps to RealtimeEvent.LiveTelemetryUpdate)
       ▼
RealtimeClient.observeEvents() Flow
```

---

## 5. Lifecycle Management & Android Platform Rules

### 5.1 Trigger Matrix
1. **Trip Started (`POST /api/v1/trips/:tripId/start` -> 200 OK)**:
   - `DriverViewModel` triggers `StartDriverTrackingUseCase(tripId)`.
   - `DriverLocationService.start(context, tripId)` promotes execution to foreground priority.
   - Ongoing notification posted (`NotificationCompat.CATEGORY_SERVICE`).
2. **Trip Completed / Cancelled (`POST /complete` or `/cancel` -> 200 OK)**:
   - `DriverViewModel` triggers `StopDriverTrackingUseCase()`.
   - `DriverLocationService.stop(context)` removes notification and calls `stopSelf()`.
3. **Driver Toggles Offline (`POST /status/offline` -> 200 OK)**:
   - Tracking is immediately halted.
4. **App Enters Background**:
   - Foreground Service exemption keeps location updates and network active; Android OS does not throttle to hourly batches.
5. **App Process Killed & Relaunched**:
   - `DriverViewModel.loadOperationalContext()` queries `/operations/context`. If `context.activeTrip?.status == DriverTripStatus.ACTIVE`, tracking is automatically restored.
6. **Permission Revoked**:
   - Location flow catches `SecurityException`, stops tracking, and transitions status to `DriverTrackingStatus.PermissionRequired`.

---

## 6. Offline & Network Resiliency Policy

- **No Historical Batch Accumulation**:
  - Unbounded queuing of stale GPS points is strictly prohibited.
  - Reason: The backend endpoint `PATCH /api/v1/drivers/me/location` serves real-time passenger tracking with monotonic timestamp validation. High-volume replays create burst load, trigger out-of-order rejections, and distort real-time passenger route ETAs.
- **Freshest-Only Strategy**:
  - While offline: Keep only the single most recent valid GPS fix.
  - When network returns: Transmit the single freshest fix and drop all stale intermediate fixes.

---

## 7. Battery & Power Conservation

1. **Configurable Client Defaults (Not Backend Contract)**:
   - Sampling interval: `4,000 ms` (`DEFAULT_ACTIVE_INTERVAL_MILLIS`)
   - Minimum displacement filter: `5.0 meters` (`DEFAULT_MIN_DISPLACEMENT_METERS`)
2. **Displacement Filtering**:
   - Suppresses sensor noise and redundant GPS wakeups when the vehicle is stationary at traffic stops.
3. **Clean Teardown**:
   - `awaitClose` in `FusedLocationProvider` calls `fusedClient.removeLocationUpdates(callback)`.

---

## 8. Security & Data Protection

1. **Zero Raw Coordinates in Production Logs**:
   - `IshaaraLogger.formatCoordinatesForLog(lat, lng)` returns `"[REDACTED]"` when `isDebug = false`. In debug mode, coordinates are coarsened to 2 decimal places (`18.52***, 73.86***`).
2. **Token Protection**:
   - `IshaaraLogger.sanitize(message)` strips tokens from URLs (`?token=[PROTECTED]`) and HTTP headers (`Authorization: [REDACTED]`).
3. **No Plaintext Persistence**:
   - Transient telemetry points are never written to unencrypted local storage or SQLite databases.

---

## 9. Reconnection Strategy (WebSocket)

- **Normal Closure (1000)**: Clean termination, zero reconnect.
- **Policy Violation (1008) / Unauthorized (4401 or HTTP 401/403)**: Halts reconnection immediately; delegates to `SessionInvalidationCoordinator` to invalidate credentials.
- **Abnormal / Network Drops**:
  - Reconnects with exponential backoff: `1s`, `2s`, `4s`, `8s`, `16s`, max `30s` + random jitter (`0..200ms`).
  - Max retry attempts: 5. After 5 attempts, transitions to `RealtimeConnectionState.Failed`.

---

## 10. Verification & Quality Gates

All three quality verification checks passed completely:
1. **Unit Tests**:
   - Command: `./gradlew testDebugUnitTest`
   - Result: **133 passed, 0 failed, 0 skipped** (including 33 Phase 09 tests).
2. **Build Assembly**:
   - Command: `./gradlew assembleDebug`
   - Result: **BUILD SUCCESSFUL** (APK generated cleanly).
3. **Android Lint**:
   - Command: `./gradlew lintDebug`
   - Result: **BUILD SUCCESSFUL** (0 errors).

---

## 11. Known Backend Unknowns & Client Assumptions

The following items were identified during the audit as unstated in backend contracts; they have been implemented defensively behind isolated client configurations:
1. **PATCH response body**: Handled flexibly (any 200..299 treated as success; response body ignored).
2. **GPS ingestion interval**: Implemented as configurable client default (`4,000ms`, `5m displacement`).
3. **Stale timestamp rejection code**: Client drops stale fixes client-side before sending.
4. **WebSocket subscription envelope**: Isolated in `RealtimeClient`; currently connects to `/rides/realtime?token=<token>`.
5. **Heartbeat format**: Handled via OkHttp RFC 6455 transport-level 30s ping without inventing application-level JSON pings.
