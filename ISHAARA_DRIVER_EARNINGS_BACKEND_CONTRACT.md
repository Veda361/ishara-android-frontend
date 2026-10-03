# ISHAARA DRIVER EARNINGS & FINANCIAL BACKEND CONTRACT

**Document Version:** 1.0.0  
**Phase:** 16 — Driver Shift History, Earnings & Payout Analytics (Final Android Phase)  
**Target Platform:** Android Client / DRIVER_CONDUCTOR Subsystem  
**Backend Authority:** `ishara-backend` (`src/modules/drivers/driver-earnings.service.ts`, `src/modules/drivers/driver.routes.ts`, `src/modules/payments/payment.constants.ts`, `src/modules/rides/ride.service.ts`)

---

## 1. Executive Summary & Authoritative Principles

1. **Backend is the Financial Source of Truth:**  
   The Android client **NEVER** calculates driver earnings, platform fees, taxes, or settlement amounts by aggregating raw ride fares or multiplying client-side percentages. The backend computes authoritative aggregations over `PaymentModel` and `SettlementModel`.
2. **Passenger Fare ≠ Driver Earnings:**  
   Passenger gross fare includes platform deductions (`platformFeeMinor`) and may be impacted by refunds (`refundedAmountMinor`). Driver net earnings are explicitly defined by the backend ledger (`providerAmountMinor`).
3. **Driver Conductor ≠ Bus Owner / Agency Operator:**  
   Driver earnings reflect personal driver payouts and completed rides operated by the authenticated driver. Fleet revenues, agency margins, bus operator bank details, and platform admin reconciliation tools belong exclusively to the separate Agency/Bus Owner Web Dashboard and Administrative API.
4. **Shift Entity Contract:**  
   **Formal shift contract is NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT.** The backend has no `Shift` model or endpoints. Driver operational history is bounded by calendar date/time ranges (`today`, `week`, `month`, `custom`) and completed rides operated by the driver. The client will not fabricate shifts.

---

## 2. Verified Backend Endpoints

| Domain | Endpoint | Method | Auth | Role | Purpose |
|---|---|---|---|---|---|
| Driver Financials | `/api/v1/drivers/me/earnings` | `GET` | Bearer JWT | `DRIVER_CONDUCTOR` | Authoritative bounded read model for driver earnings, platform deductions, net payout, and settlement status |
| Driver Operations | `/api/v1/drivers/me/operations/context` | `GET` | Bearer JWT | `DRIVER_CONDUCTOR` | Operational snapshot including online status, active vehicle, active trip, in-flight rides, and today's completed rides count |
| Driver Rides | `/api/v1/drivers/me/rides` | `GET` | Bearer JWT | `DRIVER_CONDUCTOR` | Paginated ride history operated by the driver, enriched with financial breakdown when `withFinancials=true` |
| Driver Profile | `/api/v1/drivers/me/profile` | `GET` | Bearer JWT | `DRIVER_CONDUCTOR` | Authenticated driver profile with masked license |

*Note: Operator payout verification (`PATCH /api/v1/operators/:id/verify-payout`) and settlement management (`POST /api/v1/payments/settlements/:id/process`) require Admin keys (`requireAdminKey`) and are not accessible to the driver client.*

---

## 3. Endpoint Specifications

### 3.1 `GET /api/v1/drivers/me/earnings`

Retrieves the authoritative driver earnings read model for a specified time period.

#### Request Query Parameters

| Field | Type | Required | Default | Meaning / Constraints |
|---|---|---|---|---|
| `period` | String | No | `"today"` | One of `"today"`, `"week"`, `"month"`, `"custom"` |
| `from` | String | Conditional | None | ISO 8601 UTC timestamp. Required when `period="custom"`. Must be `<=` `to`. |
| `to` | String | Conditional | None | ISO 8601 UTC timestamp. Required when `period="custom"`. Must be `>=` `from`. |
| `timezone` | String | No | `"Asia/Kolkata"` | IANA timezone identifier for period boundary calculation |
| `page` | Integer | No | `1` | Page number (`min: 1`) |
| `limit` | Integer | No | `20` | Items per page (`min: 1`, `max: 50`) |

