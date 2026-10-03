# ISHAARA Phase 15 — Safety & SOS Backend Contract

This document provides the authoritative contract for Safety, SOS, and Emergency Management in the ISHAARA system, derived directly from the backend source code (`src/modules/safety/`, `src/modules/rides/ride.routes.ts`, `src/modules/realtime/`) and test suites.

---

## 1. Endpoints

| Endpoint | Method | Auth | Role | Purpose |
|---|---|---|---|---|
| `/api/v1/rides/:rideId/safety/sos` | POST | Bearer Token | `USER` or `DRIVER_CONDUCTOR` (Ride Participant) | Triggers an active SOS emergency event for an active ride. |
| `/api/v1/rides/:rideId/safety/active` | GET | Bearer Token | `USER` or `DRIVER_CONDUCTOR` (Ride Participant) | Retrieves the current active SOS event for the ride, or `null` if none. |
| `/api/v1/rides/:rideId/safety/events` | GET | Bearer Token | `USER` or `DRIVER_CONDUCTOR` (Ride Participant) | Retrieves paginated history of safety events for the ride (limit max 50). |
| `/api/v1/rides/:rideId/safety/cancel` | POST | Bearer Token | Participant who triggered SOS (`USER` or `DRIVER_CONDUCTOR`) | Cancels the caller's active SOS event on the specified ride. |
| `/api/v1/safety/events/:eventId` | GET | Bearer Token | Ride Participant | Retrieves a specific emergency event by its human-readable `eventId` (or MongoDB `_id`). |
| `/api/v1/safety/events/:eventId/cancel` | POST | Bearer Token | Participant who triggered SOS | Cancels a specific active SOS event by `eventId`. |
| `/api/v1/users/me/emergency-contacts` | GET | Bearer Token | `USER` | Lists all active emergency contacts configured for the authenticated user (max 5). |
| `/api/v1/users/me/emergency-contacts` | POST | Bearer Token | `USER` | Adds a new emergency contact for the user. |
| `/api/v1/users/me/emergency-contacts/:contactId` | PATCH | Bearer Token | `USER` (Owner) | Updates details of an existing emergency contact. |
| `/api/v1/users/me/emergency-contacts/:contactId` | DELETE | Bearer Token | `USER` (Owner) | Soft-deletes an emergency contact (sets `isActive: false`). |

---

## 2. Requests

### 2.1 Trigger SOS (`POST /api/v1/rides/:rideId/safety/sos`)

- **Headers**:
  - `Authorization: Bearer <session-token>` (Required)
  - `Idempotency-Key: <uuid>` (Optional but strongly recommended for mobile retry safety)
  - `Content-Type: application/json`

- **Schema**: Zod schema is `.strict()`. Any extra or unrecognized fields will result in `400 Bad Request`.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `emergencyType` | `String` (`"SOS"` \| `"SAFETY_CONCERN"`) | No | Type of emergency event. Defaults to `"SOS"`. |

> [!IMPORTANT]
> The client **MUST NOT** send `location`, `rideId`, `driverId`, or `triggeredByUserId` in the request body. The backend enforces strict schema validation and derives all identity, ride metadata, and location snapshots server-side.

### 2.2 Cancel SOS (`POST /api/v1/rides/:rideId/safety/cancel` & `POST /api/v1/safety/events/:eventId/cancel`)

- **Headers**:
  - `Authorization: Bearer <session-token>` (Required)
  - `Content-Type: application/json`

- **Schema**: Zod schema is `.strict()`.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `reason` | `String` (max 500 chars) | No | Optional cancellation reason (e.g. false alarm). |

### 2.3 Create Emergency Contact (`POST /api/v1/users/me/emergency-contacts`)

- **Schema**: Zod schema is `.strict()`.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `name` | `String` (1–100 chars) | Yes | Contact's full name. |
| `phoneNumber` | `String` (7–15 digits, opt `+`) | Yes | E.164-compatible phone number (e.g., `+919876543210`). |
| `relationship` | `String` (`PARENT` \| `SPOUSE` \| `SIBLING` \| `FRIEND` \| `GUARDIAN` \| `OTHER`) | Yes | Relationship to passenger. |

### 2.4 Update Emergency Contact (`PATCH /api/v1/users/me/emergency-contacts/:contactId`)

- **Schema**: Zod schema is `.strict()`. At least one field required.

| Field | Type | Required | Meaning |
|---|---|---|---|
| `name` | `String` (1–100 chars) | No | Updated contact name. |
| `phoneNumber` | `String` (7–15 digits, opt `+`) | No | Updated contact phone number. |
| `relationship` | `String` (`PARENT` \| `SPOUSE` \| `SIBLING` \| `FRIEND` \| `GUARDIAN` \| `OTHER`) | No | Updated relationship. |
| `isActive` | `Boolean` | No | Active status toggle. |

