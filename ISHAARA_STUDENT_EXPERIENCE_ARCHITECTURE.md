# ISHAARA Student / Passenger Experience Architecture

## 1. Student User Journey

The ISHAARA Student/Passenger journey is engineered around real-world transit utility, predictability, and immediate situational awareness. A passenger accessing the application typically has one primary job-to-be-done: **"I need to get to my campus, home, or transit stop with complete certainty about my route and timing."**

### End-to-End Progression Flow:
1. **Entry & Orientation (Student Home)**:
   - On cold start or authentication completion, the user is landed directly on `StudentHomeScreen`.
   - The screen answers 7 fundamental transit questions within seconds:
     1. *Where am I right now?* (Human-readable current location banner).
     2. *Is my location verified?* (Location permission state & verification badge).
     3. *Where do I usually travel?* (Recent & frequent destinations).
     4. *Where can I quickly go?* (One-tap Campus / Library / Hostel transit targets).
     5. *What is my active travel status?* (Prominent in-flight ride banner if ride is in progress).
     6. *How do I search for a new destination?* (Large, high-contrast Search Bar touch target).
     7. *What can I explore next?* (Discovery entry point foundation for Phase 06).
2. **Search Activation**:
   - Tapping the prominent search bar navigates to `LocationSearchScreen`.
   - The user experiences an auto-focused, accessible search interface with immediate display of recent destination history.
3. **Query & Debounced Discovery**:
   - As the user types (minimum 2 characters), queries are debounced by 350ms to prevent network thrashing.
   - Ongoing queries are safely cancelled via coroutines `flatMapLatest`.
   - Backend returns matching physical addresses, campuses, and transit stations with strict geo-coordinates.
4. **Destination Confirmation**:
   - Tapping a search result selects the destination, registers it in recent search history, and returns to `StudentHomeScreen`.
   - The confirmed destination is highlighted in the "Selected Destination" active card.
5. **Discovery Foundation**:
   - The primary action transitions to *"Find Available Buses"* (54dp high-contrast button).
   - Tapping this triggers a clean, decoupled navigation event to `StudentDiscovery`, ready for Phase 06 route and trip discovery without mock or fake data pollution.

---

## 2. Student Home Architecture

The Student Home experience is built following clean Architecture and unidirectional Data Flow (UDF) with Jetpack Compose:

```
┌────────────────────────────────────────────────────────┐
│                   StudentHomeScreen                    │
│   (Stateless Compose UI / Design System Tokens)        │
└───────────────────────────▲────────────────────────────┘
                            │ (State: StudentHomeUiState)
                            │ (Events: onSearchClick, etc.)
┌───────────────────────────┴────────────────────────────┐
│                 StudentHomeViewModel                   │
│         (StateFlow<StudentHomeUiState>, UDF)           │
└───────────▲──────────────────▲──────────────────▲──────┘
            │                  │                  │
┌───────────┴──────────┐ ┌─────┴──────────┐ ┌─────┴────────────┐
│ResolveCurrentLocation│ │ManageRecentDest│ │GetActiveRide     │
│       UseCase        │ │    UseCase     │ │   UseCase        │
└───────────▲──────────┘ └─────▲──────────┘ └─────▲────────────┘
            │                  │                  │
┌───────────┴──────────┐       │            ┌─────┴────────────┐
│  LocationRepository  │       │            │  RideRepository  │
└──────────────────────┘       │            └──────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │ RecentDestinationsDataSource│
                │ (Thread-Safe In-Memory / DB)│
                └─────────────────────────────┘
```

### Components:
- **`StudentHomeUiState`**: Pure Kotlin data class encapsulating:
  - `currentLocation`: `CurrentLocationDisplay` (resolved address, coordinates, permission status, verification badge).
  - `activeRide`: Optional `Ride` object indicating active passenger transit.
  - `recentDestinations`: List of `StudentDestination` items.
  - `selectedDestination`: Confirmed target destination ready for trip discovery.
  - `isLoading`: Loading state flag.
  - `errorMessage`: Localized error string resource or message.
- **`StudentHomeViewModel`**: Coordinates initial location resolution, polls or observes active in-flight rides, streams recent destinations, and handles destination selection. Accepts an optional `CoroutineScope` for deterministic JVM unit testing without Android Looper dependencies.
- **UI Sections**:
  - Top AppBar with ISHAARA branding and profile action.
  - Current Location card with permission indicator and human-readable place name.
  - In-flight ride status banner (prioritized when an active ride exists).
  - Prominent Search Box touch target (minimum 52dp height, adhering to ISHAARA design tokens).
  - Quick action chips for common university transit points.
  - Recent destinations list with one-tap re-selection.
  - Selected destination staging card with high-contrast primary CTA: *"Find Available Buses"*.
  - Accessible Bottom Navigation Bar (`Home`, `Activity`, `Account`).

---

## 3. Location Architecture

Location handling separates hardware sensors, permission contracts, and domain models to guarantee high privacy and stability:

