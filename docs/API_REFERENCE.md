# Ishaara Backend — Complete API Reference

**Base URL (Development)**: `http://localhost:5000`  
**Base URL (Production)**: `https://reposnse-ishaara.onrender.com`  
**API Version Prefix**: `/api/v1`  
**Authentication Type**: Bearer Token (`Authorization: Bearer <session-token>`)  
**Admin Route Header**: `x-admin-key: <ADMIN_SECRET_KEY>`  
**Spatial Coordinate Standard**: `[longitude, latitude]` (GeoJSON format: `[lng, lat]`)

---

## CRUD Actions Legend
- **[CREATE]**: Creates a new resource (typically POST)
- **[READ]**: Retrieves one or more resources (typically GET)
- **[UPDATE]**: Modifies an existing resource (typically PUT or PATCH)
- **[DELETE]**: Removes or deactivates a resource (typically DELETE)

---

## 1. System & Health Probes

### 1.1 [READ] Root Health Probe
- **Method**: `GET`
- **URL**: `/`
- **Auth**: Public (No auth required)
- **Description**: Root-level probe for container orchestrator readiness.
- **Response**: `{ "status": "ok", "name": "isahara-backend", "health": "/api/v1/health" }`

### 1.2 [READ] Service Health Check
- **Method**: `GET`
- **URL**: `/health`
- **Auth**: Public (No auth required)
- **Description**: Unthrottled health probe returning database connectivity status.
- **Response**: `200 OK` (healthy) / `503 Service Unavailable` (degraded)

### 1.3 [READ] Kubernetes / Cloud Healthz Probe
- **Method**: `GET`
- **URL**: `/healthz`
- **Auth**: Public (No auth required)
- **Description**: Alias probe for Kubernetes/Render liveness checks.

### 1.4 [READ] Versioned Health Check
- **Method**: `GET`
- **URL**: `/api/v1/health`
- **Auth**: Public (No auth required)
- **Description**: Returns detailed health, uptime, and database connection state.

---

## 2. Authentication & Session (Better Auth)

