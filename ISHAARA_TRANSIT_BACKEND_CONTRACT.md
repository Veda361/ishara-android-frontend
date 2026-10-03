# ISHAARA Transit Backend Contract Specification

## Phase 14 — Route Schedules, Bus Stops & Offline Transit Cache
**Document Version:** 1.0.0  
**Authority:** Production Backend Audit (`/home/dev/Desktop/ishara-backend`)  
**Status:** Verified against Backend Source & Route Definitions

---

## 1. Verified Transit & Trip Endpoints

| Domain | Endpoint | Method | Auth | Role | Purpose |
|---|---|---|---|---|---|
| Transit Trips | `/api/v1/trips/active` | GET | Bearer Token | Any Authenticated User | Discovers and lists currently active transit trips/routes across the platform |
| Transit Trips | `/api/v1/trips/:tripId` | GET | Bearer Token | Any Authenticated User | Retrieves detailed transit trip itinerary, geometry, vehicle, and driver details |
| Trip Discovery | `/api/v1/discovery/trips` | POST | Bearer Token | Any Authenticated User | Evaluates active driver trips against requested journey geometry (origin / destination) |
| Locations & Stops | `/api/v1/locations/search` | GET | Bearer Token | Any Authenticated User | Resolves transit stops, landmarks, and addresses |
| Static Routes Catalog | `/api/v1/routes` | N/A | N/A | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** (Route schedule data is not currently exposed by the backend) |
| Static Stops Catalog | `/api/v1/stops` | N/A | N/A | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** (Dedicated stop catalog is not currently exposed by the backend) |
| Timetable / Headways | `/api/v1/schedules` | N/A | N/A | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** (Static schedule timetable is not currently exposed by the backend) |

---

## 2. Request Contracts

### 2.1 GET `/api/v1/trips/active` (List Active Transit Services)

| Field | Type | Required | Meaning |
|---|---|---|---|
| `page` | integer | No (Default: 1) | Pagination page number |
| `limit` | integer | No (Default: 20) | Maximum number of active trips to return |
| `originLat` | number | No | Latitude for geospatial proximity query |
| `originLng` | number | No | Longitude for geospatial proximity query |
| `radiusMeters` | number | No | Proximity radius in meters for origin filter |
| `vehicleType` | string | No | Vehicle classification filter (e.g., `AUTO`, `BUS`) |

### 2.2 GET `/api/v1/trips/:tripId` (Get Trip Details)

| Field | Type | Required | Meaning |
|---|---|---|---|
| `tripId` | string (24-char hex) | Yes | MongoDB ObjectId identifier of the trip |

### 2.3 POST `/api/v1/discovery/trips` (Journey Discovery)

| Field | Type | Required | Meaning |
|---|---|---|---|
| `origin.latitude` | number | Yes | Passenger origin latitude |
| `origin.longitude` | number | Yes | Passenger origin longitude |
| `origin.address` | string | Yes | Human-readable origin address |
| `destination.latitude` | number | Yes | Passenger destination latitude |
| `destination.longitude` | number | Yes | Passenger destination longitude |
| `destination.address` | string | Yes | Human-readable destination address |
| `radiusMeters` | number | No | Proximity search radius (default: 5000m) |
| `vehicleType` | string | No | Filter by vehicle type |

### 2.4 GET `/api/v1/locations/search` (Stop & Place Resolution)

| Field | Type | Required | Meaning |
|---|---|---|---|
| `q` | string | Yes | Search query string (min length 2) |
| `limit` | integer | No | Max search results (default 5, max 10) |
| `latitude` | number | No | Optional bias latitude |
| `longitude` | number | No | Optional bias longitude |
| `radius` | number | No | Optional bias radius in meters |

---

## 3. Response Contracts

### 3.1 Public Active Trip Response (`PublicTripResponse`)

Enclosing envelope: `{"success": true, "data": [...]}`

