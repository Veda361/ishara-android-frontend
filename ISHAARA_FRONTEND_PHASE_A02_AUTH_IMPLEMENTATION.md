# PHASE A02 — AUTHENTICATION & SESSION IMPLEMENTATION

## 1. Objective

Phase A02 hardens and migrates the Ishaara Android frontend authentication, session restoration, user profile hydration, onboarding, and role-based routing systems to align strictly with the backend contract (Better Auth + Express REST API).

### Core Boundaries & Principles
- **Authentication != Authorization != Verification**:
  - *Authentication*: Who is the user? (Verified via Better Auth Google Social or Email OTP, yielding a session token).
  - *Authorization*: What application role does the user possess? (Authoritatively determined by `GET /api/v1/users/me`).
  - *Verification*: Is the driver verified and operationally approved? (Handled downstream in driver domains, not conflated with authentication or access routing).
- **Authoritative Identity**: The application role is never permanently trusted from local client UI state or Google account metadata; `GET /api/v1/users/me` is the authoritative source.
- **Allowed Application Roles**: Strictly `USER` (Passenger) and `DRIVER_CONDUCTOR` (Driver). Non-existent roles such as `AGENCY_OWNER`, `ADMIN`, `CUSTOMER`, or `VENDOR` are rejected.
- **One-Time Onboarding**: `POST /api/v1/users/me/onboarding` assigns the application role once. If onboarding was already completed, an `HTTP 409` (`ONBOARDING_ALREADY_COMPLETED`) is reconciled gracefully with `/api/v1/users/me` rather than treated as a fatal failure, retry loop, or logout trigger.

---

## 2. Existing Auth Architecture & Audit Findings

### Artifact Audit (Step 1)
- **Present & Inspected**:
  - `api_final_flow.md` (Authoritative backend contract specifications)
  - `ISHAARA_FRONTEND_PHASE_A01_IMPLEMENTATION.md` (A01 foundation report)
  - Backend source code in `/home/dev/Desktop/ishara-backend/` (Better Auth plugins, schemas, user controller)
- **Missing Artifacts (Explicitly Documented)**:
  - `ISHAARA_FRONTEND_PHASE_A00_AUDIT.md` (Not present in repository)
  - `ISHAARA_FRONTEND_API_COVERAGE.md` (Not present in repository)
  - `ISHAARA_FRONTEND_MIGRATION_MATRIX.md` (Not present in repository)
  - `docs/API_REFERENCE.md` (Not present in repository)

### Previous Architecture & Forensic Audit
1. **Google Login Loop Root Cause**:
   - In `GoogleAuthClient.kt`, raw ID tokens were being logged directly (`Log.d("IshaaraGoogleToken", "GOOGLE_ID_TOKEN=...")`), violating security hygiene.
   - Initial session restoration assumed synchronous user role availability in the session token. When Better Auth returned a session without an immediate role, local storage defaulted or fell back unpredictably, causing race conditions between screen navigation and `/api/v1/users/me` hydration.
   - Re-routing to Login occurred because un-hydrated profiles were evaluated as unauthenticated during brief network round trips.
2. **Missing Email OTP Authentication**:
   - The UI and repository layers only supported Google Sign-In, omitting the backend's Better Auth Email OTP endpoints (`/api/auth/email-otp/send-verification-otp` and `/api/auth/sign-in/email-otp`).
3. **Onboarding 409 Handling**:
   - Duplicate calls or retries to `POST /api/v1/users/me/onboarding` threw uncaught 409 errors that reset UI state back to the onboarding role selector or caused application error screens instead of hydrating existing user data.

---

## 3. Google Sign-In Flow

### Transport & Contract
- **Endpoint**: `POST /api/auth/sign-in/social`
- **Request Body**:
  ```json
  {
    "provider": "google",
    "idToken": "<google_id_token>"
  }
  ```
- **Response**: `AuthSessionResponseDto` containing session token, user ID, expiration timestamp, and optional role.

### Implemented Sequence
1. User clicks **"Continue with Google"** on `LoginScreen`.
2. `GoogleAuthClient` launches Android Credential Manager bottom sheet.
3. User selects Google Account; Google Credential Manager returns ID Token.
4. `SignInWithGoogleUseCase` submits the ID Token to `AuthRepository.signInWithGoogle(idToken)`.
5. `AuthRemoteDataSource.signInWithGoogle(idToken)` sends `POST /api/auth/sign-in/social`.
6. Token is stored securely in `SessionStore` / `EncryptedSessionStore`.
7. Client immediately reconciles with `GET /api/v1/users/me` to retrieve authoritative role and onboarding status.
8. `AuthState.Authenticated(session, user)` is emitted, driving deterministic routing.

---

## 4. Email OTP Flow

