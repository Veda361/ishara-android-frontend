# ISHAARA FRONTEND — PHASE A06: DRIVER OPERATIONAL READINESS & SHIFT FOUNDATION

## 1. Objective

Phase A06 implements the production-grade `DRIVER_CONDUCTOR` operational readiness foundation for the ISHAARA Android application. Its primary architectural goal is to authoritatively answer:

> **"Is this driver currently eligible and allowed to enter operational mode?"**

This phase maintains absolute domain separation across the platform lifecycle:
```
AUTHENTICATION
    ↓
ROLE (DRIVER_CONDUCTOR)
    ↓
DRIVER PROFILE (A04)
    ↓
DRIVER VERIFICATION (A04)
    ↓
AGENCY MEMBERSHIP (A05)
    ↓
OPERATIONAL READINESS (A06) ← CURRENT PHASE
    ↓
VEHICLE ASSIGNMENT (A07 — DEFERRED)
    ↓
TRIP / DISPATCH (A08+ — DEFERRED)
```

**Critical Invariant**:
- Authenticated ≠ Verified
- Verified ≠ Agency Member
- Agency Member ≠ Operationally Ready
- Operationally Ready ≠ Vehicle Assigned
- Operationally Ready ≠ Currently on a Ride

The frontend consumes authoritative backend readiness calculations (`GET /api/v1/drivers/me/readiness`) rather than duplicating or guessing business rules.

---

## 2. Backend Readiness Audit

A forensic audit of the actual backend implementation at `/home/dev/Desktop/ishara-backend` revealed:

1. **Routes (`driver.routes.ts`)**:
   - `GET /api/v1/drivers/me/readiness` (alias `GET /api/v1/drivers/me/operational-readiness`)
   - `POST /api/v1/drivers/me/status/online` (alias `POST /api/v1/drivers/me/online`)
   - `POST /api/v1/drivers/me/status/offline` (alias `POST /api/v1/drivers/me/offline`)
   - `GET /api/v1/drivers/me/operations/context` (alias `GET /api/v1/drivers/me/operational-context`)
2. **Service Logic (`driver-operations.service.ts`)**:
   - Function: `evaluateDriverOperationalReadiness(driverIdOrUserId)`
   - Rules for `status = "READY"` and `authorized = true`:
     1. `DriverProfile` exists.
     2. Driver is not suspended (`isSuspended === false`).
     3. Driver platform KYC verification is approved (`verificationStatus === VERIFIED`).
     4. Driver profile is complete (`licenseNumber` present and non-empty).
     5. If `operatingType === "AGENCY"`, driver has an `APPROVED` `AgencyMembership`. If `operatingType === "INDIVIDUAL"`, no agency membership is required.
   - **Architectural Decision on Vehicle Assignment (Section 34 Prevention of Circular Deadlocks)**:
     - Vehicle assignment is resolved and returned as **informational telemetry** (`requirements.vehicleAssigned`, `activeVehicle`).
     - It is deliberately **NOT** a gate precondition for `READY` status, preventing circular deadlocks between vehicle assignment prerequisites and operational authorization.
3. **Availability Transitions (`driver.service.ts`)**:
   - `setDriverOnline(userId)` calls `evaluateDriverOperationalReadiness(profile._id)`:
     - If not authorized, rejects with `403 Forbidden` (`DRIVER_NOT_VERIFIED`, `DRIVER_OPERATIONAL_SUSPENDED`, or `DRIVER_NOT_OPERATIONAL_READY`).
     - If already `ONLINE`, operation is idempotent.
   - `setDriverOffline(userId)`:
     - Rejects with `400 BadRequest` (`INVALID_DRIVER_STATUS_TRANSITION`) if `status === ON_RIDE`.
     - Idempotent if already `OFFLINE`.

---

## 3. Exact Readiness API Contract

### Authoritative Readiness Check
- **Endpoint**: `GET /api/v1/drivers/me/readiness` (alias `GET /api/v1/drivers/me/operational-readiness`)
- **Auth**: `Bearer <token>` (Better Auth Session)
- **Role**: `DRIVER_CONDUCTOR`
- **Response `200 OK`**:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "driverId": "65f1234567890123456789ab",
    "userId": "65f1234567890123456789aa",
    "authorized": true,
    "status": "READY",
    "reasons": [],
    "requirements": {
      "platformVerification": true,
      "agencyMembership": true,
      "profileComplete": true,
      "notSuspended": true,
      "vehicleAssigned": true
    },
    "operatingType": "INDIVIDUAL",
    "agency": null,
    "activeVehicle": {
      "id": "65f1234567890123456789ac",
      "registrationNumber": "DL01AB1234",
      "make": "Tata",
      "model": "Tigor EV"
    }
  },
  "message": "Driver operational readiness evaluated successfully."
}
```
- **Error Codes**:
  - `401 Unauthorized`: Session missing or expired.
  - `403 Forbidden` (`FORBIDDEN`): Role is `USER` or unprivileged.
  - `404 Not Found` (`DRIVER_PROFILE_NOT_FOUND`): Driver profile does not exist yet.

---

## 4. Readiness Domain Model

Created in [DriverReadinessModels.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/model/DriverReadinessModels.kt):

```kotlin
enum class DriverReadinessStatus {
    READY,
    NOT_READY,
    SUSPENDED
}

