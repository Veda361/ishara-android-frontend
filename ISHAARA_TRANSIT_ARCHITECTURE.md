# ISHAARA Transit Architecture & Offline-First Strategy

## Phase 14 — Route Schedules, Bus Stops & Offline Transit Cache
**Document Version:** 1.0.0  
**Status:** Approved Production Architecture

---

## 1. High-Level System Architecture

The ISHAARA transit architecture guarantees that passengers can browse and inspect transit routes, stops, and operational timings even under degraded connectivity or total offline conditions, without ever fabricating or hallucinating transit data.

```text
             BACKEND (Authoritative Truth)
                         │
                         ▼
                Remote Data Source
                         │
                         ▼
                   Network DTO
                         │
                         ▼
                    Data Mapper
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
      Transit Repository        Local Database
             │                 (SQLite / Cache)
             └───────────┬───────────┘
                         ▼
                   Domain Model
                         │
                         ▼
                 Transit Use Cases
                         │
                         ▼
                 Transit ViewModel
                         │
                         ▼
                     Compose UI
```

---

## 2. Online vs. Offline Data Flow

### Online Flow (Stale-While-Revalidate)
```text
User Opens Transit Routes
           │
           ▼
    Local Database
           │
           ▼ (Instantaneous Display)
       Immediate UI
           │
           ▼ (Background Coroutine)
    Transit Repository
           │
           ▼ (GET /api/v1/trips/active)
        Backend
           │
           ▼ (200 OK + Updated Active Trips)
    Atomic DB Transaction (Replace Cache)
           │
           ▼ (Fresh Data Emitted via Flow)
      Fresh UI Updated ("Updated just now")
```

### Offline Flow
```text
User Opens Transit Routes (No Network)
           │
           ▼
    Transit Repository
           │
           ▼
    Local Database Check
           │
     ┌─────┴─────────────────────────┐
     ▼                               ▼
[Data Found in Cache]        [No Data in Cache]
     │                               │
     ▼                               ▼
Emit Cached Content +        Emit Empty / Offline State
Stale Status                 ("No saved transit information")
     │                               │
     ▼                               ▼
UI: "You're offline.         UI: Empty State Banner
Showing saved information."
```

---

## 3. Route, Stop & Schedule Architecture

### 3.1 Route Domain Model (`TransitRoute`)
- `id`: Unique identifier (string).
- `name`: Human-readable route display name (e.g. "Jhansi Railway Station ↔ Bundelkhand University").
- `originStop`: Origin terminal stop (`TransitStop`).
- `destinationStop`: Destination terminal stop (`TransitStop`).
- `stops`: Ordered list of boarding/alighting stops (`List<TransitStop>`).
- `geometry`: Ordered path coordinates (`List<LocationCoordinates>`).
- `distanceMeters`: Physical distance in meters.
- `durationSeconds`: Estimated traversing time in seconds.
- `vehicleType`: `TransitVehicleType` (`BUS`, `AUTO`, `CAB`, `UNKNOWN`).
- `vehiclePlate`: Sanitized vehicle registration (e.g. "UP93AT1234").
- `driverName`: Sanitized driver name.
- `operatingStatus`: `TransitOperatingStatus` (`ACTIVE`, `SCHEDULED`, `COMPLETED`, `INACTIVE`).
- `startedAt`: `Instant?` when active transit service commenced.
- `cachedAtMillis`: Timestamp when cached locally.

### 3.2 Stop Domain Model (`TransitStop`)
- `id`: Stop identifier.
- `name`: Station/stop display name.
- `formattedAddress`: Physical address.
- `coordinates`: `LocationCoordinates` (`latitude`, `longitude`).
- `sequence`: Explicit ordering index along the route (0 for origin, 1 for destination, etc.).
- `stopType`: `TransitStopType` (`ORIGIN`, `DESTINATION`, `INTERMEDIATE`).

