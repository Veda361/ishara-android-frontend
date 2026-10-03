# ISHAARA FRONTEND — PHASE A08: API CONTRACT

This document specifies the exact backend REST API endpoints consumed by Phase A08 for Trip and Dispatch operations.
Backend Source: `/home/dev/Desktop/ishara-backend/src/modules/trips/trips.controller.ts`

---

## 1. Endpoints Specification

### 1.1 List Authenticated Driver Trips
- **Endpoint**: `GET /api/v1/drivers/me/trips`
- **Controller Action**: `TripsController.getMyTrips`
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: Role `DRIVER_CONDUCTOR`
- **Query Parameters**: None (returns all trips assigned to the requesting driver)
- **Response**: `200 OK`
```json
{
  "status": "success",
  "data": [
    {
      "id": "trip_01j9a8b7c6d5e4f3",
      "agencyId": "agency_01j9a8",
      "routeId": "route_central_01",
      "driverId": "driver_01j9a",
      "vehicleId": "veh_01j9a",
      "status": "SCHEDULED",
      "scheduledDeparture": "2026-10-01T12:00:00.000Z",
      "scheduledArrival": "2026-10-01T13:00:00.000Z",
      "actualDeparture": null,
      "actualArrival": null,
      "origin": {
        "address": "Central Station, Platform 3",
        "coordinates": [77.5946, 12.9716]
      },
      "destination": {
        "address": "Tech Park North Gate",
        "coordinates": [77.6412, 12.9141]
      },
      "route": {
        "id": "route_central_01",
        "name": "Route 33X Express",
        "code": "R33X"
      },
      "createdAt": "2026-10-01T10:00:00.000Z",
      "updatedAt": "2026-10-01T10:00:00.000Z"
    }
  ]
}
```
- **Error Responses**:
  - `401 Unauthorized`: Missing, expired, or invalid token.
  - `403 Forbidden`: Role is not `DRIVER_CONDUCTOR`.
  - `500 Internal Server Error`: Server failure.

---

### 1.2 Get Trip Details
- **Endpoint**: `GET /api/v1/trips/:tripId`
- **Controller Action**: `TripsController.getTripById`
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: Authenticated user (Driver, Passenger with booking, or Agency Operator)
- **Path Parameters**:
  - `tripId`: UUID or alphanumeric ID of the trip
- **Response**: `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "trip_01j9a8b7c6d5e4f3",
    "agencyId": "agency_01j9a8",
    "routeId": "route_central_01",
    "driverId": "driver_01j9a",
    "vehicleId": "veh_01j9a",
    "status": "ACTIVE",
    "scheduledDeparture": "2026-10-01T12:00:00.000Z",
    "scheduledArrival": "2026-10-01T13:00:00.000Z",
    "actualDeparture": "2026-10-01T12:05:00.000Z",
    "actualArrival": null,
    "origin": {
      "address": "Central Station, Platform 3",
      "coordinates": [77.5946, 12.9716]
    },
    "destination": {
      "address": "Tech Park North Gate",
      "coordinates": [77.6412, 12.9141]
    },
    "route": {
      "id": "route_central_01",
      "name": "Route 33X Express",
      "code": "R33X"
    },
    "createdAt": "2026-10-01T10:00:00.000Z",
    "updatedAt": "2026-10-01T12:05:00.000Z"
  }
}
```
- **Error Responses**:
  - `401 Unauthorized`: Unauthenticated.
  - `404 Not Found`: Trip does not exist.

---