### 2.1 [CREATE] Google Social Sign-In (Web & Mobile Android)
- **Method**: `POST`
- **URL**: `/api/auth/sign-in/social`
- **Auth**: Public
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "provider": "google",
    "idToken": "<google_id_token>"
  }
  ```
- **Description**: Verifies Google ID tokens (Android & Web Client IDs) and provisions session.
- **Response**: Returns session token, cookie, and user profile data.

### 2.2 [CREATE] Sign Out
- **Method**: `POST`
- **URL**: `/api/auth/sign-out`
- **Auth**: Bearer Token / Session Cookie
- **Description**: Invalidates active session.

### 2.3 [READ] Get Current Auth Session
- **Method**: `GET`
- **URL**: `/api/auth/get-session`
- **Auth**: Bearer Token / Session Cookie
- **Description**: Retrieves the caller's active Better Auth session.

### 2.4 [READ] Better Auth Liveness Check
- **Method**: `GET`
- **URL**: `/api/auth/ok`
- **Auth**: Public
- **Description**: Verifies that Better Auth router is responsive.

---

## 3. Users & Passenger Profile

### 3.1 [READ] Get Current User Profile
- **Method**: `GET`
- **URL**: `/api/v1/users/me`
- **Auth**: Bearer Token (Authenticated User)
- **Description**: Returns application user document matching the authenticated identity.

### 3.2 [CREATE] Complete Onboarding (Role Assignment)
- **Method**: `POST`
- **URL**: `/api/v1/users/me/onboarding`
- **Auth**: Bearer Token (Authenticated User)
- **Request Body**:
  ```json
  {
    "role": "USER" | "DRIVER_CONDUCTOR"
  }
  ```
- **Description**: Sets initial user role. Can only be invoked once per account.

### 3.3 [UPDATE] Update User Profile
- **Method**: `PATCH`
- **URL**: `/api/v1/users/me`
- **Auth**: Bearer Token (Authenticated User)
- **Request Body**:
  ```json
  {
    "name": "string (optional)",
    "phoneNumber": "string (optional)",
    "image": "string url (optional)"
  }
  ```
- **Description**: Updates editable user profile metadata.

### 3.4 [READ] List User Ride Requests
- **Method**: `GET`
- **URL**: `/api/v1/users/me/ride-requests`
- **Auth**: Bearer Token (Role: `USER`)
- **Query Params**:
  - `status`: `PENDING` | `ACCEPTED` | `REJECTED` | `CANCELLED` | `EXPIRED` (optional)
  - `page`: integer (default: `1`)
  - `limit`: integer (default: `20`, max: `50`)
- **Description**: Lists ride requests created by the authenticated passenger.

### 3.5 [READ] List User Rides History
- **Method**: `GET`
- **URL**: `/api/v1/users/me/rides`
- **Auth**: Bearer Token (Role: `USER`)
- **Query Params**:
  - `status`: `REQUESTED` | `CONFIRMED` | `DRIVER_ARRIVED` | `IN_PROGRESS` | `COMPLETED` | `CANCELLED`
  - `page`: integer (default: `1`)
  - `limit`: integer (default: `20`)
- **Description**: Lists completed and active rides taken by the passenger.

### 3.6 [READ] List Emergency Contacts
- **Method**: `GET`
- **URL**: `/api/v1/users/me/emergency-contacts`
- **Auth**: Bearer Token (Role: `USER`)
- **Description**: Lists active trusted contacts for SOS alerts (max 5).

### 3.7 [CREATE] Add Emergency Contact
- **Method**: `POST`
- **URL**: `/api/v1/users/me/emergency-contacts`
- **Auth**: Bearer Token (Role: `USER`)
- **Request Body**:
  ```json
  {
    "name": "string",
    "phoneNumber": "string (e.g. +919876543210)",
    "relationship": "FAMILY | FRIEND | OTHER"
  }
  ```
- **Description**: Registers a new emergency contact.

### 3.8 [UPDATE] Update Emergency Contact
- **Method**: `PATCH`
- **URL**: `/api/v1/users/me/emergency-contacts/:contactId`
- **Auth**: Bearer Token (Role: `USER`)
- **Request Body**:
  ```json
  {
    "name": "string (optional)",
    "phoneNumber": "string (optional)",
    "relationship": "FAMILY | FRIEND | OTHER (optional)"
  }
  ```
- **Description**: Updates contact details. Caller must own the contact.

### 3.9 [DELETE] Delete Emergency Contact
- **Method**: `DELETE`
- **URL**: `/api/v1/users/me/emergency-contacts/:contactId`
- **Auth**: Bearer Token (Role: `USER`)
- **Description**: Soft-deletes the emergency contact.

---

## 4. Driver & Conductor Operations

### 4.1 [READ] Get Driver Profile
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/profile`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Retrieves driver profile details with masked license information.

### 4.2 [CREATE] Create Driver Profile
- **Method**: `POST`
- **URL**: `/api/v1/drivers/me/profile`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "licenseNumber": "string",
    "yearsOfExperience": 0,
    "emergencyContact": {
      "name": "string",
      "phoneNumber": "string"
    }
  }
  ```
- **Description**: Initializes driver verification profile.

### 4.3 [UPDATE] Update Driver Profile
- **Method**: `PATCH`
- **URL**: `/api/v1/drivers/me/profile`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "yearsOfExperience": 0,
    "emergencyContact": { "name": "...", "phoneNumber": "..." }
  }
  ```
- **Description**: Updates safe driver profile fields.

### 4.4 [UPDATE] Set Driver Online
- **Method**: `POST`
- **URL**: `/api/v1/drivers/me/status/online`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Transitions verified driver to ONLINE status.

### 4.5 [UPDATE] Set Driver Offline
- **Method**: `POST`
- **URL**: `/api/v1/drivers/me/status/offline`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Transitions driver to OFFLINE status.

### 4.6 [READ] Get Driver Operational Context
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/operations/context`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Returns full operational snapshot: active trip, in-flight rides, summary stats.

### 4.7 [READ] Get Driver Earnings
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/earnings`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Query Params**:
  - `startDate`: ISO 8601 string (optional)
  - `endDate`: ISO 8601 string (optional)
