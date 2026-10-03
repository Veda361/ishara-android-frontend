# Ishaara Driver / Conductor Backend Contract Specification

**Document Version**: 1.0.0  
**Status**: VERIFIED & AUTHORITATIVE  
**Phase**: PHASE 08 — DRIVER / CONDUCTOR OPERATIONAL FOUNDATION  
**Target Role**: `DRIVER_CONDUCTOR`

---

## 1. Authentication & Authorization

All driver endpoints require:
1. **Bearer Token Authentication**:
   ```http
   Authorization: Bearer <token>
   ```
2. **Role Enforcement**:
   - Centralized middleware `requireAuth` + `requireDriverConductor`.
   - Only accounts with role `DRIVER_CONDUCTOR` are permitted.
   - Any access attempt by accounts with role `USER` (passengers) is rejected with HTTP 403 Forbidden:
     ```json
     {
       "success": false,
       "error": {
         "code": "FORBIDDEN",
         "message": "Access denied: Required role 'DRIVER_CONDUCTOR'."
       }
     }
     ```

---

## 2. Authoritative Endpoints

### 2.1 Driver Operational Context
- **Endpoint**: `GET /api/v1/drivers/me/operations/context`
- **Query Parameters**:
  - `timezone` (optional string, defaults to `"Asia/Kolkata"`).
- **Backend Controller**: `driverController.getOperationalContext` -> `driverOperationsService.getDriverOperationalContext`
- **Purpose**: Retrieves atomic operational snapshot containing driver profile, verification status, active vehicle, active or created trip, active in-flight rides, and today's summary stats.
- **Success Response (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "data": {
      "driver": {
        "id": "651a2b3c4d5e6f7a8b9c0d1e",
        "userId": "651a2b3c4d5e6f7a8b9c0d1f",
        "verificationStatus": "VERIFIED",
        "status": "ONLINE",
        "licenseNumberMasked": "DL••••••••1234",
        "licenseVerifiedAt": "2026-09-01T10:00:00.000Z"
      },
      "vehicle": {
        "id": "651a2b3c4d5e6f7a8b9c0d20",
        "operatorId": "651a2b3c4d5e6f7a8b9c0d21",
        "registrationNumber": "UP93AT1234",
        "vehicleType": "BUS",
        "make": "Tata",
        "model": "Starbus",
        "isVerified": true,
        "isActive": true,
        "createdAt": "2026-08-15T08:00:00.000Z",
        "updatedAt": "2026-09-20T12:00:00.000Z"
      },
      "activeTrip": {
        "id": "651a2b3c4d5e6f7a8b9c0d30",
        "driverId": "651a2b3c4d5e6f7a8b9c0d1e",
        "vehicleId": "651a2b3c4d5e6f7a8b9c0d20",
        "operatorId": "651a2b3c4d5e6f7a8b9c0d21",
        "origin": {
          "name": "Gate A",
          "formattedAddress": "Kenyatta University Gate A",
          "coordinates": {
            "type": "Point",
            "coordinates": [36.9275, -1.1818]
          }
        },
        "destination": {
          "name": "Safari Park",
          "formattedAddress": "Safari Park Hotel",
          "coordinates": {
            "type": "Point",
            "coordinates": [36.8856, -1.2185]
          }
        },
        "route": {
          "distanceMeters": 4500.0,
          "durationSeconds": 900.0
        },
        "status": "CREATED",
        "startedAt": null,
        "completedAt": null,
        "cancelledAt": null,
        "createdAt": "2026-09-24T06:00:00.000Z",
        "updatedAt": "2026-09-24T06:00:00.000Z"
      },
      "activeRides": [],
      "todayStats": {
        "completedRidesCount": 4,
        "isOnline": true,
        "currentDate": "2026-09-24",
        "timezone": "Asia/Kolkata"
      }
    }
  }
  ```

---

### 2.2 Driver Availability Transitions

#### Go Online
- **Endpoint**: `POST /api/v1/drivers/me/status/online`
- **Request Body**: Empty (`{}`)
- **Preconditions**: Driver `verificationStatus` must be `VERIFIED`.
- **Response**: HTTP 200 OK with `CleanDriverProfileResponse`.
- **Errors**:
  - `403 Forbidden` (`DRIVER_NOT_VERIFIED`): If driver verification status is `PENDING` or `REJECTED`.

#### Go Offline
- **Endpoint**: `POST /api/v1/drivers/me/status/offline`
- **Request Body**: Empty (`{}`)
- **Preconditions**: Driver must not currently be on an active ride (`status !== "ON_RIDE"`).
- **Response**: HTTP 200 OK with `CleanDriverProfileResponse`.
- **Errors**:
  - `400 Bad Request` (`INVALID_DRIVER_STATUS_TRANSITION`): If driver is currently `ON_RIDE`.

---

### 2.3 Trip Lifecycle Operations

#### Start Trip
- **Endpoint**: `POST /api/v1/trips/:tripId/start`
- **Preconditions**:
  1. Trip status must be `CREATED`.
  2. Driver verification status must be `VERIFIED`.
  3. No concurrent active trip exists for the driver (`DRIVER_HAS_ACTIVE_TRIP`).
  4. No concurrent active trip exists for the vehicle (`VEHICLE_HAS_ACTIVE_TRIP`).
  5. Vehicle is active and assigned to driver.
- **Backend Effects**:
  - Trip status transitions: `CREATED -> ACTIVE`.
  - Sets `startedAt = now()`.
  - Automatically updates driver profile status to `ON_RIDE`.
- **Response**: HTTP 200 OK with updated `CleanTripResponse` (`status: "ACTIVE"`).
- **Errors**:
  - `404 Not Found` (`TRIP_NOT_FOUND`): Trip does not exist or does not belong to driver.
  - `409 Conflict` (`INVALID_TRIP_STATUS_TRANSITION`): Trip is not in `CREATED` status.
  - `409 Conflict` (`DRIVER_HAS_ACTIVE_TRIP`): Driver already has an active trip.
  - `409 Conflict` (`VEHICLE_HAS_ACTIVE_TRIP`): Vehicle is already in use on another active trip.
  - `403 Forbidden` (`DRIVER_NOT_VERIFIED`): Driver is not verified.

#### Complete Trip
- **Endpoint**: `POST /api/v1/trips/:tripId/complete`
- **Preconditions**:
  - Trip status must be `ACTIVE`.
- **Backend Effects**:
  - Trip status transitions: `ACTIVE -> COMPLETED`.
  - Sets `completedAt = now()`.
  - Automatically restores driver profile status to `ONLINE`.
- **Response**: HTTP 200 OK with updated `CleanTripResponse` (`status: "COMPLETED"`).
- **Errors**:
  - `404 Not Found` (`TRIP_NOT_FOUND`): Trip does not exist or does not belong to driver.
  - `409 Conflict` (`INVALID_TRIP_STATUS_TRANSITION`): Trip is not in `ACTIVE` status.

#### Cancel Trip
- **Endpoint**: `POST /api/v1/trips/:tripId/cancel`
- **Preconditions**:
  - Trip status must be `CREATED` or `ACTIVE`. Cannot cancel `COMPLETED` or `CANCELLED` trips.
- **Backend Effects**:
  - Trip status transitions to `CANCELLED`.
  - Sets `cancelledAt = now()`.
  - If trip was `ACTIVE`, automatically restores driver profile status to `ONLINE`.
- **Response**: HTTP 200 OK with updated `CleanTripResponse` (`status: "CANCELLED"`).
- **Errors**:
  - `404 Not Found` (`TRIP_NOT_FOUND`): Trip does not exist or does not belong to driver.
  - `409 Conflict` (`INVALID_TRIP_STATUS_TRANSITION`): Trip is already completed or cancelled.

---

## 3. Verified State Machines

### 3.1 Driver Status (`DriverStatus`)
```
    ┌──────────┐
    │ OFFLINE  │
    └────┬─────┘
         │ POST /me/status/online (requires VERIFIED)
         ▼
    ┌──────────┐   Trip Started    ┌──────────┐
    │  ONLINE  ├──────────────────►│ ON_RIDE  │
    └────▲─────┤◄──────────────────┴──────────┘
         │         Trip Completed / Cancelled
         │ POST /me/status/offline (prohibited if ON_RIDE)
    ┌────┴─────┐
    │ OFFLINE  │
    └──────────┘
