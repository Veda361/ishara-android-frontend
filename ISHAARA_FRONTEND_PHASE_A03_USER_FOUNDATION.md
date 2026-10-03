# PHASE A03 — USER / PASSENGER FOUNDATION AUDIT & IMPLEMENTATION REPORT

## 1. Objective

Phase A03 establishes the production-ready **USER / Student / Passenger** frontend foundation for the Ishaara Android application, strictly decoupled from driver workflows and operating on top of verified backend contracts.

### Core Architectural Rules & Scope Boundaries
- **Strict Role Demarcation**:
  - The application recognizes two authoritative roles: `USER` (Passenger) and `DRIVER_CONDUCTOR` (Driver).
  - Phase A03 implements and hardens **only** the `USER` passenger experience.
  - Driver onboarding, operational readiness, vehicle dispatch, driver ride requests, and earnings (Phases A04–A08, A16) are deferred and kept strictly inaccessible to passenger accounts.
- **Backend as Authoritative Source of Truth**:
  - User identity, profile data, onboarding status, and transit matches are authoritative from backend endpoints (`GET /api/v1/users/me`, `POST /api/v1/users/me/onboarding`, `POST /api/v1/discovery/trips`).
  - No client-side guessing, mock role switches, or fabricated payload fields.
- **Strict Contract Compliance**:
  - Trip discovery payload strictly enforces verified backend schema fields (`origin`, `destination`, and optional `options`).
  - **No seat reservation or fare calculation logic is invented** on the passenger side during discovery (seat reservation and fare locks belong downstream to ride-request creation in Phase A10 and payment in Phase A13).
- **Graceful Onboarding Re-entrancy**:
  - `POST /api/v1/users/me/onboarding` handles `HTTP 409` (`ONBOARDING_ALREADY_COMPLETED`) as an idempotent signal to reconcile profile state rather than triggering a fatal error, re-login, or navigation loop.

---

## 2. Existing Architecture & Audit Findings

### Artifact Audit
- **Inspected Specifications**:
  - `api_final_flow.md` (Authoritative backend contract specifications)
  - `ISHAARA_FRONTEND_PHASE_A02_AUTH_IMPLEMENTATION.md` (Phase A02 authentication architecture)
  - `ISHAARA_DESIGN_SYSTEM.md` (Core design tokens, components, and color palette)
  - `app/src/main/java/com/ishara/app/` (Application source tree)
- **Status of Phase A03 Code**:
  - The codebase contains the complete architectural foundation for Phase A03, including domain models, repository implementations, use cases, ViewModels, Compose screens, and automated tests.
  - 43 comprehensive unit tests in `UserPhaseA03Test.kt` along with strict contract tests in `DiscoveryStrictContractTest.kt` and `StudentHomeViewModelTest.kt` are implemented and passing.
  - Formal Phase A03 completion documentation and changelog were pending and are now officially documented herein.

---

## 3. Files Implemented & Audited in Phase A03

### Domain Models & Repositories
- `app/src/main/java/com/ishara/app/domain/model/UserProfile.kt` — Authoritative domain user profile model
- `app/src/main/java/com/ishara/app/domain/model/LocationPoint.kt` — Validated geographic coordinate point with boundary invariants
- `app/src/main/java/com/ishara/app/domain/model/Location.kt` — Search result location, student destination, and `LocationPermissionStatus` enum
- `app/src/main/java/com/ishara/app/domain/model/DiscoveryModels.kt` — Strict discovery query, discovered trip, driver, vehicle, and compatibility models
- `app/src/main/java/com/ishara/app/domain/model/OnboardingState.kt` — Domain onboarding state machine
- `app/src/main/java/com/ishara/app/domain/repository/UserRepository.kt` — User profile observation, hydration, and update interface
- `app/src/main/java/com/ishara/app/domain/repository/LocationRepository.kt` — Geocoding, place search, device location, and recent destinations
- `app/src/main/java/com/ishara/app/domain/repository/OnboardingRepository.kt` — Onboarding submission and role reconciliation contract

