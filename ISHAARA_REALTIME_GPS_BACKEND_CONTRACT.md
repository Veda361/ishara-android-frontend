# Ishaara Realtime GPS & Telemetry Backend Contract Specification

**Document Version**: 1.0.0  
**Phase**: PHASE 09 — GPS + REALTIME DRIVER TELEMETRY  
**Status**: AUDITED & AUTHORITATIVE  
**Target Roles**: `DRIVER_CONDUCTOR`, `USER` (Passenger Observer)  
**Rule**: NEVER invent backend fields or protocols. Mark undocumented items as `NOT DETERMINED FROM CURRENT BACKEND CONTRACT`.

---

## 1. Driver Location Ingestion Endpoint

### 1.1 Ingestion Endpoint Overview
- **HTTP Method**: `PATCH` (VERIFIED)
- **URL**: `/api/v1/drivers/me/location` (VERIFIED)
- **Protocol**: HTTPS (REST)
- **Backend Controller**: `driverController.updateLocation` -> `driverOperationsService.updateLocation`
- **Functional Description**: Ingests high-frequency GPS ping for the authenticated driver with monotonic timestamp enforcement. Updates live vehicle telemetry and spatial indexing for active passenger rides.
- **Related Read Endpoint**: `GET /api/v1/drivers/me/location` (Retrieves driver's own recorded GPS position and freshness status).

---

## 2. Exact Request Schema (`PATCH /api/v1/drivers/me/location`)

### 2.1 Request Payload (JSON)
```json
{
  "coordinates": [73.8567, 18.5204],
  "heading": 85.5,
  "speed": 12.3,
  "accuracy": 4.8,
  "recordedAt": "2026-09-25T10:00:00.000Z"
}
```

### 2.2 Field Breakdown & Specifications
| Field | Type | Unit / Format | Status | Required / Optional | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `coordinates` | `Array<Double>` | `[longitude, latitude]` | **VERIFIED** | **REQUIRED** | GeoJSON Point array strictly ordered as `[lng, lat]`. Bounds: lng `[-180.0, 180.0]`, lat `[-90.0, 90.0]`. |
| `heading` | `Double` / `Float` | Degrees (`0.0 .. 360.0`) | **VERIFIED** | NOT DETERMINED FROM CURRENT BACKEND CONTRACT (Assumed optional in payload) | Vehicle heading relative to true north. |
| `speed` | `Double` / `Float` | Meters per second (`m/s`) | **VERIFIED** | NOT DETERMINED FROM CURRENT BACKEND CONTRACT (Assumed optional in payload) | Current ground speed. |
| `accuracy` | `Double` / `Float` | Meters (`m`) | **VERIFIED** | NOT DETERMINED FROM CURRENT BACKEND CONTRACT (Assumed optional in payload) | 68% confidence horizontal accuracy radius. |
| `recordedAt` | `String` | ISO-8601 UTC timestamp (`YYYY-MM-DDTHH:mm:ss.sssZ`) | **VERIFIED** | **REQUIRED** | Device hardware GPS fix timestamp. Monotonically enforced by backend. |
| `altitude` | N/A | N/A | **ABSENT** | N/A | NOT DETERMINED FROM CURRENT BACKEND CONTRACT (Not in current schema). |
| `tripId` | N/A | N/A | **ABSENT** | N/A | NOT DETERMINED FROM CURRENT BACKEND CONTRACT (Not in payload; server resolves active trip from driver session context). |
| `vehicleId` | N/A | N/A | **ABSENT** | N/A | NOT DETERMINED FROM CURRENT BACKEND CONTRACT (Server resolves assigned vehicle). |

---

## 3. Exact Response Schema (`PATCH /api/v1/drivers/me/location`)

### 3.1 HTTP 200 OK (Success)
- Standard envelope observed across Ishaara backend:
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "recordedAt": "2026-09-25T10:00:00.000Z",
    "status": "PROCESSED"
  }
}
```
*Exact response payload body*: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT** (Endpoint may return HTTP 200 with metadata envelope or HTTP 204 No Content). Client must treat any `200..299` code as success.

---

## 4. Authentication & Authorization

- **Header**: `Authorization: Bearer <token>` (VERIFIED)
- **Role Requirement**: `DRIVER_CONDUCTOR` (VERIFIED)
- **Token Type**: JWT issued upon Google Sign-In verification or session restore.
- **Unauthorized Behavior (401)**: Intercepted by `SessionInvalidationCoordinator` to revoke local credentials and prompt re-authentication.
- **Forbidden Behavior (403)**:
  - If authenticated user role is `USER`: Rejected with HTTP 403 Forbidden.
  - If driver `verificationStatus` is not `VERIFIED`: Rejected with HTTP 403 (`DRIVER_NOT_VERIFIED`).

---

## 5. Error Codes & Validation

| Status Code | Error Code | Condition | Client Recovery |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `INVALID_COORDINATES` | Latitude or longitude out of physical bounds. | Drop corrupted sample. Log warning (redacted). |
| `400 Bad Request` | `OUT_OF_ORDER_TIMESTAMP` / `STALE_TIMESTAMP` | `recordedAt` is older than previously recorded monotonic timestamp. | Drop stale ping; do not retry older timestamp. Transmit next fresh fix. |
| `401 Unauthorized` | `UNAUTHORIZED` / `TOKEN_EXPIRED` | Bearer token invalid or expired. | Abort telemetry transmission. Invalidate session. |
| `403 Forbidden` | `FORBIDDEN` / `DRIVER_NOT_VERIFIED` | Driver profile pending or unverified. | Cease GPS upload. Transition UI to Error/Verification state. |
| `429 Too Many Requests` | `RATE_LIMITED` | Telemetry transmission exceeds backend ingestion limits. | Exponential backoff / increase sampling interval. |
| `500 Server Error` | `INTERNAL_SERVER_ERROR` | Backend database or ingestion pipeline transient failure. | Discard failed ping; wait for next periodic GPS tick. |

*Specific backend error string codes for monotonic timestamp violation*: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT**.

---

## 6. Coordinate Conventions & Anti-Inversion Rules

- **Standard**: RFC 7946 GeoJSON.
- **Backend Array Format**: `[longitude, latitude]` (`doubleArrayOf(lng, lat)`).
  - Index 0: `longitude` (-180.0 to 180.0)
  - Index 1: `latitude` (-90.0 to 90.0)
- **Critical Anti-Inversion Guard**:
  - Android `Location` class exposes `.latitude` and `.longitude`.
  - Android Google Maps and UI components use `(lat, lng)`.
  - Ingestion DTO strictly maps via `com.ishara.app.core.location.GeoJsonCoordinate` or direct `[location.longitude, location.latitude]`.
  - Coordinates inversion hazard: Passing `[lat, lng]` will map the vehicle to Antarctica or international waters, causing geo-query matching failures.

---

## 7. Realtime WebSocket Endpoints Overview

The backend API specification documents 4 distinct WebSocket upgrade endpoints under `ws(s)://<host><endpoint>?token=<token>`:

