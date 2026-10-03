# ISHAARA FRONTEND — PHASE A08: TRIP & DISPATCH OPERATIONAL FOUNDATION

## Executive Summary
Phase A08 implements the **Driver-Side Trip & Dispatch Operational Foundation** for the ISHAARA Android mobility platform (`/home/dev/Desktop/ishara-frontend`).
It builds on:
- Phase A01 (API Client & Core Network Contracts)
- Phase A02 (Authentication, Role Routing & Session Management)
- Phase A04 (Driver Onboarding & License Verification)
- Phase A05 (Agency Membership & Employment Context)
- Phase A06 (Driver Operational Readiness & Telemetry)
- Phase A07 (Vehicle Registration & Driver–Vehicle Assignment)

Phase A08 establishes the operational foundation for an authenticated `DRIVER_CONDUCTOR` to inspect authoritative trip assignments, review trip routes and vehicle allocations, transition through backend-authoritative lifecycle actions (`start`, `complete`, `cancel`), and create driver-dispatched trips, all while strictly adhering to backend concurrency rules and domain isolation.

---

## 1. Domain Separation & Architecture

### 1.1 Strict Separation of Concerns
1. **Authentication**: Handled via `AuthRepository` (A02). Validates identity and Bearer tokens.
2. **Authorization**: Governed strictly by backend RBAC (`DRIVER_CONDUCTOR` vs `USER`). Backend yields `403 Forbidden` if unauthorized.
3. **Verification**: Handled via `DriverRepository` (A04). Verifies government license credentials.
4. **Agency Membership**: Handled via `AgencyRepository` (A05). Validates organizational affiliation.
5. **Operational Readiness**: Handled via `DriverReadinessRepository` (A06). Validates driver/vehicle operational green-light.
6. **Vehicle Assignment**: Handled via `DriverVehicleRepository` (A07). Authoritative source for vehicle binding (`DriverVehicleAssignment`).
7. **Trip & Dispatch**: Handled via `DriverTripRepository` (A08). Authoritative representation of scheduled/active/completed trips and driver operational executions.
8. **Passenger Booking & Ride Request**: **STRICTLY DEFERRED TO A09/A10**. No passenger discovery, booking, ride requests, or passenger lifecycle logic is present in A08.
9. **Realtime / Telemetry**: **STRICTLY DEFERRED TO A11**. No WebSockets, live GPS streaming, or polling daemons are introduced.

### 1.2 Dispatch Domain Findings
Forensic audit of `/home/dev/Desktop/ishara-backend/src/modules/trips/` confirmed:
- **No standalone `/dispatch` API exists** in the backend.
- Dispatch is modeled entirely through `Trip` entity fields (`driverId`, `vehicleId`, `agencyId`, `status`).
- Agency operators or system dispatch directly create or assign trips with status `ASSIGNED`.
- Individual drivers can self-dispatch by creating a trip via `POST /api/v1/trips`, where the backend validates vehicle ownership/assignment and defaults initial status to `SCHEDULED`.
- Hence, the frontend implements `DriverTripRepository` and does **not** create a redundant or fabricated `DispatchRepository`.

---

## 2. Authoritative Backend State Machine

### 2.1 Reconstructed Backend State Machine
The backend Prisma schema and `trips.service.ts` enforce seven authoritative enum states:
```
                      ┌──────────────────────┐
                      │       CREATED        │
                      └──────────┬───────────┘
                                 │
                      ┌──────────▼───────────┐
                      │      SCHEDULED       │
                      └──────────┬───────────┘
                                 │
                      ┌──────────▼───────────┐
                      │       ASSIGNED       │
                      └──────────┬───────────┘
                                 │
                      ┌──────────▼───────────┐
                      │        READY         │
                      └──────────┬───────────┘
                                 │ (POST /trips/:id/start)
                      ┌──────────▼───────────┐
                      │        ACTIVE        │
                      └─────┬──────────┬─────┘
                            │          │
 (POST /trips/:id/complete) │          │ (POST /trips/:id/cancel)
                            │          │
                 ┌──────────▼───┐  ┌───▼──────────┐
                 │  COMPLETED   │  │  CANCELLED   │
                 └──────────────┘  └──────────────┘
```

### 2.2 Legal Transitions & Actor Rules
- **Start Trip** (`POST /api/v1/trips/:id/start`):
  - **Legal Prior States**: `READY`, `SCHEDULED`, `ASSIGNED`.
  - **Resulting State**: `ACTIVE`. Sets `actualDeparture` timestamp.
  - **Actor**: Must be the assigned driver (`DRIVER_CONDUCTOR`).
  - **Concurrency Invariant**: Backend enforces a partial unique index `DRIVER_HAS_ACTIVE_TRIP` and `VEHICLE_HAS_ACTIVE_TRIP`. Attempting to start a second trip while one is `ACTIVE` fails with `409 Conflict`.
