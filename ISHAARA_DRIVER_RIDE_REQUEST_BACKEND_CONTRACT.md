# ISHAARA DRIVER RIDE REQUEST & PASSENGER BOARDING BACKEND CONTRACT

**Document Status:** AUTHORITATIVE BACKEND AUDIT REFERENCE  
**Phase:** 10 — Driver / Conductor Ride Requests + Passenger Boarding  
**Author:** Ishaara Senior Architecture Team  
**Audit Source:** Verified against `ishara-backend` source code (`rideRequest.routes.ts`, `rideRequest.controller.ts`, `ride.routes.ts`, `ride.controller.ts`, `ride.service.ts`, `rideRequest.service.ts`, `realtime.service.ts`, `realtime.gateway.ts`).

---

## 1. Executive Summary & Verification Matrix

| Action / Operation | Backend Endpoint | Method | Auth / Role | Status |
| :--- | :--- | :--- | :--- | :--- |
| **List Driver Requests** | `/api/v1/drivers/me/ride-requests` | `GET` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Get Request Details** | `/api/v1/ride-requests/:id` | `GET` | Bearer, `DRIVER_CONDUCTOR` / `USER` | **VERIFIED** |
| **Accept Ride Request** | `/api/v1/ride-requests/:id/accept` | `POST` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Reject Ride Request** | `/api/v1/ride-requests/:id/reject` | `POST` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Driver Rides List** | `/api/v1/drivers/me/rides` | `GET` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Mark Driver Arrived** | `/api/v1/rides/:id/arrive` | `POST` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Passenger Boarding (Pickup)** | `/api/v1/rides/:id/pickup` | `POST` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Start Ride** | `/api/v1/rides/:id/start` | `POST` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Complete Ride** | `/api/v1/rides/:id/complete` | `POST` | Bearer, `DRIVER_CONDUCTOR` | **VERIFIED** |
| **Cancel Ride** | `/api/v1/rides/:id/cancel` | `POST` | Bearer, `DRIVER_CONDUCTOR` / `USER` | **VERIFIED** |
| **Ride Request Realtime** | `/api/v1/ride-requests/realtime?token={jwt}` | `WSS` | Query Token | **VERIFIED** |
| **Ride Realtime** | `/api/v1/rides/realtime?token={jwt}` | `WSS` | Query Token | **VERIFIED** |

### Critical Differences Identified vs. Unaudited Assumptions:
1. **Accept / Reject Endpoints**: They are located at `/api/v1/ride-requests/:id/accept` and `/api/v1/ride-requests/:id/reject` (NOT under `/drivers/me/...`).
2. **Passenger Boarding Endpoint**: The backend models passenger boarding as the Ride pickup transition: `POST /api/v1/rides/:id/pickup`.
3. **Ride Creation Mechanism**: When `POST /api/v1/ride-requests/:id/accept` is called by the driver, the backend **automatically provisions an authoritative `Ride` entity** in MongoDB transaction with status `CREATED` linked by `rideRequestId`.
4. **Unsupported / Non-existent Endpoints**:
   - `POST /drivers/me/ride-requests/:id/accept` -> **NOT AVAILABLE IN CURRENT BACKEND CONTRACT** (Use `/api/v1/ride-requests/:id/accept`)
   - `POST /rides/:id/board` -> **NOT AVAILABLE IN CURRENT BACKEND CONTRACT** (Use `/api/v1/rides/:id/pickup`)
   - `POST /rides/:id/no-show` -> **NOT AVAILABLE IN CURRENT BACKEND CONTRACT** (Use `/api/v1/rides/:id/cancel` with reason)

---

## 2. Verified Request Schemas & Endpoints

### 2.1 GET `/api/v1/drivers/me/ride-requests`
Lists pending or historical ride requests directed to the authenticated driver.