### 3.3 Schedule Domain Model (`TransitSchedule`)
- `routeId`: Associated route identifier.
- `serviceStatus`: Operational status (`ACTIVE`, `SCHEDULED`, `UNAVAILABLE`).
- `departureTime`: Timestamp of departure if available.
- `operatingNote`: Clear, truthful explanation of schedule availability (e.g., "Active service operating now; static timetables not exposed by backend").

---

## 4. Local Database & Cache Policy

### 4.1 SQLite Storage Engine (`TransitDatabaseHelper`)
Uses Android's native `SQLiteOpenHelper` with explicit schema versioning and transactional atomicity.
- **Table `cached_routes`:**
  - `route_id` (TEXT PRIMARY KEY)
  - `name` (TEXT NOT NULL)
  - `origin_name` (TEXT)
  - `origin_address` (TEXT NOT NULL)
  - `origin_lat` (REAL NOT NULL)
  - `origin_lng` (REAL NOT NULL)
  - `dest_name` (TEXT)
  - `dest_address` (TEXT NOT NULL)
  - `dest_lat` (REAL NOT NULL)
  - `dest_lng` (REAL NOT NULL)
  - `geometry_geojson` (TEXT)
  - `distance_meters` (REAL)
  - `duration_seconds` (INTEGER)
  - `vehicle_type` (TEXT NOT NULL)
  - `vehicle_plate` (TEXT NOT NULL)
  - `driver_name` (TEXT NOT NULL)
  - `operating_status` (TEXT NOT NULL)
  - `started_at_epoch` (INTEGER)
  - `cached_at_epoch` (INTEGER NOT NULL)
- **Table `transit_cache_metadata`:**
  - `cache_key` (TEXT PRIMARY KEY)
  - `last_refreshed_at` (INTEGER NOT NULL)
  - `item_count` (INTEGER NOT NULL)
  - `status` (TEXT NOT NULL)

### 4.2 Centralized Cache Policy (`TransitCachePolicy`)
Configured to prevent arbitrary magic numbers:
```kotlin
data class TransitCachePolicy(
    val freshnessDurationMillis: Long = 10 * 60 * 1000L, // 10 minutes: fresh
    val staleDurationMillis: Long = 24 * 60 * 60 * 1000L,  // 24 hours: stale but readable
    val maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L      // 7 days: expired / purge
)
```
- **`FRESH`:** Cached within `freshnessDurationMillis` (10 minutes). UI shows "Updated just now" or "Updated X min ago".
- **`STALE`:** Cached older than 10 minutes but under 24 hours. UI displays non-alarming indicator: "Showing saved transit information".
- **`UNAVAILABLE`:** Cache does not exist or expired.

---

## 5. Concurrency & Synchronization Safety

1. **Atomic Invalidation & Replacement:** Remote responses replace cached route tables in a single `db.beginTransaction()` / `db.setTransactionSuccessful()` block. No half-updated states can leak to the UI.
2. **Generation Counter / Mutex:** Concurrency locks in `TransitRepositoryImpl` protect against duplicate refresh triggers and out-of-order asynchronous completions.
3. **No Leaking Between Roles/Users:** Transit route information is public mobility catalog data, but clearing occurs on logout if user-bound cache data is cleared.

---

## 6. Boundaries with Existing Systems

1. **Schedule vs. Realtime (Phase 11):**
   - Scheduled / active transit metadata is obtained via REST and stored in SQLite.
   - Realtime bus movement and live ETA come strictly from Phase 11 WebSocket telemetry (`/tracking`), never conflated with static schedules.
2. **Map Integration (Phase 11):**
   - The route polyline geometry is rendered using the existing `IshaaraTrackingCanvasMap` coordinate drawing abstractions (`drawRoutePath`), ensuring zero new map SDK bloat.
3. **Location Abstraction:**
   - Canonical `LocationCoordinates(latitude, longitude)` is used throughout the domain. GeoJSON `[longitude, latitude]` conversion is isolated strictly at the network serialization layer.
