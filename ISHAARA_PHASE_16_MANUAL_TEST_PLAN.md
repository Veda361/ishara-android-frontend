# ISHAARA PHASE 16 — MANUAL TEST PLAN
## Driver Shift History, Earnings & Payout Analytics

**Test Suite:** Phase 16 Validation  
**Role Target:** `DRIVER_CONDUCTOR`  
**Prerequisites:** Backend running on local/staging environment, seeded with completed rides and captured Razorpay/mock payments.

---

### 1. Driver Role Authentication & Access Control

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 1.1 | Login as `USER` (Student) | Student home screen is displayed. No financial or driver navigation links exist. | [ ] |
| 1.2 | Attempt deep-link or direct route to `driver/earnings` while authenticated as `USER` | Navigation interceptor or role guard redirects to role recovery screen or student home with access denied. | [ ] |
| 1.3 | Login as `DRIVER_CONDUCTOR` | Driver operational home screen is displayed. | [ ] |
| 1.4 | Verify "Financial Summary / Earnings" entry on Driver Home | Clear "Earnings & Ride History" section is visible with "View Earnings & History" CTA. | [ ] |
| 1.5 | Click "View Earnings & History" | Driver Earnings screen opens smoothly with back navigation to Driver Home. | [ ] |

---

### 2. Period Filtering & Calculations Verification

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 2.1 | View default period ("Today") | Displays Net Driver Earnings, Gross Passenger Fare, Platform Fees, and Completed Rides count strictly matching backend values. | [ ] |
| 2.2 | Switch period to "This Week" | Screen shows loading indicator; fetches weekly bounds (Monday to today); updates net earnings and ride list. | [ ] |
| 2.3 | Switch period to "This Month" | Screen fetches 1st of month to today; aggregates updated correctly. | [ ] |
| 2.4 | Switch to "Custom Range" with valid dates (`from <= to`) | Date picker/input sets dates; server returns aggregated results for custom window. | [ ] |
| 2.5 | Set custom range where `from > to` | Error banner displayed: "Start date must be before or equal to end date". No invalid request sent to backend. | [ ] |

---

### 3. Settlement Breakdown & Status Display

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 3.1 | Inspect Settlement Card | Shows Settled amount (`PROCESSED`) with green badge. | [ ] |
| 3.2 | Inspect Pending Settlement | Shows Pending amount (`PENDING`) with warning badge. | [ ] |
| 3.3 | Verify ride item settlement badges | Each completed ride displays its exact settlement status (`PROCESSED`, `PENDING`, `UNSETTLED`, etc.). | [ ] |
| 3.4 | Verify ride payment status badges | Each completed ride displays payment status (`CAPTURED`, etc.). | [ ] |

---

### 4. Ride History & Pagination

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 4.1 | Inspect Ride History List | List displays completed rides with origin, destination, completion time, gross amount, platform deduction, and net earnings. | [ ] |
| 4.2 | Scroll to bottom of list with > 20 rides | Next page of rides is loaded incrementally without jumping or reloading previous items. | [ ] |
| 4.3 | Check for duplicate items | No duplicate `rideId` items appear in the list. | [ ] |
| 4.4 | Change filter while on page 2 | Pagination resets to page 1 for the new period. | [ ] |

---

### 5. Pull-to-Refresh & Concurrency

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 5.1 | Trigger Pull-to-Refresh | Refresh indicator appears; data is refreshed from backend; totals update without duplicated items. | [ ] |
| 5.2 | Rapidly trigger refresh multiple times | Only one in-flight refresh executes. No race condition or visual flickering. | [ ] |
| 5.3 | Switch period while refresh is in-flight | In-flight request is safely cancelled/ignored and new period data is rendered. | [ ] |

---

### 6. Offline & Network Resiliency

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 6.1 | Disconnect network with existing loaded data | UI shows "Offline — Showing data loaded earlier". Stale data is not implied to be live. | [ ] |
| 6.2 | Disconnect network on cold start | UI shows friendly offline error: "Connect to the internet to view earnings" with "Retry" CTA. | [ ] |
| 6.3 | Reconnect network and tap "Retry" | UI recovers and displays live backend financial figures. | [ ] |

---

### 7. Security, Privacy & Memory Safety

| Step | Action | Expected Result | Pass/Fail |
|---|---|---|---|
| 7.1 | Verify Logcat output | Zero auth tokens, session keys, bank account numbers, or raw monetary credentials logged. | [ ] |
| 7.2 | Passenger privacy audit | Completed ride cards do not expose passenger phone numbers, email addresses, or personal names. | [ ] |
| 7.3 | Multi-driver isolation | Sign out Driver A, sign in Driver B. Driver B sees only Driver B's earnings and ride history. No cross-driver cache leakage. | [ ] |
| 7.4 | Process recreation | Rotate screen or trigger system background recreation. ViewModel restores UI state cleanly without crash. | [ ] |

---

### 8. Regression Verification

| Step | Subsystem | Action | Pass/Fail |
|---|---|---|---|
| 8.1 | Phase 03/04 Auth & Onboarding | Sign in, sign out, role routing works seamlessly. | [ ] |
| 8.2 | Phase 08 Driver Operations | Online/offline toggle, active vehicle, active trip lifecycle operational. | [ ] |
| 8.3 | Phase 09 Driver GPS | Location tracking service continues functioning without interference. | [ ] |
| 8.4 | Phase 10 Driver Boarding | Incoming ride requests, accept/reject, boarding verification unaffected. | [ ] |
| 8.5 | Phase 11 Live Tracking | Passenger live tracking functions correctly. | [ ] |
| 8.6 | Phase 12 Voice Trip | Hands-free voice trip creation works unaffected. | [ ] |
| 8.7 | Phase 13 Digital Payment | Passenger checkout and payment verification unaltered. | [ ] |
| 8.8 | Phase 14 Offline Transit | Route schedule and offline stops cache intact. | [ ] |
| 8.9 | Phase 15 Safety / SOS | Emergency SOS button and contacts work as expected. | [ ] |