#### Response Shape

```json
{
  "success": true,
  "data": {
    "period": {
      "period": "today",
      "from": "2026-09-28T00:00:00.000+05:30",
      "to": "2026-09-28T23:59:59.999+05:30",
      "timezone": "Asia/Kolkata"
    },
    "summary": {
      "grossEarningsMinor": 80000,
      "platformDeductionsMinor": 8000,
      "netEarningsMinor": 72000,
      "refundDeductionsMinor": 0,
      "completedRidesCount": 2,
      "settlementSummary": {
        "settledAmountMinor": 45000,
        "pendingSettlementAmountMinor": 27000,
        "unreadySettlementAmountMinor": 0,
        "failedSettlementAmountMinor": 0
      },
      "currency": "INR"
    },
    "items": [
      {
        "rideId": "66f7f0a1b2c3d4e5f6789012",
        "tripId": "66f7f0a1b2c3d4e5f6789034",
        "completedAt": "2026-09-28T08:15:00.000Z",
        "pickupAddress": "Sector 62, Noida",
        "destinationAddress": "Botanical Garden, Noida",
        "grossAmountMinor": 50000,
        "platformFeeMinor": 5000,
        "netAmountMinor": 45000,
        "currency": "INR",
        "paymentStatus": "CAPTURED",
        "settlementStatus": "PROCESSED"
      }
    ],
    "pagination": {
      "total": 2,
      "page": 1,
      "limit": 20,
      "hasMore": false
    }
  },
  "timestamp": "2026-09-28T14:20:00.000Z"
}
```

#### Financial Fields in Summary

| Field | Type | Unit | Meaning |
|---|---|---|---|
| `grossEarningsMinor` | Long (Int64) | Paise (1/100 INR) | Sum of passenger captured gross fares (`PaymentModel.grossAmountMinor`) |
| `platformDeductionsMinor` | Long (Int64) | Paise (1/100 INR) | Sum of platform service commission retained (`PaymentModel.platformFeeMinor`) |
| `netEarningsMinor` | Long (Int64) | Paise (1/100 INR) | Driver's authoritative net share (`PaymentModel.providerAmountMinor`) |
| `refundDeductionsMinor` | Long (Int64) | Paise (1/100 INR) | Sum of passenger refunds debited (`PaymentModel.refundedAmountMinor`) |
| `completedRidesCount` | Int | Count | Total completed rides within the bounded period |
| `currency` | String | ISO Code | Currency code (verified `"INR"`) |

#### Settlement Summary Fields

| Field | Type | Unit | Meaning |
|---|---|---|---|
| `settledAmountMinor` | Long (Int64) | Paise | Sum of driver shares where `SettlementModel.status == "PROCESSED"` (transferred) |
| `pendingSettlementAmountMinor` | Long (Int64) | Paise | Sum of driver shares where `SettlementModel.status == "PENDING"` (in queue) |
| `unreadySettlementAmountMinor` | Long (Int64) | Paise | Sum of driver shares where `SettlementModel.status == "NOT_READY"` |
| `failedSettlementAmountMinor` | Long (Int64) | Paise | Sum of driver shares where `SettlementModel.status == "FAILED"` |

#### Per-Ride Earnings Item Fields

| Field | Type | Nullable | Meaning |
|---|---|---|---|
| `rideId` | String | No | Unique identifier of completed ride |
| `tripId` | String | No | Identifier of the operational trip |
| `completedAt` | String (ISO) | Yes | Timestamp of ride completion |
| `pickupAddress` | String | No | Sanitized pickup address |
| `destinationAddress` | String | No | Sanitized destination address |
| `grossAmountMinor` | Long | No | Total passenger fare charged |
| `platformFeeMinor` | Long | No | Commission retained by platform |
| `netAmountMinor` | Long | No | Driver's net earning for this ride |
| `currency` | String | No | Currency (e.g. `"INR"`) |
| `paymentStatus` | String | No | Authoritative payment status (`CAPTURED`, etc.) |
| `settlementStatus` | String | No | Authoritative settlement status (`PROCESSED`, `PENDING`, `UNSETTLED`, etc.) |