- **Domain Model (`Location.kt`)**:
  - `SearchResultLocation`: Represents a verified geographic location from the backend search API.
  - `StudentDestination`: Represents a destination selected by the passenger, with timestamp and use count.
  - `CurrentLocationDisplay`: Model containing display text (e.g., "Kenyatta University Main Campus"), latitude, longitude, and `LocationPermissionStatus`.
  - `LocationPermissionStatus`: Strongly typed enum: `GRANTED_FINE`, `GRANTED_COARSE`, `DENIED`, `NOT_REQUESTED`.
- **Use Cases**:
  - `ResolveCurrentLocationUseCase`: Resolves the device's current location into a human-readable display model. In Phase 05, handles permission fallback gracefully without crashing.
- **Coordinate System**:
  - All coordinates comply with **RFC 7946 GeoJSON format**: `[longitude, latitude]`.
  - Represented using the `GeoJsonCoordinate` value class and `toGeoJson()` converters.
  - Latitudes and longitudes are clamped and validated: Latitude $\in [-90.0, 90.0]$, Longitude $\in [-180.0, 180.0]$.

---

## 4. Search Architecture

Search is designed for rapid transit use cases with debouncing, lifecycle-aware coroutines, and request cancellation:

```
[User Input Event]
        │
        ▼
[MutableStateFlow<String>]
        │
        ▼ (debounce 350ms)
        │
        ▼ (distinctUntilChanged)
        │
        ▼ (filter { length >= 2 })
        │
        ▼ (flatMapLatest — cancels in-flight HTTP requests)
[SearchLocationsUseCase.execute(query, limit = 5)]
        │
        ▼
[LocationRepository.searchLocations(query)]
        │
        ▼
[LocationRemoteDataSource -> GET /api/v1/locations/search?q=...]
        │
        ▼
[LocationMapper: Dto -> SearchResultLocation]
        │
        ▼
[LocationSearchUiState.Success(results)]
```

### Key Architectural Characteristics:
1. **Debounce (350ms)**: Avoids excessive network calls while passenger is typing.
2. **Cancellation (`flatMapLatest`)**: Previous in-flight search requests are automatically cancelled when a new character is entered.
3. **Query Length Guard**: Minimum 2 characters required before triggering remote calls.
4. **Result Size Limit**: Capped between 1 and 10 results (default 5) to minimize payload overhead.

---

## 5. Destination Selection & History

- **Selection Flow**:
  1. User selects a `SearchResultLocation` from the search result list.
  2. `LocationSearchViewModel` maps it to a `StudentDestination` and records it via `ManageRecentDestinationsUseCase.recordDestination(...)`.
  3. The destination is passed back to `StudentHomeViewModel.selectDestination(...)`.
  4. `StudentHomeScreen` renders the selected destination prominently and unlocks the *"Find Available Buses"* action.
- **Recent Destinations Management (`RecentDestinationsLocalDataSource`)**:
  - Thread-safe in-memory cache using `Mutex`.
  - Max capacity: 5 items.
  - Deduplication: Selecting an existing destination moves it to the top of the list and increments its use count.
  - Cleared on logout via `clear()`.

---

## 6. State Management

All state adheres strictly to Unidirectional Data Flow (UDF):
- Events flow up from Composable UI to ViewModel (`onSearchQueryChanged`, `onDestinationSelected`, `onClearSearch`).
- Immutable UI state flows down via `StateFlow<UiState>`.
- UI State is represented by sealed interfaces or immutable data classes:
  - `StudentHomeUiState`: Data class with default initial state, loading state, error states.
  - `LocationSearchUiState`: Sealed interface (`Initial`, `Searching`, `Success`, `Empty`, `Error`).
- No side effects are triggered directly inside Composable functions without `LaunchedEffect` or proper lifecycle scoping.

---

## 7. Navigation Architecture

Navigation is decoupled from UI widgets using `NavigationManager` and typed `ApplicationDestination` / `IshaaraDestinations`:

- **Destinations**:
  - `IshaaraDestinations.StudentSearch = "student/search"`
  - `IshaaraDestinations.StudentDiscovery = "student/discovery"`
  - `ApplicationDestination.StudentHome`: Root container for student experience.
- **Back Navigation**:
  - `BackHandler` in `StudentContainerScreen` navigates back from `StudentSearch` to `StudentHome`.
  - Double-back or back from `StudentHome` triggers system exit/minimize.
- **Navigation Commands**:
  - `NavigationManager` utilizes `MutableSharedFlow<NavigationCommand>(replay = 1, extraBufferCapacity = 1)` ensuring no dropped navigation commands on configuration changes.

---

## 8. Backend Endpoints Contract

Phase 05 strictly adheres to the verified backend API:

| Endpoint | Method | Query Parameters | Description |
|---|---|---|---|
| `/api/v1/locations/search` | `GET` | `q` (string, min 2 chars)<br>`latitude` (optional)<br>`longitude` (optional)<br>`radius` (optional)<br>`limit` (optional, 1..10) | Searches verified geographic transit locations |
| `/api/v1/rides/passenger/active` | `GET` | None | Returns active in-flight ride for authenticated passenger (404/null if none) |

