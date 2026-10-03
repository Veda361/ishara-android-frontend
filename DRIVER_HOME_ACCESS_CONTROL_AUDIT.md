# ISHAARA Android — Driver Home / Verification Gate Access Control Audit

**Project:** ISHAARA Android Application  
**Corpus / Path:** `/home/dev/Desktop/ishara-frontend`  
**Backend Environment:** `https://reposnse-ishaara.onrender.com`  
**API Prefix:** `/api/v1`  
**Status:** COMPLETE & VERIFIED  

---

## 1. Current Navigation Flow

Prior to this fix, the application enforced a tight coupling between **Authentication**, **Administrative Platform Verification**, and **Screen Routing**.

### Old Flow
```text
LOGIN
  ↓
AUTHENTICATED (User role = DRIVER_CONDUCTOR)
  ↓
MainActivity.LaunchedEffect(onboardingState, driverProfileState)
  ↓
IF onboardingState is PendingVerification, Rejected, or Suspended:
  → FORCED to DriverScreenState.Verification
  → If currentScreen == Home, kicked back to Verification
  ↓
From Verification:
  → "Check Fleet Affiliation" button navigates to DriverScreenState.Agency
  ↓
From Agency:
  → Back button / BackHandler check:
      if (onboardingState !is Verified) DriverScreenState.Verification else DriverScreenState.Home
  ↓
TRAPPED IN INFINITE LOOP:
  DriverVerificationScreen ⇄ DriverAgencyMembershipScreen
  (DriverHomeScreen was completely unreachable for UNDER_REVIEW drivers)
```

---

## 2. Root Cause of Driver Dashboard Not Opening

1. **Premature Screen Gating in `MainActivity.kt`**:
   Lines 286–295 in `MainActivity.kt` explicitly intercepted any state where `onboardingState is DriverOnboardingState.PendingVerification || Rejected || Suspended` and forced:
   ```kotlin
   if (currentScreen == DriverScreenState.Home || currentScreen == DriverScreenState.Loading ...) {
       currentScreen = DriverScreenState.Verification
   }
   ```
   This prevented an authenticated driver whose platform verification was `UNDER_REVIEW` (represented on the backend as `PENDING`) from ever reaching or staying on `DriverScreenState.Home`.

2. **Hard-Coded Loop in BackHandler & Screen Callbacks**:
   - `canGoBackToHome` in `MainActivity.kt` excluded `DriverScreenState.Verification`, disabling Android system back gesture/button on verification screens.
   - `DriverScreenState.Agency.onNavigateBack` and `BackHandler` explicitly evaluated `if (onboardingState !is DriverOnboardingState.Verified) DriverScreenState.Verification else DriverScreenState.Home`, preventing the driver from returning to Home.
   - `DriverVerificationScreen` had no back navigation affordance to return to `DriverHomeScreen`.

3. **ViewModel Crash / Blocking on `DRIVER_NOT_VERIFIED`**:
   When `DriverViewModel.loadOperationalContext()` ran, backend call `GET /api/v1/drivers/operational/context` returned HTTP 403 Forbidden with error code `DRIVER_NOT_VERIFIED`. The previous ViewModel treated this as an unrecoverable `DriverHomeStage.Error(canRetry = false)`, blocking the entire dashboard even if navigated to.

---

## 3. Verification State Handling

The application adheres strictly to the backend verification contract:
- Backend Enum: `DriverVerificationStatus` (`PENDING`, `VERIFIED`, `REJECTED`).
- UI Presentation:
  - `PENDING` → Displayed as **🟡 UNDER REVIEW** ("Verification in Progress").
  - `VERIFIED` → Displayed as **🟢 VERIFIED** ("Platform Identity Verified").
  - `REJECTED` → Displayed as **🔴 REJECTED** ("Verification Rejected").
  - `Suspended` flag on profile → Handled with administrative lock.

Under this architecture:
- Drivers with `PENDING` (`UNDER_REVIEW`), `REJECTED`, or `SUSPENDED` are granted **App Access** to `DriverHomeScreen`.
- They are **NOT** marked `VERIFIED`. Platform verification is **never** bypassed or faked in local storage.
- Operational driving features remain locked until authoritative platform verification is granted by the backend.

---

## 4. Membership State Handling

Agency membership and platform verification are maintained as **independent, orthogonal concerns**:

