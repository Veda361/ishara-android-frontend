# ISHAARA FRONTEND — PHASE A07: VEHICLE REGISTRATION & DRIVER–VEHICLE ASSIGNMENT

## 1. Objective
Phase A07 establishes the production-grade frontend architecture for vehicle information and driver-vehicle assignment on the ISHAARA Android mobility platform. It strictly reflects the authoritative backend data model while preserving clear domain separation between Driver identity, Agency membership, Operational readiness (A06), Vehicle assets, Driver–Vehicle assignments, and future Trip dispatch (A08).

---

## 2. Backend Vehicle Audit
- **Source Module**: `/src/modules/vehicles/`
- **Controller**: `vehicle.controller.ts`
- **Service**: `vehicle.service.ts`
- **Model**: `vehicle.model.ts` (`VehicleModel`)
- **Key Fields**:
  - `id`: MongoDB ObjectId string
  - `agencyId`: Optional string reference to `Agency`
  - `operatorId`: Optional string reference to `BusOperator`
  - `assignedDriverId`: Denormalized string reference to `DriverProfile`
  - `registrationNumber`: Unique uppercase alphanumeric plate string (normalized spaces and hyphens removed, e.g. "UP32AB1234")
  - `vehicleType`: Enum (`AUTO`, `E_RICKSHAW`, `CAB`, `BUS`, `CAR`, `BIKE`, `OTHER`)
  - `make`: Vehicle manufacturer/brand (1–50 characters)
  - `model`: Vehicle model name (1–50 characters)
  - `capacity`: Optional positive integer seat count (1–200)
  - `ownershipType`: Enum (`INDIVIDUAL`, `AGENCY`, `OPERATOR`)
  - `isVerified`: Platform verification flag (boolean, server-controlled)
  - `isActive`: Platform operational enablement flag (boolean)
  - `createdAt`, `updatedAt`: ISO-8601 timestamps

---

## 3. Backend Assignment Audit
- **Model**: `assignment.model.ts` (`DriverVehicleAssignmentModel`)
- **Service**: `vehicle-assignment.service.ts`
- **Authoritative Relationship**: The backend explicitly treats `DriverVehicleAssignment` as the single authoritative relationship between driver and vehicle. `vehicle.driverId` is maintained solely as a denormalized database index cache.
- **Key Fields**:
  - `id`: MongoDB ObjectId string
  - `driverId`: Reference to `DriverProfile`
  - `vehicleId`: Reference to `Vehicle`
  - `agencyId`: Optional agency boundary
  - `status`: Enum (`ACTIVE`, `ENDED`)
  - `assignedAt`: Timestamp of assignment start
  - `unassignedAt`: Timestamp of assignment termination or `null`
  - `assignedBy`: User ID of assignment creator
  - `assignedByRole`: Role of actor (`AGENCY_OWNER`, `ADMIN`, `DRIVER`)
  - `unassignedBy`: User ID of actor who unassigned
  - `unassignedByRole`: Role of unassignment actor
  - `reason`: Audit reason string or `null`
  - `vehicle`: Nested sanitized vehicle document (optional)

---

## 4. Vehicle Domain Model
The frontend defines clean immutable domain models decoupled from network DTOs:
- `Vehicle`:
  - `id: String`
  - `agencyId: String?`
  - `operatorId: String?`
  - `assignedDriverId: String?`
  - `registrationNumber: String`
  - `vehicleType: VehicleType`
  - `make: String`
  - `model: String`
  - `capacity: Int?`
  - `ownershipType: VehicleOwnershipType`
  - `isVerified: Boolean`
  - `isActive: Boolean`
  - `createdAt: String?`, `updatedAt: String?`
  - `displayTitle`: Computed helper (`"$make $model"`)
  - `formattedCapacity`: Computed helper (`"$capacity seats"`)

---

## 5. Assignment Domain Model
- `DriverVehicleAssignment`:
  - `id: String`
  - `driverId: String`
  - `vehicleId: String`
  - `agencyId: String?`
  - `status: VehicleAssignmentStatus` (`ACTIVE`, `ENDED`)
  - `assignedAt: String`
  - `unassignedAt: String?`
  - `assignedBy: String`
  - `assignedByRole: VehicleAssignmentActorRole` (`AGENCY_OWNER`, `ADMIN`, `DRIVER`)
  - `unassignedBy: String?`
  - `unassignedByRole: VehicleAssignmentActorRole?`
  - `reason: String?`
  - `vehicle: Vehicle?`
- `AssignedVehicleState`:
  - `vehicle: Vehicle?`
  - `assignment: DriverVehicleAssignment?`
  - `hasActiveAssignment`: Computed helper (`assignment?.status == ACTIVE && vehicle != null && vehicle.isActive`)

