# ISHAARA FRONTEND PHASE A10 — API CONTRACT SPECIFICATION

## 1. Scope
This document details the exact HTTP API contracts used by the ISHAARA Android client for **Phase A10 — Ride Request Lifecycle**, cross-referenced forensically against `ishara-backend/src/modules/ride-requests/`.

---

## 2. API Endpoints

### 2.1 Create Ride Request
- **Method & Path:** `POST /api/v1/ride-requests`
- **Authentication:** `Bearer <token>` (Required, Role: `STUDENT` / `PASSENGER`)
- **Headers:**
  - `Content-Type: application/json`
  - `Idempotency-Key: <uuid-v4>` (Optional but sent by frontend)
- **Request Body (Zod `.strict()`):**
```json
{
  "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
  "pickup": {
    "formattedAddress": "Banaras Hindu University, Lanka, Varanasi",
    "latitude": 25.2677,
    "longitude": 82.9913
  },
  "destination": {
    "formattedAddress": "Assi Ghat, Varanasi, Uttar Pradesh",
    "latitude": 25.2950,
    "longitude": 83.0075
  },
  "discoverySessionId": "disc_session_123"
}
```
*Note: Any extra fields such as `fare`, `seatCapacity`, `seatsRequested`, or `price` cause immediate HTTP 400 Bad Request due to Zod `.strict()`.*

- **Success Response:** `201 Created`
```json
{
  "id": "670e1c2b3f4a5b6c7d8e9f01",
  "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
  "driverId": "651a2b3c4d5e6f7a8b9c0002",
  "userId": "651a2b3c4d5e6f7a8b9c0001",
  "pickup": {
    "address": "Banaras Hindu University, Lanka, Varanasi",
    "coordinates": {
      "type": "Point",
      "coordinates": [82.9913, 25.2677]
    }
  },
  "destination": {
    "address": "Assi Ghat, Varanasi, Uttar Pradesh",
    "coordinates": {
      "type": "Point",
      "coordinates": [83.0075, 25.2950]
    }
  },
  "status": "PENDING",
  "requestedAt": "2026-10-01T08:00:00.000Z",
  "expiresAt": "2026-10-01T08:02:00.000Z",
  "discoverySessionId": "disc_session_123"
}
```

- **Possible Error Codes:**
  - `400 Bad Request`: Validation failure (e.g. coordinates distance < 50m, invalid coordinates, extra fields).
  - `401 Unauthorized`: Expired or missing authentication token.
  - `403 Forbidden`: Authenticated user is not permitted (e.g. driver attempting passenger request).
  - `404 Not Found`: Discovered trip no longer exists or was deleted.
  - `409 Conflict`: Active pending request already exists for this trip or user, or trip is full/departed.
  - `500 Server Error`: Internal failure.

---

### 2.2 Get Ride Request Details & Authoritative State
- **Method & Path:** `GET /api/v1/ride-requests/:requestId`
- **Authentication:** `Bearer <token>` (Required)
- **Success Response:** `200 OK`
```json
{
  "id": "670e1c2b3f4a5b6c7d8e9f01",
  "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
  "driverId": "651a2b3c4d5e6f7a8b9c0002",
  "userId": "651a2b3c4d5e6f7a8b9c0001",
  "pickup": {
    "address": "Banaras Hindu University, Lanka, Varanasi",
    "coordinates": { "type": "Point", "coordinates": [82.9913, 25.2677] }
  },
  "destination": {
    "address": "Assi Ghat, Varanasi, Uttar Pradesh",
    "coordinates": { "type": "Point", "coordinates": [83.0075, 25.2950] }
  },
  "status": "ACCEPTED",
  "requestedAt": "2026-10-01T08:00:00.000Z",
  "respondedAt": "2026-10-01T08:00:45.000Z",
  "expiresAt": "2026-10-01T08:02:00.000Z",
  "rideId": "ride_doc_88899"
}
```

- **Possible Error Codes:**
  - `401 Unauthorized`: Token invalid.
  - `403 Forbidden`: User does not own this request.
  - `404 Not Found`: Request ID not found.

---

### 2.3 Cancel Ride Request (Passenger-Side)
- **Method & Path:** `POST /api/v1/ride-requests/:requestId/cancel`
- **Authentication:** `Bearer <token>` (Required)
- **Request Body (Zod `.strict()`):**
```json
{
  "reason": "Change of plans, took an auto rickshaw instead."
}
```
*Note: `reason` is optional (max 250 characters).*

- **Success Response:** `200 OK`
```json
{
  "id": "670e1c2b3f4a5b6c7d8e9f01",
  "status": "CANCELLED",
  "cancelledAt": "2026-10-01T08:01:10.000Z"
}
```

- **Possible Error Codes:**
  - `400 Bad Request`: `reason` string exceeds 250 characters.
  - `401 Unauthorized`: Token invalid.
  - `403 Forbidden`: User does not own this request.
  - `404 Not Found`: Request not found.
  - `409 Conflict`: Request is already `ACCEPTED`, `REJECTED`, `EXPIRED`, or `CANCELLED`.

---

### 2.4 List Authenticated User's Ride Requests
- **Method & Path:** `GET /api/v1/ride-requests/me`
- **Query Parameters:**
  - `status`: Filter by `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`
  - `tripId`: Filter by associated trip ID
  - `limit`: Page size (default 20)
  - `page`: Page index (default 1)
- **Authentication:** `Bearer <token>` (Required)
- **Success Response:** `200 OK`
```json
{
  "items": [ ... ],
  "total": 1,
  "page": 1,
  "limit": 20
}
```

---

## 3. Contract Discrepancy Matrix

| Item | Prior Documentation / Assumption | Actual Backend Contract | Frontend Resolution |
|---|---|---|---|
| Seat Selection / Seats Requested | Assumed client sends `seatsRequested: 1` | `POST /api/v1/ride-requests` uses Zod `.strict()` and strictly rejects any seat fields. | Frontend payload completely omits seat fields (`CreateRideRequestDto`). |
| Cancellation Endpoint | Hypothesized `DELETE /api/v1/ride-requests/:id` | Backend defines `POST /api/v1/ride-requests/:id/cancel` with optional `{ reason }`. | Frontend calls `POST .../cancel` with optional `reason` (max 250 chars). |
| Coordinate Format | Assumed latitude/longitude flat properties in response | Backend GeoJSON RFC 7946 `Point` format: `coordinates: [longitude, latitude]`. | `RideRequestMapper` safely reads `[0]` as longitude and `[1]` as latitude. |