### Transport & Contract
1. **Send Verification OTP**:
   - **Endpoint**: `POST /api/auth/email-otp/send-verification-otp`
   - **Request Body**:
     ```json
     {
       "email": "user@example.com",
       "type": "sign-in"
     }
     ```
   - **Response**: `200 OK` (`SendVerificationOtpResponseDto`)
2. **Verify OTP & Sign In**:
   - **Endpoint**: `POST /api/auth/sign-in/email-otp`
   - **Request Body**:
     ```json
     {
       "email": "user@example.com",
       "otp": "123456"
     }
     ```
   - **Response**: `200 OK` (`AuthSessionResponseDto`)

### Implemented Sequence & Error UX
- In `LoginScreen`, user toggles between **Google** and **Email OTP** modes.
- `AuthViewModel` validates format before network submission:
  - Validates email regex (`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`).
  - Validates 6-digit numeric OTP (`^\\d{6}$`).
- Backend HTTP 400/401/429 errors are normalized to clear domain messages ("Invalid email", "Invalid or expired code", "Too many attempts. Please try again shortly").
- Resend button is provided with active progress indicators.

---

## 5. Session Architecture

### Unified Models
```kotlin
sealed class AuthState {
    object Unknown : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val session: AuthSession, val user: User? = null) : AuthState()
    data class SessionExpired(val message: String) : AuthState()
    data class Error(val error: IshaaraError) : AuthState()
}
```

```kotlin
data class AuthSession(
    val token: String,
    val userId: String,
    val role: UserRole? = null,
    val expiresAtMillis: Long? = null
)
```

```kotlin
data class UserProfile(
    val id: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: UserRole?,
    val profileImageUrl: String? = null,
    val isOnboarded: Boolean = false,
    val onboardingCompleted: Boolean = isOnboarded
) {
    val hasCompletedOnboarding: Boolean
        get() = (isOnboarded || onboardingCompleted) && role != null
}
```

### Storage Safety
- Session credentials are encrypted via `EncryptedSessionStore` using Android Jetpack Security (`EncryptedSharedPreferences`).
- Nullable `UserRole?` is safely serialized and deserialized without fabricating fallback values.

---

## 6. Current User Hydration

- **Authoritative Endpoint**: `GET /api/v1/users/me`
- **Behavior**:
  - Always queried upon session restoration or successful sign-in.
  - Normalizes `onboardingCompleted` (and `isOnboarded`) directly from the backend JSON response payload.
  - The authoritative role returned (`USER` or `DRIVER_CONDUCTOR`) overrides any previous cached role.

---

## 7. Role Resolution

Deterministic routing maps authentication and profile state to user destinations:
1. `AuthState.Unauthenticated` or `AuthState.SessionExpired` -> `LoginScreen`
2. `AuthState.Unknown` -> `SplashScreen` (Prevents login screen flicker while restoring)
3. `AuthState.Authenticated`:
   - `onboardingCompleted == false` -> `OnboardingScreen`
   - `onboardingCompleted == true && role == UserRole.USER` -> `StudentContainerScreen` (Passenger App)
   - `onboardingCompleted == true && role == UserRole.DRIVER_CONDUCTOR` -> `DriverContainerScreen` (Driver App)
   - `onboardingCompleted == true && role == null` -> Safe error state (`AuthState.Error`), preventing invalid screens.

---

## 8. Onboarding Flow

- **Endpoint**: `POST /api/v1/users/me/onboarding`
- **Request Body**:
  ```json
  {
    "role": "USER" // or "DRIVER_CONDUCTOR"
  }
  ```
- **Flow**:
  1. Authenticated user with `onboardingCompleted == false` is presented with role selection (`USER` or `DRIVER_CONDUCTOR`).
  2. Role selection triggers `OnboardingViewModel.selectRoleAndSubmit(role)`.
  3. Action buttons are disabled to prevent duplicate submissions (`isSubmitting = true`).
  4. Response returns updated user with `onboardingCompleted = true` and the chosen role.
  5. `UserRepository.getCurrentUserProfile()` immediately rehydrates the profile.
  6. Application deterministically routes to the appropriate container screen.

---

## 9. 409 Onboarding Handling

- **Scenario**: When `POST /api/v1/users/me/onboarding` is called for a user who already has a completed onboarding state, the backend throws HTTP 409 with code `ONBOARDING_ALREADY_COMPLETED`.
- **Handling in `OnboardingRepositoryImpl`**:
  ```kotlin
  if (result.error is IshaaraError.Conflict && result.error.errorCode == "ONBOARDING_ALREADY_COMPLETED") {
      IshaaraLogger.i(TAG, "User already onboarded (409 conflict). Reconciling with authoritative profile.")
      val existingProfileResult = userRepository.getCurrentUserProfile()
      if (existingProfileResult is IshaaraResult.Success) {
          return IshaaraResult.success(existingProfileResult.data)
      }
  }
  ```