- **Complete Trip** (`POST /api/v1/trips/:id/complete`):
  - **Legal Prior State**: `ACTIVE` only.
  - **Resulting State**: `COMPLETED`. Sets `actualArrival` timestamp.
  - **Actor**: Assigned driver or authorized agency operator/admin.
- **Cancel Trip** (`POST /api/v1/trips/:id/cancel`):
  - **Legal Prior States**: Any state prior to `COMPLETED`.
  - **Resulting State**: `CANCELLED`. Requires `reason` in request payload.
  - **Actor**: Assigned driver, agency operator, or admin.
- **No Client-Side Optimistic Mutation**:
  - The UI does not prematurely update local state to `ACTIVE` or `COMPLETED`. Every transition issues an API call and triggers an authoritative refetch (`fetchTripDetails(tripId)` and `loadDriverTrips()`).

---

## 3. Architecture & Package Structure

```
com.ishara.app/
├── data/
│   ├── mapper/
│   │   └── TripMapper.kt                          <- Pure bidirectional DTO <-> Domain transformations
│   ├── remote/
│   │   ├── datasource/
│   │   │   └── TripRemoteDataSource.kt             <- Bracket-depth JSON parser for all 6 trip endpoints
│   │   └── dto/
│   │       └── TripDtos.kt                        <- CleanTripResponseDto, CreateTripRequestDto, etc.
│   └── repository/
│       └── DriverTripRepositoryImpl.kt            <- Mutex-safe, flow-driven, role-guarded repository
├── domain/
│   ├── model/
│   │   └── Trip.kt                                <- Immutable domain models (Trip, TripStatus, TripRoute, etc.)
│   ├── repository/
│   │   └── DriverTripRepository.kt                <- Repository interface
│   └── usecase/
│       └── DriverTripUseCases.kt                  <- 9 Granular use cases for list, detail, start, complete, cancel, etc.
├── feature/
│   └── driver/
│       ├── trip/
│       │   ├── DriverTripUiState.kt               <- Unidirectional UI state models (TripListUiState, TripDetailUiState)
│       │   ├── DriverTripListViewModel.kt         <- List ViewModel with tab filtering and trip creation
│       │   ├── DriverTripDetailViewModel.kt       <- Detail ViewModel with anti-double-click action locks
│       │   └── ui/
│       │       ├── DriverTripListScreen.kt        <- Compose screen with Active banner, tabs, create dialog
│       │       └── DriverTripDetailScreen.kt      <- Compose screen with route specs, vehicle details, action CTAs
```

---

## 4. Screens & ViewModels

### 4.1 DriverTripListScreen & DriverTripListViewModel
- **Navigation Route**: `driver/trips`
- **Tabs**:
  - `ALL`: Complete history of driver trips.
  - `ACTIVE`: Highlights current `ACTIVE` trip (with a dedicated high-priority operational banner).
  - `UPCOMING`: Scheduled and assigned trips (`CREATED`, `SCHEDULED`, `ASSIGNED`, `READY`).
  - `COMPLETED`: Completed and cancelled trips.
- **Trip Creation**: Includes dialog allowing drivers to create a trip with origin address, destination address, departure time, and vehicle ID.
- **State Handling**: Explicit visual handling for Loading, Empty ("No trips found for this filter"), Error with retry button, and Action In-Flight indicator.

### 4.2 DriverTripDetailScreen & DriverTripDetailViewModel
- **Navigation Route**: `driver/trips/{tripId}`
- **Authoritative Display**:
  - Displays Trip ID, Route Name, Origin & Destination with GeoJSON coordinates.
  - Scheduled vs Actual Departure & Arrival timestamps.
  - Assigned Vehicle ID and Agency ID.
  - Status pill with color-coded chip based on authoritative `TripStatus`.
- **Operational Action Controls**:
  - **Start Trip**: Enabled for `READY`, `SCHEDULED`, or `ASSIGNED` trips.
  - **Complete Trip**: Enabled only when trip is `ACTIVE`.
  - **Cancel Trip**: Displays an alert dialog prompting for a cancellation reason, enabled for non-terminal states.
  - **Anti-Double-Click Guard**: In-flight requests lock UI action buttons (`isActionInProgress = true`), rejecting duplicate taps.

### 4.3 DriverHomeScreen Integration
- `DriverHomeScreen` features an authoritative "Active Trip" card directly on the driver dashboard.
- Displays origin, destination, vehicle, and status chip with direct CTA button "View Active Trip" linking to `driver/trips/{tripId}`.
- If no trip is active, displays an "Operational Trips" card with CTA "View All Assigned Trips".

