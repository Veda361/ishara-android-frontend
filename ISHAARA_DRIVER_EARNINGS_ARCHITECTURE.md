# ISHAARA DRIVER EARNINGS & PAYOUT ANALYTICS ARCHITECTURE

**Document Version:** 1.0.0  
**Phase:** 16 — Driver Shift History, Earnings & Payout Analytics (Final Android Phase)  
**Target Subsystem:** Driver / Conductor Financial Domain  

---

## 1. Architectural Mandate & Philosophy

In mobility platforms, financial reporting is mission-critical. Drivers depend on accurate compensation figures. Errors, client-side rounding drifts, or fabricated calculations destroy driver trust.

### Core Architecture Axioms:
1. **The Backend is the Single Source of Financial Truth:**  
   The Android application is strictly a presentation and interaction layer. It **never** calculates driver earnings, platform commissions, or settlement statuses locally.
2. **Exact Money Representation:**  
   Floating-point types (`Double`, `Float`) are prohibited for financial calculations and storage. The domain uses `Money(val amountMinor: Long, val currency: String = "INR")` operating in integer paise.
3. **Role Segregation:**  
   Driver financial screens are protected by `UserRole.DRIVER_CONDUCTOR` validation in the UI and Bearer JWT verification on the backend. Passenger users cannot navigate to or inspect driver earnings.
4. **Honest Operational & Shift Semantics:**  
   Because the backend does not define a formal `Shift` entity, the app does not invent shifts. It presents date-bounded periods (`Today`, `This Week`, `This Month`, `Custom Range`) and completed ride history.
5. **Glanceable, Professional UX:**  
   Designed with the Phase 02 Design System. Calm, readable typography, clear badges, distraction-free numbers, and zero gamification, neon graphs, or speculative stock charts.

---

## 2. End-to-End Financial Pipeline

```
                 ISHAARA BACKEND
                       │
              ┌────────┴────────┐
              ▼                 ▼
     GET /me/earnings     GET /me/rides?withFinancials=true
              │                 │
              └────────┬────────┘
                       ▼
         DriverEarningsRemoteDataSource
                       │
                       ▼
           DriverEarningsRepository
                       │
                       ▼
          Domain Use Cases & Mappers
          - GetDriverEarningsUseCase
          - RefreshDriverEarningsUseCase
          - GetDriverRideHistoryUseCase
                       │
                       ▼
           DriverEarningsViewModel
                       │
                       ▼
          DriverEarningsUiState (MVI/Flow)
                       │
                       ▼
              DriverEarningsScreen
```

---

## 3. Domain Model Architecture

### 3.1 Money & Currency Handling
Reuses the Phase 13 `Money` abstraction:
```kotlin
data class Money(
    val amountMinor: Long,
    val currency: String = "INR"
) {
    init {
        require(amountMinor >= 0) { "Amount minor cannot be negative: $amountMinor" }
    }

    val amountMajor: Double
        get() = amountMinor.toDouble() / 100.0

    fun formatDisplay(): String {
        return when (currency.uppercase(Locale.ROOT)) {
            "INR" -> String.format(Locale.forLanguageTag("en-IN"), "₹%.2f", amountMajor)
            else -> String.format(Locale.US, "$currency %.2f", amountMajor)
        }
    }
}
```

### 3.2 Earnings Summary
```kotlin
data class DriverEarningsSummary(
    val grossEarnings: Money,
    val platformDeductions: Money,
    val netEarnings: Money,
    val refundDeductions: Money,
    val completedRidesCount: Int,
    val settlementSummary: DriverSettlementSummary,
    val currency: String
)
```

### 3.3 Settlement Summary & Statuses
```kotlin
data class DriverSettlementSummary(
    val settledAmount: Money,
    val pendingSettlementAmount: Money,
    val unreadySettlementAmount: Money,
    val failedSettlementAmount: Money
)

enum class DomainSettlementStatus {
    NOT_READY,
    PENDING,
    PROCESSING,
    PROCESSED,
    RECONCILING,
    FAILED,
    UNSETTLED,
    UNKNOWN;
}
```

### 3.4 Per-Ride Financial Item
```kotlin
data class DriverRideEarningsItem(
    val rideId: String,
    val tripId: String,
    val completedAt: String?,
    val pickupAddress: String,
    val destinationAddress: String,
    val grossAmount: Money,
    val platformFee: Money,
    val netAmount: Money,
    val currency: String,
    val paymentStatus: DomainPaymentStatus,
    val settlementStatus: DomainSettlementStatus
)
```

---

## 4. Concurrency, Race Condition & Pagination Protection

1. **Stale Filter Protection:**  
   When the driver switches period filters (e.g., from "Today" to "This Week"), the active coroutine job is cancelled, page is reset to 1, and any in-flight response from the previous filter is discarded.
2. **Duplicate Request Guard:**  
   `isRefreshing` and `isLoadingMore` boolean flags prevent concurrent duplicate API triggers when pulling to refresh or scrolling the list.
3. **Pagination Reset:**  
   Whenever date filters change, pagination restarts from `page = 1`. Next page requests strictly append non-duplicate ride items keyed by `rideId`.

---

## 5. Security & Privacy Safeguards

- **Auth Injection:** Reuses `SessionStore` and `IshaaraHttpClient` Bearer token injection.
- **Role Guarding:** `MainActivity` and `DriverContainerScreen` ensure only users with `UserRole.DRIVER_CONDUCTOR` can access the driver graph.
- **Zero Sensitive Credential Logging:** Financial amounts and tokens are never logged to console or crash reporting.
- **Passenger PII Privacy:** Driver ride history only displays pickup and destination formatted addresses; passenger phone numbers, email addresses, and personal profiles are omitted.

---

## 6. Offline Strategy & Freshness Display

- Financial data is network-first to guarantee truth.
- When network is unavailable:
  - If cached/existing data is present in memory, it displays with an honest freshness notice (e.g., "Offline — Showing data loaded earlier").
  - If no data was loaded, the screen presents a clean offline state with a "Connect to Internet & Retry" CTA.
  - The client **never** displays stale financial data as current live balances.
