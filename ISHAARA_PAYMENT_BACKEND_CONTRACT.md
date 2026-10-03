# ISHAARA — Payment, Fare & Ticketing Backend Contract Audit
**Phase 13: Digital Ticketing, Fare & Payment Foundation**

---

## 1. Executive Summary

This document defines the audited, authoritative backend contract for payments, fares, and ticketing within the ISHAARA platform.
The findings are based upon deep static analysis of the backend source code located in `/home/dev/Desktop/ishara-backend/src/modules/payments/` and `/home/dev/Desktop/ishara-backend/src/modules/rides/`.

### Verified Endpoints Matrix

| Endpoint | HTTP Method | Auth Required | Allowed Roles | Middleware / Rate Limit | Purpose |
|:---|:---:|:---:|:---:|:---:|:---|
| `/api/v1/rides/:rideId/payment` | `POST` | Bearer JWT | `USER` (Passenger) | `requireUser`, `paymentRateLimiter`, `validateBody(createPaymentOrderSchema)` | Creates or retrieves an active payment order session for an eligible ride. |
| `/api/v1/rides/:rideId/payment` | `GET` | Bearer JWT | `USER` (Passenger) or `DRIVER_CONDUCTOR` (Assigned Driver) | `requireAuth` | Retrieves the authoritative payment record and capture status for a ride. |
| `/api/v1/payments/:paymentId/verify` | `POST` | Bearer JWT | `USER` (Payment Owner) | `requireAuth`, `paymentRateLimiter`, `validateBody(verifyPaymentSchema)` | Authoritatively verifies provider checkout signature and transitions payment to `CAPTURED`. |
| `/api/v1/payments/:paymentId/refund` | `POST` | Bearer JWT | `USER` (Owner) or Admin (`x-admin-key`) | `requireAuth`, `paymentRateLimiter`, `validateBody(refundPaymentSchema)` | Initiates a full or partial refund for a `CAPTURED` payment. |
| `/api/v1/payments/webhooks/razorpay` | `POST` | HMAC Header | Public / Gateway | `webhookRateLimiter` | Asynchronous webhook ingestion for gateway events (`payment.captured`, etc.). |
| `/api/v1/payments/settlements/:settlementId/process` | `POST` | Admin Key | Admin (`x-admin-key`) | `requireAuth`, `requireAdminKey` | Triggers real-money settlement payout to operator/driver bank account. |
| `/api/v1/payments/settlements/:settlementId/reconcile` | `POST` | Admin Key | Admin (`x-admin-key`) | `requireAuth`, `requireAdminKey` | Reconciles hung settlement against gateway. |

---

## 2. Endpoint 1: Create Payment Order (`POST /api/v1/rides/:rideId/payment`)

### 2.1 Protocol Details
* **Method**: `POST`
* **Path**: `/api/v1/rides/:rideId/payment`
* **Authentication**: `Authorization: Bearer <jwt_token>`
* **Allowed Roles**: `USER` strictly enforced via `requireUser` middleware.
* **Headers**:
  * `Content-Type: application/json`
  * `Idempotency-Key: <string>` *(Optional, checked in `payment.controller.ts` and stored in `PaymentModel`)*.

### 2.2 Request Body Schema
Schema defined by Zod in `payment.schema.ts`:
```typescript
export const createPaymentOrderSchema = z.object({
  fareOverrideMinor: z.number().int().positive().optional(),
});
```

* **Required Fields**: None (empty JSON object `{}` is valid).
* **Optional Fields**:
  * `fareOverrideMinor` (integer, strictly positive): Optional manual fare override in minor units (paise).

### 2.3 Preconditions & Business Invariants
1. **Valid Ride ID**: `rideId` must be a valid 24-character MongoDB `ObjectId`. If invalid $\rightarrow$ `400 Bad Request` (`INVALID_ID`).
2. **Ride Existence**: Ride must exist in database. If missing $\rightarrow$ `404 Not Found` (`RIDE_NOT_FOUND`).
3. **Ownership**: Authenticated caller `userId` must match `ride.userId`. If caller is not owner $\rightarrow$ `403 Forbidden` (`RIDE_NOT_AUTHORIZED`).
4. **Payable Ride Status**: Ride must be in an eligible state:
   - `CREATED`
   - `DRIVER_ARRIVING`
   - `PICKED_UP`
   - `IN_PROGRESS`
   - `COMPLETED`
   If ride status is `CANCELLED` $\rightarrow$ `409 Conflict` (`RIDE_NOT_PAYABLE`).