---

## 6. Authorization Model
- **Driver Role**: Restricted strictly to `DRIVER_CONDUCTOR`.
- **USER Role Blocked**: Passengers cannot access vehicle assignment endpoints, repositories, or UI screens.
- **Agency Fleet Boundary**: Drivers cannot create, edit, or delete Agency fleet vehicles. Agency fleet assignment is performed exclusively by `AGENCY_OWNER` or `ADMIN`.
- **Individual Drivers**: Drivers operating individually can register their own vehicle (`POST /api/v1/vehicles`), assign themselves to their registered vehicle (`POST /api/v1/vehicles/:id/assignments`), and terminate their assignment (`POST /api/v1/vehicles/:id/unassign`).
- **No Client Authority**: The frontend never self-assigns or creates local assignment truth.

---

## 7. Driver UI
- **`DriverVehicleScreen`**:
  - **Top Bar**: Back navigation to Driver Home, title, and explicit refresh button.
  - **Active Assigned Vehicle Card**: Displays registration number, make/model, platform verification badge, active assignment status badge, ownership type, capacity, assigned timestamp, and assigned-by role.
  - **No Assigned Vehicle Card**: Clean empty state explaining operational impact with CTA to register individual vehicle.
  - **Registered Vehicles List**: Shows driver's individually registered vehicles with one-tap self-assignment CTA.
  - **Assignment Audit History**: Chronological audit trail of past assignments, end timestamps, and reasons.
  - **Register Vehicle Dialog**: Validated modal allowing plate, make, model, type, and capacity entry.
  - **Banner Messaging**: Top-level danger/success banners with dismiss actions.
- **`DriverHomeScreen` Integration**:
  - `DriverVehicleCard` upgraded with interactive navigation ("View Vehicle Details" or "Manage Vehicle Assignment").
- **`DriverOperationalReadinessScreen` Integration**:
  - `VehicleTelemetryCard` updated with "View Vehicle Assignment" CTA linking to `DriverVehicleScreen`.

---

## 8. Navigation
- Route defined: `IshaaraDestination.DriverVehicle ("driver/vehicle")` in `IshaaraDestinations.kt`.
- Screen state: `DriverScreenState.Vehicle` in `MainActivity.kt`.
- Guard: Only reachable when authenticated user role is `DRIVER_CONDUCTOR`. Non-driver attempts are blocked at the container boundary.

---

## 9. A06 Operational Readiness Integration
- Domain separation maintained:
  - Phase A07 owns `Vehicle` and `DriverVehicleAssignment`.
  - Phase A06 owns `DriverOperationalReadiness`.
- Integration pattern: When vehicle assignment changes (assignment or unassignment), the frontend use-case layer notifies `DriverReadinessRepository` to refresh authoritative readiness from the backend.
- Rule: Vehicle assignment does **not** grant client-side operational readiness. The backend readiness gate remains authoritative.

---

## 10. API Integration
- `GET /api/v1/vehicles/me/assigned` (alias `GET /api/v1/drivers/me/vehicle`): Retrieves currently assigned vehicle and active assignment record.
- `GET /api/v1/vehicles`: Lists vehicles owned by the authenticated driver.
- `GET /api/v1/vehicles/:vehicleId`: Retrieves details of driver-owned vehicle.
- `POST /api/v1/vehicles`: Registers an individual vehicle under driver's profile.
- `POST /api/v1/vehicles/:vehicleId/assignments`: Assigns driver to owned vehicle.
- `POST /api/v1/vehicles/:vehicleId/unassign`: Safely terminates assignment.
- `GET /api/v1/vehicles/:vehicleId/assignments`: Retrieves assignment audit history.

---

## 11. Error Handling
- `401 Unauthorized` -> Centralized session expiration handling (`IshaaraError.Authentication`).
- `403 Forbidden` -> Access blocked with human-readable error (`IshaaraError.Forbidden`).
- `404 Not Found` -> Vehicle or driver profile not found (`IshaaraError.NotFound`).
- `409 Conflict` -> Plate already exists or driver/vehicle already assigned (`IshaaraError.Conflict`).
- `400 / 422 Validation` -> Input validation error (`IshaaraError.Validation`).
- `429 Rate Limited` -> Throttled requests with retry guidance (`IshaaraError.RateLimited`).
- `5xx Server` -> Server outage message (`IshaaraError.Server`).
- `Timeout / Offline` -> Network error state with retry (`IshaaraError.Timeout`, `IshaaraError.Network`).

---