- **Description**: Returns earnings summaries, completed rides count, and ledger payouts.

### 4.8 [READ] Get Driver Trips
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/trips`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Query Params**:
  - `status`: `CREATED` | `ACTIVE` | `COMPLETED` | `CANCELLED` (optional)
  - `page`: integer (default: `1`)
  - `limit`: integer (default: `20`)
- **Description**: Lists trips operated by the authenticated driver.

### 4.9 [READ] Get Driver Ride Requests
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/ride-requests`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Query Params**:
  - `status`: `PENDING` | `ACCEPTED` | `REJECTED` (optional)
  - `page`: integer (default: `1`)
  - `limit`: integer (default: `20`)
- **Description**: Lists passenger requests waiting for driver confirmation.

### 4.10 [READ] Get Driver Rides
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/rides`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Query Params**:
  - `status`: `CONFIRMED` | `DRIVER_ARRIVED` | `IN_PROGRESS` | `COMPLETED` (optional)
  - `page`: integer (default: `1`)
  - `limit`: integer (default: `20`)
- **Description**: Lists ride instances assigned to the driver.

### 4.11 [READ] Get Driver Rating Summary
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/rating-summary`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Returns driver's average rating score and total rating count.

### 4.12 [READ] Get Driver Latest GPS Location
- **Method**: `GET`
- **URL**: `/api/v1/drivers/me/location`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Retrieves driver's own recorded GPS position and freshness status.

### 4.13 [UPDATE] Update Driver GPS Location
- **Method**: `PATCH`
- **URL**: `/api/v1/drivers/me/location`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "coordinates": [longitude, latitude],
    "heading": 0.0,
    "speed": 0.0,
    "accuracy": 5.0,
    "recordedAt": "2026-09-21T10:00:00.000Z"
  }
  ```
- **Description**: Ingests high-frequency GPS ping with monotonic timestamp enforcement. Note: Coordinates are `[longitude, latitude]`.

---

## 5. Vehicles Management

### 5.1 [CREATE] Register Vehicle
- **Method**: `POST`
- **URL**: `/api/v1/vehicles`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "registrationNumber": "string (e.g. MH12AB1234)",
    "model": "string",
    "type": "BUS | MINI_BUS | VAN | AUTO",
    "capacity": 40
  }
  ```
- **Description**: Registers a new vehicle under the driver profile.

### 5.2 [READ] List Driver Vehicles
- **Method**: `GET`
- **URL**: `/api/v1/vehicles`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Lists all vehicles registered by the authenticated driver.

### 5.3 [READ] Get Vehicle Details
- **Method**: `GET`
- **URL**: `/api/v1/vehicles/:vehicleId`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Retrieves vehicle specifications and active status.

