# Phase A09 — Passenger Trip Discovery API Contract & Forensic Specification

## 1. Source of Truth Identification

- **Primary Source Code:**
  - `ishara-backend/src/modules/matching/discovery.routes.ts`
  - `ishara-backend/src/modules/matching/discovery.controller.ts`
  - `ishara-backend/src/modules/matching/discovery.schema.ts`
  - `ishara-backend/src/modules/matching/matching.service.ts`
  - `ishara-backend/src/modules/trips/trip.routes.ts`
  - `ishara-backend/src/modules/trips/trip.controller.ts`
  - `ishara-backend/src/modules/trips/trip.service.ts`

---

## 2. API Endpoint 1: Discover Trips

### Specification
- **Method:** `POST`
- **Path:** `/api/v1/discovery/trips`
- **Authentication:** Required (`Authorization: Bearer <access_token>`)
- **Authorization:** `USER` (Passenger)
- **Content-Type:** `application/json`

### Request Schema (Zod `.strict()`)
```json
{
  "origin": {
    "latitude": 25.4484,
    "longitude": 78.5685,
    "name": "Jhansi Railway Station",
    "formattedAddress": "Station Road, Jhansi, UP"
  },
  "destination": {
    "latitude": 25.4358,
    "longitude": 78.5522,
    "name": "Bundelkhand University",
    "formattedAddress": "Kanpur Road, Jhansi, UP"
  },
  "options": {
    "maxPickupDistanceMeters": 1000.0,
    "maxDestinationDeviationMeters": 2000.0,
    "maxResults": 20,
    "cursor": "optional_cursor_string"
  }
}
```

### Validation Rules
1. `origin.latitude`: Number between `-90.0` and `90.0` (inclusive). Required.
2. `origin.longitude`: Number between `-180.0` and `180.0` (inclusive). Required.
3. `origin.name`: String, optional.
4. `origin.formattedAddress`: String, optional.
5. `destination.latitude`: Number between `-90.0` and `90.0` (inclusive). Required.
6. `destination.longitude`: Number between `-180.0` and `180.0` (inclusive). Required.
7. `destination.name`: String, optional.
8. `destination.formattedAddress`: String, optional.
9. `options`: Optional object.
   - `maxPickupDistanceMeters`: Positive number, max `50000`.
   - `maxDestinationDeviationMeters`: Positive number, max `50000`.
   - `maxResults`: Positive integer, max `50`. Default: `20`.
   - `cursor`: String, optional.
10. **Strict Validation:** Unrecognized properties cause immediate HTTP 400 Bad Request.

### Prohibited Outbound Fields
The following fields MUST NOT be sent in the request:
- `seatCapacity`, `seatsRequested`, `availableSeats`, `passengerCount`
- `fare`, `price`, `paymentMethod`
- `pickup`, `dropoff` (use `origin` and `destination`)
- `bookingId`, `rideRequestId`

### Response Schema (`200 OK`)
```json
{
  "success": true,
  "data": {
    "discoverySessionId": "disc_session_67890",
    "items": [
      {
        "tripId": "trip_001",
        "driver": {
          "id": "driver_123",
          "name": "Vikram Singh",
          "profileImageUrl": "https://example.com/avatar.jpg"
        },
        "vehicle": {
          "id": "veh_456",
          "make": "Tata",
          "model": "Magic EV",
          "registrationNumber": "UP93 BT 1234",
          "vehicleType": "VAN"
        },
        "origin": {
          "name": "Jhansi Junction",
          "location": {
            "type": "Point",
            "coordinates": [78.5685, 25.4484]
          }
        },
        "destination": {
          "name": "Medical College Gate",
          "location": {
            "type": "Point",
            "coordinates": [78.5522, 25.4358]
          }
        },
        "routeSummary": {
          "scheduledStartTime": "2026-10-01T08:00:00Z",
          "status": "SCHEDULED"
        },
        "match": {
          "pickupWalkingDistanceMeters": 150.0,
          "destinationDeviationMeters": 320.0,
          "compatibilityScore": 0.94
        },
        "estimatedFare": {
          "amount": 25.0,
          "currency": "INR"
        }
      }
    ],
    "pagination": {
      "limit": 20,
      "hasMore": false,
      "nextCursor": null
    }
  }
}
```

### Response Error Codes
- `400 Bad Request`: Validation failure on coordinates or presence of unpermitted fields (`ZodError`).
- `401 Unauthorized`: Token missing, expired, or invalid signature.
- `403 Forbidden`: Account role lacks `USER` permission.
- `500 Internal Server Error`: Corridor matching failure or database error.

---

## 3. API Endpoint 2: Get Public Trip Details

### Specification
- **Method:** `GET`
- **Path:** `/api/v1/trips/:tripId`
- **Authentication:** Optional or Authenticated (`Authorization: Bearer <access_token>`)
- **Authorization:** Any authenticated user or public passenger query. When requested by a passenger, backend sanitizes driver personal details.

### Response Schema (`200 OK`)
```json
{
  "success": true,
  "data": {
    "id": "trip_001",
    "status": "SCHEDULED",
    "route": {
      "origin": {
        "address": "Jhansi Junction",
        "coordinates": [78.5685, 25.4484]
      },
      "destination": {
        "address": "Medical College Gate",
        "coordinates": [78.5522, 25.4358]
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [78.5685, 25.4484],
          [78.5600, 25.4400],
          [78.5522, 25.4358]
        ]
      }
    },
    "schedule": {
      "scheduledStartTime": "2026-10-01T08:00:00Z"
    },
    "driver": {
      "id": "driver_123",
      "name": "Vikram Singh",
      "profileImageUrl": "https://example.com/avatar.jpg"
    },
    "vehicle": {
      "id": "veh_456",
      "registrationNumber": "UP93 BT 1234",
      "vehicleType": "VAN",
      "make": "Tata",
      "model": "Magic EV"
    }
  }
}
```

### Privacy & Data Redaction Guarantees
- Driver phone number (`driver.phone`) is omitted from passenger payloads.
- Driver driving license (`driver.licenseNumber`) is omitted.
- Operational internal telemetry and agency internal financials are excluded.

---

## 4. Discrepancy Matrix

| Feature | Legacy / Stale Docs | Actual Backend Source of Truth | Frontend Phase A09 Compliance |
|---|---|---|---|
| **Discovery Route** | `GET /api/v1/trips/search` | `POST /api/v1/discovery/trips` | Implemented `POST` request with JSON body |
| **Origin/Destination Schema** | Separate query params (`lat`, `lng`, `radius`) | Embedded `{latitude, longitude, name?, formattedAddress?}` objects | Exact nested object structure sent |
| **Coordinate Ordering** | Inconsistent `[lat, lng]` vs `[lng, lat]` | GeoJSON RFC 7946 `[longitude, latitude]` for geometry points | RFC 7946 compliant GeoJSON parsing |
| **Seat Availability** | Assumed `seatsAvailable` or `seatCapacity` | Backend exposes NO seat availability in discovery response | Prohibited from outbound DTO; removed from UI |
| **Fares in Discovery** | Assumed client-side fare calculation | Backend optionally supplies `estimatedFare` object | Authoritative backend fare rendered only when present; no local calculations |
| **Pagination** | Page/Limit (`page=1&limit=10`) | Cursor-based (`options.cursor`, `pagination.nextCursor`) | Implemented cursor pagination |
