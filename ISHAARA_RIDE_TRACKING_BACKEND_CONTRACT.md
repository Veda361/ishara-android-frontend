# ISHAARA — Phase 11 Ride Tracking Backend Contract Specification

**Document Version:** 1.0.0  
**Phase:** 11 — Live Passenger Map / Ride Tracking  
**Audited Backend Modules:**  
- `src/modules/tracking/tracking.controller.ts`
- `src/modules/tracking/tracking.service.ts`
- `src/modules/tracking/tracking.types.ts`
- `src/modules/tracking/tracking-event.publisher.ts`
- `src/modules/realtime/realtime.gateway.ts`
- `src/modules/rides/ride.controller.ts`
- `src/modules/drivers/driver-location.service.ts`

---

## 1. Executive Summary

Phase 11 implements the live passenger ride tracking experience for the `USER` (Student / Passenger) role. The backend contract defines a hybrid **REST Snapshot + Realtime Live Streaming** architecture:
1. **REST Snapshot:** Used on initial screen load, user pull-to-refresh, and post-reconnection state recovery (`GET /api/v1/rides/:rideId/tracking`).
2. **Driver Location Snapshot:** Secondary fallback endpoint for raw driver coordinates (`GET /api/v1/rides/:rideId/driver-location`).
3. **Realtime WebSocket Channel:** Bi-directional JSON protocol over `wss://<host>/api/v1/rides/realtime?token=<jwt>`. The passenger explicitly sends `RIDE_TRACKING_SUBSCRIBE`, receives an immediate `TRACKING_SNAPSHOT`, followed by throttled `RIDE_TRACKING_UPDATED` telemetry frames as the driver's GPS updates, and a final `RIDE_TRACKING_ENDED` frame upon completion or cancellation.

---

## 2. REST Endpoints

### 2.1 GET /api/v1/rides/:rideId/tracking

Retrieves authoritative live ride tracking information for an authorized passenger or driver.

- **HTTP Method:** `GET`
- **Path:** `/api/v1/rides/:rideId/tracking`
- **Headers:**  
  `Authorization: Bearer <jwt_access_token>`
- **Authorization Rules:**  
  - If caller is `USER` (Passenger): Caller `userId` MUST match `ride.userId`. (Returns 403 `RIDE_NOT_AUTHORIZED` otherwise).
  - If caller is `DRIVER_CONDUCTOR`: Driver profile `_id` MUST match `ride.driverId`.
  - Non-existent ride ID returns 404 `RIDE_NOT_FOUND`.
  - Invalid Mongo ObjectId format returns 400 `INVALID_ID`.

#### Response Payload (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "rideId": "651234567890abcdef123456",
    "status": "IN_PROGRESS",
    "trackingState": "FRESH",
    "driver": {
      "location": {
        "latitude": 25.4484,
        "longitude": 78.5685
      },
      "accuracyMeters": 6.5,
      "headingDegrees": 182.0,
      "speedMps": 11.2,
      "recordedAt": "2026-09-25T14:15:30.000Z",
      "receivedAt": "2026-09-25T14:15:31.000Z",
      "freshness": "FRESH"
    },
    "route": {
      "distanceMeters": 8500,
      "completedDistanceMeters": 3200,
      "remainingDistanceMeters": 5300,
      "progressPercent": 37.6,
      "distanceFromRouteMeters": 12.4,
      "isOffRoute": false
    },
    "distanceToPickupMeters": null,
    "distanceToDestinationMeters": 5300,
    "eta": {
      "available": true,
      "seconds": 473,
      "source": "LOCAL_ESTIMATE",
      "confidence": "MEDIUM"
    },
    "updatedAt": "2026-09-25T14:15:31.100Z"
  }
}
```

#### Terminal Ride Response
When a ride is in a terminal state (`COMPLETED` or `CANCELLED`), the backend clears route projection and returns:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "rideId": "651234567890abcdef123456",
    "status": "COMPLETED",
    "trackingState": "UNAVAILABLE",
    "driver": null,
    "route": null,
    "distanceToPickupMeters": null,
    "distanceToDestinationMeters": null,
    "eta": {
      "available": false
    },
    "updatedAt": "2026-09-25T14:18:00.000Z"
  }
}
```