---

### 3.2 `GET /api/v1/drivers/me/rides`

Retrieves paginated rides operated by the authenticated driver.

#### Query Parameters

| Field | Type | Required | Meaning |
|---|---|---|---|
| `status` | String | No | Filter by ride status (`REQUESTED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) |
| `tripId` | String | No | Filter by 24-char Hex ObjectId of trip |
| `page` | Integer | No | Page number (default `1`) |
| `limit` | Integer | No | Page size (default `20`, max `50`) |
| `withFinancials` | Boolean | No | When `"true"`, enriches each item with `financialStatus` via anti-N+1 lookup |
| `period` | String | No | `"today"`, `"week"`, `"month"` |
| `from` | String | No | ISO 8601 start timestamp |
| `to` | String | No | ISO 8601 end timestamp |
| `timezone` | String | No | Target timezone (default `"Asia/Kolkata"`) |

---

## 4. Status Enums & Client Mapping

### 4.1 Payment Statuses (`PAYMENT_STATUS`)

| Backend Status | Domain Enum | Meaning | Client Behavior |
|---|---|---|---|
| `CAPTURED` | `CAPTURED` | Payment successfully collected | Counted in driver earnings |
| `PARTIALLY_REFUNDED` | `PARTIALLY_REFUNDED` | Partial refund processed | Reflected in refund deductions |
| `REFUNDED` | `REFUNDED` | Full fare refunded | Driver net earnings adjusted |
| `AUTHORIZED` | `AUTHORIZED` | Pre-auth hold placed | Not yet captured into driver earnings |
| `CREATED` / `ORDER_CREATED` | `ORDER_CREATED` | Order initialized | In-progress / pending |
| `FAILED` | `FAILED` | Payment failed | No driver earnings captured |
| `CANCELLED` | `CANCELLED` | Payment aborted | No driver earnings captured |
| *(unknown)* | `UNKNOWN` | Forward compatibility | Fallback gracefully without crash |

### 4.2 Settlement Statuses (`SETTLEMENT_STATUS`)

| Backend Status | Domain Enum | Meaning | Client Representation |
|---|---|---|---|
| `PROCESSED` | `PROCESSED` | Payout transferred to bank account | "Settled" / Success Badge |
| `PENDING` | `PENDING` | Scheduled for next payout cycle | "Processing Payout" / Warning Badge |
| `NOT_READY` | `NOT_READY` | Under hold or awaiting clearing | "Unready / On Hold" / Neutral Badge |
| `RECONCILING` | `RECONCILING` | Ledger reconciliation in progress | "Reconciling" / Info Badge |
| `FAILED` | `FAILED` | Bank payout transfer failed | "Payout Failed" / Danger Badge |
| `UNSETTLED` | `UNSETTLED` | Payment captured but not yet queued | "Unsettled" / Neutral Badge |
| *(unknown)* | `UNKNOWN` | Forward compatibility | "Status Unknown" / Neutral Badge |

---

## 5. Unverified / Unsupported Capabilities (Explicit Exclusions)

The following items are **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** and must **NOT** be invented:

1. **Formal Shift Entity:**  
   *NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT.* No shift check-in/out or shift ID exists on backend. Operational context and date-bounded earnings are used instead.
2. **Driver Bank Account Management in Client:**  
   *NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT.* Bus operator payout bank accounts are managed by operators and verified via Admin API. Drivers do not submit bank credentials in the mobile app.
3. **Client-Side Financial Calculations:**  
   *PROHIBITED.* Total earnings, fees, and taxes must never be computed by summing arbitrary ride fares on Android.
4. **Estimated / Future Earnings Projections:**  
   *NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT.* The backend only aggregates captured transactions. No predictions or gamified streaks exist.
5. **Cash Collections as Authoritative Earnings:**  
   *NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT.* In Phase 13/16, all captured payments flow through Razorpay or Mock digital gateway. Cash is not modeled as driver earnings.