```

### 3.2 Driver Verification Status (`VerificationStatus`)
- `PENDING`: Profile created, documents awaiting administrative approval. Cannot start trips or go online.
- `VERIFIED`: Approved by agency/platform. Permitted to go online and operate trips.
- `REJECTED`: Application denied. Blocked from operational workflows.

### 3.3 Trip Lifecycle Status (`TripStatus`)
```
    ┌───────────┐
    │  CREATED  │
    └──┬─────┬──┘
       │     │
 POST /start │ POST /cancel
       │     │
       ▼     │
    ┌────────▼──┐          POST /complete          ┌───────────┐
    │  ACTIVE   ├─────────────────────────────────►│ COMPLETED │
    └──┬────────┘                                  └───────────┘
       │
 POST /cancel
       │
       ▼
    ┌───────────┐
    │ CANCELLED │
    └───────────┘
```

---

## 4. Vehicle & Fleet Ownership Rules

1. **Decoupled Roles**: The driver operates an assigned vehicle. The driver is not necessarily the legal fleet owner.
2. **Read-Only in Driver App**: Vehicle details (`registrationNumber`, `vehicleType`, `make`, `model`, `isActive`, `isVerified`) are surfaced in operational context.
3. **No In-App Vehicle Creation**: Vehicle registration and operator administration are managed via the web dashboard; no vehicle onboarding forms exist in Phase 08.

---

## 5. Explicitly Deferred Functionality (Phase 09 & Later)

- **Continuous GPS Streaming** (`PATCH /api/v1/drivers/me/location`): Deferred to Phase 09.
- **WebSocket Realtime Updates**: Deferred to Phase 09.
- **Realtime Passenger Ride Requests & Boarding**: Deferred to Phase 10.
- **Driver Earnings Settlement / Bank Payouts**: Deferred to Phase 13/16.
- **SOS Emergency Broadcast**: Deferred to Phase 15.