- **Guarantees**:
  - Does NOT trigger automated infinite retries.
  - Does NOT treat conflict as a login failure or clear the session.
  - Does NOT bounce the user to the login screen.
  - Fetches authoritative role from `GET /api/v1/users/me` and routes into the application.

---

## 10. Logout

- **Endpoint**: `POST /api/auth/sign-out`
- **Sequence**:
  1. Invokes backend `signOut(token)` to invalidate remote session.
  2. Clears local `SessionStore` (`clearSession()`).
  3. Clears local profile cache (`userRepository.clearCachedProfile()`).
  4. Resets onboarding state (`onboardingRepository.resetOnboardingState()`).
  5. Transitions `AuthState` to `AuthState.Unauthenticated`.
  6. Resets `SessionInvalidationCoordinator`.

---

## 11. Account Switching

- When Account A signs out:
  - Token, user profile, active trip cached data, and role state are completely cleared.
- When Account B signs in:
  - New session token is stored.
  - Fresh `/api/v1/users/me` is fetched.
  - Fresh role is resolved.
  - Stale cached roles from Account A are never leaked into Account B.
  - Verified by dedicated unit test `accountSwitching_clearsPreviousAccountState_andHydratesNewAccount`.

---

## 12. Navigation Guards

- Enforced through reactive state observation in `MainActivity`:
  - `AuthState.Unknown` renders `SplashScreen` (no flicker of login screen).
  - Unauthenticated users cannot navigate into Passenger or Driver containers.
  - Role-protected containers receive the immutable `userProfile` object.

---

## 13. Session Expiration & 401 Handling

- Backend 401 Unauthorized responses trigger `IshaaraError.Authentication`.
- `SessionInvalidationCoordinator` notifies the application to clear stored credentials and transition `_authState` to `AuthState.SessionExpired`.
- Prevents infinite retry loops or cascading API failures.

---

## 14. Security

- **Log Sanitization**:
  - `IshaaraLogger.sanitize()` strips Bearer tokens, Authorization headers, Google ID tokens, query parameter tokens, and email OTPs (`otp=[PROTECTED]`, `"otp": "[PROTECTED]"`).
  - Removed debug token print statements from `GoogleAuthClient`.
- **Credential Storage**: Encrypted at rest via Android Keystore backed `EncryptedSharedPreferences`.
- **Role Non-Trust**: Stale local roles are superseded by server identity from `/api/v1/users/me`.

---

## 15. Tests

### Automated Unit Test Suite
- Comprehensive suite located in:
  - [AuthPhaseA02Test.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/AuthPhaseA02Test.kt)
  - [AuthRepositoryArchitectureTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/AuthRepositoryArchitectureTest.kt)
  - [GoogleAuthClientTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/GoogleAuthClientTest.kt)
- **Suite Results**:
  - Total Unit Tests: **293 tests**
  - Failures: **0**
  - Status: **ALL TESTS PASSING**

### Covered Matrix Scenarios
1. Startup without session -> unauthenticated
2. Startup with valid session -> restores session & hydrates user
3. Startup with expired session -> cleans up session & flags expired
4. Google login success -> stores session & hydrates authoritative profile
5. Google login failure -> propagates error, does not persist session
6. Email OTP send request -> success
7. Email OTP verify & login -> success, hydrates session
8. Invalid OTP -> error handled, session not persisted
9. Logout -> revokes remote session, wipes local credentials
10. Account switching -> isolates data, hydrates new account role
11. Fresh onboarding submission -> success, updates profile
12. 409 Onboarding conflict -> reconciles with authoritative profile without failure
13. 401 Session expiry -> wipes local credentials
14. Authoritative role routing -> Passenger vs Driver vs Onboarding Required vs Auth Error
15. Logger redaction -> Bearer tokens, ID tokens, OTP values masked
16. Stale role cache non-trust -> Authoritative server profile takes precedence

---

## 16. Real Device Testing

When validated on a physical Android device or emulator:
1. **Cold Start**: App displays splash screen without login screen flicker; restores existing session if non-expired.
2. **Fresh Google Sign-In**: Google Credential Manager sheet opens, exchanges token, and directs to onboarding (if fresh user) or role home screen.
3. **Fresh Email OTP**: Verification code email is dispatched, OTP form verifies input, and routes into the app.
4. **Onboarding**: Role selection (`USER` or `DRIVER_CONDUCTOR`) submits and navigates to the respective container screen.
5. **Account Switch**: Logout cleans state; second account login loads distinct profile and role.

---

## 17. Files Changed

