# ISHAARA Ride Request Architecture (Phase 07)

## 1. Scope & System Objective

Phase 07 implements the **Student / Passenger Ride Request Flow** for the ISHAARA Android mobility platform. This phase directly connects a passenger's selected transit trip (from Phase 06 Discovery) to an authoritative backend reservation submitted via `POST /api/v1/ride-requests`.

### Key Tenets:
- **No Mock or Fabricated Data**: Production UI relies strictly on verified backend data.
- **Strict Backend Schema Compliance**: The client sends only fields validated by `createRideRequestSchema.strict()`.
- **Factual Status Representation**: Requests land in `PENDING` status. The UI never presumes driver acceptance or displays "Confirmed" before authoritative backend confirmation.
- **Idempotency & Duplicate Protection**: Client generates and reuses a persistent UUID `Idempotency-Key` header and enforces UI-level submission locking.

---

## 2. Authoritative Backend Contract

- **Endpoint**: `POST /api/v1/ride-requests`
- **Authentication**: `Authorization: Bearer <token>`
- **Role Requirement**: Middleware strictly requires role `USER` (`requireUser`). Drivers receive HTTP 403 `FORBIDDEN`.
- **Rate Limit**: Enforced via `rideRequestRateLimiter`.
- **Idempotency**: Client attaches `Idempotency-Key` HTTP header. Replay attempts return the existing request record with HTTP 201 without duplicate database writes.

### Request Body Schema
```json
{
  "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
  "pickup": {
    "formattedAddress": "Kenyatta University Gate A, Nairobi",
    "latitude": -1.1818,
    "longitude": 36.9275,
    "name": "Gate A"
  },
  "destination": {
    "formattedAddress": "Safari Park Hotel, Kasarani, Nairobi",
    "latitude": -1.2185,
    "longitude": 36.8856,
    "name": "Safari Park"
  }
}
```

### Response Body Schema (HTTP 201 Created)
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Ride request submitted successfully.",
  "data": {
    "id": "670e1c2b3f4a5b6c7d8e9f01",
    "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
    "driverId": "651a00112233445566778899",
    "userId": "6519ffeeddccbbaa99887766",
    "pickup": {
      "formattedAddress": "Kenyatta University Gate A, Nairobi",
      "coordinates": {
        "type": "Point",
        "coordinates": [36.9275, -1.1818]
      }
    },
    "destination": {
      "formattedAddress": "Safari Park Hotel, Kasarani, Nairobi",
      "coordinates": {
        "type": "Point",
        "coordinates": [36.8856, -1.2185]
      }
    },
    "status": "PENDING",
    "requestedAt": "2026-09-23T13:00:00.000Z",
    "respondedAt": null,
    "expiresAt": "2026-09-23T13:02:00.000Z",
    "rejectionReason": null,
    "cancellationReason": null,
    "createdAt": "2026-09-23T13:00:00.000Z",
    "updatedAt": "2026-09-23T13:00:00.000Z"
  }
}
```

---

## 3. Request Flow & Progression

```
[Student Home]
      │
      ▼
[Destination Search & Selection]
      │
      ▼
[Trip Discovery (POST /api/v1/discovery/trips)]
      │
      ▼ (Select Trip Option)
[Trip Details Sheet -> "Continue to ride request"]
      │
      ▼ (Pass DiscoveredTrip & Physical Addresses)
[RideRequestReviewScreen]
      │
      ▼ (User taps "Request ride" [54dp CTA])
[CreateRideRequestUseCase]
      ├── 1. Validates 24-hex ObjectId tripId
      ├── 2. Validates address length (2..300 chars)
      └── 3. Validates minimum 50m physical separation
      │
      ▼
[RideRepository.submitRideRequest]
      ├── Injects Authorization: Bearer <sessionToken>
      └── Injects Idempotency-Key: <uuid>
      │
      ▼
[POST /api/v1/ride-requests]
      │
      ├── (HTTP 201) -> RideRequestStage.Submitted(result) -> Displays PENDING confirmation
      └── (HTTP 4xx/5xx) -> RideRequestStage.Error(error) -> Displays recovery action