| Endpoint | Target Role | Primary Purpose | Ingestion vs Egress | Relevance to Driver GPS |
| :--- | :--- | :--- | :--- | :--- |
| `/api/v1/voice/realtime` | `DRIVER_CONDUCTOR` | Bi-directional audio chunk streaming & transcription | Bi-directional | None (Voice assistant only) |
| `/api/v1/discovery/realtime` | `USER` (Passenger) | Push notifications for newly active matching trips | Server -> Client | None (Passenger discovery) |
| `/api/v1/ride-requests/realtime` | `DRIVER_CONDUCTOR` & `USER` | Ride request creation, acceptance, rejection events | Bi-directional / Push | Secondary (Driver ride dispatch) |
| `/api/v1/rides/realtime` | `USER` (Passenger) & `DRIVER_CONDUCTOR` | Live GPS tracking, ETA, vehicle heading, route progress | Server -> Client (Egress) | **Primary Egress Channel** |

---

## 8. Realtime Channel Analysis for Driver GPS Telemetry

### 8.1 Ingestion Channel (Driver -> Server)
- **Authoritative Ingestion Mechanism**: **HTTP REST `PATCH /api/v1/drivers/me/location`** (VERIFIED).
- **WebSocket Driver Ingestion**: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT**.
  The API specification does not define a driver-to-server GPS streaming message protocol over `/api/v1/rides/realtime`. Ingestion is formally documented under REST Section 4.13.

### 8.2 Egress Channel (Server -> Passenger / Map Viewers)
- **Authoritative Egress Stream**: **`wss://<host>/api/v1/rides/realtime?token=<bearer_token>`** (VERIFIED).
- **Backend Role**: Server broadcasts telemetry received via `PATCH /drivers/me/location` down to passengers subscribed to active rides on `/api/v1/rides/realtime`.

