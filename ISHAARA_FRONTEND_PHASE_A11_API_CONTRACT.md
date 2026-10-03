# ISHAARA FRONTEND PHASE A11 — API CONTRACT SPECIFICATION

## 1. Overview
This document specifies the actual transport endpoints, WebSocket protocol frames, and REST endpoints for **Phase A11 — Live Ride, Realtime State & GPS Foundation**, cross-verified against `ishara-backend/src/modules/realtime/` and `src/modules/rides/`.

---

## 2. Realtime WebSocket Transport

### 2.1 Connection Upgrade
- **Endpoint:** `wss://<host>/api/v1/rides/realtime` (or `ws://<host>/api/v1/rides/realtime`)
- **Query Parameter:** `?token=<jwt_bearer_token>`
- **Authentication:** Validates active Better Auth session.
- **Heartbeat:** Server pings every 30 seconds; client responds with standard pong frames.

### 2.2 Client Messages

#### 1. Subscribe to Live Ride Tracking
```json
{
  "type": "RIDE_TRACKING_SUBSCRIBE",
  "payload": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001"
  }
}
```

#### 2. Unsubscribe from Live Ride Tracking
```json
{
  "type": "RIDE_TRACKING_UNSUBSCRIBE",
  "payload": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001"
  }
}
```

---

### 2.3 Server Messages

#### 1. Subscription Confirmation (`RIDE_TRACKING_SUBSCRIBED`)
```json
{
  "type": "RIDE_TRACKING_SUBSCRIBED",
  "sessionId": "rreq_abc123",
  "sequence": 1,
  "timestamp": "2026-10-01T08:00:00.000Z",
  "payload": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001",
    "driverId": "651a2b3c4d5e6f7a8b9c0002",
    "timestamp": "2026-10-01T08:00:00.000Z"
  }
}
```

#### 2. Initial Tracking Snapshot (`TRACKING_SNAPSHOT`)
```json
{
  "type": "TRACKING_SNAPSHOT",
  "sessionId": "rreq_abc123",
  "sequence": 2,
  "timestamp": "2026-10-01T08:00:01.000Z",
  "payload": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001",
    "status": "IN_PROGRESS",
    "trackingState": "FRESH",
    "driver": {
      "location": { "latitude": 25.2677, "longitude": 82.9913 },
      "accuracyMeters": 5.0,
      "headingDegrees": 180.0,
      "speedMps": 8.5,
      "recordedAt": "2026-10-01T08:00:00.000Z",
      "receivedAt": "2026-10-01T08:00:01.000Z",
      "freshness": "FRESH"
    },
    "route": {
      "distanceMeters": 5000,
      "completedDistanceMeters": 2000,
      "remainingDistanceMeters": 3000,
      "progressPercent": 40.0,
      "distanceFromRouteMeters": 2.5,
      "isOffRoute": false
    },
    "distanceToPickupMeters": null,
    "distanceToDestinationMeters": 3000,
    "eta": {
      "available": true,
      "seconds": 360,
      "source": "LOCAL_ESTIMATE",
      "confidence": "MEDIUM"
    },
    "updatedAt": "2026-10-01T08:00:01.000Z"
  }
}
```

#### 3. Realtime Location Update (`DRIVER_LOCATION_UPDATED`)
```json
{
  "type": "DRIVER_LOCATION_UPDATED",
  "payload": {
    "event": "DRIVER_LOCATION_UPDATED",
    "driverId": "651a2b3c4d5e6f7a8b9c0002",
    "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
    "rideId": "651a2b3c4d5e6f7a8b9c0001",
    "location": { "latitude": 25.2750, "longitude": 82.9950 },
    "accuracyMeters": 4.2,
    "headingDegrees": 175.0,
    "speedMps": 9.1,
    "altitudeMeters": 76.0,
    "recordedAt": "2026-10-01T08:01:00.000Z",
    "receivedAt": "2026-10-01T08:01:01.000Z"
  }
}
```

#### 4. Realtime Tracking Progress Update (`RIDE_TRACKING_UPDATED`)
```json
{
  "type": "RIDE_TRACKING_UPDATED",
  "payload": {
    "rideId": "651a2b3c4d5e6f7a8b9c0001",
    "driverLocation": { "latitude": 25.2750, "longitude": 82.9950 },
    "freshness": "FRESH",
    "trackingState": "FRESH",
    "routeProgress": {
      "completedDistanceMeters": 2500,
      "remainingDistanceMeters": 2500,
      "progressPercent": 50.0
    },
    "distanceToPickupMeters": null,
    "distanceToDestinationMeters": 2500,
    "eta": { "available": true, "seconds": 300 },
    "recordedAt": "2026-10-01T08:01:00.000Z"
  }
}
```

#### 5. Ride Lifecycle State Events
- `RIDE_DRIVER_ARRIVING`: `{"type":"RIDE_DRIVER_ARRIVING","payload":{"rideId":"..."}}`
- `RIDE_PICKED_UP`: `{"type":"RIDE_PICKED_UP","payload":{"rideId":"..."}}`
- `RIDE_STARTED`: `{"type":"RIDE_STARTED","payload":{"rideId":"..."}}`
- `RIDE_COMPLETED`: `{"type":"RIDE_COMPLETED","payload":{"rideId":"..."}}`
- `RIDE_CANCELLED`: `{"type":"RIDE_CANCELLED","payload":{"rideId":"...","reason":"..."}}`
- `RIDE_TRACKING_ENDED`: `{"type":"RIDE_TRACKING_ENDED","payload":{"rideId":"...","status":"COMPLETED"}}`

---

## 3. Authoritative REST Endpoints

### 3.1 Get Authoritative Ride Tracking Snapshot (Hydration & Reconciliation)
- **Path:** `GET /api/v1/rides/:rideId/tracking`
- **Authentication:** `Bearer <jwt_token>` (USER or DRIVER_CONDUCTOR)
- **Success:** `200 OK` with `TrackingResponse` payload matching `TRACKING_SNAPSHOT`.

### 3.2 Get Driver Location (Fallback)
- **Path:** `GET /api/v1/rides/:rideId/driver-location`
- **Authentication:** `Bearer <jwt_token>` (USER only)
- **Success:** `200 OK` with `{ rideId, driverId, location, isStale, status, recordedAt }`.

### 3.3 Ingest Driver GPS Location (Driver-Side)
- **Path:** `PATCH /api/v1/drivers/me/location`
- **Authentication:** `Bearer <jwt_token>` (DRIVER_CONDUCTOR only)
- **Body:** `{ latitude, longitude, accuracyMeters?, headingDegrees?, speedMps?, altitudeMeters?, recordedAt? }`
- **Success:** `200 OK`.