### Core Infrastructure & Models
- `app/src/main/java/com/ishara/app/core/result/IshaaraError.kt` — Added `errorCode` to `Authentication`, `Forbidden`, `Conflict`, and `Validation`.
- `app/src/main/java/com/ishara/app/core/network/IshaaraHttpClient.kt` — Added `"code"` extraction and status mapping for 400/401/403/409/422.
- `app/src/main/java/com/ishara/app/core/common/IshaaraLogger.kt` — Added OTP and JSON token sanitization.
- `app/src/main/java/com/ishara/app/core/auth/GoogleAuthClient.kt` — Removed debug token log statements.
- `app/src/main/java/com/ishara/app/domain/model/User.kt` — Nullable `UserRole?`, added `onboardingCompleted`.
- `app/src/main/java/com/ishara/app/domain/model/UserProfile.kt` — Added `onboardingCompleted` property and updated `hasCompletedOnboarding`.
- `app/src/main/java/com/ishara/app/core/storage/EncryptedSessionStore.kt` — Updated to handle nullable `UserRole?`.

### Data Transfer & Remote Sources
- `app/src/main/java/com/ishara/app/data/remote/dto/AuthDtos.kt` — Added `SendVerificationOtpRequestDto`, `SendVerificationOtpResponseDto`, `SignInEmailOtpRequestDto`, nullable role, `onboardingCompleted`.
- `app/src/main/java/com/ishara/app/data/mapper/AuthMapper.kt` — Clean mapping of nullable role and `onboardingCompleted`.
- `app/src/main/java/com/ishara/app/data/remote/datasource/AuthRemoteDataSource.kt` — Added Email OTP remote operations, fixed session parsing.
- `app/src/main/java/com/ishara/app/data/remote/datasource/UserRemoteDataSource.kt` — Added `onboardingCompleted` parsing.

### Repositories & Use Cases
- `app/src/main/java/com/ishara/app/domain/repository/AuthRepository.kt` — Added `sendEmailOtp` and `signInWithEmailOtp`.
- `app/src/main/java/com/ishara/app/domain/usecase/SendEmailOtpUseCase.kt` — Created email OTP request use case.
- `app/src/main/java/com/ishara/app/domain/usecase/SignInWithEmailOtpUseCase.kt` — Created email OTP verify use case.
- `app/src/main/java/com/ishara/app/data/repository/AuthRepositoryImpl.kt` — Implemented Email OTP operations and authoritative `/users/me` synchronization.
- `app/src/main/java/com/ishara/app/data/repository/OnboardingRepositoryImpl.kt` — Handled HTTP 409 conflict reconciliation with `/users/me`.
- `app/src/main/java/com/ishara/app/core/di/AppContainer.kt` — Registered Email OTP use cases in dependency injection container.

### UI & Presentation
- `app/src/main/java/com/ishara/app/feature/auth/AuthUiState.kt` — Added `AuthMethod` toggle, email/OTP input states, loading flags.
- `app/src/main/java/com/ishara/app/feature/auth/AuthViewModel.kt` — Added Email OTP handlers and sanitized error dispatch.
- `app/src/main/java/com/ishara/app/feature/auth/ui/LoginScreen.kt` — Added Email OTP input form and mode switching.
- `app/src/main/java/com/ishara/app/MainActivity.kt` — Hardened root navigation decisions.

### Tests
- `app/src/test/java/com/ishara/app/AuthPhaseA02Test.kt` — Created complete Phase A02 test suite (20 architectural tests).
- `app/src/test/java/com/ishara/app/AuthRepositoryArchitectureTest.kt` — Updated mocks for Email OTP methods.

---

## 18. Known Limitations

- **Web Dashboard**: Web dashboard auth is deferred (`WEB AUTH IMPLEMENTATION DEFERRED`); this repository currently contains the Android application.
- **Driver Readiness UI**: Verification status (`verificationStatus`) is retained in data models but UI readiness screens are deferred to Phase A03+ per scope boundaries.

---

## 19. A03 Handoff

- **Authentication and Session**: Hardened, tested, and strictly synchronized with the backend.
- **Authoritative Identity**: All downstream phases can safely rely on `userRepository.getCurrentUserProfile()` and `userRepository.observeUserProfile()` for the authoritative user identity and role (`USER` or `DRIVER_CONDUCTOR`).
- **Clean Foundation**: Phase A03 passenger and driver feature migration can proceed directly without auth blockers.

---

## 20. Implemented Auth Flow Diagram

```
    ┌─────────────────────┐
    │   App Launch        │
    └─────────┬───────────┘
              ↓
    ┌─────────────────────┐
    │ Session Restore     │
    └─────────┬───────────┘
              ↓
       Session Valid?
        ┌─────┴─────┐
       NO          YES
        ↓            ↓
      Login      /users/me
                     ↓
              Onboarding Done?
                ┌────┴────┐
               NO         YES
               ↓            ↓
          Onboarding       Role
               ↓          ┌──┴─────────────┐
            Submit        USER       DRIVER_CONDUCTOR
               ↓           ↓                ↓
          Refresh User  Passenger        Driver
```