| Platform Verification | Fleet Affiliation (`fdggbv`) | Resulting App Access | Operational Transit Capability |
| :--- | :--- | :--- | :--- |
| `UNDER_REVIEW` (Pending) | `ACTIVE` (Approved) | ✅ Home Accessible | 🔒 Locked (Awaiting Verification) |
| `VERIFIED` | `ACTIVE` (Approved) | ✅ Home Accessible | 🟢 Unlocked |
| `VERIFIED` | `NO_MEMBERSHIP` | ✅ Home Accessible | 🔒 Locked (Fleet Affiliation Required) |
| `UNDER_REVIEW` (Pending) | `NO_MEMBERSHIP` | ✅ Home Accessible | 🔒 Locked (Both Required) |
| `REJECTED` | `ACTIVE` | ✅ Home Accessible | 🔒 Locked (Verification Rejected) |

Platform verification and fleet affiliation are rendered as independent overview cards on `DriverHomeScreen` (e.g., "Platform Verification: 🟡 UNDER REVIEW" and "Fleet Affiliation: 🟢 ACTIVE fdggbv • Jhansi"). They are never collapsed into an inaccurate unified "Driver Approved" badge.

---

## 5. Operational Capability Rules (Capability Guard)

Created the centralized domain model `com.ishara.app.domain.model.DriverAccessState`:

```kotlin
data class DriverAccessState(
    val appAccessible: Boolean = true,
    val platformVerified: Boolean = false,
    val verificationStatus: DriverVerificationStatus = DriverVerificationStatus.PENDING,
    val membershipActive: Boolean = false,
    val membershipStatus: AgencyMembershipStatus? = null,
    val agencyName: String? = null,
    val vehicleAssigned: Boolean = false,
    val operationalAccess: Boolean = false,
    val operationalLockReason: String? = null
)
```

### Authoritative Resolution Logic
```kotlin
val isVerified = verificationStatus == DriverVerificationStatus.VERIFIED && !isSuspended
val isMembershipActive = when {
    !isAgency -> true // Individual drivers
    membershipState is DriverMembershipState.Approved -> true
    else -> false
}
val canOperate = isVerified && isMembershipActive
```

Operational features guarded centrally:
- Going online (`goOnline()`)
- Starting trips (`startTrip()`)
- Completing / cancelling trips
- Broadcasting operational GPS location
- Viewing / accepting passenger ride requests and boarding workflows
- Receiving vehicle assignments

When `operationalAccess == false`, `goOnline()` and `startTrip()` fail gracefully in the ViewModel with clear user feedback rather than triggering unauthorized backend mutation requests.

---

## 6. Navigation Changes

### New Stable Flow
```text
Authenticated Driver (DRIVER_CONDUCTOR)
         ↓
   DriverHomeScreen (Root Destination)
    ├── 1. Verification Overview Card ──> [Check Details] ──> DriverVerificationScreen
    │                                                                ↓ (Back button / gesture)
    │                                                           DriverHomeScreen
    │
    ├── 2. Fleet Overview Card ──────────> [View Fleet] ────> DriverAgencyMembershipScreen
    │                                                                ↓ (Back button / gesture)
    │                                                           DriverHomeScreen
    │
    ├── 3. Readiness Gate Card ──────────> [Check Gate] ────> DriverOperationalReadinessScreen
    │                                                                ↓ (Back button / gesture)
    │                                                           DriverHomeScreen
    │
    ├── 4. Transit Operations (Conditional)
    │       ├── If UNDER_REVIEW: "DriverOperationsLockedCard" (Operational controls locked)
    │       └── If VERIFIED + ACTIVE: Go Online, Start Trip, Boarding, Vehicle, Earnings
    │
    └── 5. Quick Actions Bar (Direct sub-navigation to Verification, Fleet, Readiness, Vehicle)
```

- Hardware back and top back arrow buttons on `DriverVerificationScreen` and `DriverAgencyMembershipScreen` now return directly to `DriverHomeScreen`.
- Logging out from `DriverHomeScreen` cleanly flushes memory state and returns to `LoginScreen`.

---

## 7. UI Changes

