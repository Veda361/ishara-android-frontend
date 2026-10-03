# ISHAARA FRONTEND PHASE A13 — API CONTRACT & SCHEMA SPECIFICATION

## Overview
This document specifies the exact REST API contract governing Phase A13 (Payments & Payment Lifecycle) between the Android client and the Node.js/Express backend (`/api/v1`).

---

## 1. POST /api/v1/rides/:rideId/payment (or /payment/order)
Creates or retrieves an active checkout session for an eligible ride.

### Authentication & Authorization
- **Headers:** `Authorization: Bearer <token>` (Required, `UserRole.USER`)
- **Headers:** `Idempotency-Key: <UUID>` (Optional / Recommended)
- **Role Invariant:** Only the passenger who created the ride (`ride.userId == req.user.id`) can create a payment order. Drivers and other passengers receive `403 Forbidden` (`RIDE_NOT_AUTHORIZED` or `FORBIDDEN`).

### Request Body
Strict Zod schema: `createPaymentOrderSchema` (`.strict()`):
```json
{
  "idempotencyKey": "f3b3c3d2-1234-4567-89ab-cdef01234567"
}
```
*Note: Any extra keys (such as `amount`, `currency`, `fareOverrideMinor`) will immediately trigger `400 Bad Request` with `unrecognized_keys` validation failure.*

### Success Response (201 Created)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Payment order created successfully",
  "data": {
    "paymentId": "651a2b3c4d5e6f7a8b9c0001",
    "rideId": "651a2b3c4d5e6f7a8b9c0002",
    "grossAmountMinor": 4500,
    "currency": "INR",
    "provider": "razorpay",
    "providerOrderId": "order_Rzp1234567890",
    "keyId": "rzp_test_public_key_id",
    "qrPayload": "upi://pay?pa=rzp_test@icici&pn=IsaharaMobility&tr=651a2b3c4d5e6f7a8b9c0001&am=45.00&cu=INR&mc=4121&tn=Ride_651a2b3c4d5e6f7a8b9c0002",
    "expiresAt": "2026-10-01T19:00:00.000Z"
  }
}
```

### Error Responses
- `400 Bad Request`: Invalid ID format or schema validation failure.
- `401 Unauthorized`: Missing or invalid Bearer token.
- `403 Forbidden`: `RIDE_NOT_AUTHORIZED` (User does not own the ride).
- `404 Not Found`: `RIDE_NOT_FOUND` (Ride does not exist).
- `409 Conflict`: `RIDE_NOT_PAYABLE` (Ride is in CANCELLED status), `IDEMPOTENCY_CONFLICT` (Idempotency key reused for different ride), or `PAYMENT_ALREADY_CAPTURED`.

---

## 2. GET /api/v1/rides/:rideId/payment
Retrieves authoritative payment status and ledger capture record for a ride.

### Authentication & Authorization
- **Headers:** `Authorization: Bearer <token>` (Required)
- **Role Invariant:** Allowed for owning passenger (`userId`) or assigned driver (`driverProfile.userId`).

### Success Response (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Payment retrieved successfully",
  "data": {
    "id": "651a2b3c4d5e6f7a8b9c0001",
    "rideId": "651a2b3c4d5e6f7a8b9c0002",
    "userId": "651a2b3c4d5e6f7a8b9c0003",
    "driverId": "651a2b3c4d5e6f7a8b9c0004",
    "grossAmountMinor": 4500,
    "platformFeeMinor": 450,
    "providerAmountMinor": 4050,
    "refundedAmountMinor": 0,
    "currency": "INR",
    "status": "CAPTURED",
    "provider": "razorpay",
    "providerOrderId": "order_Rzp1234567890",
    "providerPaymentId": "pay_tx_987654321",
    "providerSignature": "mock_signature",
    "idempotencyKey": "idem_test_key_001",
    "capturedAt": "2026-10-01T18:15:00.000Z",
    "expiresAt": "2026-10-01T19:00:00.000Z",
    "createdAt": "2026-10-01T18:10:00.000Z",
    "updatedAt": "2026-10-01T18:15:00.000Z"
  }
}
```

### Error Responses
- `401 Unauthorized`: Unauthenticated request.
- `403 Forbidden`: `PAYMENT_NOT_AUTHORIZED` (User is neither passenger nor driver).
- `404 Not Found`: `PAYMENT_NOT_FOUND` (No payment record exists for this ride).

---

## 3. POST /api/v1/payments/:paymentId/verify
Authoritative client checkout signature verification and fund capture.

### Authentication & Authorization
- **Headers:** `Authorization: Bearer <token>` (Required, owning passenger)

### Request Body
Strict Zod schema: `verifyPaymentSchema` (`.strict()`):
```json
{
  "providerOrderId": "order_Rzp1234567890",
  "providerPaymentId": "pay_tx_987654321",
  "signature": "mock_signature"
}
```

### Success Response (200 OK)
Returns authoritative `PaymentRecord` with status `CAPTURED` and timestamp `capturedAt`.
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Payment verified and captured successfully",
  "data": {
    "id": "651a2b3c4d5e6f7a8b9c0001",
    "rideId": "651a2b3c4d5e6f7a8b9c0002",
    "status": "CAPTURED",
    "grossAmountMinor": 4500,
    "currency": "INR",
    "providerPaymentId": "pay_tx_987654321",
    "capturedAt": "2026-10-01T18:15:00.000Z"
  }
}
```

### Error Responses
- `400 Bad Request`: `PAYMENT_SIGNATURE_INVALID` (HMAC verification failure or orderId mismatch) or `PAYMENT_EXPIRED`.
- `403 Forbidden`: `PAYMENT_NOT_AUTHORIZED` (IDOR violation).
- `404 Not Found`: `PAYMENT_NOT_FOUND`.
- `409 Conflict`: `PAYMENT_INVALID_STATE` (Invalid transition).

---

## 4. Contract Mismatch Resolution
| Documented Artifact | Actual Backend Contract | Frontend Resolution |
|---|---|---|
| Older docs suggested client `amount` or `fareOverrideMinor` in POST /payment | `createPaymentOrderSchema` uses `.strict()`, allowing ONLY `idempotencyKey` | Removed `fareOverrideMinor` entirely; frontend never sends amount; backend uses authoritative `ride.fareSnapshot` |
| Local callback treated as success | Backend signature verification is strictly required | Frontend triggers `POST /api/v1/payments/:paymentId/verify` before updating UI to `Paid` |