---

## 5. Security & Authorization

1. **Role Enforcement**:
   - `DriverTripRepositoryImpl` checks `authRepository.getCurrentUser()?.role == UserRole.DRIVER_CONDUCTOR`.
   - Access attempts by passengers (`USER` role) throw `IshaaraError.Forbidden("Access restricted to DRIVER_CONDUCTOR role")`.
2. **Account Switching & Session Invalidation**:
   - `DriverTripRepositoryImpl.clearDriverTripState()` immediately purges in-memory caches (`_driverTripsFlow.value = emptyList()`, `_activeTripFlow.value = null`).
   - Wired into `MainActivity.handleSignOut()` and role switching. Old trip data never leaks into a subsequent driver or passenger session.
3. **No PII or Secret Logging**:
   - Bearer tokens are injected strictly via `AuthRepository.getAuthHeader()` and omitted from error logs.
   - Trip IDs are not treated as credentials; the backend verifies authorization for every query.

---

## 6. Cache Policy
- **Authoritative State**: Trip status, active trip assignments, and transition confirmations are treated as strictly authoritative.
- **Authoritative Invalidation**: Mutating operations (`start`, `complete`, `cancel`, `create`) immediately fetch updated states from the backend.
- **Offline / Stale Guard**: Caches are never used as an authorization authority. If the network is unavailable, cached trips may be displayed with an informational indicator, but action buttons require network validation.

---

## 7. Backend Contract Discrepancies

### Discrepancy 1: Trip Accept / Reject Endpoints
- **Documented / Assumed Behavior**: Older documentation referenced `POST /api/v1/trips/:id/accept` and `POST /api/v1/trips/:id/reject`.
- **Actual Backend Behavior**: Backend `trips.controller.ts` exposes only `start`, `complete`, and `cancel`. Drivers are assigned directly by agency dispatch or self-creation; there is no intermediate acceptance handshake.
- **Frontend Decision**: Do not fabricate accept/reject APIs or mock endpoints. Respect backend state flow directly from `ASSIGNED`/`READY` to `ACTIVE` via `start`.
- **Impact**: Clean, unbloated architecture matching authoritative backend implementation.

### Discrepancy 2: Standalone Dispatch API
- **Documented / Assumed Behavior**: Conceptual prompts suggested a possible separate `/dispatch` domain or `DispatchRepository`.
- **Actual Backend Behavior**: Dispatch is an attribute of the `Trip` entity (`driverId`, `vehicleId`, `agencyId`, `status`).
- **Frontend Decision**: Centralize all trip dispatch operations in `DriverTripRepository`.
- **Impact**: Avoids redundant abstraction layers.

### Discrepancy 3: GeoJSON Coordinate Ordering
- **Documented / Assumed Behavior**: Standard coordinate order is often written `[latitude, longitude]`.
- **Actual Backend Behavior**: Backend schema adheres to RFC 7946 GeoJSON format: `coordinates: [longitude, latitude]`.
- **Frontend Decision**: `TripMapper` explicitly maps `TripLocation.longitude` to `coordinates[0]` and `TripLocation.latitude` to `coordinates[1]`.
- **Impact**: Prevents inverted coordinates when querying or submitting trip locations to backend GeoJSON services.

---

## 8. Verification & Testing

- **Dedicated Test Suite**: `app/src/test/java/com/ishara/app/DriverTripPhaseA08Test.kt`
- **42 Unit Tests**:
  - API endpoint paths and HTTP methods (`GET /api/v1/drivers/me/trips`, `POST /api/v1/trips/:id/start`, etc.)
  - Bearer token header propagation
  - Authorization guards (passenger role denied, driver role allowed)
  - All 7 enum states correctly mapped and represented in UI
  - State machine lifecycle transitions (`start` -> `ACTIVE`, `complete` -> `COMPLETED`, `cancel` -> `CANCELLED`)
  - Error responses (401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict / already active trip, 500 Internal Error, Timeout, Offline)
  - Anti-duplicate click lock verification
  - Account switching and logout cache flushing
  - ViewModel UI state emissions
- **Regression Suite**: 100% passing across all 543+ tests in the codebase.
- **Build Status**: `./gradlew assembleDebug` builds successfully without warnings.

---

## 9. Deferred to Future Phases
- **A09**: Passenger Trip Discovery UI, search, filtering, and route schedules.
- **A10**: Passenger Ride Request lifecycle (booking, acceptance, passenger cancellation).
- **A11**: Realtime WebSockets, live GPS driver tracking, trip location streaming.
- **A12**: Fare computation and dynamic pricing.
- **A13**: Payment processing and wallet settlements.
