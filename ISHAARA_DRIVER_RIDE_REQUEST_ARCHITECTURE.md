# ISHAARA DRIVER RIDE REQUEST & PASSENGER BOARDING ARCHITECTURE

**System Component:** Android Mobile Application  
**Subsystem:** Driver / Conductor Operational Workflow (Phase 10)  
**Author:** Ishaara Senior Architecture Team  
**Contract Alignment:** Verified against `ishara-backend` source (`rideRequest.routes.ts`, `ride.routes.ts`, `realtime.gateway.ts`).

---

## 1. Architectural Overview

Phase 10 implements the Driver/Conductor operational workflow for incoming passenger ride requests and the step-by-step passenger boarding lifecycle. The architecture follows strict Clean Architecture principles, Unidirectional Data Flow (UDF), and decoupling of concerns across network, domain, and UI layers.

```
                         [Backend MongoDB & REST API]
                                     ▲
                                     │ (OkHttp / HttpURLConnection)
                                     ▼
                      DriverRideRequestRemoteDataSource
                                     │
                                     ▼
                       DriverRideRequestRepository
                                     │
               ┌─────────────────────┼────────────────────┐
               ▼                     ▼                    ▼
   GetDriverRideRequests   AcceptRideRequest    ManagePassengerBoarding
               │                     │                    │
               └─────────────────────┼────────────────────┘
                                     ▼
                        DriverRideRequestsViewModel
                                     │ (DriverRideRequestsUiState)
                                     ▼
                        DriverRideRequestsScreen
                        (Compose UI / 54dp CTA)
```

---

## 2. Authoritative Backend Contract & Endpoints

### 2.1 Driver Ride Requests
1. **List Driver Ride Requests:**
   - `GET /api/v1/drivers/me/ride-requests`
   - Role: `DRIVER_CONDUCTOR`
   - Query: `status`, `tripId`, `page`, `limit`, `cursor`
   - Returns paginated list of incoming passenger requests.
2. **Accept Ride Request:**
   - `POST /api/v1/ride-requests/:requestId/accept`
   - Atomically transitions request from `PENDING` to `ACCEPTED`.
   - Backend automatically spawns an authoritative `Ride` entity linked to the request in MongoDB transaction.
3. **Reject Ride Request:**
   - `POST /api/v1/ride-requests/:requestId/reject`
   - Transitions request from `PENDING` to `REJECTED`.
   - Optional request body: `{ "reason": string }`.

### 2.2 Passenger Boarding & Operational Ride Lifecycle
1. **List Driver Rides:**
   - `GET /api/v1/drivers/me/rides`
2. **Driver Arrival:**
   - `POST /api/v1/rides/:rideId/arrive`
   - State: `CREATED` → `DRIVER_ARRIVING`
3. **Passenger Boarding (Pickup Confirmation):**
   - `POST /api/v1/rides/:rideId/pickup`
   - State: `DRIVER_ARRIVING` → `PICKED_UP`
4. **Start Transit:**
   - `POST /api/v1/rides/:rideId/start`
   - State: `PICKED_UP` → `IN_PROGRESS`
5. **Complete Ride:**
   - `POST /api/v1/rides/:rideId/complete`
   - State: `IN_PROGRESS` → `COMPLETED`
6. **Cancel Active Ride:**
   - `POST /api/v1/rides/:rideId/cancel`
   - State: `CREATED | DRIVER_ARRIVING` → `CANCELLED`
   - Required body: `{ "reason": string }`

---

## 3. Realtime WebSocket Architecture

The driver client connects to the verified WebSocket gateway:
`wss://<host>/api/v1/ride-requests/realtime?token=<driver_bearer_token>`

- **Channel:** Channel 19.3 (Driver/Passenger Ride Request Updates).
- **Driver Room:** Authenticated driver automatically placed in `driver:<driverId>`.
- **Verified Events Handled:**
  - `RIDE_REQUEST_CREATED`: Triggered when passenger submits request on driver trip. Reconciles list automatically.
  - `RIDE_REQUEST_CANCELLED`: Passenger cancelled prior to response. Removes request card instantly.
  - `RIDE_REQUEST_EXPIRED`: Request TTL elapsed. Cleans up pending list.
  - `RIDE_CREATED`: Spawns newly provisioned operational ride card in active boarding tab.

---

## 4. UI/UX Design System Compliance

- **No Cyberpunk or Neon:** Strictly adheres to minimalist monochrome + transit amber palette.
- **Ergonomics & Large Touch Targets:** Primary action buttons are 54dp high (`IshaaraButtonSize.Large`), allowing one-tap operation while vehicle is stationary.
- **Strict Data Integrity:** Never fabricates unverified data (e.g. fare, seat count, ETA, phone numbers). Displays only verified pickup, destination, and passenger identification.
- **Safety Confirmations:** Rejections and ride cancellations require explicit confirmation dialogs with reason tracking.

---

## 5. Security & Error Handling

- **Role Protection:** Client navigation requires `UserRole.DRIVER_CONDUCTOR`. Students are blocked and cannot navigate into driver workspaces.
- **Token Redaction:** All network logs sanitize Authorization headers to prevent token leakage.
- **No Optimistic False Mutations:** If a network failure occurs, the UI maintains the authoritative server state and prompts the driver to retry rather than assuming success.