### 5.4 [UPDATE] Update Vehicle Metadata
- **Method**: `PATCH`
- **URL**: `/api/v1/vehicles/:vehicleId`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "model": "string (optional)",
    "capacity": 0
  }
  ```
- **Description**: Modifies vehicle specifications.

### 5.5 [UPDATE] Activate Vehicle
- **Method**: `POST`
- **URL**: `/api/v1/vehicles/:vehicleId/activate`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Designates vehicle as active for operational trips.

### 5.6 [UPDATE] Deactivate Vehicle
- **Method**: `POST`
- **URL**: `/api/v1/vehicles/:vehicleId/deactivate`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Deactivates vehicle without hard-deleting record.

---

## 6. Operators & Fleet Settlement (Web Dashboard Admin)

### 6.1 [CREATE] Create Bus Operator
- **Method**: `POST`
- **URL**: `/api/v1/operators`
- **Auth**: Header `x-admin-key`
- **Request Body**:
  ```json
  {
    "companyName": "string",
    "contactEmail": "string",
    "contactPhone": "string",
    "payoutAccount": {
      "accountNumber": "string",
      "ifsc": "string",
      "beneficiaryName": "string"
    }
  }
  ```

### 6.2 [READ] Get Operator Details
- **Method**: `GET`
- **URL**: `/api/v1/operators/:id`
- **Auth**: Bearer Token (Authenticated User)

### 6.3 [UPDATE] Verify Operator Payout Account
- **Method**: `PATCH`
- **URL**: `/api/v1/operators/:id/verify-payout`
- **Auth**: Header `x-admin-key`
- **Request Body**: `{ "verified": true }`

### 6.4 [CREATE] Assign Vehicle to Operator
- **Method**: `POST`
- **URL**: `/api/v1/operators/:id/vehicles/:vehicleId`
- **Auth**: Header `x-admin-key`

---

## 7. Locations & Geocoding

### 7.1 [READ] Search Locations & Landmarks
- **Method**: `GET`
- **URL**: `/api/v1/locations/search`
- **Auth**: Bearer Token (Authenticated User)
- **Query Params**:
  - `q`: string (search query, min 2 chars)
  - `latitude`: number (optional bias)
  - `longitude`: number (optional bias)
  - `radius`: number (meters, optional bias)
  - `limit`: integer (default: `5`)
- **Description**: Discovers places via cached Google Maps / SerpApi geo-orchestrator.

---

## 8. Trips & Scheduled Routes

### 8.1 [READ] List Active Trips (Public / Passenger Discovery)
- **Method**: `GET`
- **URL**: `/api/v1/trips/active`
- **Auth**: Bearer Token (Authenticated User)
- **Query Params**: `originLat`, `originLng`, `destLat`, `destLng`, `limit` (default: `20`)
- **Description**: Returns active trips across the system.

### 8.2 [CREATE] Create Trip
- **Method**: `POST`
- **URL**: `/api/v1/trips`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "vehicleId": "string (ObjectId)",
    "origin": { "address": "...", "coordinates": [lng, lat] },
    "destination": { "address": "...", "coordinates": [lng, lat] },
    "waypoints": [{ "address": "...", "coordinates": [lng, lat] }],
    "totalSeats": 40,
    "baseFarePaise": 2000
  }
  ```
- **Description**: Creates a new trip in `CREATED` status.

### 8.3 [READ] Get Trip Details
- **Method**: `GET`
- **URL**: `/api/v1/trips/:tripId`
- **Auth**: Bearer Token (Authenticated User)
- **Description**: Retrieves trip itinerary, route polyline, and remaining seat counts.

### 8.4 [UPDATE] Start Trip
- **Method**: `POST`
- **URL**: `/api/v1/trips/:tripId/start`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Transitions trip from `CREATED` to `ACTIVE` and starts live tracking.

### 8.5 [UPDATE] Complete Trip
- **Method**: `POST`
- **URL**: `/api/v1/trips/:tripId/complete`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Transitions trip to `COMPLETED` and completes all onboard rides.

### 8.6 [UPDATE] Cancel Trip
- **Method**: `POST`
- **URL**: `/api/v1/trips/:tripId/cancel`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**: `{ "cancellationReason": "string" }`

---

## 9. Voice Trip Drafts & Speech-to-Text

### 9.1 [CREATE] Create Voice Trip Draft
- **Method**: `POST`
- **URL**: `/api/v1/voice/trip-drafts`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Headers**: `Content-Type: multipart/form-data` OR `application/json`
- **Request Body**: Form field `audio` (binary file `.wav`, `.mp3`, `.m4a`, max 5MB) OR JSON `{ "transcript": "..." }`
- **Description**: Transcribes driver speech via Whisper/OpenAI/Google and extracts route intent.

### 9.2 [READ] Get Voice Trip Draft
- **Method**: `GET`
- **URL**: `/api/v1/voice/trip-drafts/:draftId`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

### 9.3 [CREATE] Confirm Voice Draft (Creates Trip)
- **Method**: `POST`
- **URL**: `/api/v1/voice/trip-drafts/:draftId/confirm`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**:
  ```json
  {
    "vehicleId": "string (ObjectId)",
    "overrides": {
      "totalSeats": 40,
      "baseFarePaise": 2000
    }
  }
  ```

### 9.4 [UPDATE] Cancel Voice Draft
- **Method**: `POST`
- **URL**: `/api/v1/voice/trip-drafts/:draftId/cancel`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

