# ISHAARA FRONTEND PHASE A11 — CHANGELOG

## Phase Summary
- **Phase:** A11 — Live Ride, Realtime State & GPS Foundation
- **Status:** Complete & Verified
- **Scope:** Authoritative Realtime WebSocket Infrastructure, Live Ride State Machine, Driver GPS Monotonic Tracking, Passenger Live Tracking Screen with Canvas Map, Exponential Backoff Reconnection, and Authoritative REST Reconciliation.

---

## 1. Files Created

### Core Realtime Infrastructure
1. `app/src/main/java/com/ishara/app/core/realtime/RealtimeClient.kt`
   - Contract interface for realtime streaming, connection states, and bidirectional messaging.
2. `app/src/main/java/com/ishara/app/core/realtime/OkHttpRealtimeClient.kt`
   - Production WebSocket implementation using OkHttp. Enforces authenticated query parameters (`?token=`), sanitized log outputs, ping interval keep-alives (30s), exponential backoff reconnection with jitter (1s, 2s, 4s, 8s, 16s, max 30s), and session invalidation triggers on auth failure (1008, 4401, HTTP 401/403).
3. `app/src/main/java/com/ishara/app/core/realtime/RealtimeConnectionState.kt`
   - Exhaustive sealed hierarchy: `Disconnected`, `Connecting`, `Connected`, `Reconnecting`, and `Failed`.
4. `app/src/main/java/com/ishara/app/core/realtime/RealtimeEvent.kt`
   - Strongly-typed domain event hierarchy for ride lifecycle, tracking snapshots, driver location fixes, route progress, and safe unknown fallbacks.
5. `app/src/main/java/com/ishara/app/core/realtime/RealtimeEventParser.kt`
   - Crash-proof JSON parser supporting both camelCase and snake_case backend payloads, extracting typed events with zero reflection and fallback to `UnknownEvent` / `RawMessage`.

### Domain & Data Layers
6. `app/src/main/java/com/ishara/app/domain/model/RideTrackingModels.kt`
   - Immutable domain models: `RideTrackingSnapshot`, `TrackingRideStatus`, `TrackingState`, `TrackingFreshness`, `TrackingDriverLocation`, `TrackingRouteInfo`, `TrackingEtaInfo`.
7. `app/src/main/java/com/ishara/app/domain/repository/RideTrackingRepository.kt`
   - Repository interface providing snapshot retrieval, driver location queries, subscription management, and connection/event observation flows.
8. `app/src/main/java/com/ishara/app/data/remote/dto/RideTrackingDtos.kt`
   - Backend wire DTOs matching `ishara-backend/src/modules/tracking/` and `src/modules/rides/`.
9. `app/src/main/java/com/ishara/app/data/remote/datasource/RideTrackingRemoteDataSource.kt`
   - REST data source executing `GET /api/v1/rides/:rideId/tracking` and `GET /api/v1/rides/:rideId/driver-location`.
10. `app/src/main/java/com/ishara/app/data/mapper/RideTrackingMapper.kt`
    - Mapping layer transforming backend DTOs into domain models and reconciling realtime update frames into existing snapshots.
11. `app/src/main/java/com/ishara/app/data/repository/RideTrackingRepositoryImpl.kt`
    - Production repository orchestrating REST snapshot queries, role validation (`USER`), WebSocket subscriptions (`RIDE_TRACKING_SUBSCRIBE`), and ride-filtered event flows.

### Use Cases
12. `app/src/main/java/com/ishara/app/domain/usecase/GetRideTrackingUseCase.kt`
    - Retrieves authoritative ride tracking snapshot from backend REST endpoint.
13. `app/src/main/java/com/ishara/app/domain/usecase/GetDriverLocationUseCase.kt`
    - Queries latest driver coordinates and freshness for a specific ride.
14. `app/src/main/java/com/ishara/app/domain/usecase/ObserveRideTrackingUseCase.kt`
    - Subscribes, unsubscribes, and exposes connection state and ride-filtered event flows.

