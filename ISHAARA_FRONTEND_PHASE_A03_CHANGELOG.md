# CHANGELOG — PHASE A03: USER / PASSENGER FOUNDATION

All notable architectural additions, modifications, and fixes for Phase A03 are documented here.

---

## [Phase A03] — 2026-09-30

### Added
- **Domain Models**:
  - `UserProfile`: Authoritative user entity containing `id`, `name`, `email`, `phoneNumber`, `role`, and onboarding flags.
  - `LocationPoint`: Strict geographic coordinate pair with domain boundary validations (-90..90 lat, -180..180 lon).
  - `LocationPermissionStatus`: Domain enumeration representing full runtime permission and GPS hardware lifecycle states.
  - `DiscoveryQuery` & `DiscoveredTrip`: Pure domain query and match models mapping to backend transit discovery responses without inventing unsupported fields.
- **Repository & Data Layer**:
  - `UserRepositoryImpl`: Profile caching with `Mutex` synchronization and active session token binding.
  - `OnboardingRepositoryImpl`: State machine for role assignment with graceful `HTTP 409` (`ONBOARDING_ALREADY_COMPLETED`) conflict reconciliation.
  - `LocationRepositoryImpl`: Contextual GPS coordinates resolution and local recent destinations history (max 5 items, privacy-preserving).
  - `DiscoveryRemoteDataSourceImpl`: Strict JSON serialization for `POST /api/v1/discovery/trips` ensuring zero extra fields are transmitted.
- **Use Cases**:
  - `GetCurrentUserProfileUseCase`, `UpdateUserProfileUseCase`, `ResolveCurrentLocationUseCase`, `SearchLocationsUseCase`, `ManageRecentDestinationsUseCase`, `DiscoverTripsUseCase`, `ResolveApplicationDestinationUseCase`.
- **UI & Presentation**:
  - `StudentHomeScreen`: Passenger entry dashboard with location status pill, quick action bar, and destination search card.
  - `StudentHomeViewModel`: State holder managing greeting calculation, location detection, and destination selection.
  - `StudentProfileScreen`: Profile viewer and in-place field editor with validation for passenger phone and name.
  - `ProfileViewModel`: State holder with input validation and optimistic updates.
  - `DiscoveryScreen`: Clean trip search results screen with match chips, driver/vehicle details, and error/empty states.
  - `DiscoveryViewModel`: Monotonically increasing query ID race protection, debounce, and error mapping.
  - `LocationSearchScreen` & `LocationSearchViewModel`: Origin and destination search with autocomplete suggestions.
- **Documentation**:
  - `ISHAARA_FRONTEND_PHASE_A03_USER_FOUNDATION.md`: Forensic audit and architectural specification report.

### Changed
- **Navigation Architecture**:
  - `MainActivity.kt`: Centralized state-driven navigation container gating `USER` into `StudentContainerScreen` and `DRIVER_CONDUCTOR` into `DriverContainerScreen`.
  - Prohibits cross-role navigation; attempts by `USER` to deep link or transition to `driver/*` routes are intercepted and redirected.
- **Session & Identity Handling**:
  - Synchronized `UserRepository` with `SessionStore` to ensure session changes automatically clear or update the cached profile.

### Fixed
- **Onboarding Infinite Loop on HTTP 409**:
  - Prevented duplicate onboarding attempts from throwing fatal errors. When receiving `ONBOARDING_ALREADY_COMPLETED`, the app reconciles against `GET /api/v1/users/me` and routes the passenger directly to `StudentHome`.
- **Duplicate Discovery Request Re-emission**:
  - Guarded against duplicate network calls caused by Compose recompositions or screen re-entry.
- **Strict Backend Discovery Contract Violation**:
  - Removed any client-side assumptions about seat availability, reservation count, or fare calculations from discovery payloads to avoid HTTP 400 rejection by the backend's strict Zod schema.

### Tests
- **Automated Test Suite**:
  - `UserPhaseA03Test.kt`: 43 unit tests covering current user hydration, onboarding 409 handling, location permissions, discovery strict contracts, navigation security, state restoration, and profile editing.
  - `DiscoveryStrictContractTest.kt`: Validated JSON payload schema and GeoJSON coordinate compliance.
  - `StudentHomeViewModelTest.kt`, `LocationSearchViewModelTest.kt`, `LocationRepositoryTest.kt`, `OnboardingArchitectureTest.kt`.
  - All test suites passing (`BUILD SUCCESSFUL`).

### Known Limitations
- Booking and live ride creation (`POST /api/v1/ride-requests`) are deferred to Phase A10.
- Realtime WebSocket GPS telemetry is deferred to Phase A11.
- Fare breakdown calculation and Razorpay payment orders are deferred to Phases A12 and A13.
- Driver onboarding and operations (Phases A04–A08, A16) remain strictly separated and will be implemented in their respective phases.
