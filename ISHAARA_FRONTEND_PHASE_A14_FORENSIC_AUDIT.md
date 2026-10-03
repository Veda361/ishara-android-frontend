# ISHAARA Frontend Phase A14 — Forensic Audit & Backend Contract

**Date:** 2026-10-01  
**Target:** Safety, SOS, Emergency Contacts & Ratings Implementation  
**Backend Source:** `ishara-backend/src/modules/safety/` and `ishara-backend/src/modules/ratings/`

---

## PART 1 — BACKEND SAFETY FORENSIC AUDIT

### 1. Which endpoints actually exist?
The following safety & emergency endpoints are active in the backend:
1. `POST /api/v1/rides/:rideId/safety/sos` (Ride-scoped SOS trigger)
2. `GET /api/v1/rides/:rideId/safety/active` (Ride-scoped active SOS lookup)
3. `GET /api/v1/rides/:rideId/safety/events` (Ride-scoped paginated safety events)
4. `POST /api/v1/rides/:rideId/safety/cancel` (Ride-scoped cancel active SOS)
5. `GET /api/v1/safety/events/:eventId` (Global safety event lookup by eventId)
6. `POST /api/v1/safety/events/:eventId/cancel` (Global cancel SOS by eventId)
7. `GET /api/v1/users/me/emergency-contacts` (List active contacts)
8. `POST /api/v1/users/me/emergency-contacts` (Create emergency contact)
9. `PATCH /api/v1/users/me/emergency-contacts/:contactId` (Update emergency contact)
10. `DELETE /api/v1/users/me/emergency-contacts/:contactId` (Soft-delete emergency contact)

### 2. Which HTTP methods are supported?
- `POST`: Triggering SOS, cancelling SOS, creating emergency contacts.
- `GET`: Fetching active SOS, listing safety events, fetching event by ID, listing emergency contacts.
- `PATCH`: Updating emergency contacts.
- `DELETE`: Soft-deleting emergency contacts.

### 3. Which roles can call each endpoint?
- **SOS Endpoints** (`POST /safety/sos`, `GET /safety/active`, `GET /safety/events`, `POST /safety/cancel`): Both `USER` (passenger) and `DRIVER_CONDUCTOR` (driver) can call these, provided they are verified participants in the ride.
- **Cancel SOS**: ONLY the participant (`USER` or `DRIVER_CONDUCTOR`) who originally triggered the SOS may cancel it. Other participants receive `403 Forbidden` (`SAFETY_EVENT_NOT_AUTHORIZED`).
- **Emergency Contacts** (`/users/me/emergency-contacts`): Authenticated `USER` (passengers).

### 4. What authorization rules exist?
- Bearer session token is required (`requireAuth` middleware).
- `assertRideParticipant` verifies `ride.userId === callerUserId` OR `driverProfile.userId === callerUserId && ride.driverId === driverProfile._id`.
- The caller's identity (`callerUserId`, `callerRole`) is strictly derived from the session token, NEVER accepted from the request body or path params.
- Emergency contacts enforce strict ownership (`doc.userId.toString() === callerUserId`).

### 5. Does an SOS require an active ride?
- **YES.** `rideId` must be a valid MongoDB ObjectId pointing to an existing ride document in MongoDB.

### 6. Does it require a specific ride state?
- **YES.** The ride status must be one of `ELIGIBLE_SOS_RIDE_STATUSES`:
  - `CREATED`
  - `DRIVER_ARRIVING`
  - `PICKED_UP`
  - `IN_PROGRESS`
- If ride is `COMPLETED` or `CANCELLED`, backend rejects with `400 Bad Request` (`SOS_NOT_ELIGIBLE`).

### 7. What fields are required?
- For `POST /rides/:rideId/safety/sos`: **None**. Request body `{}` is valid.
- For `POST /rides/:rideId/safety/cancel`: **None**.
- For `POST /users/me/emergency-contacts`: `name` (1-100 chars), `phoneNumber` (7-15 digits regex `^\+?[0-9]{7,15}$`), `relationship` (`PARENT`, `SPOUSE`, `SIBLING`, `FRIEND`, `GUARDIAN`, `OTHER`).

### 8. What fields are optional?
- For `POST /rides/:rideId/safety/sos`: `emergencyType` (`"SOS"` | `"SAFETY_CONCERN"`, defaults to `"SOS"`).
- For `POST /rides/:rideId/safety/cancel`: `reason` (string, max 500 chars).
- Headers: `Idempotency-Key` (UUID string).
- Request body uses `.strict()`: extra fields (such as client location, user IDs) will trigger a `400 Bad Request`.

### 9. What states exist?
- `ACTIVE`: Alert active and unacknowledged.
- `ACKNOWLEDGED`: Operations team acknowledged alert.
- `RESOLVED`: Alert resolved by operations (terminal).
- `CANCELLED`: Alert cancelled by the triggering participant (terminal).

### 10. Who can transition those states?
- `ACTIVE` -> `CANCELLED`: The triggering participant.
- `ACTIVE` -> `ACKNOWLEDGED`: Backend Operations / Dispatch.
- `ACKNOWLEDGED` -> `RESOLVED`: Backend Operations / Dispatch.
- `RESOLVED` and `CANCELLED`: Terminal states; no further transitions allowed.

### 11. Is an SOS idempotent?
- **YES.** If an `Idempotency-Key` header is provided, the backend records it. If the same user retries with the same key, the existing `EmergencyEvent` is returned with status 201/200 without creating a duplicate.