---

## 3. Responses

### 3.1 Emergency Event Response (`EmergencyEventResponse`)

Wrapped in standard API envelope `{ "success": true, "data": { ... }, "message": "..." }`:

| Field | Type | Nullable | Meaning |
|---|---|---|---|
| `id` | `String` | No | MongoDB document ObjectId string. |
| `eventId` | `String` | No | Short unique human-readable identifier (prefix `se_`, e.g. `se_abc123...`). |
| `rideId` | `String` | No | Associated ride ID. |
| `tripId` | `String` | Yes | Associated trip ID if transit trip exists. |
| `triggeredByUserId` | `String` | No | User ID who triggered the SOS. |
| `triggeredByRole` | `String` | No | Role of triggerer (`USER` or `DRIVER_CONDUCTOR`). |
| `driverId` | `String` | No | Driver profile ID assigned to the ride. |
| `passengerUserId` | `String` | No | Passenger user ID for the ride. |
| `emergencyType` | `String` | No | `"SOS"` or `"SAFETY_CONCERN"`. |
| `status` | `String` | No | Lifecycle status (`ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`, `CANCELLED`). |
| `locationSnapshot` | `Object` | No | Location details captured at trigger time (see below). |
| `locationSnapshot.coordinates` | `[Double, Double]` | Yes | `[longitude, latitude]` (GeoJSON). Null if GPS unavailable at trigger. |
| `locationSnapshot.accuracyMeters` | `Double` | Yes | GPS accuracy in meters. |
| `locationSnapshot.headingDegrees` | `Double` | Yes | Heading in degrees. |
| `locationSnapshot.speedMps` | `Double` | Yes | Speed in meters per second. |
| `locationSnapshot.isStale` | `Boolean` | No | True if location exceeded staleness threshold at capture time. |
| `locationSnapshot.capturedAt` | `String` | Yes | ISO-8601 timestamp when GPS fix was recorded. |
| `locationSnapshot.provider` | `String` | No | Location source (`"driver_profile"` or `"ride_pickup"`). |
| `triggeredAt` | `String` | No | ISO-8601 timestamp when SOS was triggered. |
| `acknowledgedAt` | `String` | Yes | ISO-8601 timestamp when operations acknowledged the SOS. |
| `resolvedAt` | `String` | Yes | ISO-8601 timestamp when SOS was resolved. |
| `cancelledAt` | `String` | Yes | ISO-8601 timestamp when SOS was cancelled. |
| `cancellationReason` | `String` | Yes | Reason for cancellation if supplied. |
| `createdAt` | `String` | No | Creation timestamp. |
| `updatedAt` | `String` | No | Last update timestamp. |

### 3.2 Emergency Contact Response (`EmergencyContactResponse`)

| Field | Type | Nullable | Meaning |
|---|---|---|---|
| `id` | `String` | No | Contact ID. |
| `name` | `String` | No | Contact's name. |
| `phoneNumber` | `String` | No | Contact phone number. |
| `relationship` | `String` | No | `PARENT`, `SPOUSE`, `SIBLING`, `FRIEND`, `GUARDIAN`, `OTHER`. |
| `isVerified` | `Boolean` | No | Verification status. Always `false` in Phase 15 (SMS gateway not yet integrated). |
| `isActive` | `Boolean` | No | Whether the contact is active. |
| `createdAt` | `String` | No | Creation timestamp. |
| `updatedAt` | `String` | No | Update timestamp. |

---

## 4. Status Lifecycle & Transition Matrix

```
       [Trigger SOS]
             │
             ▼
          ACTIVE ──────────────┐
          │    │               │
  (ops)   │    │ (cancel)      │
          ▼    ▼               │
   ACKNOWLEDGED  CANCELLED ◄───┘
          │      (Terminal)
  (ops)   │
          ▼
       RESOLVED
      (Terminal)
```

| Status | Meaning | Client Behavior |
|---|---|---|
| `ACTIVE` | SOS has been triggered and is actively tracked by backend. | Display active safety alert banner, show SOS details (eventId, triggeredAt), offer "Cancel Alert" action. |
| `ACKNOWLEDGED` | Backend operations team has acknowledged the event. | Update UI to indicate acknowledgment; cannot be cancelled directly by user once acknowledged. |
| `RESOLVED` | Emergency has been resolved by operator/authorities. | Terminal state: show resolution status, provide navigation back to ride. |
| `CANCELLED` | Event cancelled by the participant who initiated it. | Terminal state: dismiss active alert, return to normal ride tracking. |
| Unknown status | Unknown future state returned by evolving backend. | Safe fallback to generic state; does not crash application. |

---

## 5. Errors