- **Authentication**: `Bearer <accessToken>`
- **Authorization Role**: `DRIVER_CONDUCTOR`
- **Query Parameters**:
  - `status` (optional string): e.g. `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`. If omitted, returns all.
  - `tripId` (optional string): Filter requests belonging to a specific driver trip.
  - `limit` (optional number, default: 20)
  - `page` (optional number, default: 1)
  - `cursor` (optional string): Pagination cursor (Mongo ObjectId).
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Ride requests retrieved successfully",
  "data": {
    "items": [
      {
        "id": "673f8a...",
        "tripId": "673e4b...",
        "driverId": "673d2a...",
        "userId": "673c1f...",
        "pickup": {
          "formattedAddress": "Main Gate, Sector 4",
          "coordinates": [77.5946, 12.9716]
        },
        "destination": {
          "formattedAddress": "Academic Block 3",
          "coordinates": [77.5982, 12.9754]
        },
        "status": "PENDING",
        "requestedAt": "2026-09-25T13:00:00.000Z",
        "expiresAt": "2026-09-25T13:05:00.000Z",
        "respondedAt": null,
        "rejectionReason": null,
        "cancellationReason": null
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 20,
    "hasMore": false
  }
}
```

### 2.2 GET `/api/v1/ride-requests/:requestId`
Retrieves detailed state of a single ride request.
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: Driver of the trip or Requesting User.
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Ride request retrieved successfully",
  "data": {
    "id": "673f8a...",
    "tripId": "673e4b...",
    "driverId": "673d2a...",
    "userId": "673c1f...",
    "pickup": {
      "formattedAddress": "Main Gate, Sector 4",
      "coordinates": [77.5946, 12.9716]
    },
    "destination": {
      "formattedAddress": "Academic Block 3",
      "coordinates": [77.5982, 12.9754]
    },
    "status": "PENDING",
    "requestedAt": "2026-09-25T13:00:00.000Z",
    "expiresAt": "2026-09-25T13:05:00.000Z",
    "respondedAt": null,
    "rejectionReason": null,
    "cancellationReason": null
  }
}
```

### 2.3 POST `/api/v1/ride-requests/:requestId/accept`
Driver accepts an incoming `PENDING` ride request. Backend atomically transitions status to `ACCEPTED` and provisions an authoritative `Ride` instance.
- **Authentication**: `Bearer <accessToken>`
- **Authorization Role**: `DRIVER_CONDUCTOR`
- **Request Body**: Empty (`{}`)
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Ride request accepted successfully",
  "data": {
    "id": "673f8a...",
    "tripId": "673e4b...",
    "driverId": "673d2a...",
    "userId": "673c1f...",
    "pickup": {
      "formattedAddress": "Main Gate, Sector 4",
      "coordinates": [77.5946, 12.9716]
    },
    "destination": {
      "formattedAddress": "Academic Block 3",
      "coordinates": [77.5982, 12.9754]
    },
    "status": "ACCEPTED",
    "requestedAt": "2026-09-25T13:00:00.000Z",
    "expiresAt": "2026-09-25T13:05:00.000Z",
    "respondedAt": "2026-09-25T13:01:10.000Z",
    "rejectionReason": null,
    "cancellationReason": null
  }
}
```
- **Error Responses**:
  - `400 Bad Request`: `Request is not pending (status: EXPIRED)` or `Request has expired`.
  - `403 Forbidden`: `Only the assigned driver can accept this request`.
  - `404 Not Found`: `Ride request not found`.

### 2.4 POST `/api/v1/ride-requests/:requestId/reject`
Driver rejects a `PENDING` ride request.
- **Authentication**: `Bearer <accessToken>`
- **Authorization Role**: `DRIVER_CONDUCTOR`
- **Request Body**:
```json
{
  "reason": "Vehicle full" // optional string
}
```
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Ride request rejected successfully",
  "data": {
    "id": "673f8a...",
    "tripId": "673e4b...",
    "driverId": "673d2a...",
    "userId": "673c1f...",
    "pickup": {
      "formattedAddress": "Main Gate, Sector 4",
      "coordinates": [77.5946, 12.9716]
    },
    "destination": {
      "formattedAddress": "Academic Block 3",
      "coordinates": [77.5982, 12.9754]
    },
    "status": "REJECTED",
    "requestedAt": "2026-09-25T13:00:00.000Z",
    "expiresAt": "2026-09-25T13:05:00.000Z",
    "respondedAt": "2026-09-25T13:01:15.000Z",
    "rejectionReason": "Vehicle full",
    "cancellationReason": null
  }
}
```