---

### 2.2 GET /api/v1/rides/:rideId/driver-location

Secondary endpoint strictly retrieving raw operational driver location.

- **HTTP Method:** `GET`
- **Path:** `/api/v1/rides/:rideId/driver-location`
- **Headers:**  
  `Authorization: Bearer <jwt_access_token>`
- **Authorization Rules:**  
  - Caller MUST be the passenger (`USER`) whose `userId` matches `ride.userId`. Drivers receive 403 `RIDE_NOT_AUTHORIZED` on this endpoint.

#### Response Payload (200 OK)
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "rideId": "651234567890abcdef123456",
    "driverId": "650987654321fedcba654321",
    "location": {
      "latitude": 25.4484,
      "longitude": 78.5685
    },
    "accuracyMeters": 6.5,
    "headingDegrees": 182.0,
    "speedMps": 11.2,
    "altitudeMeters": 245.0,
    "recordedAt": "2026-09-25T14:15:30.000Z",
    "receivedAt": "2026-09-25T14:15:31.000Z",
    "isStale": false,
    "status": "FRESH"
  },
  "message": "Driver location retrieved successfully."
}
```

---

## 3. Realtime WebSocket Protocol

- **Endpoint URL:** `wss://<host>/api/v1/rides/realtime?token=<jwt_access_token>`
- **Transport:** WebSocket text frames (JSON serialized).
- **Session Auth:** Provided as query parameter `token`.
- **Heartbeat:** Client may send `{"type": "PING"}` and receives `{"type": "PONG", "payload": {"pongAt": "..."}}`.

### 3.1 Subscription Envelopes

#### Client Subscribe Request
Upon entering the tracking screen, client sends:
```json
{
  "type": "RIDE_TRACKING_SUBSCRIBE",
  "payload": {
    "rideId": "651234567890abcdef123456"
  }
}
```

#### Server Subscription Confirmation
Backend immediately validates participant authorization and active ride status (`CREATED`, `DRIVER_ARRIVING`, `PICKED_UP`, `IN_PROGRESS`), adds client transport to `rideTrackingSubscribers`, and sends:
```json
{
  "type": "RIDE_TRACKING_SUBSCRIBED",
  "payload": {
    "rideId": "651234567890abcdef123456",
    "driverId": "650987654321fedcba654321",
    "timestamp": "2026-09-25T14:15:30.500Z"
  }
}
```

#### Server Initial Snapshot Delivery
Directly following `RIDE_TRACKING_SUBSCRIBED`, the server computes and sends the current full tracking snapshot:
```json
{
  "type": "TRACKING_SNAPSHOT",
  "payload": {
    "rideId": "651234567890abcdef123456",
    "status": "IN_PROGRESS",
    "trackingState": "FRESH",
    "driver": {
      "location": { "latitude": 25.4484, "longitude": 78.5685 },
      "accuracyMeters": 6.5,
      "headingDegrees": 182.0,
      "speedMps": 11.2,
      "recordedAt": "2026-09-25T14:15:30.000Z",
      "receivedAt": "2026-09-25T14:15:31.000Z",
      "freshness": "FRESH"
    },
    "route": {
      "distanceMeters": 8500,
      "completedDistanceMeters": 3200,
      "remainingDistanceMeters": 5300,
      "progressPercent": 37.6,
      "distanceFromRouteMeters": 12.4,
      "isOffRoute": false
    },
    "distanceToPickupMeters": null,
    "distanceToDestinationMeters": 5300,
    "eta": {
      "available": true,
      "seconds": 473,
      "source": "LOCAL_ESTIMATE",
      "confidence": "MEDIUM"
    },
    "updatedAt": "2026-09-25T14:15:31.100Z"
  }
}
```