### 9.5 [CREATE] Pre-Allocate Realtime Voice Session
- **Method**: `POST`
- **URL**: `/api/v1/voice/sessions`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**: `{ "inputMode": "AUDIO_STREAM" | "TEXT" }`

### 9.6 [READ] Get Voice Streaming Session State
- **Method**: `GET`
- **URL**: `/api/v1/voice/sessions/:sessionId`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

---

## 10. Trip Discovery & Route Matching

### 10.1 [CREATE] Discover Matching Trips
- **Method**: `POST`
- **URL**: `/api/v1/discovery/trips`
- **Auth**: Bearer Token (Authenticated User)
- **Request Body**:
  ```json
  {
    "pickup": { "coordinates": [lng, lat], "address": "..." },
    "destination": { "coordinates": [lng, lat], "address": "..." },
    "passengerCount": 1
  }
  ```
- **Description**: Evaluates running trips using 6-dimension spatial heuristic (pickup radius, detour distance, heading alignment, freshness). Returns sorted trips with match scores and ETAs.

---

## 11. Ride Requests

### 11.1 [CREATE] Create Ride Request
- **Method**: `POST`
- **URL**: `/api/v1/ride-requests`
- **Auth**: Bearer Token (Role: `USER`)
- **Request Body**:
  ```json
  {
    "tripId": "string (ObjectId)",
    "pickupLocation": { "coordinates": [lng, lat], "address": "..." },
    "dropoffLocation": { "coordinates": [lng, lat], "address": "..." },
    "seatsRequested": 1
  }
  ```
- **Description**: Requests to board an active trip. Automatically expires in 120s if ignored.

### 11.2 [READ] List User's Pending Ride Requests
- **Method**: `GET`
- **URL**: `/api/v1/ride-requests/me`
- **Auth**: Bearer Token (Role: `USER`)

### 11.3 [READ] Get Ride Request Details
- **Method**: `GET`
- **URL**: `/api/v1/ride-requests/:requestId`
- **Auth**: Bearer Token (Passenger or Trip Driver)

### 11.4 [UPDATE] Cancel Ride Request
- **Method**: `POST`
- **URL**: `/api/v1/ride-requests/:requestId/cancel`
- **Auth**: Bearer Token (Role: `USER`)
- **Request Body**: `{ "reason": "string (optional)" }`

### 11.5 [UPDATE] Driver Accepts Ride Request
- **Method**: `POST`
- **URL**: `/api/v1/ride-requests/:requestId/accept`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Description**: Driver accepts request. Automatically provisions a Ride instance.

### 11.6 [UPDATE] Driver Rejects Ride Request
- **Method**: `POST`
- **URL**: `/api/v1/ride-requests/:requestId/reject`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)
- **Request Body**: `{ "reason": "SEATS_FULL | OFF_ROUTE | OUT_OF_SERVICE | OTHER (optional)" }`

---

## 12. Rides, Lifecycle & Live Tracking

### 12.1 [READ] List Passenger Rides
- **Method**: `GET`
- **URL**: `/api/v1/rides/me`
- **Auth**: Bearer Token (Role: `USER`)

### 12.2 [READ] Get Ride by ID
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId`
- **Auth**: Bearer Token (Authorized Passenger or Driver)

### 12.3 [READ] Get Assigned Driver's Live Location
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/driver-location`
- **Auth**: Bearer Token (Authorized Passenger)

### 12.4 [READ] Get Authoritative Ride Progress & ETA
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/tracking`
- **Auth**: Bearer Token (Authorized Passenger or Driver)
- **Description**: Computes route progress percentage, remaining distance (meters), dynamic ETA (seconds).

### 12.5 [UPDATE] Driver Arrived at Pickup
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/arrive`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

### 12.6 [UPDATE] Driver Boarded / Picked Up Passenger
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/pickup`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

### 12.7 [UPDATE] Start Ride
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/start`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

### 12.8 [UPDATE] Complete Ride
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/complete`
- **Auth**: Bearer Token (Role: `DRIVER_CONDUCTOR`)

### 12.9 [UPDATE] Cancel Ride
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/cancel`
- **Auth**: Bearer Token (Passenger or Driver)
- **Request Body**: `{ "reason": "string" }`