enum class DriverReadinessReason {
    PLATFORM_VERIFICATION_PENDING,
    PLATFORM_VERIFICATION_REJECTED,
    AGENCY_MEMBERSHIP_REQUIRED,
    AGENCY_MEMBERSHIP_PENDING,
    AGENCY_MEMBERSHIP_REJECTED,
    DRIVER_SUSPENDED,
    PROFILE_INCOMPLETE,
    VEHICLE_NOT_ASSIGNED,
    UNKNOWN
}

data class DriverReadinessRequirements(
    val platformVerification: Boolean,
    val agencyMembership: Boolean,
    val profileComplete: Boolean,
    val notSuspended: Boolean,
    val vehicleAssigned: Boolean
)

data class DriverOperationalReadiness(
    val driverId: String,
    val userId: String,
    val authorized: Boolean,
    val status: DriverReadinessStatus,
    val reasons: List<DriverReadinessReason>,
    val requirements: DriverReadinessRequirements,
    val operatingType: String,
    val agency: DriverReadinessAgency?,
    val activeVehicle: DriverReadinessVehicle?
) {
    val isReady: Boolean get() = status == DriverReadinessStatus.READY && authorized
    val isSuspended: Boolean get() = status == DriverReadinessStatus.SUSPENDED || !requirements.notSuspended
}
```

---

## 5. Readiness States

The UI explicitly maps the backend model into six deterministic stages:
1. `Loading`: Evaluating platform readiness.
2. `Ready`: Authorized to operate (`status == READY && authorized == true`).
3. `NotReady`: Pending requirements (`status == NOT_READY`), with actionable blocker items.
4. `Suspended`: Account privileges administratively restricted (`status == SUSPENDED`).
5. `Error`: Backend error, non-retryable 403, or invalid state.
6. `Offline`: Network unreachable.

---

## 6. Blocker Mapping

Mapped authoritatively via [DriverReadinessMapper.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/mapper/DriverReadinessMapper.kt):

| Backend Reason Code | Action Type | User-Facing Action |
| :--- | :--- | :--- |
| `PLATFORM_VERIFICATION_PENDING` | `NAVIGATE_VERIFICATION` | Routes to Phase A04 Verification Screen |
| `PLATFORM_VERIFICATION_REJECTED` | `NAVIGATE_VERIFICATION` | Routes to Phase A04 Verification Screen to inspect notes & resubmit |
| `AGENCY_MEMBERSHIP_REQUIRED` | `NAVIGATE_AGENCY` | Routes to Phase A05 Agency Affiliation Screen |
| `AGENCY_MEMBERSHIP_PENDING` | `NAVIGATE_AGENCY` | Routes to Phase A05 Agency Affiliation Screen to monitor request |
| `AGENCY_MEMBERSHIP_REJECTED` | `NAVIGATE_AGENCY` | Routes to Phase A05 Agency Affiliation Screen to select new agency |
| `DRIVER_SUSPENDED` | `ADMIN_RESTRICTED` | Shows platform restriction notice; operational actions disabled |
| `PROFILE_INCOMPLETE` | `COMPLETE_PROFILE` | Shows profile completeness requirement (license number required) |
| `VEHICLE_NOT_ASSIGNED` | `VEHICLE_DEFERRED` | Displays informational telemetry notice; handled under Phase A07 |

---

## 7. Driver Home Integration

- Integrated an **Operational Readiness Gate Card** into [DriverHomeScreen.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/driver/ui/DriverHomeScreen.kt).
- Displays readiness eligibility status and provides a "Check Eligibility" button navigating to the dedicated readiness screen.
- Enhanced availability control to handle backend readiness rejection codes (`DRIVER_NOT_VERIFIED`, `DRIVER_OPERATIONAL_SUSPENDED`, `DRIVER_NOT_OPERATIONAL_READY`).

---

## 8. Operational Mode Integration

- **Readiness vs Shift Separation**:
  - READINESS: "Is this driver authorized to operate?" (`GET /api/v1/drivers/me/readiness`).
  - SHIFT / ONLINE STATE: "Is this driver currently choosing to operate?" (`POST /api/v1/drivers/me/status/online` and `/offline`).
- **Safety**:
  - `goOnline()` enforces `isActionInProgress` to prevent duplicate parallel taps.
  - Server-side validation rejects unauthorized attempts with 403 Forbidden.
  - Offline transition is rejected if the driver is actively on a ride (`ON_RIDE`).

---

## 9. Navigation

- Destination: `IshaaraDestination.DriverReadiness` (`"driver/readiness"`).
- Container State: `DriverScreenState.Readiness`.
- Role Gate: Enforced by `DriverContainerScreen` in [MainActivity.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt) and `ResolveApplicationDestinationUseCase`. Passengers (`role == USER`) cannot navigate into driver routes.
- BackHandler: Automatically handles returning from `DriverScreenState.Readiness` to `DriverScreenState.Home`.

---

## 10. Error Handling

- `401 Unauthorized` → Centralized auth expiration handling.
- `403 Forbidden` → Access denied; non-retryable error state.
- `404 Not Found` → Driver profile missing on backend.
- `409 Conflict` → State conflict; refreshes authoritative state.
- `422 Unprocessable Entity` → Validation failure.
- `429 Rate Limited` → Backoff and rate limit error notification.
- `5xx Server Error` → Backend failure; allows user retry.
- `Timeout` / `Network` → Offline stage with retry prompt.

---

## 11. Security

1. **Backend Authoritative**: The frontend never calculates `isReady = verified && agencyMember`. Local state cannot grant operational privileges.
2. **Account Suspension Invariant**: Suspended drivers cannot activate operational mode.
3. **No Sensitive Logging**: Tokens, credentials, and full license numbers are never printed to logs.
4. **No Admin Privileges**: The client never uses admin secret keys or administrative suspension/approval routes.

---

## 12. Cache Policy

- `DriverReadinessRepositoryImpl` maintains in-memory state for transient rendering.
- Cached state is **never** considered permanent authority.
- `refreshDriverReadiness()` forces a remote fetch.
- On logout or account switch, `clearReadinessState()` flushes all cached state immediately.

---

## 13. Account Switching & State Isolation

- When Driver A logs out, `clearDriverReadinessStateUseCase()` flushes memory.
- When Driver B logs in, Driver A's readiness state is guaranteed not to leak.
- When switching between Driver and Passenger accounts, role checks prevent unauthorized readiness checks.

---

## 14. Tests

Created [DriverPhaseA06Test.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/DriverPhaseA06Test.kt) with **44 comprehensive unit tests**:
- Readiness State: Tests 1–7
- Dependencies & Blockers: Tests 8–13
- Operational Mode / Shift: Tests 14–19
- Authorization & Route Protection: Tests 20–24
- Backend Error Handling: Tests 25–33
- State Isolation & Account Switching: Tests 34–39
- Security Invariants: Tests 40–44

All 44 tests pass.

---

## 15. Manual Verification Matrix

| Scenario | Condition | Expected Result | Verified |
| :--- | :--- | :--- | :--- |
| Case 1 | Verified individual driver | Status = READY, authorized = true | Pass |
| Case 2 | Driver KYC pending | Status = NOT_READY, PLATFORM_VERIFICATION_PENDING blocker | Pass |
| Case 3 | Agency driver with no membership | Status = NOT_READY, AGENCY_MEMBERSHIP_REQUIRED blocker | Pass |
| Case 4 | Verified + Agency Approved, no vehicle | Status = READY (telemetry indicates vehicleAssigned: false) | Pass |
| Case 5 | Suspended driver | Status = SUSPENDED, operational mode blocked | Pass |
| Case 6 | Admin approves verification externally | Refresh updates status to READY | Pass |
| Case 7 | Admin suspends driver externally | Refresh updates status to SUSPENDED | Pass |
| Case 8 | Device offline | Offline error state displayed with retry | Pass |
| Case 9 | Driver A logout → Driver B login | Zero state leakage between accounts | Pass |

---

## 16. Build Status

- Kotlin Compilation: `BUILD SUCCESSFUL`
- Targeted Tests (`DriverPhaseA06Test`): `44/44 PASSED`
- Full Regression Test Suite (`testDebugUnitTest`): `ALL PASSED`
- Debug Assembly (`assembleDebug`): `BUILD SUCCESSFUL`

---

## 17. Known Limitations

- Vehicle assignment is informational only in Phase A06; vehicle registration and assignment are owned by Phase A07.
- Shift scheduling (start shift / end shift with time bounds) is not supported by the backend; availability is toggled via `POST /me/status/online` and `/offline`.

---

## 18. Backend Discrepancies

- **Documentation (`api_final_flow.md`)**:
  Documented response as `{ success: true, data: { isReady: true, requirements: { isVerified, hasAssignedVehicle, hasActiveTrip, isSuspended } } }`.
- **Actual Backend (`ishara-backend`)**:
  Exposes `DriverOperationalReadinessResponse` with `status: "READY" | "NOT_READY" | "SUSPENDED"`, `authorized: boolean`, `reasons: string[]`, `requirements: { platformVerification, agencyMembership, profileComplete, notSuspended, vehicleAssigned }`, `operatingType`, `agency`, and `activeVehicle`.
- **Frontend Decision**:
  Implemented strictly against the actual authoritative backend contract.
- **Reason**:
  Per Rule 1 and Rule 35, the actual backend implementation is the ground truth.

---

## 19. Deferred A07+ Work

- Vehicle registration, document upload, and vehicle editing (Phase A07).
- Vehicle assignment to drivers and active fleet vehicle selection (Phase A07).
- Trip dispatch, passenger queues, and ride acceptance (Phase A08+).
- Live GPS tracking telemetry and background services (Phase A11).

---

## 20. Production Readiness Assessment

Phase A06 satisfies all production engineering, architectural, and security requirements. The DRIVER operational readiness foundation is complete, thoroughly tested, and ready for Phase A07.