### Data Layer & Remote Services
- `app/src/main/java/com/ishara/app/data/repository/UserRepositoryImpl.kt` — Profile caching with thread-safe `Mutex` and session sync
- `app/src/main/java/com/ishara/app/data/repository/OnboardingRepositoryImpl.kt` — Idempotent role assignment with 409 conflict handling
- `app/src/main/java/com/ishara/app/data/repository/LocationRepositoryImpl.kt` — Location search and privacy-preserving local recent destinations cache
- `app/src/main/java/com/ishara/app/data/remote/datasource/DiscoveryRemoteDataSource.kt` — Strict JSON serialization for `/discovery/trips`
- `app/src/main/java/com/ishara/app/data/remote/dto/DiscoveryDtos.kt` — Strict backend-matching request and response DTOs
- `app/src/main/java/com/ishara/app/data/mapper/DiscoveryMapper.kt` — Bi-directional mapping between domain queries and backend DTOs
- `app/src/main/java/com/ishara/app/data/mapper/UserMapper.kt` — User DTO to domain model mapper

### Use Cases
- `app/src/main/java/com/ishara/app/domain/usecase/GetCurrentUserProfileUseCase.kt` — Hydrates profile from backend
- `app/src/main/java/com/ishara/app/domain/usecase/UpdateUserProfileUseCase.kt` — Validates and updates user name/phone
- `app/src/main/java/com/ishara/app/domain/usecase/ResolveCurrentLocationUseCase.kt` — Resolves hardware GPS coordinates to human-readable labels
- `app/src/main/java/com/ishara/app/domain/usecase/SearchLocationsUseCase.kt` — Enforces search query minimum length and delegates to geo-orchestrator
- `app/src/main/java/com/ishara/app/domain/usecase/ManageRecentDestinationsUseCase.kt` — Manages recent destinations with privacy-safe storage
- `app/src/main/java/com/ishara/app/domain/usecase/DiscoverTripsUseCase.kt` — Validates query bounds and executes trip discovery
- `app/src/main/java/com/ishara/app/domain/usecase/ResolveApplicationDestinationUseCase.kt` — Evaluates role and onboarding state to decide root destination

### Presentation Layer (Jetpack Compose & ViewModels)
- `app/src/main/java/com/ishara/app/feature/student/home/StudentHomeScreen.kt` — Student entry point with greeting, location pill, search card, and recent destinations
- `app/src/main/java/com/ishara/app/feature/student/home/StudentHomeViewModel.kt` — State holder for home dashboard and navigation events
- `app/src/main/java/com/ishara/app/feature/student/profile/StudentProfileScreen.kt` — Profile viewer and editor with validation and sign-out
- `app/src/main/java/com/ishara/app/feature/student/profile/ProfileViewModel.kt` — In-place profile edit state holder with field validation
- `app/src/main/java/com/ishara/app/feature/student/discovery/DiscoveryScreen.kt` — Available matching transit trips list with trip detail bottom sheet
- `app/src/main/java/com/ishara/app/feature/student/discovery/DiscoveryViewModel.kt` — Search execution with debounce, duplicate guard, and error mapping
- `app/src/main/java/com/ishara/app/feature/student/search/LocationSearchScreen.kt` — Origin/destination search screen
- `app/src/main/java/com/ishara/app/feature/student/search/LocationSearchViewModel.kt` — Debounced search suggestions and place resolution
- `app/src/main/java/com/ishara/app/MainActivity.kt` — Root activity state-driven navigation container gating USER vs DRIVER

### Test Suites
- `app/src/test/java/com/ishara/app/UserPhaseA03Test.kt` — Comprehensive 43-test suite covering all A03 requirements
- `app/src/test/java/com/ishara/app/DiscoveryStrictContractTest.kt` — Payload strictness and GeoJSON compliance tests
- `app/src/test/java/com/ishara/app/StudentHomeViewModelTest.kt` — Student home state transitions
- `app/src/test/java/com/ishara/app/LocationSearchViewModelTest.kt` — Search query validation and debounce tests
- `app/src/test/java/com/ishara/app/LocationRepositoryTest.kt` — Location repository and recents caching tests

---

## 4. USER Architecture & Component Overview

```
                      [ Compose UI ]
   StudentHomeScreen | DiscoveryScreen | StudentProfileScreen
                            │
                      [ ViewModels ]
 StudentHomeViewModel | DiscoveryViewModel | ProfileViewModel
                            │
                      [ Use Cases ]
 DiscoverTripsUseCase | GetCurrentUserProfileUseCase | ResolveCurrentLocationUseCase
                            │
                     [ Repositories ]
   UserRepository | TripRepository | LocationRepository | OnboardingRepository
                            │
               [ Data Sources & Clients ]
   UserRemoteDataSource | DiscoveryRemoteDataSource | LocationProvider
                            │
                  [ Backend REST API ]
   GET /users/me | POST /onboarding | POST /discovery/trips
```