---

## 13. Ratings & Reviews

### 13.1 [READ] Check Rating Eligibility
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/rating-eligibility`
- **Auth**: Bearer Token (Authorized Passenger or Driver)

### 13.2 [CREATE] Submit Ride Rating
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/ratings`
- **Auth**: Bearer Token (Role: `USER`)
- **Request Body**:
  ```json
  {
    "score": 5,
    "feedback": "string (optional)",
    "tags": ["PUNCTUAL", "CLEAN_VEHICLE", "SAFE_DRIVING"]
  }
  ```

### 13.3 [READ] Get Ratings for Ride
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/ratings`
- **Auth**: Bearer Token (Ride Participants)

---

## 14. Safety & SOS Emergency System

### 14.1 [CREATE] Trigger SOS Emergency Alert
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/safety/sos`
- **Auth**: Bearer Token (Passenger or Driver)
- **Headers**: `Idempotency-Key: <uuid>` (recommended)
- **Request Body**:
  ```json
  {
    "location": {
      "coordinates": [longitude, latitude],
      "accuracy": 5.0
    },
    "reason": "MEDICAL | ACCIDENT | HARASSMENT | THREAT | OTHER"
  }
  ```

### 14.2 [READ] Get Active SOS Event for Ride
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/safety/active`
- **Auth**: Bearer Token (Ride Participants)

### 14.3 [READ] List Safety Events for Ride
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/safety/events`
- **Auth**: Bearer Token (Ride Participants)