```

---

## 4. Domain Layer

### Models (`com.ishara.app.domain.model.RideRequestModels.kt`)
- `RideRequestLocationWaypoint`: Encapsulates physical address, coordinates, and optional Google / SerpApi place identifiers.
- `RideRequestInput`: Strict domain payload containing `tripId`, `pickup`, and `destination`.
- `RideRequestStatus`: Domain enum matching backend lifecycle: `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`.
- `RideRequestResult`: Clean domain representation of the submitted request.

### Use Case (`com.ishara.app.domain.usecase.CreateRideRequestUseCase.kt`)
Enforces pre-network business logic:
- Validates 24-character hexadecimal ObjectId format regex `/^[0-9a-fA-F]{24}$/`.
- Validates minimum 50-meter Haversine separation distance between pickup and destination.

---

## 5. Data Layer

- **`RideRequestDtos.kt`**: Contains `CreateRideRequestDto`, `LocationWaypointDto`, and `RideRequestResponseDto`.
- **`RideRequestMapper.kt`**: Bridges DTOs and domain models, ensuring coordinates array indexing (`[longitude, latitude]`) is strictly isolated to data mappers.
- **`RideRequestRemoteDataSource.kt`**: Pure Kotlin HTTP execution and JSON serialization/deserialization. Injects `Authorization` and `Idempotency-Key` headers.
- **`RideRepositoryImpl.kt`**: Integrates session tokens, remote data source, and centralized error mapping.

---

## 6. ViewModel State Machine

`RideRequestViewModel` exposes an immutable `StateFlow<RideRequestUiState>`:
```
           ┌──────────────┐
           │ ReviewReady  │
           └──────┬───────┘
                  │ (User taps "Request ride")
                  ▼
           ┌──────────────┐
           │  Submitting  │ (Buttons locked, isSubmitting = true)
           └──┬────────┬──┘
              │        │
   (Success)  │        │  (Failure)
              ▼        ▼
┌──────────────────┐ ┌────────────────┐
│    Submitted     │ │     Error      │
│ (status:PENDING) │ │ (retry uses    │
└──────────────────┘ │  same idempotency)
                     └──────┬─────────┘
                            │ (User taps "Retry")
                            └───────► (Re-enters Submitting)
```

---

## 7. Duplicate Submission & Idempotency Protection

1. **Client-Side Lock**:
   - `isSubmitting` flag locks UI interactions immediately upon tap.
   - Secondary rapid clicks while a request is in flight are dropped as no-ops.
2. **Stable Idempotency Key**:
   - `val idempotencyKey = UUID.randomUUID().toString()` is instantiated once per `RideRequestViewModel` instance.
   - Retries reuse the same key, ensuring the backend idempotent replay returns the same record without creating duplicates.
3. **Backend Compound Conflict Handling**:
   - If a duplicate request is submitted for the same user and trip, backend responds with HTTP 409 `DUPLICATE_RIDE_REQUEST`, cleanly mapped to `IshaaraError.Conflict`.

---

## 8. Error Mapping & Recovery

| Backend Status / Code | Client Domain Error | User Guidance | Recovery Action |
|---|---|---|---|
| `400 TRIP_NOT_ELIGIBLE` | `IshaaraError.Validation` | "This trip is no longer active. Please choose another available trip." | [Back to available trips] |
| `400 SAME_ORIGIN_DESTINATION` | `IshaaraError.Validation` | "Pickup and destination cannot be the same physical location." | [Edit destination] |
| `401 UNAUTHORIZED` | `IshaaraError.Authentication` | "Your session has expired. Please sign in again." | [Sign in] |
| `403 FORBIDDEN` | `IshaaraError.Forbidden` | "Ride requests can only be submitted by passenger accounts." | [Back to home] |
| `409 DUPLICATE_RIDE_REQUEST` | `IshaaraError.Conflict` | "You already have an active pending ride request for this trip." | [View pending ride] |
| Network timeout / offline | `IshaaraError.Network` | "You're offline. Check your connection and try again." | [Retry request] |

---

## 9. Navigation & Role Guard

- **Role Isolation**: Only authenticated users with `UserRole.USER` can access `StudentContainerScreen` and `RideRequestReviewScreen`. `DRIVER_CONDUCTOR` users are confined to the Driver Graph.
- **Back Navigation**:
  - During review: Back returns smoothly to `DiscoveryScreen`.
  - While submitting: Back is locked to prevent race-condition resubmission.
  - After submission (`Submitted`): Tapping "Back to home" navigates to `IshaaraDestination.StudentHome.route`.

---

## 10. Verification & Test Suite

- **Unit Tests**:
  - `RideRequestStrictContractTest`: Validates request JSON strictly excludes `fare`, `seatCapacity`, `price`, `status`, etc., and enforces 50m separation.
  - `RideRequestRepositoryTest`: Validates HTTP 201 success, Bearer header, Idempotency-Key header, HTTP 409 conflict, and HTTP 400 ineligible mapping.
  - `RideRequestViewModelTest`: Validates state transitions, duplicate submit prevention, and idempotency key stability across retries.
- **Regression Suite**: All 56 existing unit tests from Phases 01–06 pass with zero regressions.

---

## 11. Known Backend Limitations & Deferred Scope

The following features are **explicitly deferred to future phases**:
- ❌ **Payment & Checkout**: Payment gateways (Razorpay, cash settlement) belong to later payment phases.
- ❌ **Live Realtime Tracking**: WebSockets and driver live GPS tracking belong to dedicated realtime tracking phases.
- ❌ **Driver Acceptance UI**: Driver-side request response workflows belong to the driver application.
- ❌ **SOS & Safety**: Emergency protocols and ratings belong to future safety modules.
