# ISHAARA FRONTEND PHASE A12 — CHANGELOG

## Phase Summary
- **Phase:** A12 — Fare & Pricing Foundation
- **Status:** Complete & Verified
- **Scope:** Authoritative Server-Side Fare Consumption, Integer Minor Unit Money Models, Immutable Billing Snapshot Handling, Itemized Breakdown Review Screen, and Phase A13 Payment Handoff Boundary.

---

## 1. Files Created

### Data & Remote Layers
1. `app/src/main/java/com/ishara/app/data/remote/dto/FareDtos.kt`
   - Wire DTOs for `RideFareResponseDto`, `FareEstimateDto`, and `FareSnapshotDto` matching backend schema.
2. `app/src/main/java/com/ishara/app/data/remote/datasource/FareRemoteDataSource.kt`
   - Remote data source interface and `DefaultFareRemoteDataSource` implementing `GET /api/v1/rides/:rideId/fare` with robust, reflection-free JSON parsing and status code mappings (400, 401, 403, 404, 429, 500).
3. `app/src/main/java/com/ishara/app/data/mapper/FareMapper.kt`
   - Maps backend DTOs into domain models (`RideFare`, `FareEstimate`, `FareSnapshot`) while enforcing financial sanity invariants.
4. `app/src/main/java/com/ishara/app/domain/repository/FareRepository.kt`
   - Domain repository contract for authoritative fare queries and cache clearing.
5. `app/src/main/java/com/ishara/app/data/repository/FareRepositoryImpl.kt`
   - Production repository implementation managing network calls, session verification, and in-memory cache.

### Domain Layer & Use Cases
6. `app/src/main/java/com/ishara/app/domain/model/FareModels.kt`
   - Domain entities: `RideFare`, `FareEstimate`, `FareSnapshot`, and `FareBreakdownDetails`.
7. `app/src/main/java/com/ishara/app/domain/usecase/GetRideFareUseCase.kt`
   - Use case retrieving authoritative fare calculations or immutable snapshots.
8. `app/src/main/java/com/ishara/app/domain/usecase/ClearFareStateUseCase.kt`
   - Use case clearing cached fare states during logout or role/account switching.

### Presentation Layer
9. `app/src/main/java/com/ishara/app/feature/student/fare/FareUiState.kt`
   - Unidirectional UI state hierarchy (`Loading`, `Content`, `Error`) with estimate vs final status banners.
10. `app/src/main/java/com/ishara/app/feature/student/fare/FareSummaryViewModel.kt`
    - Lifecycle-aware ViewModel driving initial fare load, refresh, error handling, and payment navigation.
11. `app/src/main/java/com/ishara/app/feature/student/fare/FareSummaryScreen.kt`
    - Material 3 Compose screen rendering authoritative total, status chip, itemized breakdown (base fare, distance fare, time fare, subtotal, taxes, service fee, discounts), pricing policy metadata, and "Proceed to Payment" action.

### Tests & Documentation
12. `app/src/test/java/com/ishara/app/FarePhaseA12Test.kt`
    - 10 exhaustive unit tests covering REST contract, integer money representation, DTO-to-domain mapping, client invariance, IDOR/403 security, 400/404 errors, cache isolation, and payment handoff.
13. `ISHAARA_FRONTEND_PHASE_A12_FARE_PRICING.md`
    - Architecture, state machine, money representation, and security documentation.
14. `ISHAARA_FRONTEND_PHASE_A12_API_CONTRACT.md`
    - Complete API contract specification.
15. `ISHAARA_FRONTEND_PHASE_A12_CHANGELOG.md`
    - This file.

---

## 2. Files Modified

1. `app/src/main/java/com/ishara/app/navigation/IshaaraDestinations.kt`
   - Added destination route: `StudentFareSummary("student/ride/{rideId}/fare")`.
2. `app/src/main/java/com/ishara/app/core/di/AppContainer.kt`
   - Registered `fareRepository`, `getRideFareUseCase`, and `clearFareStateUseCase`.
3. `app/src/main/java/com/ishara/app/MainActivity.kt`
   - Added `StudentScreenState.FareSummary`, back handling, route listener, and composable wiring.
4. `app/src/main/java/com/ishara/app/feature/student/tracking/RideTrackingViewModel.kt`
   - Updated `onPayFareClicked()` to route to `StudentFareSummary` for authoritative fare review before payment.

---

## 3. Verification & Build Status

- **A12 Specific Suite:** 10/10 Passed (`com.ishara.app.FarePhaseA12Test`).
- **A11 Regression Suite:** Passed (`com.ishara.app.RealtimePhaseA11Test`).
- **A10 Regression Suite:** Passed (`com.ishara.app.RideRequestPhaseA10Test`).
- **A09 Regression Suite:** Passed (`com.ishara.app.DiscoveryPhaseA09Test`).
- **A03 Regression Suite:** Passed (`com.ishara.app.UserPhaseA03Test`).
- **Full Unit Test Suite:** 592/592 Passed across all 53 test suites (`./gradlew testDebugUnitTest`).
- **Debug APK Build:** Successfully assembled (`./gradlew assembleDebug` - 39 actionable tasks: 4 executed, 35 up-to-date).

---

## 4. Phase Boundary Compliance & Deferred Work

- **No Client-Side Fare Calculation:** Client strictly consumes and displays server-provided totals and components.
- **No Payment Processing in A12:** Gateway initialization, Razorpay orders, payment intents, and card/UPI collection are strictly deferred to Phase A13 (`StudentPayment`).
- **No Driver Earnings / Commission in A12:** Driver payouts, operator margins, and platform revenue calculations are strictly deferred to Phases A16–A18.