| HTTP | Error Code | Meaning | Client UI Handling |
|---|---|---|---|
| `400` | `INVALID_ID` | Malformed `rideId` or `eventId`. | Display calm error: "Invalid ride reference." |
| `400` | `SOS_NOT_ELIGIBLE` | Ride is not in an eligible status (`CREATED`, `DRIVER_ARRIVING`, `PICKED_UP`, `IN_PROGRESS`). | "Safety alerts can only be sent for active rides." |
| `400` | `SOS_ALREADY_CANCELLED` | SOS is already cancelled. | "Safety alert is already cancelled." Reconcile status. |
| `400` | `SOS_ALREADY_RESOLVED` | SOS is already resolved. | "Safety alert has already been resolved." Reconcile status. |
| `401` | `UNAUTHORIZED` | Session missing or expired. | Redirect to login / session recovery. |
| `403` | `SAFETY_EVENT_NOT_AUTHORIZED` | Caller is not a participant of this ride or did not initiate the SOS. | "You are not authorized to perform safety actions for this ride." |
| `404` | `RIDE_NOT_FOUND` | Ride does not exist. | "Ride not found." |
| `404` | `SAFETY_EVENT_NOT_FOUND` | No active safety event found. | "No active safety alert found." |
| `409` | `SOS_ALREADY_ACTIVE` | An active SOS already exists for this ride and user. | "A safety alert is already active for this ride." Retrieve and display active event. |
| `409` | `EMERGENCY_CONTACT_LIMIT_REACHED` | Max 5 contacts exceeded. | "Maximum of 5 emergency contacts reached. Please remove one before adding another." |
| `429` | `SAFETY_RATE_LIMITED` | Rate limit exceeded. | "Too many requests. Please wait a moment before trying again." |
| `500` | `INTERNAL_SERVER_ERROR` | Server error during SOS creation. | "We couldn't send the safety alert. Please check your connection and try again." |

---

## 6. Realtime Safety Events

The backend `SafetyEventPublisher` broadcasts targeted WebSocket events over the existing `RealtimeGateway` to active passenger and driver sessions:

| Realtime Event | Channel Target | Payload Summary | Meaning |
|---|---|---|---|
| `SOS_CREATED` | `sendToRideUser` & `sendToRideDriver` | `{ eventId, rideId, emergencyType, status, triggeredByUserId, triggeredByRole, locationSnapshot, triggeredAt }` | Dispatched immediately after SOS is persisted in MongoDB. |
| `SOS_CANCELLED` | `sendToRideUser` & `sendToRideDriver` | `{ eventId, rideId, emergencyType, status: "CANCELLED", cancelledAt, cancellationReason }` | Dispatched immediately after SOS cancellation is committed. |

---

## 7. Notifications

| Notification Channel | Verified Support in Backend | Notes |
|---|---|---|
| Push Notification (FCM) | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** | In Phase 15 backend, `safetyEventPublisher` only calls WebSocket `gateway.sendToRideUser` and `sendToRideDriver`. No FCM dispatch is executed in `safety.service.ts`. |
| SMS Notification | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** | `isVerified` on `EmergencyContact` is hardcoded to `false` with explicit backend comment: "no SMS verification gateway integrated yet". No SMS gateway is invoked. |
| Email Notification | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** | No email dispatch in backend. |
| Police / Ambulance Integration | **NOT SUPPORTED / NOT INTEGRATED** | The backend contains NO third-party emergency service integrations. The UI **MUST NEVER** claim emergency services have been dispatched. |

---

## 8. Location Handling

- **Source of Location**:
  - The client **does not transmit coordinates** in the `POST /rides/:rideId/safety/sos` payload (strict schema rejects them).
  - The backend server derives the location snapshot automatically:
    - For `CREATED` and `DRIVER_ARRIVING` statuses: derived from `ride.pickup.coordinates` with `provider: "ride_pickup"`.
    - For `PICKED_UP` and `IN_PROGRESS` statuses: derived from `driverProfile.currentLocation` with `provider: "driver_profile"`.
- **GPS Unavailable / Stale**:
  - If driver GPS is unavailable or null, the backend records `coordinates: null` and `isStale: true`.
  - The backend **never blocks SOS creation** due to missing or stale location.
- **Client Location Role**:
  - Client GPS may be used in the UI for local visualization/confirmation, but the authoritative snapshot sent to the emergency record is managed server-side.

---

## 9. Idempotency & Deduplication

- The backend accepts an `Idempotency-Key` header on `POST /api/v1/rides/:rideId/safety/sos`.
- If a client retries with the same `Idempotency-Key` for the same user, the backend returns the existing `EmergencyEventResponse` without creating duplicate events.
- In addition, MongoDB enforces a partial unique compound index `{ rideId: 1, triggeredByUserId: 1 }` where `status: "ACTIVE"`. Duplicate concurrent requests are caught via code `11000` and returned as `409 Conflict` (`SOS_ALREADY_ACTIVE`).