### 1.3 Create Trip (Self-Dispatch / Driver Route Creation)
- **Endpoint**: `POST /api/v1/trips`
- **Controller Action**: `TripsController.createTrip`
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: `DRIVER_CONDUCTOR`, `OPERATOR`, `ADMIN`
- **Request Body**:
```json
{
  "origin": {
    "address": "Terminal 1, Bay A",
    "coordinates": [77.5946, 12.9716]
  },
  "destination": {
    "address": "Suburban Hub East",
    "coordinates": [77.6412, 12.9141]
  },
  "scheduledDeparture": "2026-10-01T14:30:00.000Z",
  "scheduledArrival": "2026-10-01T15:45:00.000Z",
  "vehicleId": "veh_01j9a",
  "routeId": "route_central_01"
}
```
- **Response**: `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "trip_new_001",
    "status": "SCHEDULED",
    "driverId": "driver_01j9a",
    "vehicleId": "veh_01j9a",
    "origin": { "address": "Terminal 1, Bay A", "coordinates": [77.5946, 12.9716] },
    "destination": { "address": "Suburban Hub East", "coordinates": [77.6412, 12.9141] },
    "scheduledDeparture": "2026-10-01T14:30:00.000Z",
    "createdAt": "2026-10-01T10:30:00.000Z"
  }
}
```
- **Error Responses**:
  - `400 Bad Request`: Validation failure on coordinates or departure time.
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Driver lacks operational authorization.
  - `409 Conflict`: Vehicle or driver is already assigned to a conflicting active trip.

---

### 1.4 Start Trip
- **Endpoint**: `POST /api/v1/trips/:tripId/start`
- **Controller Action**: `TripsController.startTrip`
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: Assigned `DRIVER_CONDUCTOR`
- **Path Parameters**:
  - `tripId`: UUID or alphanumeric ID of the trip
- **Request Body**: Empty `{}`
- **Response**: `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "trip_01j9a8b7c6d5e4f3",
    "status": "ACTIVE",
    "actualDeparture": "2026-10-01T12:05:00.000Z",
    "updatedAt": "2026-10-01T12:05:00.000Z"
  }
}
```
- **Error Responses**:
  - `400 Bad Request`: Trip is in an illegal state for starting (e.g. `COMPLETED` or `CANCELLED`).
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: User is not the assigned driver.
  - `404 Not Found`: Trip not found.
  - `409 Conflict`: Driver or vehicle already has an ongoing `ACTIVE` trip (`DRIVER_HAS_ACTIVE_TRIP` / `VEHICLE_HAS_ACTIVE_TRIP`).

---

### 1.5 Complete Trip
- **Endpoint**: `POST /api/v1/trips/:tripId/complete`
- **Controller Action**: `TripsController.completeTrip`
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: Assigned `DRIVER_CONDUCTOR`, `OPERATOR`, `ADMIN`
- **Path Parameters**:
  - `tripId`: UUID or alphanumeric ID of the trip
- **Request Body**: Empty `{}`
- **Response**: `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "trip_01j9a8b7c6d5e4f3",
    "status": "COMPLETED",
    "actualArrival": "2026-10-01T13:10:00.000Z",
    "updatedAt": "2026-10-01T13:10:00.000Z"
  }
}
```
- **Error Responses**:
  - `400 Bad Request`: Trip is not in `ACTIVE` status.
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Not authorized to complete this trip.
  - `404 Not Found`: Trip not found.

---

### 1.6 Cancel Trip
- **Endpoint**: `POST /api/v1/trips/:tripId/cancel`
- **Controller Action**: `TripsController.cancelTrip`
- **Authentication**: `Bearer <accessToken>`
- **Authorization**: Assigned `DRIVER_CONDUCTOR`, `OPERATOR`, `ADMIN`
- **Path Parameters**:
  - `tripId`: UUID or alphanumeric ID of the trip
- **Request Body**:
```json
{
  "reason": "Severe mechanical breakdown requiring tow"
}
```
- **Response**: `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "trip_01j9a8b7c6d5e4f3",
    "status": "CANCELLED",
    "updatedAt": "2026-10-01T12:30:00.000Z"
  }
}
```
- **Error Responses**:
  - `400 Bad Request`: Missing cancellation reason, or trip is already in a terminal state (`COMPLETED` or `CANCELLED`).
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Not authorized to cancel this trip.
  - `404 Not Found`: Trip not found.

---

## 2. Inactive / Non-Existent Endpoints (Forensic Confirmation)
The following endpoints were explicitly audited and confirmed **NOT TO EXIST** in the backend:
- `POST /api/v1/trips/:id/accept` (Does not exist)
- `POST /api/v1/trips/:id/reject` (Does not exist)
- `GET /api/v1/dispatch/*` (Does not exist)
- `POST /api/v1/trips/:id/pause` (Does not exist)
- `POST /api/v1/trips/:id/resume` (Does not exist)