5. **Already Captured Guard**: If an existing payment record for this ride has status `CAPTURED` $\rightarrow$ `409 Conflict` (`PAYMENT_ALREADY_CAPTURED`).
6. **Active Session Reuse**: If an active payment record exists with status `ORDER_CREATED` and `expiresAt > now`, the backend reuses and returns that session rather than creating duplicate gateway orders.
7. **Ride Status Mutation**: On successful order creation, if `ride.paymentStatus === "UNPAID"`, it is updated to `"PENDING"`.

### 2.4 Response Body (`201 Created` or `200 OK`)
Standard `sendSuccess` wrapper containing `CheckoutSessionDetails`:
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Payment order created successfully",
  "data": {
    "paymentId": "6504a1b2c3d4e5f678901234",
    "rideId": "6504a1b2c3d4e5f678901235",
    "grossAmountMinor": 2500,
    "currency": "INR",
    "provider": "razorpay",
    "providerOrderId": "order_NXyz1234567890",
    "keyId": "rzp_test_YourKeyHere",
    "qrPayload": "upi://pay?pa=rzp_test_YourKeyHere@icici&pn=IsaharaMobility&tr=6504a1b2c3d4e5f678901234&am=25.00&cu=INR&mc=4121&tn=Ride_6504a1b2c3d4e5f678901235",
    "expiresAt": "2026-09-27T16:30:00.000Z"
  }
}
```

| Response Field | Type | Required | Description |
|:---|:---:|:---:|:---|
| `paymentId` | `string` | Yes | 24-character hexadecimal MongoDB ID of the internal payment document. |
| `rideId` | `string` | Yes | ID of the ride being paid for. |
| `grossAmountMinor` | `number` (integer) | Yes | Total fare payable in minor currency units (paise for INR; e.g. 2500 = ₹25.00). |
| `currency` | `string` | Yes | 3-letter ISO currency code (strictly `"INR"`). |
| `provider` | `string` | Yes | Payment provider type: `"razorpay"` or `"mock"`. |
| `providerOrderId` | `string` | Yes | Gateway order reference (e.g. Razorpay Order ID `order_...`). |
| `keyId` | `string` | No | Public gateway client key (e.g. Razorpay Key ID `rzp_test_...` or `rzp_live_...`). Safe for mobile client use. |
| `qrPayload` | `string` | No | Authoritative standard NPCI UPI Intent URI (`upi://pay?...`) for opening UPI apps or rendering dynamic QR. |
| `expiresAt` | `string` (ISO 8601) | Yes | Expiration timestamp of the payment order session. |

---

## 3. Endpoint 2: Get Ride Payment Status (`GET /api/v1/rides/:rideId/payment`)

### 3.1 Protocol Details
* **Method**: `GET`
* **Path**: `/api/v1/rides/:rideId/payment`
* **Authentication**: `Authorization: Bearer <jwt_token>`
* **Allowed Roles**: Either the owning Passenger (`USER`) or the assigned Driver (`DRIVER_CONDUCTOR`).
* **Purpose**: Allows client to reconcile or check authoritative payment status after app restart, backgrounding, or timeout.