## 12. Security
- Zero hardcoded vehicle IDs or assignment IDs.
- Zero admin credentials in driver mobile app (never transmits `x-admin-key`).
- Bearer JWT token transmitted exclusively in HTTP `Authorization` headers.
- Zero token or sensitive credential logging in application logs or domain models.
- Concurrency mutex prevents duplicate parallel mutation requests.

---

## 13. Cache Policy
- In-memory caching via `StateFlow` in `DriverVehicleRepositoryImpl`.
- Cached vehicle data is non-authoritative and used solely for transient UI display.
- Cached vehicle never bypasses readiness checks or grants local dispatch authority.
- `clearVehicleState()` immediately invalidates all cached vehicle and assignment flows.

---

## 14. Account Switching & Isolation
- On driver logout: `clearVehicleState()` is invoked.
- Driver A -> Logout -> Driver B: Driver B never observes Driver A's assigned or registered vehicles.
- Driver -> Logout -> Passenger (USER): Passenger cannot access driver vehicle state or routes.

---

## 15. Tests
- Test Suite: `DriverVehiclePhaseA07Test.kt`
- Total Test Cases: 40 tests (all passing).
- Coverage:
  - Vehicle loading & empty states (tests 1–6)
  - Assignment loading & external change propagation (tests 7–11)
  - Authorization & role guarding (tests 12–15)
  - Error mapping for 401, 403, 404, 409, 422, 429, 5xx, timeout, offline (tests 16–24)
  - Account switching and data isolation (tests 25–27)
  - Readiness integration and non-authoritative caching (tests 28–30)
  - Security invariants (tests 31–35)
  - Driver mutations: registration, self-assignment, unassignment, audit history (tests 36–40)

---

## 16. Manual Verification Matrix
| Case | Scenario | Expected Outcome | Result |
|---|---|---|---|
| Case 1 | Driver with assigned vehicle | UI displays active vehicle card with plate, make, model, capacity, and assignment timestamp | Verified |
| Case 2 | Driver without vehicle | UI displays "No vehicle currently assigned" with register CTA | Verified |
| Case 3 | External assignment by agency | Pull-to-refresh queries backend; assigned vehicle appears | Verified |
| Case 4 | External unassignment by agency | Pull-to-refresh removes vehicle card; reverts to empty state | Verified |
| Case 5 | Driver account switch (A -> B) | Driver A vehicle state cleared on signout; Driver B loads fresh state | Verified |
| Case 6 | Driver -> USER account switch | Driver vehicle state cleared; USER cannot access driver vehicle route | Verified |
| Case 7 | USER opens driver/vehicle | Blocked at container boundary with 403 / redirect | Verified |
| Case 8 | Network failure during fetch | Shows friendly network error banner with retry option | Verified |

---

## 17. Build Status
- `compileDebugKotlin`: Passed
- `compileDebugUnitTestKotlin`: Passed
- `testDebugUnitTest --tests com.ishara.app.DriverVehiclePhaseA07Test`: Passed (40/40 tests)
- `testDebugUnitTest` (Full Regression Suite): Passed (47 test suites, 0 failures, 0 errors)
- `assembleDebug`: Passed (`BUILD SUCCESSFUL in 27s`)

---

## 18. Known Limitations
- Real-time WebSocket assignment push (`DRIVER_VEHICLE_ASSIGNED`, `DRIVER_VEHICLE_UNASSIGNED`) is ingested via HTTP refresh in Phase A07. Full realtime subscriber dispatch is deferred to Phase A11.

---

## 19. Backend Discrepancies Documented
- **Documented Contract (`api_final_flow.md`)**:
  Listed `POST /api/v1/vehicles` with payload field `"type": "BUS"`.
- **Actual Backend Implementation (`vehicle.schema.ts`, `vehicle.types.ts`)**:
  Requires `"vehicleType"` (`AUTO`, `E_RICKSHAW`, `CAB`, `BUS`, `CAR`, `BIKE`, `OTHER`) and requires `"make"` and `"model"` as non-empty strings.
- **Frontend Resolution**: Adhered strictly to actual backend schema (`vehicleType`, `make`, `model`, `registrationNumber`, `capacity`).

---

## 20. Deferred A08+ Functionality
- Trip creation, dispatch, and lifecycle (Phase A08).
- Passenger discovery and route browsing (Phase A09).
- Ride request acceptance/rejection (Phase A10).
- Live vehicle GPS telemetry and map tracking (Phase A11).
- Agency web dashboard and operator fleet management (Phase A17).

---

## 21. Production Readiness Assessment
Phase A07 is complete, verified against backend code, regression-tested with zero regressions, and fully production-ready.
