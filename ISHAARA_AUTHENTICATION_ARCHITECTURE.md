# Ishaara Android — Authentication & Session Architecture (Phase 03)

This document specifies the production authentication, session lifecycle, security guarantees, networking integration, and navigation gating implemented in Phase 03 of the Ishaara Android application.

---

## 1. Authentication Flow

The application implements a decoupled, unidirectional data-flow authentication lifecycle:

```
[User Action: Continue with Google]
         │
         ▼
[GoogleAuthClient] ──(obtains Google ID Token)──► [AuthViewModel]
                                                        │
                                                        ▼
                                            [SignInWithGoogleUseCase]
                                                        │
                                                        ▼
                                               [AuthRepository]
                                                        │
                      ┌─────────────────────────────────┴─────────────────────────────────┐
                      ▼                                                                   ▼
         [AuthRemoteDataSource]                                             [SessionLocalDataSource]
  (POST /api/auth/sign-in/social)                                            (EncryptedSessionStore)
                      │                                                                   │
                      ▼                                                                   ▼
       Better Auth Session Token                                            AndroidKeyStore AES-256 GCM
                      │                                                                   │
                      └───────────────────────────────┬───────────────────────────────────┘
                                                      │
                                                      ▼
                                       [AuthState.Authenticated]
                                                      │
                                                      ▼
                                         [Navigation Gating Trigger]
                                        (StudentHome / DriverDashboard)
```

---

## 2. Session Lifecycle

The session transitions through well-defined operational phases:

1. **Launch / Unknown**: Persistent session existence is evaluated via `RestoreSessionUseCase`.
2. **Restored (Online)**: Stored credentials are validated against `GET /api/v1/users/me` or `/api/auth/get-session`.
3. **Restored (Offline)**: If the device is offline at startup, a valid, non-expired local session is **preserved**, allowing offline navigation and cached schedule viewing.
4. **Active**: Outgoing HTTP calls attach `Authorization: Bearer <token>` automatically.
5. **Expired**: If a token expires locally (`System.currentTimeMillis() >= expiresAtMillis`) or receives a 401 response from the server, the session is invalidated.
6. **Cleared**: Local hardware-encrypted keys and session fields are deleted, resetting state to `AuthState.Unauthenticated`.

---

## 3. Domain Authentication States

