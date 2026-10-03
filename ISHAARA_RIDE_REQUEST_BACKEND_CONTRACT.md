# ISHAARA Ride Request Backend Contract

## 1. Endpoint Overview

- **Endpoint**: `POST /api/v1/ride-requests`
- **Controller**: `RideRequestController.create` (`src/modules/ride-requests/ride-request.controller.ts`)
- **Service**: `RideRequestService.createRideRequest` (`src/modules/ride-requests/ride-request.service.ts`)
- **Validation**: `createRideRequestSchema.strict()` (`src/modules/ride-requests/ride-request.schema.ts`)
- **Authentication**: `requireAuth` (Bearer token in `Authorization` header required)
- **Role Authorization**: `requireUser` (Caller MUST have role `USER`). Calls by `DRIVER_CONDUCTOR` are rejected with HTTP 403 `FORBIDDEN`.
- **Rate Limit**: Governed by `rideRequestRateLimiter`.
- **Idempotency**: Supports `Idempotency-Key` HTTP header. Replaying with the same key returns the existing request with 201/200 without creating a duplicate.

---

## 2. Request Contract

The backend schema strictly enforces `.strict()`. Any unrecognized or server-controlled field (e.g., `status`, `driverId`, `fare`, `seatCapacity`, `seatsRequested`) will result in HTTP 400 `VALIDATION_ERROR`.

### Request Headers
```http
POST /api/v1/ride-requests HTTP/1.1
Host: reposnse-ishaara.onrender.com
Authorization: Bearer <token>
Content-Type: application/json
Idempotency-Key: <unique-uuid-v4>
```

### Request Body (JSON)
```json
{
  "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
  "pickup": {
    "name": "Kenyatta University Main Gate",
    "formattedAddress": "Thika Rd, Nairobi",
    "latitude": -1.1818,
    "longitude": 36.9275,
    "googlePlaceId": "ChIJb8...",
    "serpApiDataId": "0x182..."
  },
  "destination": {
    "name": "Safari Park Hotel",
    "formattedAddress": "Thika Rd, Kasarani, Nairobi",
    "latitude": -1.2185,
    "longitude": 36.8856,
    "googlePlaceId": "ChIJc9...",
    "serpApiDataId": "0x183..."
  }
}
```

### Field Specifications
| Field | Type | Required | Constraints / Description |
|---|---|---|---|
| `tripId` | String | Yes | Exactly 24-character hexadecimal ObjectId regex `/^[0-9a-fA-F]{24}$/`. |
| `pickup.name` | String | No | Max 100 characters. |
| `pickup.formattedAddress` | String | Yes | 2 to 300 characters. |
| `pickup.latitude` | Number | Yes | Finite number between -90 and 90. |
| `pickup.longitude` | Number | Yes | Finite number between -180 and 180. |
| `pickup.googlePlaceId` | String | No | Max 100 characters. |
| `pickup.serpApiDataId` | String | No | Max 100 characters. |
| `destination.name` | String | No | Max 100 characters. |
| `destination.formattedAddress` | String | Yes | 2 to 300 characters. |
| `destination.latitude` | Number | Yes | Finite number between -90 and 90. |
| `destination.longitude` | Number | Yes | Finite number between -180 and 180. |
| `destination.googlePlaceId` | String | No | Max 100 characters. |
| `destination.serpApiDataId` | String | No | Max 100 characters. |

---

## 3. Response Contract

