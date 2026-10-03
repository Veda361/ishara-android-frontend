# ISHAARA Phase 14 Manual Test Plan

## Route Schedules, Bus Stops & Offline Transit Cache
**Document Version:** 1.0.0  
**Verification Target:** Physical Device / Android Emulator / Connected Device `ZXCI59SGINRCS4RK`

---

## 1. Online Scenarios

### Step 1: Login & Access Transit
- **Action:** Launch the Ishaara application, sign in as a student (`USER`).
- **Expected:** Student home screen displays the "Browse Routes & Schedules" transit entry card.

### Step 2: Open Transit Routes
- **Action:** Tap "Browse Routes & Schedules".
- **Expected:** `TransitRoutesScreen` opens. Loading spinner appears briefly while querying `/api/v1/trips/active`.

### Step 3: Load Route List
- **Action:** Verify routes loaded from backend.
- **Expected:** Active transit services appear with origin, destination, vehicle plate/type, and "Updated just now". No fake routes (e.g. "Route 101") appear.

### Step 4: Open Route Details
- **Action:** Tap any active route card.
- **Expected:** `RouteDetailsScreen` displays:
  - Route header with origin and destination.
  - Terminal stops with sequence numbers (Stop 1 = Origin, Stop 2 = Destination).
  - Physical distance and estimated duration.
  - Vehicle details and operating status badge (`ACTIVE`).

### Step 5: View Stops Sequence
- **Action:** Inspect the stops timeline card.
- **Expected:** Boarding point and alighting point appear in strict sequence order with address and coordinates.

### Step 6: View Schedule Information
- **Action:** Inspect the operational schedule card.
- **Expected:** Shows commencement time (`startedAt`) and clear notice: "Active service operating now; static timetables not exposed by backend". No fabricated ETA or fake departure intervals.

### Step 7: Pull-to-Refresh / Tap Refresh
- **Action:** Tap the "Refresh" action button on the top app bar.
- **Expected:** Spinner indicates revalidation. Cache is updated in SQLite.

### Step 8: Verify Updated Timestamp
- **Action:** Observe the freshness badge.
- **Expected:** Label reads "Updated just now".

---

## 2. Offline Scenarios

### Step 9: Cache Verification
- **Action:** Ensure route list and at least one route's details have been loaded while online.

### Step 10: Disable Device Network
- **Action:** Turn on Airplane Mode or run `adb shell svc wifi disable && adb shell svc data disable`.

### Step 11: Reopen Route List While Offline
- **Action:** Navigate back to Student Home, then reopen "Browse Routes & Schedules".
- **Expected:** Cached routes display immediately from SQLite local database without hanging or crashing.

### Step 12: Verify Cached Content & Stale/Offline Indicator
- **Action:** Observe the top banner in the route list.
- **Expected:** Banner states: "You're offline. Showing saved transit information."

### Step 13: Verify No Fake Data
- **Action:** Inspect all displayed cards.
- **Expected:** Data matches exactly what was previously fetched from the backend. No synthetic filler data.

### Step 14: Test Route Without Cache
- **Action:** If cache is cleared or on fresh install with network off:
- **Expected:** Screen displays honest empty state: "No saved transit information is available offline."

---

## 3. Network Recovery Scenarios

### Step 15: Re-enable Network
- **Action:** Turn off Airplane Mode / `adb shell svc wifi enable`.

### Step 16: Trigger Refresh
- **Action:** Tap "Refresh".
- **Expected:** Background network query executes. Fresh data from backend replaces local cache atomically in SQLite.

### Step 17: Verify UI Updates
- **Action:** Verify offline banner disappears and freshness indicator changes to "Updated just now".

---

## 4. Lifecycle & Configuration Change

### Step 18: Background & Foreground Transition
- **Action:** Press Home button while viewing Route Details, wait 10 seconds, then reopen Ishaara.
- **Expected:** Route Details screen resumes instantly with zero state loss.

### Step 19: Configuration Change (Screen Rotation)
- **Action:** Rotate device between portrait and landscape.
- **Expected:** State remains intact without triggering duplicate network fetches.

---

## 5. Concurrency & Race Conditions

### Step 20: Rapid Repeated Refresh
- **Action:** Tap the "Refresh" button 5 times in rapid succession.
- **Expected:** Duplicate requests are throttled / discarded by the repository mutex. Only one active network request completes.

### Step 21: Navigate Away During Refresh
- **Action:** Tap "Refresh" and immediately tap "Back".
- **Expected:** Screen coroutines cancel safely. No memory leak or crash occurs.

### Step 22: Logout During Refresh
- **Action:** Trigger refresh and immediately initiate user sign-out.
- **Expected:** Session clears cleanly, app navigates to Login/Splash screen safely.

---

## 6. Regression Testing (Phases 00–13)

### Step 23: Student Discovery & Ride Request (Phases 05–07)
- **Action:** Perform location search, view discovery matches, create ride request.
- **Expected:** Normal flow operates without regressions.

### Step 24: Student Live Tracking & Payment (Phases 11 & 13)
- **Action:** Verify live ride tracking map and digital payment checkout.
- **Expected:** Fully functional, no broken dependencies.

### Step 25: Driver Operations (Phases 08–10)
- **Action:** Switch to driver role, test online toggle, ride request acceptance, passenger boarding, and GPS emission.
- **Expected:** Driver operations remain completely intact.