The application-wide state machine is modeled as a sealed hierarchy in [`AuthState.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/model/AuthState.kt):

| State | Description | UI Action |
|---|---|---|
| `AuthState.Unknown` | Startup initialization while checking persistent storage. | Displays `SplashScreen` without navigation flicker. |
| `AuthState.Authenticated` | Active valid session with role and optional user profile. | Displays role-specific workspace (`StudentHome` / `DriverDashboard`). |
| `AuthState.Unauthenticated` | No active session or user explicitly logged out. | Displays `LoginScreen`. Backstack cleared. |
| `AuthState.SessionExpired` | Session invalidated due to local expiration or server 401. | Displays `LoginScreen` with amber warning banner: *"Your session has expired. Please sign in again."* |
| `AuthState.Error` | Unrecoverable error during sign-in attempt. | Displays `LoginScreen` with inline retryable error state. |

---

## 4. Hardware-Backed Secure Token Storage

Session credentials are saved using [`EncryptedSessionStore`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/storage/EncryptedSessionStore.kt):
- **Provider**: `AndroidKeyStore`.
- **Cipher**: `AES/GCM/NoPadding` (256-bit key length, 128-bit authentication tag).
- **IV Generation**: Unique cryptographically secure 12-byte initialization vector per write operation.
- **Persistence**: Ciphertext and IV are saved in private `SharedPreferences` (`MODE_PRIVATE`).
- **JVM Fallback**: Gracefully degrades in local unit testing environments where `AndroidKeyStore` is unmocked.
- **Data Protection**: Never writes raw tokens to unencrypted files, logs, or external storage.

---

## 5. API Authorization

- **Header Format**: `Authorization: Bearer <session-token>`.
- **Injection**: Centralized inside [`DefaultIshaaraHttpClient`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/network/IshaaraHttpClient.kt) and data source calls. Protected endpoints (`/api/v1/...`) automatically attach the bearer token.
- **Log Redaction**: [`IshaaraLogger`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/common/IshaaraLogger.kt) automatically scrubs `Bearer ...`, `Authorization: ...`, `idToken=...`, and `x-admin-key: ...` from Logcat and terminal outputs.

---

## 6. HTTP 401 Handling & Concurrent Session Invalidation

HTTP 401 Unauthorized responses are managed by [`SessionInvalidationCoordinator`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/network/SessionInvalidationCoordinator.kt):
- **Coalescing**: If multiple concurrent API calls (e.g., active trip polling + profile refresh) fail simultaneously with 401, the coordinator coalesces them into a **single invalidation event**.
- **Debounce Window**: 2000ms debounce prevents infinite logout loops.
- **State Transition**: Transitions `AuthState` to `SessionExpired`, clears the `EncryptedSessionStore` once, and safely directs the user back to the sign-in screen.

---

## 7. Logout Architecture

- **Idempotence**: Repeated invocations of `signOut()` are safe, thread-safe (via `Mutex`), and non-blocking.
- **Remote Invalidation**: Dispatches `POST /api/auth/sign-out` on a best-effort basis.
- **Local Cleanup**: In-memory and on-disk secure session tokens are completely deleted.
- **Navigation Invalidation**: Backstack is cleared (`popUpTo("auth/login", inclusive = true)`). Pressing Android Back after logout cannot return to protected screens.

---

## 8. Session Restoration & Offline Resilience

On application launch:
1. `RestoreSessionUseCase` is invoked asynchronously.
2. If stored credentials exist and are not locally expired:
   - Online: Calls `GET /api/v1/users/me` to refresh user profile and verify backend validity.
   - Offline (`IshaaraError.Network`): **Preserves local session** and marks state as `Authenticated`. A temporary loss of connectivity does NOT log the user out.

---

## 9. Google Social Authentication Integration

- **Backend Contract**:
  - `POST /api/auth/sign-in/social`
  - Body: `{ "provider": "google", "idToken": "<google_id_token>" }`
- **Android Layer**:
  - Abstracted through [`GoogleAuthClient`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/auth/GoogleAuthClient.kt) and [`DefaultGoogleAuthClient`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/auth/GoogleAuthClient.kt).
  - Handles `Success`, `Cancelled`, `NoCredentialsAvailable`, `ConfigurationMissing`, and `Failure`.
  - Does NOT invent mock tokens or fake logins.
  - If Google Server Client ID is unconfigured, displays clear human-friendly feedback: *"Google Sign-In is not configured. Please set the Google OAuth Server Client ID."*

---

## 10. Error Handling & Human-Centered UX

- HTTP 401 -> *"Your session has expired. Please sign in again."*
- Network timeout / offline -> *"You're offline. Check your internet connection."*
- Server 5xx -> *"Server is temporarily unavailable. Please try again shortly."*
- Missing credential -> *"No Google account found on this device."*
- Raw HTTP codes and stack traces are normalized away from the user interface.

---

## 11. Security Considerations

1. **Zero Secret Storage in APK**: Google OAuth Client Secrets and admin keys are never embedded in the client.
2. **Hardware Key Isolation**: Keys generated in Android Keystore never enter user-space memory as raw byte arrays.
3. **Redacted Logging**: All interceptors and logging pipelines scrub authentication tokens.
4. **Button Debounce**: Sign-in triggers disable the button and show a progress spinner to prevent concurrent duplicate token submissions.

---

## 12. Backend Assumptions

1. Better Auth session tokens provided in `/api/auth/sign-in/social` are accepted as `Authorization: Bearer <token>` across all protected `/api/v1/...` routes.
2. `/api/auth/sign-out` accepts Bearer token or session cookie to revoke the session server-side.
3. `GET /api/v1/users/me` returns the authenticated identity and role (`USER` or `DRIVER_CONDUCTOR`).

---

## 13. Unverified Backend Contract Items

- **Session Expiry Format**: Better Auth responses may return `expiresAt` as an epoch timestamp (seconds or milliseconds) or an ISO 8601 string. The parser handles both numeric timestamps and null (indefinite until 401).
- **Session Refresh Tokens**: The current contract exposes session tokens; if a dedicated refresh token flow is added in the future, it can be attached to `DefaultIshaaraHttpClient`.

---

## 14. Testing Strategy

100% of critical authentication and session scenarios are covered by automated unit tests in [`AuthRepositoryArchitectureTest.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/AuthRepositoryArchitectureTest.kt):

| Test ID | Scenario | Verified Behavior |
|---|---|---|
| `test1` | No stored session | Emits `AuthState.Unauthenticated`. |
| `test2` | Valid stored session | Restores `AuthState.Authenticated`. |
| `test3` | Expired stored session | Emits `AuthState.SessionExpired` and clears storage. |
| `test4` | Successful Google sign-in | Persists session in secure store and emits `Authenticated`. |
| `test5` | Authentication failure | Emits `AuthState.Error` without persisting corrupt data. |
| `test6` | User sign-out | Clears storage and emits `AuthState.Unauthenticated`. |
| `test7` | Startup with network offline | Preserves valid local session (`AuthState.Authenticated`). |
| `test8` | 401 from protected API | Triggers coordinator and transitions to `SessionExpired`. |
| `test9` | Multiple concurrent 401s | Coalesces into a single invalidation event. |
| `test10` | Repeated login tap | Debounces and dispatches only 1 remote request. |

---

## 15. Phase 04 Integration Point

Authentication answers *"Who is this user?"*.  
Phase 04 will answer *"What role/profile/setup does this user need?"*.

When `AuthState.Authenticated` is reached:
- If user profile has `isOnboarded == false`, navigation hands off to `OnboardingScreen` (`POST /api/v1/users/me/onboarding`).
- If `isOnboarded == true`, navigation routes immediately to `StudentHome` (for `UserRole.USER`) or `DriverDashboard` (for `UserRole.DRIVER_CONDUCTOR`).