---

## 9. WebSocket Authentication Mechanism

- **Handshake URL**:
  ```http
  GET /api/v1/rides/realtime?token=<bearer_token> HTTP/1.1
  Host: <host>
  Upgrade: websocket
  Connection: Upgrade
  Sec-WebSocket-Key: <key>
  Sec-WebSocket-Version: 13
  ```
- **Token Delivery**: HTTP upgrade query parameter `?token=<bearer_token>` (VERIFIED).
- **Subprotocol**: Default / none specified.

---

## 10. Channel & Event Definitions (`/api/v1/rides/realtime`)

### 10.1 Telemetry Payload Schema (Server -> Client)
Observed in `RealtimeEvent.LiveTelemetryUpdate`:
```json
{
  "event": "live_telemetry_update",
  "rideId": "651a2b3c4d5e6f7a8b9c0d50",
  "coordinates": [73.8567, 18.5204],
  "heading": 85.5,
  "speed": 12.3,
  "remainingDistanceMeters": 1450,
  "etaSeconds": 320,
  "timestamp": "2026-09-25T10:00:00.000Z"
}
```
*Exact event naming and envelope framing*: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT** (Backend contract states telemetry fields: coordinates `[lng, lat]`, vehicle heading, distance-to-pickup, route progress updates; wire frame envelope is unstated).

### 10.2 Subscription Messages (Client -> Server)
- Does the client need to send a `{"action": "subscribe", "rideId": "..."}` or is subscription automatic based on the user's active ride context?
  **NOT DETERMINED FROM CURRENT BACKEND CONTRACT**.

---

## 11. Connection Lifecycle & Reconnection Behavior

- **Connection States**:
  `Disconnected` -> `Connecting` -> `Connected` -> `Reconnecting(attempt, delay)` -> `Failed(error)`.
- **Reconnection Policy**:
  - Exponential backoff with jitter: Initial delay 1s, multiplier 2.0, max delay 30s.
  - Max automatic retry attempts before reporting failure: 5 attempts.
  - Abort on auth failure (HTTP 401 / WebSocket close code 4401 or 1008).
- **Backend Disconnect Close Codes**:
  - `1000`: Normal closure.
  - `1008` / `4401`: Policy violation / Unauthorized token expired.
  - *Custom backend close codes*: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT**.

---

## 12. Heartbeat / Keep-Alive Behavior

- **Ping/Pong Protocol**:
  - Transport-level standard RFC 6455 Ping (0x9) / Pong (0xA) frames.
  - Recommended client ping interval: 30 seconds (OkHttp `pingInterval(30, TimeUnit.SECONDS)`).
  - *Application-level JSON heartbeat (e.g. `{"type": "ping"}`)*: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT**.

---

## 13. Location Lifecycle Matrix

| Driver State | App Foreground | App Background | Transmission Target | Action / Service State |
| :--- | :--- | :--- | :--- | :--- |
| **Logged Out** | Any | Any | None | Location disabled. Zero network. |
| **Offline** (`status == OFFLINE`) | Any | Any | None | Location disabled. Foreground service stopped. |
| **Online Idle** (`status == ONLINE`, no trip) | Foreground | Inactive | `PATCH /me/location` (Low Freq / Option B: Paused) | **REQUIRES PRODUCT DECISION** (Whether to transmit when idle). |
| **Online Idle** (`status == ONLINE`, no trip) | Background | Suspended | None | Service stopped if idle tracking is not enabled. |
| **Active Trip** (`status == ON_RIDE`) | Foreground | Active | `PATCH /me/location` (High Freq, e.g. 4s) | In-app location provider streaming + UI map updates. |
| **Active Trip** (`status == ON_RIDE`) | Background | Active | `PATCH /me/location` (High Freq, e.g. 4s) | **Foreground Service Required** with persistent notification. |
| **Trip Completed / Cancelled** | Any | Any | Transitions to Online Idle | Foreground service gracefully tears down or switches to idle. |
| **Screen Locked** | Locked | Active | `PATCH /me/location` | Foreground Service continues execution. |
| **Process Killed** | Dead | Dead | None | OS terminates service. Restart on next boot/launch if trip active. |
| **Network Lost** | Any | Any | Ingestion paused | Discard intermediate points. Transmit latest upon reconnect. |
| **GPS Lost / Disabled** | Any | Any | Ingestion paused | Display warning to driver; prompt system Location Settings. |
| **Permission Revoked** | Any | Any | Transmission halted | Stop service, prompt permission dialog. |