### 3.2 Response Body (`200 OK`)
Returns authoritative `PaymentRecord`:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Payment retrieved successfully",
  "data": {
    "id": "6504a1b2c3d4e5f678901234",
    "rideId": "6504a1b2c3d4e5f678901235",
    "userId": "6504a1b2c3d4e5f678901236",
    "driverId": "6504a1b2c3d4e5f678901237",
    "grossAmountMinor": 2500,
    "platformFeeMinor": 250,
    "providerAmountMinor": 2250,
    "refundedAmountMinor": 0,
    "currency": "INR",
    "status": "CAPTURED",
    "provider": "razorpay",
    "providerOrderId": "order_NXyz1234567890",
    "providerPaymentId": "pay_NXyz0987654321",
    "providerSignature": "abcdef0123456789...",
    "idempotencyKey": "idem_123",
    "capturedAt": "2026-09-27T16:05:12.345Z",
    "expiresAt": "2026-09-27T16:30:00.000Z",
    "createdAt": "2026-09-27T16:00:00.000Z",
    "updatedAt": "2026-09-27T16:05:12.345Z"
  }
}
```

---

## 4. Endpoint 3: Verify Payment (`POST /api/v1/payments/:paymentId/verify`)

### 4.1 Protocol Details
* **Method**: `POST`
* **Path**: `/api/v1/payments/:paymentId/verify`
* **Authentication**: `Authorization: Bearer <jwt_token>`
* **Allowed Roles**: `USER` (Owner of the payment record).

### 4.2 Request Body Schema
Schema defined in `payment.schema.ts`:
```typescript
export const verifyPaymentSchema = z.object({
  providerOrderId: z.string().min(1, "providerOrderId is required"),
  providerPaymentId: z.string().min(1, "providerPaymentId is required"),
  signature: z.string().min(1, "signature is required"),
});
```

* **Required Fields**:
  * `providerOrderId`: Must match `payment.providerOrderId`.
  * `providerPaymentId`: Gateway transaction reference (e.g. `pay_...`).
  * `signature`: Cryptographic HMAC-SHA256 signature generated by provider.
* **Optional Fields**: None.

### 4.3 Preconditions & Verification Logic
1. **Payment Record Existence**: If `paymentId` not found $\rightarrow$ `404 Not Found` (`PAYMENT_NOT_FOUND`).
2. **Ownership**: Caller must be the `userId` of the payment $\rightarrow$ `403 Forbidden` (`PAYMENT_NOT_AUTHORIZED`).
3. **Idempotent Capture**: If `payment.status === "CAPTURED"`, the backend safely returns the existing record immediately without re-processing.
4. **Expiration Check**: If `expiresAt < now`, backend marks payment `FAILED` and throws `400 Bad Request` (`PAYMENT_EXPIRED`).
5. **Order ID Matching**: `providerOrderId` must match `payment.providerOrderId` $\rightarrow$ `400 Bad Request` (`PAYMENT_SIGNATURE_INVALID`).
6. **Signature Verification**:
   - Live mode: Computes HMAC-SHA256 of `${providerOrderId}|${providerPaymentId}` using `RAZORPAY_KEY_SECRET` and performs timing-safe comparison.
   - Mock/test mode: Allows `"mock_signature"` or dev secret HMAC.
   - If invalid $\rightarrow$ `400 Bad Request` (`PAYMENT_SIGNATURE_INVALID`).
7. **Mutations on Success**:
   - `payment.status` becomes `"CAPTURED"`.
   - `payment.capturedAt` set to timestamp.
   - Authoritative `Ride.paymentStatus` is updated to `"PAID"`.
   - Double-entry ledger entries recorded (debit clearing, credit platform revenue, credit driver payable).
   - Settlement record initiated.
   - Realtime notification dispatched to driver channel (`"driver:payment_confirmed"`).

### 4.4 Response Body (`200 OK`)
Returns the captured `PaymentRecord` with status `"CAPTURED"`.

---

## 5. Payment Statuses & State Transitions

### Verified Backend Payment Statuses
Defined in `payment.constants.ts`:
```typescript
export const PAYMENT_STATUS = {
  CREATED: "CREATED",
  ORDER_CREATED: "ORDER_CREATED",
  AUTHORIZED: "AUTHORIZED",
  CAPTURED: "CAPTURED",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
  REFUND_PENDING: "REFUND_PENDING",
  PARTIALLY_REFUNDED: "PARTIALLY_REFUNDED",
  REFUNDED: "REFUNDED",
} as const;
```

### Client Handling Matrix

| Backend Payment Status | Meaning | Android Client UI State | Permitted Actions |
|:---|:---|:---|:---|
| `ORDER_CREATED` | Payment session created on server & provider; waiting for student checkout. | `CheckoutReady` | Open UPI app or Gateway UI; cancel payment. |
| `AUTHORIZED` | Payment authorized at provider, pending capture. | `Verifying` | Await verification completion; poll status if network drops. |
| `CAPTURED` | Payment authoritatively captured and verified. Ride is marked `PAID`. | `Paid` / `Receipt` | View digital receipt; proceed to live ride tracking. |
| `FAILED` | Gateway payment failed or order expired. | `PaymentFailed` | Retry payment with fresh order. |
| `CANCELLED` | Order cancelled by user or invalidated. | `Cancelled` | Re-initiate payment or change method. |
| `REFUND_PENDING` | Refund requested and processing. | `RefundPending` | View refund details. |
| `PARTIALLY_REFUNDED`| Partial refund completed. | `PartiallyRefunded`| View receipt with refund adjustment. |
| `REFUNDED` | Full refund completed. | `Refunded` | View refund receipt. |
| *Unknown Status* | Future unmapped status string. | `Unknown` | Fallback gracefully; do not crash. |

---

## 6. Fare Representation & Units

### Backend Invariant
All financial calculations and representations in ISHAARA are executed strictly in **integer minor units (paise in INR)**.
- $1\text{ INR} = 100\text{ paise}$.
- Floating point money calculations are strictly prohibited.
- `grossAmountMinor`: Integer paise (e.g. `2500` = ₹25.00).
- `platformFeeMinor`: Platform revenue share (10% standard or fixed paise).
- `providerAmountMinor`: Driver share ($90\%$ standard).
- Hard Invariant: $\text{grossAmountMinor} \equiv \text{platformFeeMinor} + \text{providerAmountMinor}$.

### Currency
- Strictly `"INR"` (Indian Rupee, symbol `₹`).

---

## 7. Ticket & Digital Receipt Audit

* **Digital Ticket Subsystem**: `NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT`.
  - There is no `/api/v1/tickets` endpoint or `Ticket` collection in MongoDB.
  - The backend returns authoritative payment receipt references via `paymentId`, `providerOrderId`, `providerPaymentId`, and `capturedAt`.
  - The Android client renders an authoritative **Ride Payment Receipt** utilizing verified ride details and payment record data (`paymentId`, `rideId`, `grossAmountMinor`, `capturedAt`, and route information), without fabricating unsupported ticket serial numbers or QR codes.

---

## 8. Cash / COD Payment Audit

* **Cash Payment Endpoint**: `NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT`.
  - The backend schema `createPaymentOrderSchema` and service implementation do not provide a cash settlement endpoint.
  - The API documentation references cash conceptually, but no backend implementation exists.
  - Android client only supports verified digital payment flows (`razorpay` / UPI intent / simulated mock in test mode).

---

## 9. Error Codes & Client Handling

| HTTP Code | Error Code | Meaning | Client Action |
|:---:|:---|:---|:---|
| `400` | `INVALID_ID` | Malformed rideId or paymentId format | Show generic error; abort. |
| `400` | `PAYMENT_EXPIRED` | Payment order session expired | Prompt user to create fresh order. |
| `400` | `PAYMENT_SIGNATURE_INVALID`| Signature mismatch or verification failed | Mark verification failed; allow retry. |
| `401` | `UNAUTHORIZED` | Session expired or missing token | Redirect to login via `SessionInvalidationCoordinator`. |
| `403` | `RIDE_NOT_AUTHORIZED` | Caller does not own the ride | Deny access with informative alert. |
| `404` | `RIDE_NOT_FOUND` | Ride does not exist | Return to Home screen. |
| `404` | `PAYMENT_NOT_FOUND` | No payment record exists for ride | Display "Payment required" initiation card. |
| `409` | `RIDE_NOT_PAYABLE` | Ride cancelled or in invalid state | Inform user ride is not payable. |
| `409` | `PAYMENT_ALREADY_CAPTURED`| Payment already completed | Transition immediately to Paid / Receipt state. |
| `429` | `RATE_LIMIT_EXCEEDED` | Payment rate limiter triggered | Show "Too many requests. Please wait a moment." |
| `502` | `PAYMENT_PROVIDER_ERROR`| Gateway unreachable | Inform user gateway issue; allow retry. |
| `500` | `INTERNAL_SERVER_ERROR` | Server exception | Show friendly error message. |

---

## 10. Summary of What is NOT in the Backend Contract

1. **Dedicated Ticket Collection**: No `/tickets` route or separate ticket database model exists.
2. **Cash Confirmation Endpoint**: No server-side cash recording route is active.
3. **Passenger Realtime Payment Events**: WebSocket events for payments (`driver:payment_confirmed`) are emitted to the driver channel; the passenger confirms synchronously via `POST /payments/:paymentId/verify` and reconciles via `GET /rides/:rideId/payment`.