### Passenger UI & Map Visualization
15. `app/src/main/java/com/ishara/app/feature/student/tracking/RideTrackingUiState.kt`
    - Unidirectional state holder (`Loading`, `Content`, `Terminal`, `Error`) with degraded-connection notices and formatted statuses.
16. `app/src/main/java/com/ishara/app/feature/student/tracking/RideTrackingViewModel.kt`
    - Lifecycle-aware ViewModel managing initial hydration, realtime event ingestion, silent reconciliation after reconnect, terminal cleanup, and navigation handoffs.
17. `app/src/main/java/com/ishara/app/feature/student/tracking/RideTrackingScreen.kt`
    - Compose screen featuring status chips, ETA pills, route addresses, stale location banners, and terminal completion screens.
18. `app/src/main/java/com/ishara/app/feature/student/tracking/components/IshaaraTrackingCanvasMap.kt`
    - Lightweight, keyless custom canvas map rendering origin, destination, driver marker with pulse animation, and auto-centering bounds without third-party dependencies or API key exposure.

### Driver GPS & Location Publishing
19. `app/src/main/java/com/ishara/app/feature/driver/tracking/DriverTrackingCoordinator.kt`
    - Driver GPS coordinator enforcing monotonic timestamp progression, physical coordinate validation (`[-90, 90]`, `[-180, 180]`), freshest-only offline buffering (at most 1 fix), and active-trip lifecycle bounds.
20. `app/src/main/java/com/ishara/app/feature/driver/tracking/DriverTrackingStatus.kt`
    - Operational status hierarchy: `Idle`, `Active`, `NetworkUnavailable`, `PermissionRequired`, `Error`.
21. `app/src/main/java/com/ishara/app/feature/driver/tracking/DriverLocationService.kt`
    - Android foreground service with notification channel for compliant background driver location streaming during active trips.

### Test Suites
22. `app/src/test/java/com/ishara/app/RealtimePhaseA11Test.kt`
    - Exhaustive A11 verification test suite covering WebSocket handshake, token sanitization, subscription protocol, event taxonomy, unknown event isolation, REST initial hydration, silent reconnect reconciliation, terminal cleanup, and role security.
23. `app/src/test/java/com/ishara/app/RealtimeEventParserTrackingTest.kt`
    - Event parsing suite verifying all tracking and lifecycle payloads.
24. `app/src/test/java/com/ishara/app/RealtimeEventParserSafetyTest.kt`
    - Event parsing tests ensuring zero crash on malformed payloads.
25. `app/src/test/java/com/ishara/app/RealtimeGpsTelemetryTest.kt`
    - Coordinate validation and telemetry stream verification.
26. `app/src/test/java/com/ishara/app/RideTrackingMapperTest.kt`
    - Domain mapping tests covering snapshot and incremental updates.
27. `app/src/test/java/com/ishara/app/DriverTrackingLifecycleTest.kt`
    - Driver GPS lifecycle tests verifying monotonic checks, freshest-only buffering, and trip start/stop bounds.

### Documentation
28. `ISHAARA_FRONTEND_PHASE_A11_REALTIME.md`
    - Architecture, security, state machine, reconnect, and GPS documentation.
29. `ISHAARA_FRONTEND_PHASE_A11_API_CONTRACT.md`
    - Complete WebSocket and REST contract specification against backend.
30. `ISHAARA_FRONTEND_PHASE_A11_CHANGELOG.md`
    - This file.

---

## 2. Files Modified

1. `app/src/main/java/com/ishara/app/navigation/IshaaraDestinations.kt`
   - Added destination route: `StudentRideTracking("student/ride/{rideId}")`.
2. `app/src/main/java/com/ishara/app/core/di/AppContainer.kt`
   - Registered `rideTrackingRepository`, `getRideTrackingUseCase`, `getDriverLocationUseCase`, `observeRideTrackingUseCase`, `driverTrackingCoordinator`, and `driverLocationService`.
