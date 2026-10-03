# ISHAARA Frontend Phase A16 — Forensic Audit & Backend Contract

**Date:** 2026-10-02  
**Target:** Driver Earnings, Shift History, Settlement Status & Payout Analytics  
**Backend Source:** `ishara-backend/src/modules/drivers/` (`driver-earnings.service.ts`, `driver.routes.ts`), `ishara-backend/src/modules/payments/` (`payment.constants.ts`), `ishara-backend/src/modules/rides/` (`ride.service.ts`)  
**Status:** **AUDIT PASSED (100% Verified, Ready for Phase A17)**

---

## PART 1 — DRIVER EARNINGS FORENSIC AUDIT

### 1. Which endpoints actually exist?
The following driver earnings and operations endpoints are active in the backend:
1. `GET /api/v1/drivers/me/earnings` (Authoritative driver earnings read model, summary, and paginated ride breakdown)
2. `GET /api/v1/drivers/me/operations/context` (Driver operational context, online status, active vehicle, active trip, today's ride count)
3. `GET /api/v1/drivers/me/rides` (Paginated completed ride history, enriches financials when `withFinancials=true`)
4. `GET /api/v1/drivers/me/profile` (Driver profile & verification status)

### 2. Which HTTP methods are supported?
- `GET`: All driver financial data is read-only from the mobile client. Mutation of settlements or operator margins is restricted to administrative and web agency portals.

### 3. Which roles can call each endpoint?
- **Only `DRIVER_CONDUCTOR`**: Both backend middleware (`requireRole(["DRIVER_CONDUCTOR"])`) and Android client navigation guards enforce role isolation. Authenticated `USER` (passengers) attempting to query `/drivers/me/earnings` receive `403 Forbidden` (`FORBIDDEN`).

### 4. What authorization rules exist?
- Bearer session JWT (`requireAuth`).
- Caller identity and driver profile are derived strictly from server-validated session tokens; caller user ID or driver ID is NEVER accepted via query parameters or path variables.

### 5. Who calculates driver earnings?
- **THE BACKEND IS THE SOLE SOURCE OF TRUTH.**
- The Android client NEVER computes earnings, commissions, taxes, or settlement amounts by aggregating ride fares or applying floating-point percentages. The backend computes authoritative aggregations over `PaymentModel` and `SettlementModel`.

### 6. What query parameters does `GET /api/v1/drivers/me/earnings` accept?
- `period` (optional string): `"today"`, `"week"`, `"month"`, `"custom"`. Defaults to `"today"`.
- `from` (conditional string): ISO 8601 UTC timestamp. Required when `period="custom"`. Must satisfy `from <= to`.
- `to` (conditional string): ISO 8601 UTC timestamp. Required when `period="custom"`. Must satisfy `to >= from`.
- `timezone` (optional string): IANA timezone identifier (e.g. `"Asia/Kolkata"`).
- `page` (optional integer): Defaults to `1` (min: `1`).
- `limit` (optional integer): Defaults to `20` (min: `1`, max: `50`).

### 7. What response structure is returned?
- Standard JSON envelope:
  - `period`: `{ period, from, to, timezone }`
  - `summary`:
    - `grossEarningsMinor` (Long, paise)
    - `platformDeductionsMinor` (Long, paise)
    - `netEarningsMinor` (Long, paise)
    - `refundDeductionsMinor` (Long, paise)
    - `completedRidesCount` (Int)
    - `currency` (String, e.g. `"INR"`)
    - `settlementSummary`:
      - `settledAmountMinor` (Long, paise)
      - `pendingSettlementAmountMinor` (Long, paise)
      - `unreadySettlementAmountMinor` (Long, paise)
      - `failedSettlementAmountMinor` (Long, paise)
  - `items`: List of completed ride financial breakdowns:
    - `rideId`, `tripId`, `completedAt`, `pickupAddress`, `destinationAddress`
    - `grossAmountMinor`, `platformFeeMinor`, `netAmountMinor`, `currency`
    - `paymentStatus` (`CAPTURED`, `PENDING`, `FAILED`, `REFUNDED`)
    - `settlementStatus` (`PROCESSED`, `PENDING`, `UNSETTLED`, `FAILED`)
  - `pagination`: `{ total, page, limit, hasMore }`

### 8. How is money represented?
- Zero floating-point arithmetic.
- Strict 64-bit integer minor units (`paise`) encapsulated in the domain model `Money(amountMinor: Long, currency: String = "INR")`.

### 9. Does a formal `Shift` entity exist in the backend?
- **NO.** The backend has no `Shift` database model, entity, or lifecycle. Driver earnings are bounded by calendar dates and time periods (`today`, `week`, `month`, `custom`). The mobile frontend strictly avoids fabricating shift models or punch-in/out mocks.

### 10. How are operator margins, fleet payouts, and reconciliation handled?
- Bus owner revenues, fleet commission splits, bank account IFSC routing, and payout processing (`PATCH /api/v1/operators/:id/verify-payout`, `POST /api/v1/payments/settlements/:id/process`) require Admin/Agency keys and are deferred to **Phase A17 (Agency Dashboard & Fleet Management Portal)**.

---

## PART 2 — ANDROID FRONTEND ARCHITECTURE AUDIT

| Layer | Component | Status | Verification Details |
|---|---|---|---|
| **Data (DTOs)** | [`DriverEarningsDtos.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/dto/DriverEarningsDtos.kt) | Verified | Full JSON serialization/deserialization for period, summary, settlementSummary, ride items, and pagination. |
| **Data (Remote DS)** | [`DriverEarningsRemoteDataSourceImpl.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/datasource/DriverEarningsRemoteDataSource.kt) | Verified | Authorized HTTP GET execution to `/api/v1/drivers/me/earnings` with query param encoding. |
| **Data (Repository)** | [`DriverEarningsRepositoryImpl.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/repository/DriverEarningsRepositoryImpl.kt) | Verified | Bearer token injection, cache strategy, custom date validation, error translation. |
| **Domain (Models)** | [`DriverEarningsModels.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/model/DriverEarningsModels.kt) | Verified | Integer paise `Money` types, enum domain representations, immutable summary and ride models. |
| **Domain (UseCases)** | [`GetDriverEarningsUseCase.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/usecase/GetDriverEarningsUseCase.kt)<br>[`RefreshDriverEarningsUseCase.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/usecase/RefreshDriverEarningsUseCase.kt) | Verified | Clean single-responsibility use cases injected via `AppContainer`. |
| **Presentation (ViewModel)** | [`DriverEarningsViewModel.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/driver/earnings/DriverEarningsViewModel.kt) | Verified | `DriverEarningsUiState` state machine, anti-race atomic request counter, deduplicated pagination, pull-to-refresh, session expiration handling, account-switch state clearing. |
| **Presentation (UI)** | [`DriverEarningsScreen.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/driver/earnings/DriverEarningsScreen.kt) | Verified | TopBar, Period Selector Chips, Metric Cards (Net, Gross, Deductions), Settlement Breakdown Card, Paginated Ride Cards with badges, Empty/Error/Loading states. |
| **Navigation & Wiring** | [`MainActivity.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt)<br>[`DriverHomeScreen.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/driver/ui/DriverHomeScreen.kt) | Verified | Driver Home "Earnings & Ride History" CTA triggers route to `DriverScreenState.Earnings`. Back navigation returns cleanly to `DriverScreenState.Home`. |

---

## PART 3 — TEST COVERAGE & AUDIT VERDICT

### Test Suite Execution Summary
- **Phase A16 Dedicated Tests:** 68 unit & contract tests across 7 test suites:
  1. `DriverEarningsStrictContractTest.kt`: 8 tests (API contracts, query params, headers, error parsing)
  2. `DriverEarningsPhaseA16Test.kt`: 29 tests (Master 29-point specification coverage)
  3. `DriverEarningsDomainAndMappingTest.kt`: 6 tests (Data mapping, enum fallbacks, minor-to-major conversions)
  4. `DriverEarningsRemoteDataSourceTest.kt`: 3 tests (HTTP client integration)
  5. `DriverEarningsRepositoryTest.kt`: 4 tests (Caching, validation, error mapping)
  6. `DriverEarningsSecurityAndPrecisionTest.kt`: 4 tests (paise math integrity, PII protection, isolation)
  7. `DriverEarningsViewModelTest.kt`: 14 tests (UI states, pagination deduplication, refresh, errors)
- **Full Application Regression:**
  - Total Unit Tests Executed: **727**
  - Passed: **727 (100%)**
  - Failed: **0**
  - Skipped: **0**

### Conclusion & Phase A17 Readiness
Phase A16 is **COMPLETE, TESTED, AND VERIFIED**.
The Android mobile client has achieved full feature completion through Phase A16 (the final Android phase). You are **FULLY SET** to advance to **Phase A17 (Agency Dashboard & Operator Fleet Management)**.