#### Live Tracking Updates
As the driver transmits GPS updates (`PATCH /api/v1/drivers/location`), `TrackingEventPublisher` calculates the updated tracking view and broadcasts to all subscribers (throttled by `TRACKING_REALTIME_MIN_INTERVAL_MS`):
```json
{
  "type": "RIDE_TRACKING_UPDATED",
  "payload": {
    "rideId": "651234567890abcdef123456",
    "driverLocation": {
      "latitude": 25.4489,
      "longitude": 78.5689
    },
    "freshness": "FRESH",
    "trackingState": "FRESH",
    "routeProgress": {
      "completedDistanceMeters": 3280,
      "remainingDistanceMeters": 5220,
      "progressPercent": 38.6
    },
    "distanceToPickupMeters": null,
    "distanceToDestinationMeters": 5220,
    "eta": {
      "available": true,
      "seconds": 465,
      "source": "LOCAL_ESTIMATE",
      "confidence": "MEDIUM"
    },
    "recordedAt": "2026-09-25T14:15:35.000Z"
  }
}
```

#### Terminal State (Ride Ended)
When the driver completes (`COMPLETED`) or cancels (`CANCELLED`) the ride, the server notifies all tracking subscribers and clears subscription channels:
```json
{
  "type": "RIDE_TRACKING_ENDED",
  "payload": {
    "rideId": "651234567890abcdef123456",
    "status": "COMPLETED",
    "reason": null,
    "timestamp": "2026-09-25T14:20:00.000Z"
  }
}
```

#### Client Unsubscribe Request
Upon leaving the tracking screen or when the screen is destroyed, client sends:
```json
{
  "type": "RIDE_TRACKING_UNSUBSCRIBE",
  "payload": {
    "rideId": "651234567890abcdef123456"
  }
}
```

---

## 4. Authoritative Enums & Types

### 4.1 Ride Status (`status`)
- `CREATED`: Ride request accepted, vehicle assigned.
- `DRIVER_ARRIVING`: Driver en route to student pickup point.
- `PICKED_UP`: Passenger successfully boarded and OTP/ticket verified.
- `IN_PROGRESS`: Bus in transit to passenger destination.
- `COMPLETED`: Passenger arrived and safely dropped off (terminal).
- `CANCELLED`: Ride cancelled by driver or dispatch (terminal).

### 4.2 Tracking State (`trackingState`)
Explicit hierarchical evaluation order:
1. `UNAVAILABLE`: No usable GPS coordinates exist or ride is terminal.
2. `STALE`: Location exists but recorded timestamp exceeds stale threshold (`GPS_LOCATION_STALE_AFTER_SECONDS`).
3. `OFF_ROUTE`: Fresh location but driver cross-track distance exceeds route corridor threshold.
4. `FRESH`: Fresh location within planned route corridor.

### 4.3 Freshness Status (`freshness`)
- `FRESH`: GPS timestamp age is within freshness threshold.
- `STALE`: GPS timestamp age exceeds stale threshold.
- `UNAVAILABLE`: Missing coordinates or unrecorded.

---

## 5. Coordinate Conventions

1. **REST & Tracking WebSocket Payload:**
   Normalized 2D spherical object:
   ```json
   { "latitude": 25.4484, "longitude": 78.5685 }
   ```
2. **GeoJSON Internal Convention (Trip / Route geometry):**
   RFC 7946 GeoJSON format:
   ```json
   [ longitude, latitude ]
   ```
3. **Android Client Domain Convention:**
   Centralized `LocationCoordinates(latitude: Double, longitude: Double, ...)` model. Conversion between GeoJSON `[lng, lat]` and `{latitude, longitude}` occurs strictly at the data/mapper layer.

---

## 6. ETA Specification

- Backend supplies `eta: { available: Boolean, seconds?: Int, source?: String, confidence?: String }`.
- Client MUST check `eta.available == true` and `eta.seconds != null` before rendering ETA to the passenger.
- Client MUST NEVER invent or fabricate an artificial ETA. If `available == false`, client displays "Estimating..." or omits ETA without breaking layout.