---

## 3. Passenger Boarding & Ride Operational Lifecycle

The backend `Ride` model tracks passenger transit lifecycle:
`CREATED` → `DRIVER_ARRIVING` → `PICKED_UP` (Boarded) → `IN_PROGRESS` → `COMPLETED` (or `CANCELLED`).

### 3.1 GET `/api/v1/drivers/me/rides`
Retrieves rides assigned to the driver (active or completed).
- **Query Parameters**: `status`, `page`, `limit`
- **Response**: List of `RideResponse` objects.

### 3.2 POST `/api/v1/rides/:rideId/arrive`
Driver signals arrival at passenger pickup location.
- **State Transition**: `CREATED` → `DRIVER_ARRIVING`
- **Success (200 OK)**: Returns updated `RideResponse`.

### 3.3 POST `/api/v1/rides/:rideId/pickup` (PASSENGER BOARDING)
**This is the authoritative passenger boarding endpoint.**
Driver confirms that the passenger has boarded the vehicle.
- **State Transition**: `DRIVER_ARRIVING` → `PICKED_UP`
- **Success (200 OK)**: Returns updated `RideResponse` with `status: "PICKED_UP"` and `pickupTime: ISO-8601`.

### 3.4 POST `/api/v1/rides/:rideId/start`
Driver starts the transit segment towards the destination.
- **State Transition**: `PICKED_UP` → `IN_PROGRESS`
- **Success (200 OK)**: Returns updated `RideResponse` with `status: "IN_PROGRESS"` and `startTime: ISO-8601`.

### 3.5 POST `/api/v1/rides/:rideId/complete`
Driver marks ride finished at destination.
- **State Transition**: `IN_PROGRESS` → `COMPLETED`
- **Success (200 OK)**: Returns updated `RideResponse` with `status: "COMPLETED"` and `completionTime: ISO-8601`.

### 3.6 POST `/api/v1/rides/:rideId/cancel`
Driver or passenger cancels an active ride before completion.
- **Request Body**: `{ "reason": "Passenger no-show" }`
- **State Transition**: `CREATED | DRIVER_ARRIVING` → `CANCELLED`
- **Success (200 OK)**: Returns updated `RideResponse` with `status: "CANCELLED"` and `cancellationReason`.

---

## 4. Authoritative Status Enums

### 4.1 RideRequestStatus
- `PENDING`: Waiting for driver review
- `ACCEPTED`: Driver accepted; ride created
- `REJECTED`: Driver rejected request
- `CANCELLED`: Passenger cancelled prior to response
- `EXPIRED`: Request exceeded expiration window without driver response

### 4.2 RideStatus (Boarding & Transit Lifecycle)
- `CREATED`: Ride instantiated upon request acceptance
- `DRIVER_ARRIVING`: Driver approaching pickup location
- `PICKED_UP`: Passenger boarded vehicle
- `IN_PROGRESS`: Vehicle in transit to destination
- `COMPLETED`: Ride finished at destination
- `CANCELLED`: Ride cancelled

---

## 5. Realtime WebSocket Architecture

### 5.1 Ride Request Realtime Gateway
- **Endpoint**: `wss://<host>/api/v1/ride-requests/realtime?token=<bearerToken>`
- **Driver Room**: Authenticated driver auto-joined to `driver:<driverId>`.
- **Driver Events Received**:
  - `RIDE_REQUEST_CREATED`: Triggered when passenger submits request on driver's active trip.
    Payload: `{ "requestId": string, "tripId": string, "pickup": { ... }, "destination": { ... }, "expiresAt": string }`
  - `RIDE_REQUEST_CANCELLED`: Passenger cancelled request.
    Payload: `{ "requestId": string, "reason": string }`
  - `RIDE_REQUEST_EXPIRED`: Request reached TTL.
    Payload: `{ "requestId": string }`

### 5.2 Ride Realtime Gateway
- **Endpoint**: `wss://<host>/api/v1/rides/realtime?token=<bearerToken>`
- **Driver Room**: Authenticated driver auto-joined to `driver:<driverId>` and `ride:<rideId>`.
- **Events**: `RIDE_CREATED`, `RIDE_CANCELLED`.
