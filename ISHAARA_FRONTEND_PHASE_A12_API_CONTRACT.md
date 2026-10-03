# ISHAARA FRONTEND PHASE A12 — API CONTRACT SPECIFICATION

## 1. Overview
This document specifies the exact REST endpoint, request/response headers, JSON payload structures, error formats, and security rules for **Phase A12 — Fare & Pricing Foundation**, forensically verified against `ishara-backend/src/modules/payments/` and `ishara-backend/src/modules/rides/`.

---

## 2. API Specification

### 2.1 Get Ride Fare Breakdown & Snapshot
- **Method:** `GET`
- **Path:** `/api/v1/rides/:rideId/fare`
- **Rate Limit:** 60 requests per minute (`rideRateLimiter`)
- **Authentication:** `Bearer <token>` in `Authorization` header (`requireAuth`)
- **Authorization:** Owning passenger (`userId == ride.userId`) or assigned driver (`driverProfileId == ride.driverId`).

#### Headers
```http
GET /api/v1/rides/651a2b3c4d5e6f7a8b9c0001/fare HTTP/1.1
Host: api.ishaara.com
Authorization: Bearer <jwt_bearer_token>
Accept: application/json
```

#### Pre-Ride / In-Flight Response (`200 OK`)
Returned when the ride is active or in-flight (`CREATED`, `DRIVER_ARRIVING`, `PICKED_UP`, `IN_PROGRESS`):
```json
{
  "success": true,
  "data": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001",
    "status": "IN_PROGRESS",
    "currency": "INR",
    "currentFareMinor": 3500,
    "isFinal": false,
    "fareEstimate": {
      "currency": "INR",
      "pricingPolicyVersion": "policy_v1",
      "distanceMeters": 3000,
      "estimatedDurationSeconds": 450,
      "baseFareMinor": 2000,
      "distanceComponentMinor": 1500,
      "timeComponentMinor": 0,
      "subtotalMinor": 3500,
      "serviceFeeMinor": 350,
      "taxMinor": 0,
      "totalMinor": 3500,
      "providerAmountMinor": 3150,
      "isEstimate": true,
      "calculatedAt": "2026-10-01T08:10:00.000Z"
    },
    "fareSnapshot": null
  },
  "message": "Ride fare retrieved successfully."
}
```

#### Post-Ride Completed Response (`200 OK`)
Returned once the ride has been completed by the driver (`COMPLETED`):
```json
{
  "success": true,
  "data": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001",
    "status": "COMPLETED",
    "currency": "INR",
    "currentFareMinor": 4500,
    "isFinal": true,
    "fareEstimate": {
      "currency": "INR",
      "pricingPolicyVersion": "policy_v1",
      "distanceMeters": 5000,
      "estimatedDurationSeconds": 600,
      "baseFareMinor": 2000,
      "distanceComponentMinor": 2500,
      "timeComponentMinor": 0,
      "subtotalMinor": 4500,
      "serviceFeeMinor": 450,
      "taxMinor": 0,
      "totalMinor": 4500,
      "providerAmountMinor": 4050,
      "isEstimate": true,
      "calculatedAt": "2026-10-01T08:00:00.000Z"
    },
    "fareSnapshot": {
      "currency": "INR",
      "pricingPolicyVersion": "policy_v1",
      "distanceMeters": 5000,
      "actualDurationSeconds": 660,
      "baseFareMinor": 2000,
      "distanceComponentMinor": 2500,
      "timeComponentMinor": 0,
      "subtotalMinor": 4500,
      "serviceFeeMinor": 450,
      "taxMinor": 0,
      "discountMinor": 0,
      "totalMinor": 4500,
      "providerAmountMinor": 4050,
      "isEstimate": false,
      "calculatedAt": "2026-10-01T08:20:00.000Z"
    }
  },
  "message": "Ride fare retrieved successfully."
}
```

---

## 3. Error Contract & Status Codes

| HTTP Status | Error Code | Meaning | Client Handling |
|---|---|---|---|
| `400 Bad Request` | `INVALID_ID` | `rideId` is not a valid 24-character hexadecimal ObjectId | Mapped to `IshaaraError.Validation` |
| `401 Unauthorized` | `UNAUTHORIZED` | Bearer token is missing, malformed, or expired | Mapped to `IshaaraError.Authentication`; triggers session refresh |
| `403 Forbidden` | `RIDE_NOT_AUTHORIZED` | Caller is not the owning passenger or assigned driver | Mapped to `IshaaraError.Forbidden`; prevents IDOR |
| `404 Not Found` | `RIDE_NOT_FOUND` | No ride exists for the provided `rideId` | Mapped to `IshaaraError.NotFound` |
| `429 Too Many Requests` | `RATE_LIMIT_EXCEEDED` | Request frequency exceeds 60 req/min limit | Mapped to `IshaaraError.Network`; prevents request spamming |
| `500 Internal Server Error` | `LEDGER_IMBALANCE` / `SERVER_ERROR` | Internal calculation or financial invariant failure | Mapped to `IshaaraError.Server` |