1. **`DriverHomeScreen` Restored & Enhanced**:
   - **Identity Top Bar**: Driver name, masked license badge, unified "Refresh" and "Sign Out" actions.
   - **Section 1 — Platform Verification Card (`DriverVerificationOverviewCard`)**:
     - Visual badge: `🟡 UNDER REVIEW` (for `PENDING`), `🟢 VERIFIED`, or `🔴 REJECTED`.
     - Clear description: Explains why operations are locked and links to full verification details.
   - **Section 2 — Fleet Affiliation Card (`DriverFleetAffiliationOverviewCard`)**:
     - Visual badge: `🟢 ACTIVE` or `⚪ NO FLEET`.
     - Displays agency name (`fdggbv`) and city (`Jhansi`).
     - Button: "View Fleet Affiliation" / "Join a Transport Fleet".
   - **Section 3 — Operations Gate (`DriverOperationsLockedCard`)**:
     - Replaces operational controls when unverified.
     - Explains exact requirements needed before commercial operations can begin.
   - **Section 4 — Quick Actions (`DriverQuickActionsCard`)**:
     - Fast links to Verification Status, Fleet Affiliation, and Readiness Gate.

2. **`DriverVerificationScreen` Navigation Enhancements**:
   - Added top navigation bar with back arrow (`←`) returning to `DriverHomeScreen`.
   - Added `"Go to Driver Home"` action button to `PendingVerificationCard`, `RejectedCard`, and `SuspendedCard`.

---

## 8. GPS Behavior

Strict GPS Rule Enforced:
- **No Background GPS Broadcasting for Unverified Drivers**: Opening `DriverHomeScreen` or browsing sub-destinations while `UNDER_REVIEW` **never** starts continuous operational GPS telemetry broadcasting.
- `startDriverTrackingUseCase` is strictly bound to `onStartTrip` or authorized operational transitions, which are unreachable while `operationalAccess == false`.
- Calling `goOffline()` or logging out immediately terminates tracking via `stopDriverTrackingUseCase`.

---

## 9. API Calls Used

All integrations use existing, authoritative backend endpoints under `/api/v1`:

1. `GET /api/v1/drivers/me` — Fetches authoritative driver profile, `verificationStatus`, and `operatingType`.
2. `GET /api/v1/drivers/me/verification` — Fetches detailed verification status, submission notes, and review timestamps.
3. `POST /api/v1/drivers/me/verification` — Submits/resubmits driver credentials.
4. `GET /api/v1/agency-memberships/my-membership` — Resolves driver's agency affiliation (`fdggbv`, `status: APPROVED`).
5. `GET /api/v1/drivers/operational/context` — Resolves vehicle, active trip, and daily stats when verified.
   - Handled gracefully when backend responds with `403 DRIVER_NOT_VERIFIED`.
6. `PATCH /api/v1/drivers/operational/status` — Sets driver online/offline (invoked only when `operationalAccess == true`).
7. `POST /api/v1/driver/trips/{tripId}/start` — Starts trip lifecycle (invoked only when `operationalAccess == true`).

---

## 10. Automated Tests Added & Passing

Added test suite: `com.ishara.app.DriverAccessStateAndVerificationGateTest`
- `State A - UNDER_REVIEW driver with ACTIVE membership can access app but operations are locked`: **PASSED**
- `State B - VERIFIED driver with ACTIVE membership has operational access unlocked`: **PASSED**
- `State C - VERIFIED agency driver with NO active membership has operations locked until fleet joined`: **PASSED**
- `State D - UNDER_REVIEW driver with NO membership has operations locked and fleet required`: **PASSED**
- `State E - REJECTED driver has app accessible but operations locked with rejection guidance`: **PASSED**
- `State F - Suspended driver has operations locked regardless of verification`: **PASSED**
- `DriverViewModel handles DRIVER_NOT_VERIFIED without crashing or blocking home access`: **PASSED**
- `Verification upgrade dynamically unlocks operational features after refresh`: **PASSED**

Total unit tests passing in project: **All unit tests pass (`./gradlew test`).**

---

## 11. Build Result

- **Unit Tests:** `./gradlew test` → `BUILD SUCCESSFUL in 10s` (27 actionable tasks).
- **Assemble Debug APK:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL in 36s` (39 actionable tasks).
- **Target APK Artifact:** `app/build/outputs/apk/debug/app-debug.apk` compiled successfully.

---

## 12. Remaining Backend Dependencies

1. **Administrative Verification Granting**:
   - The driver is currently in `UNDER_REVIEW` (`PENDING`) on backend `https://reposnse-ishaara.onrender.com`.
   - Once an administrator verifies the driver on the backend, clicking **"Refresh"** on `DriverHomeScreen` or **"Check for Updates"** on `DriverVerificationScreen` immediately updates `DriverAccessState.operationalAccess` to `true` without requiring logout or app reinstallation.
2. **Vehicle Assignment**:
   - Assigned vehicles are provisioned by agency `fdggbv`. Once verified, vehicle details will populate automatically in `DriverVehicleCard`.