| Field | Type | Nullable | Meaning |
|---|---|---|---|
| `id` | string | No | Unique MongoDB ObjectId identifier of the active trip |
| `status` | string | No | Trip status: strictly `ACTIVE` for active trips list |
| `origin` | object | No | Origin terminal boarding stop/location |
| `origin.name` | string | Yes | Human-friendly stop/landmark name |
| `origin.formattedAddress` | string | No | Full postal/street address of the origin stop |
| `origin.coordinates` | object | No | GeoJSON Point `{"type": "Point", "coordinates": [lng, lat]}` |
| `origin.googlePlaceId` | string | Yes | Google Places identifier if resolved |
| `origin.serpApiDataId` | string | Yes | SerpApi identifier if resolved |
| `destination` | object | No | Destination terminal alighting stop/location |
| `destination.name` | string | Yes | Human-friendly stop/landmark name |
| `destination.formattedAddress` | string | No | Full postal/street address of destination stop |
| `destination.coordinates` | object | No | GeoJSON Point `{"type": "Point", "coordinates": [lng, lat]}` |
| `destination.googlePlaceId` | string | Yes | Google Places identifier if resolved |
| `destination.serpApiDataId` | string | Yes | SerpApi identifier if resolved |
| `route` | object | Yes | Route metadata and polyline geometry |
| `route.geometry` | object | Yes | GeoJSON LineString `{"type": "LineString", "coordinates": [[lng, lat], ...]}` |
| `route.distanceMeters` | number | Yes | Total route distance along geometry in meters |
| `route.durationSeconds` | number | Yes | Estimated traversing duration in seconds |
| `route.provider` | string | Yes | Routing engine provider (e.g. `google_routes`) |
| `startedAt` | string (ISO-8601) | Yes | UTC timestamp when driver initiated active service |
| `createdAt` | string (ISO-8601) | No | UTC timestamp when trip record was created |
| `driver` | object | No | Sanitized public driver representation |
| `driver.id` | string | No | Driver profile identifier |
| `driver.name` | string | No | Public driver name (e.g. "Verified Driver") |
| `driver.image` | string | Yes | Public profile photo URL |
| `vehicle` | object | No | Vehicle metadata |
| `vehicle.id` | string | No | Vehicle identifier |
| `vehicle.registrationNumber` | string | No | Vehicle plate number (e.g. "UP65XXXXXX") |
| `vehicle.vehicleType` | string | No | Vehicle type: `AUTO`, `BUS`, `CAB` |
| `vehicle.make` | string | No | Manufacturer make |
| `vehicle.model` | string | No | Vehicle model |

---

## 4. Schedule Specifications

| Field | Type | Meaning |
|---|---|---|
| `serviceDate` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |
| `operatingDays` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |
| `firstDepartureTime` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |
| `lastDepartureTime` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |
| `headwayMinutes` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |
| `timetableEntries` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |
| `startedAt` | string (ISO-8601 UTC) | Actual start time of active transit service (from `trip.startedAt`) |
| `createdAt` | string (ISO-8601 UTC) | Scheduling creation time (from `trip.createdAt`) |
| `operatingStatus` | string | Derived from backend trip status (`ACTIVE`, `COMPLETED`, `CANCELLED`) |

> **Architectural Note:** The backend currently does not provide static timetables or fixed headway schedules. Operational timing is derived strictly from real-time active trips (`startedAt`, `createdAt`, and live tracking).

---

## 5. Bus Stop Specifications

| Field | Type | Meaning |
|---|---|---|
| `stopId` | string | Stop identifier (derived from origin/destination `googlePlaceId`, `serpApiDataId`, or coordinate hash) |
| `name` | string | Station / stop name (e.g. "Jhansi Railway Station") |
| `formattedAddress` | string | Full physical street address |
| `latitude` | number | Boarding/alighting latitude |
| `longitude` | number | Boarding/alighting longitude |
| `sequence` | integer | Position along the route (0 = Origin Stop, 1 = Destination Stop) |
| `stopType` | string | `ORIGIN`, `DESTINATION`, or `WAYPOINT` |
| `accessibility` | N/A | **NOT DETERMINED FROM CURRENT PROJECT/BACKEND CONTRACT** |

> **Architectural Note:** Intermediate scheduled waypoints are not currently modeled in the backend `Trip` Mongoose schema. The origin and destination stops constitute the terminal boarding and alighting points. Intermediate points are captured along the route path geometry line.

---

## 6. Route Geometry Specifications

| Field | Representation | Meaning |
|---|---|---|
| `geometry.type` | String constant `"LineString"` | GeoJSON LineString geometry type per RFC 7946 |
| `geometry.coordinates` | Array of `[longitude, latitude]` pairs | Ordered geographic coordinates tracing the physical transit path |
| `distanceMeters` | Float / Double (meters) | Total physical distance of the transit path |
| `durationSeconds` | Integer / Long (seconds) | Estimated traversing time under normal traffic conditions |
| `provider` | String | Underlying routing engine (e.g., `google_routes`) |

---

## 7. Gaps & Audit Summary

1. **Dedicated `/routes` API:** Not exposed by backend. Active routes are discovered through active transit trips (`GET /api/v1/trips/active`).
2. **Dedicated `/stops` API:** Not exposed by backend. Stops are represented via `TripLocation` (origin and destination) and resolvable via `/api/v1/locations/search`.
3. **Static Schedules / Timetables:** Not exposed by backend.
4. **Conditional Headers (ETag / If-None-Match):** Not implemented on backend trip endpoints. Freshness must be tracked client-side via cache timestamps (`cachedAt`, `serverUpdatedAt`).