### 12. Can multiple SOS records exist for one ride?
- A partial unique index exists on `{ rideId, triggeredByUserId }` where `status: "ACTIVE"`.
- A user can only have **ONE** active SOS per ride at any time. A second concurrent trigger returns `409 Conflict` (`SOS_ALREADY_ACTIVE`).
- After cancellation or resolution, a new SOS may be triggered if the ride is still in an eligible status.

### 13. Can an SOS be cancelled?
- **YES.** By calling `POST /api/v1/rides/:rideId/safety/cancel` or `POST /api/v1/safety/events/:eventId/cancel`.

### 14. Can an SOS be resolved?
- **YES**, but only by backend operations personnel. Mobile clients do not have resolve authority.

### 15. Can the passenger see its status?
- **YES**, via `GET /api/v1/rides/:rideId/safety/active` and WebSocket events.

### 16. Can the driver see its status?
- **YES**, the driver is an authorized ride participant and can query the active SOS or receive WebSocket events.

### 17. Is emergency location captured?
- **YES.** A `locationSnapshot` object is captured and stored with the event.

### 18. Is location automatically attached by backend?
- **YES.** 
  - For `CREATED` and `DRIVER_ARRIVING`, backend uses `ride.pickup.coordinates` (provider: `"ride_pickup"`).
  - For `PICKED_UP` and `IN_PROGRESS`, backend reads the driver's latest GPS from `DriverProfile.currentLocation` (provider: `"driver_profile"`).

### 19. Does the frontend need to send location?
- **NO! ABSOLUTELY NOT.** The backend strictly rejects any client-supplied location fields in `createSosSchema.strict()`.

### 20. Is emergency-contact notification handled by backend?
- The backend manages contact CRUD. However, SMS gateway integration is currently marked as unimplemented (`isVerified: false` always). No outbound SMS is sent yet.

### 21. Are notifications synchronous or asynchronous?
- Dispatches and audit records are executed asynchronously/isolated; failure of notifications never rolls back the persisted SOS event.

### 22. Are safety events delivered through WebSocket/realtime?
- **YES.** The backend dispatches `SOS_CREATED` and `SOS_CANCELLED` targeting `passengerUserId` and `driverId`.

### 23. What happens after logout?
- The Bearer token is cleared. Any subsequent safety requests receive `401 Unauthorized`.

### 24. What happens if the network fails during SOS submission?
- The client should supply a client-generated UUID in `Idempotency-Key`. On network timeout or retry, re-sending with the same key returns the existing alert safely.

### 25. What happens if the request times out but backend actually created the SOS?
- Re-sending with the same `Idempotency-Key` or calling `GET /api/v1/rides/:rideId/safety/active` reconciles the active SOS and prevents duplicate alerts.

---

## PART 2 — SAFETY STATE MACHINE

```text
[ NONE ]
   │
   │ POST /rides/:rideId/safety/sos (Participant: USER or DRIVER_CONDUCTOR)
   ▼
[ ACTIVE ] ─────────── POST /safety/cancel (Triggering participant only) ───────────► [ CANCELLED ] (Terminal)
   │
   │ Admin / Operations Acknowledgment
   ▼
[ ACKNOWLEDGED ]
   │
   │ Admin / Operations Resolution
   ▼
[ RESOLVED ] (Terminal)
```

---

## PART 3 — BACKEND RATINGS FORENSIC AUDIT

### 1. Active Endpoints
1. `GET /api/v1/rides/:rideId/rating-eligibility`
2. `POST /api/v1/rides/:rideId/ratings`
3. `GET /api/v1/rides/:rideId/ratings`
4. `GET /api/v1/drivers/me/rating-summary`
5. History enrichment: `GET /api/v1/rides?includeRatingStatus=true`

### 2. Authorization & Roles
- **Eligibility Check:** Authenticated ride participant (`USER` or `DRIVER_CONDUCTOR`).
- **Rating Submission:** Authenticated `USER` (passenger) rating the driver. In Phase 14, `DRIVER_CONDUCTOR` -> `USER` rating is explicitly not supported (`RATING_NOT_ELIGIBLE`).
- **Rating Retrieval:** Ride participants.
- **Driver Summary:** Authenticated `DRIVER_CONDUCTOR`.

### 3. Invariants & Rules
- Ride must be in `COMPLETED` status. Non-completed rides return `403 Forbidden` (`RIDE_NOT_COMPLETED`).
- Reviewer cannot rate themselves (`reviewerUserId !== revieweeUserId`).
- `score` must be an integer between 1 and 5 (`MIN_RATING_SCORE = 1`, `MAX_RATING_SCORE = 5`).
- `review` is optional, trimmed string up to 500 characters (`MAX_REVIEW_LENGTH = 500`).
- Strict validation: client cannot pass `reviewerUserId`, `revieweeUserId`, or `rideId` in the body.
- Idempotency key supported via `Idempotency-Key` header.
- Unique index on `{ rideId, reviewerUserId, revieweeUserId }`. Repeated submission returns `409 Conflict` (`RATING_ALREADY_SUBMITTED`).
- Privacy: `RatingResponse` omits reviewer/reviewee identities (`{ id, rideId, score, review, createdAt }`).
- Driver rating summary: `{ driverId, averageScore, ratingCount }`. When `ratingCount === 0`, `averageScore` is `null` to avoid 0-star ambiguity.