### 14.4 [UPDATE] Cancel SOS by Ride ID
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/safety/cancel`
- **Auth**: Bearer Token (Participant who triggered SOS)
- **Request Body**: `{ "reason": "string (optional)" }`

### 14.5 [READ] Get Emergency Event by ID
- **Method**: `GET`
- **URL**: `/api/v1/safety/events/:eventId`
- **Auth**: Bearer Token (Authorized Participant)

### 14.6 [UPDATE] Cancel Emergency Event by ID
- **Method**: `POST`
- **URL**: `/api/v1/safety/events/:eventId/cancel`
- **Auth**: Bearer Token (Participant who triggered SOS)
- **Request Body**: `{ "reason": "string" }`

---

## 15. Notifications & Device Tokens

### 15.1 [READ] List In-App Notifications
- **Method**: `GET`
- **URL**: `/api/v1/notifications`
- **Auth**: Bearer Token (Authenticated User)
- **Query Params**: `unreadOnly: boolean`, `limit: integer`

### 15.2 [READ] Get Unread Notifications Count
- **Method**: `GET`
- **URL**: `/api/v1/notifications/unread-count`
- **Auth**: Bearer Token (Authenticated User)

### 15.3 [UPDATE] Mark Notification as Read
- **Method**: `POST`
- **URL**: `/api/v1/notifications/:notificationId/read`
- **Auth**: Bearer Token (Authenticated User)

### 15.4 [UPDATE] Mark All Notifications as Read
- **Method**: `POST`
- **URL**: `/api/v1/notifications/read-all`
- **Auth**: Bearer Token (Authenticated User)

### 15.5 [CREATE] Register FCM Push Device Token
- **Method**: `POST`
- **URL**: `/api/v1/devices/push-token`
- **Auth**: Bearer Token (Authenticated User)
- **Request Body**:
  ```json
  {
    "token": "string (Firebase FCM registration token)",
    "platform": "ANDROID"
  }
  ```

### 15.6 [DELETE] Remove FCM Push Device Token
- **Method**: `DELETE`
- **URL**: `/api/v1/devices/push-token`
- **Auth**: Bearer Token (Authenticated User)
- **Request Body**: `{ "token": "string" }`

---

## 16. Payments, Webhooks & Settlements

### 16.1 [CREATE] Create Razorpay Payment Order
- **Method**: `POST`
- **URL**: `/api/v1/rides/:rideId/payment`
- **Auth**: Bearer Token (Role: `USER`)
- **Request Body**: `{ "paymentMethod": "RAZORPAY_UPI | RAZORPAY_CARD | CASH" }`

### 16.2 [READ] Get Payment Status by Ride ID
- **Method**: `GET`
- **URL**: `/api/v1/rides/:rideId/payment`
- **Auth**: Bearer Token (Ride Participants)

### 16.3 [CREATE] Verify Razorpay Payment Signature
- **Method**: `POST`
- **URL**: `/api/v1/payments/:paymentId/verify`
- **Auth**: Bearer Token (Authenticated User)
- **Request Body**:
  ```json
  {
    "razorpayOrderId": "string",
    "razorpayPaymentId": "string",
    "razorpaySignature": "string"
  }
  ```

### 16.4 [CREATE] Process Payment Refund
- **Method**: `POST`
- **URL**: `/api/v1/payments/:paymentId/refund`
- **Auth**: Bearer Token (Authenticated User)

### 16.5 [CREATE] Razorpay Webhook Ingestion
- **Method**: `POST`
- **URL**: `/api/v1/payments/webhooks/razorpay`
- **Auth**: Public (`x-razorpay-signature` header HMAC)

### 16.6 [UPDATE] Process Operator Settlement
- **Method**: `POST`
- **URL**: `/api/v1/payments/settlements/:settlementId/process`
- **Auth**: Header `x-admin-key`

### 16.7 [UPDATE] Reconcile Settlement
- **Method**: `POST`
- **URL**: `/api/v1/payments/settlements/:settlementId/reconcile`
- **Auth**: Header `x-admin-key`

---

## 17. Survey Research Platform

### 17.1 [CREATE] Submit Campus Transportation Survey
- **Method**: `POST`
- **URL**: `/api/v1/survey`
- **Auth**: Public (Rate limited)
- **Request Body**:
  ```json
  {
    "collegeName": "string",
    "travelFrequency": "DAILY | WEEKLY | OCCASIONAL",
    "currentMode": "BUS | AUTO | CAB | TWO_WHEELER | WALKING",
    "averageCost": 50,
    "painPoints": ["SAFETY", "DELAY", "OVERCROWDING", "EXPENSIVE"],
    "willingnessToUse": 9,
    "comments": "string (optional)"
  }
  ```

---

## 18. Administrative Tools & Analytics (Web Dashboard)

- **18.1** [READ] `/api/v1/admin/analytics/overview` (Header `x-admin-key`)
- **18.2** [READ] `/api/v1/admin/surveys/export` (Header `x-admin-key`)
- **18.3** [READ] `/api/v1/admin/surveys` (Header `x-admin-key`)
- **18.4** [READ] `/api/v1/admin/surveys/:id` (Header `x-admin-key`)
- **18.5** [DELETE] `/api/v1/admin/surveys/:id` (Header `x-admin-key`)

---

## 19. Realtime WebSocket Streaming Protocols

All WebSocket connections are initiated over HTTP upgrade requests:
`ws://<host>:<port><endpoint>?token=<bearer_token>` (or `wss://...`)

### 19.1 Driver Realtime Voice Streaming
- **URL**: `wss://<host>/api/v1/voice/realtime`
- **Auth**: Upgrade query param `?token=<driver_bearer_token>`
- **Description**: Bi-directional audio chunk streaming and partial transcription for hands-free driver conductor interactions.

### 19.2 Passenger Realtime Trip Discovery Stream
- **URL**: `wss://<host>/api/v1/discovery/realtime`
- **Auth**: Upgrade query param `?token=<user_bearer_token>`
- **Description**: Realtime push notifications when nearby matching trips become active.

### 19.3 Driver / Passenger Ride Request Notifications
- **URL**: `wss://<host>/api/v1/ride-requests/realtime`
- **Auth**: Upgrade query param `?token=<bearer_token>`
- **Description**: Instant notifications for ride request creation, driver acceptance, and rejection events.

### 19.4 Live GPS Ride Tracking & Telemetry
- **URL**: `wss://<host>/api/v1/rides/realtime`
- **Auth**: Upgrade query param `?token=<bearer_token>`
- **Description**: High-frequency live GPS coordinates (`[lng, lat]`), vehicle heading, distance-to-pickup, and route progress updates pushed directly to passenger map views.