3. `app/src/main/java/com/ishara/app/MainActivity.kt`
   - Wired `StudentScreenState.RideTracking` composable, factory, and route handling.
4. `app/src/main/res/values/strings.xml`
   - Added strings for tracking title, status descriptions, ETA prefixes, distance labels, and error messages.

---

## 3. Realtime Implementation Highlights

- **Protocol & Endpoint:** Standard WebSocket on `wss://<host>/api/v1/rides/realtime?token=<jwt>`.
- **Subscription Protocol:** Client emits `{"type": "RIDE_TRACKING_SUBSCRIBE", "payload": {"rideId": "..."}}` upon entering the live ride screen.
- **Unsubscription:** Client emits `{"type": "RIDE_TRACKING_UNSUBSCRIBE", "payload": {"rideId": "..."}}` upon screen exit, terminal state, or account logout.
- **Reconnection with Backoff:** Reconnection uses exponential delays: 1s, 2s, 4s, 8s, 16s (max 30s) + jitter.
- **Reconciliation:** After reconnection succeeds, `RideTrackingViewModel.refreshAuthoritativeStateSilently()` immediately fetches `GET /api/v1/rides/:rideId/tracking` to heal any missed state transitions.
- **Event Resiliency:** Unknown event types map safely to `RealtimeEvent.UnknownEvent` without throwing or altering ride state.

---

## 4. GPS & Location Implementation Highlights

- **Driver Monotonicity:** Coordinates with `timestampMillis <= lastSentTimestamp` are dropped to prevent out-of-order network updates.
- **Freshest-Only Buffering:** When offline, at most one newest location fix is retained in memory.
- **Physical Bounds:** Rejects coordinates outside latitude `[-90, 90]` or longitude `[-180, 180]`.
- **Lifecycle Bound:** Location publishing only runs during active trips (`tripId != null`) and immediately halts on trip completion or driver logout.
- **Passenger Display:** Passenger views driver marker with freshness indicator (`FRESH` vs `STALE`).

---

## 5. Security & Session Integrity

- **Token Sanitization:** Tokens are stripped from URLs in all logs (`?token=***`).
- **Session Expiry:** WebSocket close codes 1008/4401 or HTTP 401/403 dispatch to `SessionInvalidationCoordinator` to force re-authentication.
- **Role Isolation:** Non-passenger sessions attempting to track rides fail with `IshaaraError.Forbidden`.
- **Account Switching:** Active sockets close (code 1000) and scopes cancel on logout or user switch.

---

## 6. Build & Test Verification

- **A11 Test Suite:** 8/8 Passed (`com.ishara.app.RealtimePhaseA11Test`).
- **A10 Regression:** Passed (`com.ishara.app.RideRequestPhaseA10Test`).
- **A09 Regression:** Passed (`com.ishara.app.DiscoveryPhaseA09Test`).
- **A03 Location Regression:** Passed (`com.ishara.app.UserPhaseA03Test`).
- **Driver Tracking Lifecycle Suite:** Passed (`com.ishara.app.DriverTrackingLifecycleTest`).
- **Full Unit Test Suite:** 582/582 Passed across 52 test suites (`./gradlew testDebugUnitTest`).
- **Debug APK Build:** Successfully assembled (`./gradlew assembleDebug`).

---

## 7. Phase Boundary Compliance & Deferred Work

- **No Fare / Pricing Logic:** Deferred to Phase A12 (`student/ride/{rideId}/fare`).
- **No Payment Processing:** Deferred to Phase A13 (`student/ride/{rideId}/payment`).
- **No Safety / Ratings Implementation:** SOS routes to `student/safety/{rideId}` (Phase A14).
- **No Notifications Implementation:** Deferred to Phase A15.
- **Handoff:** Concludes at active ride realtime tracking and terminal state (`COMPLETED` / `CANCELLED`).