---

## 5. Current-User State & Synchronization

### State Lifecycle
1. **Source of Truth**: `GET /api/v1/users/me` is called upon authentication and whenever profile data needs reconciliation.
2. **State Representation**:
   - `AuthState.Unknown`: Splash screen rendered during session restoration.
   - `AuthState.Authenticated(session, user)`: Emits active session and authoritative `UserProfile`.
   - `AuthState.Unauthenticated` / `AuthState.SessionExpired`: Redirects immediately to `LoginScreen`.
3. **Cache Invalidation & Account Switching**:
   - When a user signs out (`authRepository.signOut()`), `userRepository.clearCachedProfile()` resets cached memory flows to `null`.
   - Subsequent login by a different user hydates fresh data from `/api/v1/users/me`, preventing passenger state leakage across accounts.

---

## 6. Onboarding Flow & Conflict Handling

### The Problem
In the backend contract, `POST /api/v1/users/me/onboarding` assigns the initial role and marks the profile as onboarded. If an onboarded user re-attempts onboarding (e.g., due to duplicate button tap or app kill during transit), the backend returns `409 Conflict` with `ONBOARDING_ALREADY_COMPLETED`.

### Implemented Resolution
- In `OnboardingRepositoryImpl.kt`, HTTP 409 responses are explicitly intercepted:
  ```kotlin
  val isConflict = error is IshaaraError.Conflict ||
      (error as? IshaaraError.Conflict)?.errorCode == "ONBOARDING_ALREADY_COMPLETED" ||
      error.message.contains("already completed", ignoreCase = true)
  ```
- Upon intercepting a 409:
  1. The repository does not bubble up a fatal failure.
  2. It immediately fetches the authoritative profile via `userRepository.getCurrentUserProfile()`.
  3. Updates the active `sessionStore` with the authoritative role.
  4. Transitions `OnboardingState` directly to `OnboardingState.Completed(profile, StudentHome)`.
  5. Navigates the student directly to `StudentHome` without entering an infinite onboarding loop.

---

## 7. Location Permission Foundation & Abstraction

### Architecture
- UI components never directly interact with Android runtime permission frameworks or hardware GPS APIs.
- Hardware interaction is encapsulated behind `LocationProvider` (`core/location/LocationProvider.kt` and `FusedLocationProvider.kt`).
- The domain exposes explicit `LocationPermissionStatus` states:
  - `NOT_REQUESTED`
  - `GRANTED`
  - `DENIED`
  - `PERMANENTLY_DENIED`
  - `SERVICE_DISABLED`
  - `REQUEST_IN_PROGRESS`
  - `UNAVAILABLE`
  - `STALE`
- Permission is **not requested on app launch**. Instead, it is requested contextually when the student taps the location bar or initiates trip search.
- When permission is denied, clear fallback UI enables manual origin/destination selection.

---

## 8. Passenger Discovery Foundation & Strict Contract

### Endpoint Contract: `POST /api/v1/discovery/trips`
- **Request Body**:
  ```json
  {
    "origin": {
      "latitude": 25.4484,
      "longitude": 78.5685,
      "name": "Hostel Gate 2",
      "formattedAddress": "Campus Road, Jhansi"
    },
    "destination": {
      "latitude": 25.4358,
      "longitude": 78.5522,
      "name": "City Tech Park",
      "formattedAddress": "Tech Park Rd, Jhansi"
    },
    "options": {
      "maxPickupDistanceMeters": 1000.0,
      "maxResults": 20
    }
  }
  ```
- **Strict Compliance Enforcement**:
  - The backend schema enforces `.strict()`. Any extra client fields (such as `fare`, `seatCapacity`, `seatsRequested`, `price`) cause immediate `HTTP 400 Bad Request`.
  - `DiscoveryRemoteDataSourceImpl.serializeRequest()` guarantees that **only** `origin`, `destination`, and `options` are emitted in JSON payloads.
  - Coordinate arrays returned by the backend adhere strictly to GeoJSON RFC 7946 `[longitude, latitude]` format and are mapped to `GeoJsonCoordinate`.

### Discovery UI States
- **IDLE**: Initial state before query execution.
- **LOADING**: Displaying `"Finding available trips..."` with animated progress indicator.
- **SUCCESS**: Displaying discovered route matches with driver, vehicle, and walking distance chips.
- **EMPTY**: Displaying `"No trips found for this route"` with option to modify origin/destination.
- **ERROR**: Displaying domain-mapped error messages with safe retry button.

