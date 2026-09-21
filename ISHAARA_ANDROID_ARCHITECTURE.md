# Ishaara Android Architecture

**Document Version**: 1.0.0  
**Status**: ACTIVE / PRODUCTION ARCHITECTURE  
**Platform**: Android (Kotlin, Jetpack Compose, Material 3)  
**Author**: Senior Android Architect & Production Software Architect  

---

## 1. Current Architecture

Prior to Phase 01, the Ishaara project was an initial baseline Android Studio project scaffolded with Android Gradle Plugin (AGP) 9.4.1, Gradle 9.6.0, and Kotlin 2.2.10.

Key findings from the Phase 00 Audit ([ISHAARA_ANDROID_PHASE_00_AUDIT.md](file:///home/dev/Desktop/ishara-frontend/ISHAARA_ANDROID_PHASE_00_AUDIT.md)):
- Single `:app` module without layered packages.
- Zero network client or API interface.
- Zero local database, DataStore, or persistent session storage.
- Zero location or GPS provider abstractions.
- Missing runtime permissions in `AndroidManifest.xml` (no `INTERNET`, `ACCESS_FINE_LOCATION`, etc.).
- Default placeholder Composable (`Greeting("Android")`) in [MainActivity.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt).

Phase 01 safely established the foundational production architecture on top of this clean baseline without deleting existing code or breaking the build.

---

## 2. Target Architecture

The target production architecture strictly adheres to **Clean Architecture** and **Unidirectional Data Flow (UDF)**:

```
┌───────────────────────────────────────────────────────────┐
│                       UI LAYER                            │
│  Jetpack Compose Screens & Reusable Design Components     │
└─────────────────────────────┬─────────────────────────────┘
                              │ observes UiState
                              ▼
┌───────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER                        │
│  Lifecycle-Aware ViewModels + StateFlow + NavigationManager│
└─────────────────────────────┬─────────────────────────────┘
                              │ executes use cases
                              ▼
┌───────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                           │
│  Use Cases + Pure Business Models + Repository Interfaces  │
└─────────────────────────────┬─────────────────────────────┘
                              │ calls repository contracts
                              ▼
┌───────────────────────────────────────────────────────────┐
│                     DATA LAYER                            │
│  Repository Implementations + Remote/Local Data Sources   │
│  DTOs + Domain Mappers (GeoJSON [lng, lat] Converters)    │
└─────────────────────────────┬─────────────────────────────┘
                              │ delegates to infrastructure
                              ▼
┌───────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                      │
│  HTTP Client (NetworkConfig) • SessionStore • Location    │
│  Realtime WebSocket Client • Notification Push Manager    │
└───────────────────────────────────────────────────────────┘
```

---

## 3. Package Structure

The codebase is organized into domain-oriented, modular packages under `com.ishara.app`:

```
com.ishara.app/
├── IshaaraApplication.kt                # Application entry point & AppContainer owner
├── MainActivity.kt                      # Root ComponentActivity (Edge-to-Edge Compose)
│
├── core/                                # Reusable cross-feature infrastructure
│   ├── common/
│   │   ├── DispatcherProvider.kt        # Coroutine Dispatchers abstraction
│   │   ├── IshaaraLogger.kt             # Production-safe logging with token masking
│   │   └── UiState.kt                   # Generic UI state holder (Idle, Loading, Success, Error)
│   ├── result/
│   │   ├── IshaaraError.kt              # Normalized domain errors (Network, Auth, Validation, etc.)
│   │   └── IshaaraResult.kt             # Sealed Result<T> monad (Success / Failure)
│   ├── location/
│   │   ├── GeoJsonCoordinate.kt         # Strict [lng, lat] coordinate validator & serializer
│   │   ├── LocationCoordinates.kt       # Domain GPS model (lat, lng, heading, speed, accuracy)
│   │   └── LocationProvider.kt          # Clean GPS hardware & service abstraction
│   ├── network/
│   │   ├── NetworkConfig.kt             # Dev, Staging, Production environments & API prefix
│   │   └── IshaaraHttpClient.kt         # Transport abstraction & ErrorNormalizer
│   ├── storage/
│   │   └── SessionStore.kt              # In-memory and secure persistent session storage
│   ├── realtime/
│   │   ├── RealtimeConnectionState.kt   # Lifecycle states (Connecting, Connected, Reconnecting)
│   │   ├── RealtimeEvent.kt             # Typed streaming events (Telemetry, Requests, Discovery)
│   │   └── RealtimeClient.kt            # Bidirectional WebSocket interface abstraction
│   ├── notifications/
│   │   ├── NotificationChannelConfig.kt # Android 8.0+ system channel constants
│   │   └── PushTokenManager.kt          # FCM registration abstraction
│   └── di/
│       └── AppContainer.kt              # Compile-time Dependency Injection container
│
├── domain/                              # Pure Kotlin business rules (zero Android UI imports)
│   ├── model/
│   │   ├── UserRole.kt                  # USER (Student) & DRIVER_CONDUCTOR
│   │   ├── User.kt                      # Authenticated user profile
│   │   ├── AuthSession.kt               # Session token & expiry state
│   │   ├── Trip.kt                      # Trip, TripLocation, TripStatus
│   │   ├── Ride.kt                      # Ride, RideStatus, RideRequest, RideRequestStatus
│   │   └── DriverProfile.kt             # Driver profile & ratings
│   ├── repository/
│   │   ├── AuthRepository.kt            # Auth, onboarding & session contract
│   │   ├── TripRepository.kt            # Discovery & trip lifecycle contract
│   │   ├── RideRepository.kt            # Ride booking & tracking contract
│   │   └── DriverRepository.kt          # Driver status & telemetry contract
│   └── usecase/
│       ├── GetAuthSessionUseCase.kt     # Observe current auth session
│       ├── SignInWithGoogleUseCase.kt   # Google ID token sign-in
│       ├── SignOutUseCase.kt            # Invalidate session & clear storage
│       ├── DiscoverTripsUseCase.kt      # Heuristic trip search
│       └── UpdateDriverLocationUseCase.kt # High-frequency driver GPS ping
│
├── data/                                # Data layer & implementation details
│   ├── remote/
│   │   ├── dto/
│   │   │   ├── AuthDtos.kt              # Session & User JSON transfer objects
│   │   │   ├── TripDtos.kt              # Trip & GeoJSON coordinate DTOs
│   │   │   └── DriverDtos.kt            # Driver telemetry DTOs
│   │   └── datasource/
│   │       ├── AuthRemoteDataSource.kt  # Remote /api/auth and /api/v1/users/me calls
│   │       ├── TripRemoteDataSource.kt  # Remote /api/v1/discovery and /trips calls
│   │       └── DriverRemoteDataSource.kt# Remote /api/v1/drivers calls
│   ├── local/
│   │   └── datasource/
│   │       └── SessionLocalDataSource.kt# Reactive local session cache
│   ├── mapper/
│   │   ├── AuthMapper.kt                # DTO ↔ Domain user converters
│   │   └── TripMapper.kt                # DTO ↔ Domain trip & coordinate converters
│   └── repository/
│       ├── AuthRepositoryImpl.kt        # AuthRepository implementation
│       ├── TripRepositoryImpl.kt        # TripRepository implementation
│       └── DriverRepositoryImpl.kt      # DriverRepository implementation
│
├── feature/                             # Feature-specific UI & ViewModels
│   └── auth/
│       ├── AuthUiState.kt               # Presentation state for login & onboarding
│       └── AuthViewModel.kt             # ViewModel with StateFlow & UseCases
│
└── navigation/
    ├── IshaaraDestinations.kt           # Type-safe route hierarchy (Auth, Student, Driver)
    └── NavigationManager.kt             # Decoupled navigation event channel
```

---

## 4. Dependency Flow

Dependencies flow in **one strict direction**:
- **Presentation (`feature/`)** depends on **Domain (`domain/`)** and **Core (`core/`)**.
- **Data (`data/`)** depends on **Domain (`domain/`)** and **Core (`core/`)**.
- **Domain (`domain/`)** has **zero dependencies** on Data, Presentation, Android UI, or external libraries.

```
Feature / UI  ───►  Domain (Interfaces / UseCases / Models)  ◄───  Data
      │                                                              │
      └────────────────────────► Core ◄──────────────────────────────┘
```

---

## 5. Authentication Architecture

The authentication boundary handles the Better Auth protocol specified in [docs/API_REFERENCE.md](file:///home/dev/Desktop/ishara-frontend/docs/API_REFERENCE.md):
1. **Sign-In Flow**:
   - `AuthViewModel.signInWithGoogle(idToken)` invokes `SignInWithGoogleUseCase`.
   - `SignInWithGoogleUseCase` calls `AuthRepository.signInWithGoogle(idToken)`.
   - `AuthRepositoryImpl` calls `AuthRemoteDataSource.signInWithGoogle(idToken)` (`POST /api/auth/sign-in/social`).
   - On success, `AuthSession` is stored reactively in `SessionLocalDataSource` / `SessionStore`.
2. **Session Persistence**:
   - `SessionStore` provides atomic token storage and exposes an active `Flow<AuthSession?>`.
   - Any 401 Unauthorized response triggers `AuthRepository.signOut()`, automatically resetting the session flow to `null`.
3. **Session Interception**:
   - Outgoing HTTP calls retrieve the active bearer token from `SessionStore` and append `Authorization: Bearer <token>`.

---

## 6. Role Architecture

Ishaara mobile clients support **two explicit roles**:
- `UserRole.USER`: Students and general passengers.
- `UserRole.DRIVER_CONDUCTOR`: Transit vehicle drivers and conductors.

> [!IMPORTANT]
> `AGENCY_OWNER` belongs exclusively to the future separate web dashboard and is excluded from the Android client architecture.

When an authenticated user signs in:
1. If the account is new, they are routed to `IshaaraDestination.Onboarding` to assign their role (`POST /api/v1/users/me/onboarding`).
2. If already onboarded, `NavigationManager.navigateForRole(role)` transitions the app to:
   - `USER` → `IshaaraDestination.StudentHome`
   - `DRIVER_CONDUCTOR` → `IshaaraDestination.DriverDashboard`

---

## 7. Networking Architecture

- **Base URL Configuration**: Handled by `NetworkConfig` with three environments:
  - `DEVELOPMENT`: `http://10.0.2.2:5000/api/v1` (Android emulator localhost)
  - `STAGING`: `https://staging-ishaara.onrender.com/api/v1`
  - `PRODUCTION`: `https://reposnse-ishaara.onrender.com/api/v1`
- **Transport Abstraction**: `IshaaraHttpClient` wraps requests (`HttpRequest`) and responses (`HttpResponse`), allowing swapping underlying network engines (e.g. Retrofit, OkHttp, Ktor) without modifying business repositories.
- **Coordinate Order Standard**:
  - The backend strictly uses **`[longitude, latitude]`** GeoJSON arrays.
  - [GeoJsonCoordinate.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/location/GeoJsonCoordinate.kt) enforces array indexing `[0] = longitude, [1] = latitude` with boundary validation (-180..180 lng, -90..90 lat), completely preventing the coordinate inversion hazard.

---

## 8. Storage Architecture

- **Separation of Concerns**: Storage is decoupled into `SessionStore` and `SessionLocalDataSource`.
- **Reactive State**: Stored sessions are exposed as read-only Kotlin `StateFlow` / `Flow` instances so UI components react immediately to sign-out or role switches without polling.
- **Security**: In-memory caching with encrypted disk persistence interface.

---

## 9. Error Handling

Errors are modeled using the closed hierarchy [IshaaraError](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/result/IshaaraError.kt):
- `IshaaraError.Network`: Offline, socket dropped, DNS failure.
- `IshaaraError.Authentication`: 401 Unauthorized, token expired, 403 Forbidden.
- `IshaaraError.Validation`: Missing or malformed parameters.
- `IshaaraError.NotFound`: 404 Resource missing or expired.
- `IshaaraError.RateLimited`: 429 Too Many Requests.
- `IshaaraError.Timeout`: Connection or read timeout.
- `IshaaraError.Server`: 500/502/503 remote service error.
- `IshaaraError.Unknown`: Unhandled exceptions.

`ErrorNormalizer` maps raw HTTP status codes to typed `IshaaraError` instances, ensuring that no raw HTTP or socket exceptions leak into ViewModels or UI Composables.

---

## 10. State Management

- ViewModels hold state in a private `MutableStateFlow<UiState<T>>` and expose a public `StateFlow<UiState<T>>`.
- UI Composables collect states using lifecycle-aware collection (`collectAsState()` or `collectAsStateWithLifecycle()`).
- No business logic or state mutations occur inside Composable functions.

---

## 11. Navigation Boundary

- [IshaaraDestinations.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/navigation/IshaaraDestinations.kt) establishes type-safe route strings.
- [NavigationManager.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/navigation/NavigationManager.kt) acts as a decoupled event bus:
  - ViewModels emit `NavigationCommand` (`NavigateTo`, `NavigateUp`, `SwitchToRole`).
  - ViewModels do NOT retain references to `Activity` or `NavController`, making them 100% unit-testable without Android UI mocks.

---

## 12. Location Boundary

- [LocationProvider.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/location/LocationProvider.kt) abstracts device GPS hardware.
- [LocationCoordinates.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/location/LocationCoordinates.kt) encapsulates latitude, longitude, bearing, speed, accuracy, and timestamp.
- Dedicated methods provide immediate GeoJSON array conversion (`toGeoJsonArray() = [lng, lat]`).

---

## 13. Realtime Boundary

- [RealtimeClient.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/realtime/RealtimeClient.kt) abstracts WebSocket streaming connections.
- [RealtimeEvent.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/realtime/RealtimeEvent.kt) specifies typed events for the 4 backend WebSocket endpoints:
  1. `/api/v1/voice/realtime` (Voice transcript streaming)
  2. `/api/v1/discovery/realtime` (Nearby trip notifications)
  3. `/api/v1/ride-requests/realtime` (Ride booking requests)
  4. `/api/v1/rides/realtime` (Live vehicle telemetry and map tracking)

---

## 14. Notification Boundary

- [PushTokenManager.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/notifications/PushTokenManager.kt) abstracts Firebase Cloud Messaging (FCM) registration and deregistration.
- [NotificationChannelConfig.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/notifications/NotificationChannelConfig.kt) defines mandatory system notification channels for Ride Updates, Driver Tracking, and Safety SOS Alerts.

---

## 15. Dependency Injection

We adopted **Pure Compile-Time Dependency Injection (Manual AppContainer Pattern)** as officially recommended in Android developer guides:
- [IshaaraApplication.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/IshaaraApplication.kt) owns a single instance of `DefaultAppContainer`.
- Sub-components are lazily initialized via Kotlin `by lazy`.
- Advantages over Dagger/Hilt at this stage:
  1. 100% compatible with AGP 9.4.1 and Kotlin 2.2.10 (no KSP/annotation processing version mismatches).
  2. Zero reflection, instantaneous build times, and zero binary bloat.
  3. Compile-time verified dependencies with transparent constructor injection.

---

## 16. Environment Configuration

- Controlled centrally by [NetworkConfig.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/network/NetworkConfig.kt).
- Switches seamlessly between Development, Staging, and Production.
- Zero secrets committed to version control.

---

## 17. Security Principles

- **Token Masking**: [IshaaraLogger.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/common/IshaaraLogger.kt) automatically masks `Bearer <token>`, `idToken`, and `x-admin-key` in log output.
- **Permission Scoping**: Permissions are explicitly declared in `AndroidManifest.xml` only as required by feature specifications.
- **No Embedded Credentials**: Admin keys and server secrets are strictly excluded from the mobile application.

---

## 18. Testing Strategy

The architecture provides complete testability:
- **Unit Testing**:
  - [GeoJsonCoordinateTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/GeoJsonCoordinateTest.kt): Verifies `[longitude, latitude]` format and out-of-bound coordinate rejection.
  - [IshaaraResultTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/IshaaraResultTest.kt): Verifies `IshaaraResult.map()` and `ErrorNormalizer` HTTP status conversions.
  - [AuthRepositoryArchitectureTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/AuthRepositoryArchitectureTest.kt): Verifies repository caching, role updates, and sign-out invalidation without Android dependencies.
- **Mockability**: All repositories, data sources, and providers are interfaces.

---

## 19. Future Feature Integration

| Feature | Architectural Placement | Primary Interacting Layers |
| :--- | :--- | :--- |
| **Auth & Onboarding** | `feature/auth` | `AuthViewModel` → `AuthRepository` → `SessionStore` |
| **Student Home & Discovery** | `feature/student/discovery` | `DiscoverTripsUseCase` → `TripRepository` → `GeoJsonCoordinate` |
| **Ride Lifecycle & Live Map** | `feature/student/tracking` | `RideRepository` + `RealtimeClient` (`/api/v1/rides/realtime`) |
| **Driver Dashboard & Context** | `feature/driver/dashboard` | `DriverRepository.getDriverProfile()` + `setOnline()` / `setOffline()` |
| **Driver GPS Background Tracking**| `feature/driver/tracking` | `LocationProvider` → `UpdateDriverLocationUseCase` (`PATCH /api/v1/drivers/me/location`) |
| **Emergency SOS Alert** | `feature/safety` | `RideRepository` + `POST /api/v1/rides/:id/safety/sos` |
| **Payments (Razorpay)** | `feature/payments` | `RideRepository` + `POST /api/v1/rides/:id/payment` |
| **Voice Assistant** | `feature/driver/voice` | `RealtimeClient` (`/api/v1/voice/realtime`) |

---

## 20. Architectural Decisions

1. **Pure Kotlin Domain**: Domain models and use cases have zero imports from `android.*`, ensuring fast unit tests and complete portability.
2. **GeoJSON `[lng, lat]` Wrapper**: Created `GeoJsonCoordinate` as a first-class value type to permanently prevent latitude/longitude inversion errors when interfacing with the backend.
3. **Manual AppContainer DI**: Selected manual constructor injection over Dagger/Hilt to ensure stability with AGP 9.4.1 and Kotlin 2.2.10, eliminating fragile annotation-processing build plugins.
4. **Decoupled Navigation**: ViewModels emit navigation commands through `NavigationManager` rather than retaining `NavController` instances, keeping presentation logic clean and testable.
5. **Preservation of Existing Code**: `MainActivity.kt` and the existing theme files were preserved without disruptive refactoring, maintaining a 100% compiling build at all times.