---

## 14. Android Permission Requirements

- `android.permission.ACCESS_FINE_LOCATION` (Required for GPS hardware accuracy)
- `android.permission.ACCESS_COARSE_LOCATION` (Required alongside fine location)
- `android.permission.ACCESS_BACKGROUND_LOCATION` (Required if accessing location without foreground service on Android 10+; however, Foreground Service with `FOREGROUND_SERVICE_LOCATION` is the preferred transit standard)
- `android.permission.FOREGROUND_SERVICE` (Required for ongoing driver tracking)
- `android.permission.FOREGROUND_SERVICE_LOCATION` (Required on Android 14+ / API 34+ for location-type foreground services)
- `android.permission.POST_NOTIFICATIONS` (Required on Android 13+ / API 33+ to display the mandatory foreground service notification)

---

## 15. Foreground vs Background Execution Strategy

1. **Foreground Service Exemption**:
   Android enforces strict background location limits (throttling apps to a few location fixes per hour when in the background).
2. **Mandatory Transit Service**:
   To deliver continuous 3-5 second telemetry during an active passenger trip, the driver app MUST execute a Foreground Service with `android:foregroundServiceType="location"`.
3. **Notification Requirement**:
   The service must display an ongoing, non-dismissible notification stating:
   *"Ishaara Driver Service Active — Sharing live location for Trip #UP-BUS-..."*

---

## 16. Offline & Network Resiliency Behavior

- **No Unbounded Historical Queue**:
  The backend enforces strictly monotonic timestamps and serves real-time passengers. Sending a burst of 50 stale locations queued during a cellular dead zone would violate real-time guarantees, potentially trigger monotonic rejection, and cause map jumps.
- **Rule**:
  - Buffer at most **one** location fix (the most recent valid GPS reading).
  - When network recovers, transmit the single freshest fix.
  - Drop all intermediate stale coordinates.
  - *Historical track logging endpoint*: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT** (Does not exist).

---

## 17. Security & Privacy Considerations

1. **Bearer Token Protection**:
   - HTTP requests use `Authorization: Bearer <token>`.
   - WebSocket URL uses `?token=<token>`. Query params MUST NOT be written to Android logcat or crash reporters.
2. **Zero Plaintext Coordinates in Production Logs**:
   - GPS precision down to 6 decimal places identifies specific doorsteps and vehicle lanes.
   - `IshaaraLogger` must never log raw latitude/longitude strings in production builds. In debug builds, coordinates must be coarsened or omitted.
3. **No Persistent Location Breadcrumbs**:
   - Do not store raw driver latitude/longitude in SharedPreferences, Room, or unencrypted storage.

---

## 18. Battery & Frequency Specifications

- **Backend Specified Frequency**: **NOT DETERMINED FROM CURRENT BACKEND CONTRACT** (Described as "high-frequency GPS ping").
- **Recommended Industry Transit Specifications (REQUIRES PRODUCT DECISION)**:
  - Active Trip (`ON_RIDE`):
    - Interval: `4,000 ms` (4 seconds)
    - Fastest Interval: `2,000 ms` (2 seconds)
    - Min Displacement: `5 meters`
    - Priority: `PRIORITY_HIGH_ACCURACY`
  - Idle Online (`ONLINE` without trip):
    - Interval: `30,000 ms` (30 seconds) or paused
    - Min Displacement: `50 meters`
    - Priority: `PRIORITY_BALANCED_POWER_ACCURACY`

---

## 19. Undetermined / Undocumented Backend Items Summary

The following items are **NOT DETERMINED FROM CURRENT BACKEND CONTRACT** and require explicit backend/product team clarification:
1. Exact response JSON body for `PATCH /api/v1/drivers/me/location`.
2. Exact rate-limiting threshold (requests/minute) for `PATCH /api/v1/drivers/me/location`.
3. Specific error code string returned on monotonic timestamp violation.
4. Optionality of `heading`, `speed`, and `accuracy` in the request body.
5. Presence of any WebSocket telemetry ingestion channel for drivers (vs HTTP PATCH).
6. Exact JSON wire envelope and message types for `wss://<host>/api/v1/rides/realtime`.
7. Client-side subscription protocol for WebSocket channels (explicit subscribe message vs token-inferred).
8. Heartbeat protocol (RFC 6455 Ping/Pong frame vs JSON message).
9. Product requirement for GPS tracking while `ONLINE` but idle (no active trip).