---

## 9. Navigation Boundaries & Route Security

### Route Protection Matrix
| Route | Access Rule | Enforced Action if Unauthorized |
|---|---|---|
| `auth/login` | Unauthenticated only | Redirects to `StudentHome` / `DriverDashboard` if session active |
| `auth/onboarding` | Authenticated without assigned role | Redirects to role home if already onboarded |
| `student/*` | Authenticated + role == `USER` | Redirects to `auth/login` or safe recovery screen |
| `driver/*` | Authenticated + role == `DRIVER_CONDUCTOR` | **Strictly blocked for USER role**; redirects to `StudentHome` |

- Evaluated in `MainActivity.kt` via state-driven composition. Deep links or manual route triggers cannot bypass role gates because the root `when (state.user.role)` structure prevents mounting unauthorized screen graphs.

---

## 10. Error Handling & User Feedback

Centralized error mapping converts HTTP status codes and transport failures into safe, user-friendly feedback:
- **401 Unauthorized**: Handled by session layer; triggers session renewal or clean redirection to login.
- **403 Forbidden**: Informs user of unauthorized resource access without leaking internal endpoint paths.
- **404 Not Found**: Maps to `"Requested trip or user profile not found."`
- **409 Conflict**: Intercepted in business logic for idempotency (e.g. onboarding).
- **400 / 422 Validation**: Surfaces field-level validation errors (e.g., `"Origin latitude out of bounds"`).
- **429 Rate Limited**: Displays `"Too many requests. Please wait a moment."`
- **5xx Server Errors**: Maps to `"Server error (500). Please try again shortly."` with retry trigger.
- **Network / Offline**: Displays offline banner and offers cached profile/destination fallbacks.

---

## 11. Security & Data Protection

- **No Hardcoded Secrets or Tokens**: API URLs and tokens are dynamically resolved and stored in encrypted storage.
- **No Token / PII Logging**: Sensitive parameters (bearer tokens, OTPs, full phone numbers) are stripped before logging.
- **No Client Authorization Assumptions**: Application permissions are verified server-side on every network round trip.
- **Account Clean-Up**: Signing out clears the memory session store, invalidating cached passenger profiles and recent destination searches.

---

## 12. Automated Test Execution

### Test Execution Summary
- **Command Executed**: `./gradlew testDebugUnitTest --tests "com.ishara.app.UserPhaseA03Test"`
- **Result**: `BUILD SUCCESSFUL` (43 of 43 tests passing)
- **All Unit Tests**: `./gradlew testDebugUnitTest` passed with 0 failures.

### Verified Test Cases in `UserPhaseA03Test.kt`:
1. `test01_authenticatedUser_loadsAuthoritativeProfile` — Verified
2. `test02_currentUserRefresh_reconcilesState` — Verified
3. `test03_unauthenticatedState_returnsAuthenticationError` — Verified
4. `test04_backendUserRefreshFailure_propagatesCleanly` — Verified
5. `test05_roleUser_isRecognizedAsPassengerDestination` — Verified
6. `test06_driverConductorRole_isNotRoutedToUserScreens` — Verified
7. `test07_incompleteOnboarding_opensOnboardingState` — Verified
8. `test08_completedOnboarding_skipsOnboarding` — Verified
9. `test09_successfulOnboarding_refreshesUser` — Verified
10. `test10_onboardingAlreadyCompleted409_isHandledGracefully` — Verified
11. `test11_onboardingDoesNotLoopOn409` — Verified
12. `test12_locationPermissionGranted_status` — Verified
13. `test13_locationPermissionDenied_status` — Verified
14. `test14_locationPermissionPermanentlyDenied_status` — Verified
15. `test15_locationUnavailable_status` — Verified
16. `test16_locationServicesDisabled_status` — Verified
17. `test17_validDiscoveryRequest_returnsMatches` — Verified
18. `test18_invalidOriginLatitude_throwsValidation` — Verified
19. `test19_invalidDestinationLongitude_throwsValidation` — Verified
20. `test20_discoveryLoadingState_isEmittedImmediately` — Verified
21. `test21_discoverySuccessfulResults_populatesTrips` — Verified
22. `test22_emptyDiscoveryResults_setsEmptyStage` — Verified
23. `test23_validationError_setsErrorStage` — Verified
24. `test24_discovery401_mapsToAuthError` — Verified
25. `test25_discovery403_mapsToForbiddenError` — Verified
26. `test26_discovery5xx_mapsToServerError` — Verified
27. `test27_discoveryTimeout_mapsToTimeoutError` — Verified
28. `test28_discoveryOffline_mapsToNetworkError` — Verified
29. `test29_discoveryRetry_reExecutesQuerySafely` — Verified
30. `test30_duplicateSearchPrevention_doesNotFireRedundantNetworkCall` — Verified
31. `test31_userNavigatesToHome` — Verified
32. `test32_userNavigatesToProfile` — Verified
33. `test33_userNavigatesToDiscovery` — Verified
34. `test34_unauthorizedDeepLink_blockedByNavigationDecision` — Verified
35. `test35_driverOnlyRoute_inaccessibleToUser` — Verified
36. `test36_logoutReturnsToAuth` — Verified
37. `test37_accountSwitching_clearsPassengerState` — Verified
38. `test38_locationPoint_constructsAndFormatsCorrectly` — Verified
39. `test39_locationPoint_fallbackToCoordinatesWhenNoName` — Verified
40. `test40_staleCachedUser_reconcilesWithBackend` — Verified
41. `test41_profileUpdate_validNameAndPhone_succeeds` — Verified
42. `test42_profileUpdate_blankName_rejectedByClientValidation` — Verified
43. `test43_profileUpdate_invalidPhone_rejectedByClientValidation` — Verified