### Success Response (`HTTP 201 Created`)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Ride request submitted successfully.",
  "data": {
    "id": "670e1c2b3f4a5b6c7d8e9f01",
    "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
    "driverId": "651a00112233445566778899",
    "userId": "6519ffeeddccbbaa99887766",
    "pickup": {
      "name": "Kenyatta University Main Gate",
      "formattedAddress": "Thika Rd, Nairobi",
      "coordinates": {
        "type": "Point",
        "coordinates": [36.9275, -1.1818]
      },
      "googlePlaceId": "ChIJb8...",
      "serpApiDataId": "0x182..."
    },
    "destination": {
      "name": "Safari Park Hotel",
      "formattedAddress": "Thika Rd, Kasarani, Nairobi",
      "coordinates": {
        "type": "Point",
        "coordinates": [36.8856, -1.2185]
      },
      "googlePlaceId": "ChIJc9...",
      "serpApiDataId": "0x183..."
    },
    "status": "PENDING",
    "requestedAt": "2026-09-23T13:00:00.000Z",
    "respondedAt": null,
    "expiresAt": "2026-09-23T13:02:00.000Z",
    "rejectionReason": null,
    "cancellationReason": null,
    "createdAt": "2026-09-23T13:00:00.000Z",
    "updatedAt": "2026-09-23T13:00:00.000Z"
  }
}
```

### Status Lifecycle
The authoritative backend status enum is `RideRequestStatus`:
- `PENDING`: Initial state upon creation. Request is waiting for driver response or expiration.
- `ACCEPTED`: Driver has accepted the request.
- `REJECTED`: Driver has rejected the request.
- `CANCELLED`: Passenger has cancelled the request.
- `EXPIRED`: Request timed out (automatically expires after 120s if ignored).

---

## 4. HTTP Status Codes & Error Mappings

| HTTP Status | Error Code (`error.code`) | Backend Condition | Client Domain Error | User-Facing Guidance |
|---|---|---|---|---|
| `201 Created` | - | Successful submission | `IshaaraResult.Success` | Request submitted; status is PENDING. |
| `400 Bad Request` | `VALIDATION_ERROR` | Unrecognized field, invalid ObjectId format, missing required field | `IshaaraError.Validation` | Invalid request details. |
| `400 Bad Request` | `TRIP_NOT_ELIGIBLE` | Trip is not in `ACTIVE` state (e.g. `CREATED`, `COMPLETED`, `CANCELLED`), or driver is offline | `IshaaraError.TripUnavailable` | This trip is no longer active. Please choose another trip. |
| `400 Bad Request` | `DRIVER_NOT_VERIFIED` | Driver profile is not verified | `IshaaraError.TripUnavailable` | Driver is not verified for this trip. |
| `400 Bad Request` | `SAME_ORIGIN_DESTINATION` | Pickup and destination separation < 50m | `IshaaraError.Validation` | Pickup and destination cannot be the same location. |
| `401 Unauthorized` | `UNAUTHORIZED` | Missing or invalid Bearer token | `IshaaraError.Authentication` | Your session has expired. Please sign in again. |
| `403 Forbidden` | `FORBIDDEN` | Caller has role `DRIVER_CONDUCTOR` | `IshaaraError.Forbidden` | Ride requests are only accessible to passengers. |
| `403 Forbidden` | `USER_INACTIVE` | Passenger account is inactive | `IshaaraError.Forbidden` | Your account is inactive. Please contact support. |
| `404 Not Found` | `TRIP_NOT_FOUND` | Trip ID does not exist | `IshaaraError.NotFound` | The requested trip could not be found. |
| `409 Conflict` | `DUPLICATE_RIDE_REQUEST` | Active pending request already exists for this user and trip | `IshaaraError.DuplicateRequest` | You already have an active request for this trip. |
| `429 Too Many Requests` | `RATE_LIMIT_EXCEEDED` | Exceeded rate limit | `IshaaraError.Network` | Too many requests. Please wait a moment. |
| `500 Server Error` | `INTERNAL_SERVER_ERROR` | Unhandled backend exception | `IshaaraError.Server` | Server error. Please try again later. |

---

## 5. Duplicate Request & Idempotency Behavior

1. **Client Idempotency Key**:
   - The backend checks `req.headers["idempotency-key"]`.
   - If an existing record with the same `(userId, idempotencyKey)` exists, it returns the existing request with HTTP 201 without creating a duplicate.
   - The Android client will generate and attach a persistent UUID idempotency key per review session.
2. **Compound Unique Index**:
   - MongoDB maintains a unique partial index on `(userId, tripId)` for active `PENDING` requests.
   - If a duplicate request is submitted without an idempotency key, MongoDB throws E11000 and the service throws `ConflictError` with HTTP 409 and code `DUPLICATE_RIDE_REQUEST`.

---

## 6. Fare and Seat Contract Verification

1. **Fare Information**:
   - **Does NOT exist** in `POST /api/v1/ride-requests`.
   - Authoritative fare calculation belongs to `modules/payments/fare.service.ts` during ride completion and checkout.
   - Android client MUST NOT fabricate or display estimated fares during ride request review.
2. **Seat Availability**:
   - **Does NOT exist** in `POST /api/v1/ride-requests`.
   - Backend `createRideRequestSchema` rejects `seatsRequested` or `seatCapacity`.
   - Android client MUST NOT send or display seat count restrictions.