### Response DTO Schema (`GET /api/v1/locations/search`):
```json
[
  {
    "latitude": -1.1818,
    "longitude": 36.9275,
    "formattedAddress": "Kenyatta University, Thika Rd, Nairobi",
    "displayName": "Kenyatta University Main Campus",
    "city": "Nairobi",
    "state": "Kiambu",
    "country": "Kenya",
    "googlePlaceId": "ChIJb8..."
  }
]
```

---

## 9. Coordinate Conventions

- **GeoJSON Standard**: Coordinates adhere strictly to **RFC 7946**:
  ```json
  {
    "type": "Point",
    "coordinates": [longitude, latitude]
  }
  ```
- **Ordering**:
  - **Index 0**: Longitude ($X$ axis, East/West).
  - **Index 1**: Latitude ($Y$ axis, North/South).
- **Validation**:
  - `GeoJsonCoordinate.isValid()` enforces $-180 \le \text{longitude} \le 180$ and $-90 \le \text{latitude} \le 90$.
  - Coordinates are NEVER displayed in raw decimal format to users in the UI; they are always transformed into human-readable address names.

---

## 10. Error Handling & Recovery

- **Granular Domain Errors**:
  - Handled via `IshaaraResult<T>` (`Success`, `Failure(IshaaraError)`).
  - Errors mapped into typed user-facing categories: `Network`, `Server`, `Validation`, `Unauthorized`, `NotFound`, `Unknown`.
- **Search UI Error Recovery**:
  - Empty results: Display informative "No locations found" view with suggestions to broaden terms.
  - Network failure: Displays a clear "Connection failed" message with a prominent "Retry" button.
  - Query < 2 characters: Safely returns to `Initial` state showing recent searches, preventing redundant network queries.

---

## 11. Offline & Degraded Network Behavior

- When offline, search attempts gracefully fail and emit `IshaaraError.Network`.
- The user is notified via non-disruptive banner or retryable card.
- Recent destinations remain available offline via local cache (`RecentDestinationsLocalDataSource`), allowing the user to select cached routes even in intermittent connectivity zones.
- The UI never hangs, freezes, or crashes on timeout or connection reset.

---

## 12. Accessibility & Usability

Adheres to ISHAARA Phase 02 Design System standards:
- **Touch Targets**: All interactive elements (chips, list items, search bar, buttons) have a minimum touch target size of **48dp × 48dp**. Primary CTAs are sized at **54dp**.
- **Contrast**: Text and icon contrast ratios exceed WCAG AA guidelines ($> 4.5:1$ for normal text, $> 3:1$ for large text and UI components).
- **Typography**: Clean, accessible system typography scale (`Inter`/system sans-serif). No hacker or cyberpunk aesthetic.
- **Content Descriptions**: Every icon and image has explicit `contentDescription` for TalkBack screen reader support.
- **Keyboard Handling**: Search input supports single-line action `ImeAction.Search` with automatic keyboard dismissal on selection.

---

## 13. Privacy & Location Permissions

- **Explicit Consent**: ISHAARA only requests location permissions when directly required for route resolution.
- **Graceful Degradation**: If the user denies fine location or coarse location, the application falls back gracefully to manual search without blocking app usability.
- **Zero Coordinate Leakage**: Raw GPS coordinates are never logged to external trackers, analytics, or console output in production builds.

---

## 14. Testing Strategy

Complete test coverage across domain, data, and presentation layers:
- **Unit Tests (`app/src/test/`)**:
  - `LocationRepositoryTest`: Tests search parameter validation, network mapping, DTO parsing, error handling, and recent destinations caching.
  - `StudentHomeViewModelTest`: Tests initial state loading, location resolution, active ride integration, and destination selection.
  - `LocationSearchViewModelTest`: Tests debounce timing, query filtering, state transitions (`Initial` -> `Searching` -> `Success` / `Empty` / `Error`), and cancellation.
  - `GeoJsonCoordinateTest`: Tests RFC 7946 coordinates ordering and bounding validations.
- **Execution**: 100% JVM unit test pass rate across 56 tests with Gradle configuration cache enabled.

---

## 15. Phase 06 Integration Boundary

Phase 05 establishes a rock-solid, production-grade foundation for Phase 06 (Trip Discovery, Live Transit, and Ride Booking):

```
┌────────────────────────────────────────────────────────┐
│                   PHASE 05 (COMPLETED)                 │
│  - Student Home Dashboard                              │
│  - Location Resolution & Permission Handling           │
│  - Debounced Location Search API Integration           │
│  - Recent Destinations Caching                         │
│  - Destination Selection Staging                       │
└───────────────────────────┬────────────────────────────┘
                            │ Selected Destination & Location
                            ▼
┌────────────────────────────────────────────────────────┐
│                   PHASE 06 BOUNDARY                    │
│  - Destination Passed to Trip Discovery                │
│  - Route Matching & Available Bus Listings             │
│  - Live Vehicle Location Streaming                     │
│  - Ride Request & Booking Flows                        │
└────────────────────────────────────────────────────────┘
```

- **Clean Hand-off**:
  - Tapping *"Find Available Buses"* routes to `IshaaraDestinations.StudentDiscovery`.
  - Phase 06 receives verified `StudentDestination` coordinates without requiring mock data or refactoring the student home architecture.