---

## 13. Manual Test Matrix

| Test Scenario | Steps | Expected Result | Result |
|---|---|---|---|
| **Student Login & Home Entry** | Complete Google or Email OTP sign-in with a `USER` account | Lands on `StudentHomeScreen` with time-based greeting, location header, and destination search card | **PASS** |
| **Profile Hydration** | Open Profile screen | Authoritative name, email, phone, and role display from `GET /api/v1/users/me` | **PASS** |
| **Profile Editing** | Edit phone number and tap Save | Sends `PATCH /api/v1/users/me`, updates UI, and displays confirmation snackbar | **PASS** |
| **Onboarding 409 Recovery** | Attempt onboarding on an already-onboarded user | Receives HTTP 409, reconciles profile with backend, and routes directly to `StudentHome` | **PASS** |
| **Trip Discovery Search** | Select origin and destination, submit search | Performs `POST /api/v1/discovery/trips` and shows matching driver & vehicle cards | **PASS** |
| **Empty Results Display** | Search for route with no active drivers | Shows empty state: `"No trips found for this route"` with modify search button | **PASS** |
| **Route Protection** | Attempt to navigate to `driver/dashboard` as `USER` | Denied access; user remains within `StudentContainerScreen` | **PASS** |
| **Sign Out & Switching** | Tap Sign Out, log in with different user | Memory session cleared, new user's profile loaded cleanly without cached overlap | **PASS** |

---

## 14. Known Limitations & Explicit Deferred Scope

1. **Ride Request Submission (Phase A10)**:
   - Selecting a trip card in Phase A03 navigates to the ride request stage preview. The actual ride booking creation (`POST /api/v1/ride-requests`) is scoped to Phase A10.
2. **Realtime GPS Tracking (Phase A11)**:
   - WebSocket GPS telemetry and driver live map tracking are deferred to Phase A11.
3. **Fare Calculation & Payments (Phases A12 & A13)**:
   - Fare estimates and Razorpay checkout orders are deferred to Phases A12 and A13.
4. **Safety & SOS Realtime (Phase A14)**:
   - SOS broadcast and emergency contact sync are deferred to Phase A14.
5. **Push Notifications (Phase A15)**:
   - FCM ride notifications are deferred to Phase A15.
6. **Driver Workflows (Phases A04–A08, A16)**:
   - Driver onboarding, documents, vehicle verification, and earnings are deferred to downstream driver phases.

---

## 15. Production Readiness Assessment

- **Compilation Status**: `BUILD SUCCESSFUL` (`./gradlew assembleDebug` passed).
- **Test Status**: All 43 Phase A03 unit tests passed with 100% pass rate.
- **Contract Fidelity**: Confirmed 100% against `api_final_flow.md` and backend Zod schemas.
- **Verdict**: **Phase A03 (User / Passenger Foundation) is fully implemented, verified, and production-ready.**
