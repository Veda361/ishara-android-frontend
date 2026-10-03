# ISHAARA FRONTEND PHASE A04 — CHANGELOG

## [Phase A04] — 2026-09-30

### Added
- **Domain Models & Enums:**
  - `DriverProfile`: Immutable domain model representing driver profile with license, status, and emergency contact.
  - `DriverEmergencyContact`: Emergency contact details with masking utilities.
  - `DriverVerificationDetails`: Verification details including status, rejection reason, and verified timestamp.
  - `DriverOnboardingState`: Sealed class hierarchy (`Loading`, `NeedsOnboarding`, `Submitting`, `PendingVerification`, `Verified`, `Rejected`, `Suspended`, `Error`).
- **DTOs & Data Layer:**
  - `CreateDriverProfileRequestDto`, `UpdateDriverProfileRequestDto`, `SubmitDriverVerificationRequestDto`.
  - `CleanEmergencyContactDto`, `CleanDriverProfileResponseDto`, `CleanDriverVerificationResponseDto`.
  - Full remote datasource endpoints in `DriverRemoteDataSource`: `getDriverProfile`, `createDriverProfileFull`, `updateDriverProfile`, `getVerificationStatus`, `submitVerification`.
  - State flows in `DriverRepositoryImpl`: `driverProfileFlow` and `driverOnboardingStateFlow`.
- **Use Cases:**
  - `GetDriverProfileUseCase`, `CreateDriverProfileUseCase`, `UpdateDriverProfileUseCase`.
  - `GetDriverVerificationStatusUseCase`, `SubmitDriverVerificationUseCase`.
  - `ObserveDriverProfileUseCase`, `ObserveDriverOnboardingStateUseCase`, `RefreshDriverProfileUseCase`, `ClearDriverStateUseCase`.
- **Presentation & UI:**
  - `DriverOnboardingViewModel` and `DriverOnboardingUiState` with UDF pattern.
  - `DriverOnboardingScreen` Composable with license number validation and emergency contact input.
  - `DriverVerificationViewModel` and `DriverVerificationUiState`.
  - `DriverVerificationScreen` Composable with dedicated views for `PENDING`, `VERIFIED`, `REJECTED`, and `SUSPENDED`.
  - Navigation destinations `IshaaraDestination.DriverOnboarding` and `IshaaraDestination.DriverVerification`.
- **Testing:**
  - `DriverPhaseA04Test.kt` with 40 comprehensive unit tests covering auth, onboarding, verification, errors, restoration, and security.

### Changed
- `DriverRepository`: Added A04 methods with default implementations to maintain backwards compatibility with existing Phase A02/A03 tests.
- `DriverRepositoryImpl`: Replaced stub auto-creation with authoritative backend state resolution and 409 conflict handling.
- `MainActivity.kt`: Integrated `DriverContainerScreen` with `DriverScreenState.Onboarding` and `DriverScreenState.Verification` dynamically driven by `observeDriverOnboardingStateUseCase`.
- `DriverHomeScreen.kt`: Added `onNavigateToVerification` callback to allow verified drivers or drivers in review to inspect their credential status.
- `AppContainer.kt`: Registered Phase A04 use cases in DI graph.

### Removed
- Removed mock auto-heal logic that fabricated artificial `"DL-"` driver licenses on 404 responses.

### Fixed
- Fixed unverified drivers inadvertently gaining immediate access to operational driver home screen.
- Fixed missing 409 conflict handling during driver profile creation.
- Fixed missing license validation before backend network calls.

### Security
- Zero client-side verification flags or bypass switches.
- Strict backend role enforcement: only `DRIVER_CONDUCTOR` can access onboarding and verification screens.
- Bearer tokens and sensitive PII are excluded from logging.
- Logout flushes driver profile state from in-memory cache to prevent data leaking between passenger and driver logins.

### Known Limitations
- Backend does not yet support file/image uploads for physical licenses; verification accepts notes only.
- Administrative review and approval is external (handled via admin panel in Phase A19).
