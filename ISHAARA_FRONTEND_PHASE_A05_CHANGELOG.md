# ISHAARA FRONTEND PHASE A05 — CHANGELOG

## [Phase A05] — 2026-09-30

### Added
- **Domain Models & Enums:**
  - `Agency`: Immutable domain model representing a transport agency with public details and contact metadata.
  - `AgencyStatus`: Enum representing agency state (`ACTIVE`, `INACTIVE`).
  - `AgencyMembershipStatus`: Enum strictly aligning with backend states (`PENDING`, `APPROVED`, `REJECTED`).
  - `DriverAgencyMembership`: Immutable domain model representing the affiliation record of a driver with an agency.
  - `DriverMembershipState`: Sealed state machine (`Loading`, `NoMembership`, `Pending`, `Approved`, `Rejected`, `Error`).
- **Data & Network Layer:**
  - `CleanPublicAgencyResponseDto`, `CleanDriverMembershipResponseDto`, `RequestAgencyMembershipRequestDto`, `CancelMembershipResponseDto`.
  - `AgencyMapper`: Bidirectional mapper between DTOs and immutable domain models.
  - `AgencyRemoteDataSource` and `AgencyRemoteDataSourceImpl`: Implementing `/agencies`, `/agencies/:id`, `/drivers/me/memberships/current`, `/drivers/me/memberships` (GET, POST), and `/drivers/me/memberships/:id` (DELETE).
  - `AgencyMembershipRepository` and `AgencyMembershipRepositoryImpl`: Centralized StateFlows for current membership and state machine with automatic 409 conflict reconciliation.
- **Use Cases:**
  - `GetCurrentAgencyMembershipUseCase`, `ObserveAgencyMembershipStateUseCase`, `ObserveCurrentAgencyMembershipUseCase`.
  - `ListAgenciesUseCase`, `GetAgencyDetailsUseCase`, `RequestAgencyMembershipUseCase`, `CancelAgencyMembershipUseCase`.
  - `RefreshAgencyMembershipUseCase`, `ClearAgencyMembershipStateUseCase`.
- **Presentation & UI:**
  - `DriverAgencyMembershipUiState` and `DriverAgencyMembershipViewModel`: Managing discovery, request submission, and cancellation with double-tap protection.
  - `DriverAgencyMembershipScreen`: Compose screen displaying status card (`Approved`, `Pending`, `Rejected`, `NoMembership`) and agency discovery search.
- **Navigation:**
  - Added `IshaaraDestination.DriverAgency` (`driver/agency`).
  - Added `DriverScreenState.Agency` to `DriverContainerScreen` in `MainActivity.kt`.
  - Added "Agency Affiliation" card to `DriverHomeScreen` with navigation to `DriverAgency`.
- **Testing:**
  - `DriverAgencyMembershipPhaseA05Test.kt`: 41 unit tests covering membership state, application actions, discovery, authorization, error handling, account switching, mutation idempotency, and security.

### Changed
- `AppContainer.kt`: Registered Phase A05 data sources, repository, and use cases.
- `MainActivity.kt`: Wired `clearAgencyMembershipStateUseCase` into `handleSignOut` to ensure cached agency affiliations are flushed upon sign out.
- `DriverHomeScreen.kt`: Added `onNavigateToAgency` callback.

### Removed
- None (preserves all Phase A02, A03, and A04 components).

### Fixed
- Fixed potential data leak between driver accounts by flushing cached agency affiliations during sign out.
- Fixed 409 duplicate membership conflict handling by automatically synchronizing with authoritative backend state.

### Security
- Protected all driver agency routes: callers without `DRIVER_CONDUCTOR` role are barred.
- Excluded all administrative endpoints and admin keys from the mobile client.
- Excluded unmasked phone numbers and private user details from logs.
- Zero client-side authority: membership status is strictly derived from the backend.

### Known Limitations
- Background approval/rejection of membership requests is performed by the agency owner via the web dashboard (Phase A17).
- Backend does not support agency invitations; affiliation is driver-initiated.
