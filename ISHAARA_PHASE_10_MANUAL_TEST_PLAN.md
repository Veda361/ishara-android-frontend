# ISHAARA PHASE 10 MANUAL TEST PLAN
**Driver / Conductor Ride Requests + Passenger Boarding Operational Workflow**

This manual test suite verifies the end-to-end operational lifecycle for Driver/Conductor users handling incoming ride requests and managing passenger boarding.

---

## Prerequisites
- Android device or emulator with Google Play Services (API 24+).
- Verified backend environment running (Local, Staging, or Production).
- Test accounts:
  1. `driver@ishaara.app` (Role: `DRIVER_CONDUCTOR`, Active trip initialized)
  2. `student@ishaara.app` (Role: `USER`, Active campus passenger)

---

## 1. Authentication & Role-Based Navigation
- [ ] **Test 1.1: Driver Access Verification**
  - Sign in using `driver@ishaara.app`.
  - Observe `MainActivity` routing directly to `DriverDashboard`.
  - Confirm driver workspace shows "ROLE: DRIVER / CONDUCTOR".
- [ ] **Test 1.2: Student Role Protection**
  - Sign in using `student@ishaara.app`.
  - Confirm student home is presented.
  - Attempt back/intent navigation to `driver/requests` or `driver/dashboard`.
  - Verify that the app protects against unauthorized role access and never displays driver operational cards.

---

## 2. Driver Requests & Boarding Workspace Navigation
- [ ] **Test 2.1: Open Requests & Boarding Screen**
  - From `DriverHomeScreen`, scroll to the "PASSENGER WORKFLOW" card.
  - Tap **"Open Requests & Boarding"**.
  - Verify top app bar displays title "Ride Requests & Boarding" with live status pill.
  - Verify presence of two tabs: **"Incoming Requests"** and **"Active Boarding"**.

---

## 3. Incoming Request Presentation
- [ ] **Test 3.1: Empty State**
  - Open "Incoming Requests" tab when no ride requests are pending.
  - Confirm `IshaaraEmptyState` is displayed with message: "No pending ride requests at this moment."
- [ ] **Test 3.2: Passenger Request Card**
  - On the student device, search for trips and submit a ride request on the driver's active trip.
  - On driver device, either observe realtime arrival or tap **"Refresh"**.
  - Verify request card renders:
    - Passenger masked ID
    - Status badge: `PENDING`
    - Pickup formatted address
    - Destination formatted address
    - Two large primary action buttons (54dp): **"Decline"** (outlined danger) and **"Accept"** (solid accent).
  - Verify zero fabricated fields (no fake fare, no fake passenger phone number, no fake ETA).

---

## 4. Ride Request Actions (Accept / Reject)
- [ ] **Test 4.1: Reject Ride Request**
  - Tap **"Decline"** on an incoming request.
  - Verify confirmation dialog appears with optional "Reason" input field.
  - Enter "Vehicle full" and tap **"Reject Request"**.
  - Verify request disappears from "Incoming Requests" list.
  - Verify student receives `REJECTED` status update.
- [ ] **Test 4.2: Duplicate Tap Protection on Accept**
  - Tap **"Accept"** on an incoming request rapidly multiple times.
  - Verify only one network mutation is submitted (subsequent taps blocked by `isActionInProgress`).
  - Confirm success snackbar: "Ride request accepted. Passenger ride created."
  - Verify request is removed from "Incoming Requests".

---

## 5. Passenger Boarding & Operational Ride Lifecycle
- [ ] **Test 5.1: Newly Provisioned Ride in Active Boarding Tab**
  - Switch to the **"Active Boarding"** tab.
  - Verify the accepted passenger ride appears with status: `ARRIVING TO PICKUP`.
  - Verify primary action button reads: **"Arrived at Pickup"**.
- [ ] **Test 5.2: Driver Arrived at Pickup**
  - Tap **"Arrived at Pickup"**.
  - Verify backend state transitions to `DRIVER_ARRIVING`.
  - Verify card status badge updates to: `READY FOR BOARDING`.
  - Verify primary action button updates to: **"Confirm Passenger Boarding"** (54dp green).
- [ ] **Test 5.3: Passenger Boarding Confirmation (Pickup)**
  - When passenger enters vehicle, tap **"Confirm Passenger Boarding"**.
  - Verify mutation executes `POST /api/v1/rides/:rideId/pickup`.
  - Verify status badge updates to: `PASSENGER BOARDED`.
  - Verify primary action button updates to: **"Start Transit to Destination"**.
- [ ] **Test 5.4: Start Ride Transit**
  - Tap **"Start Transit to Destination"**.
  - Verify mutation executes `POST /api/v1/rides/:rideId/start`.
  - Verify status badge updates to: `IN TRANSIT`.
  - Verify primary action button updates to: **"Complete Ride"**.
- [ ] **Test 5.5: Complete Ride at Destination**
  - Tap **"Complete Ride"**.
  - Verify mutation executes `POST /api/v1/rides/:rideId/complete`.
  - Verify ride moves out of active boarding list and success notification is displayed.

---

## 6. Realtime WebSocket Synchronization
- [ ] **Test 6.1: Realtime Incoming Request**
  - Keep driver screen open on "Incoming Requests".
  - Student submits request.
  - Verify request automatically appears without manual page reload.
  - Verify top notification pill displays "New ride request received!".
- [ ] **Test 6.2: Realtime Passenger Cancellation**
  - With a pending request displayed on the driver's screen, have the student cancel their request.
  - Verify the request card is immediately removed from the driver screen via `RIDE_REQUEST_CANCELLED` WebSocket event.
  - Verify snackbar message: "A ride request was cancelled by the passenger."

---

## 7. Network Loss & Offline Resilience
- [ ] **Test 7.1: Network Loss During Mutation**
  - Disable Wi-Fi and Mobile Data on driver device.
  - Tap **"Arrived at Pickup"**.
  - Verify no fake optimistic state change occurs; request remains in current state.
  - Verify snackbar: "Network connection failed. Please check your internet connection."
  - Re-enable network and retry operation; confirm successful sync.

---

## 8. Process Recreation & Ergonomics
- [ ] **Test 8.1: Screen Rotation & Back Navigation**
  - Rotate device from portrait to landscape on "Ride Requests & Boarding" screen.
  - Verify request list and tab selection remain intact without losing current state.
  - Press Android hardware Back button or TopAppBar "← Back" button.
  - Verify clean navigation back to `DriverHomeScreen`.
- [ ] **Test 8.2: Driver Accessibility & Touch Targets**
  - Verify all primary action buttons have a minimum touch target height of 48dp (primary CTA is 54dp).
  - Verify high-contrast color styling conforming strictly to the Ishaara design system.
