# ISHAARA — Phase 13 Manual Test Plan
**Digital Ticketing, Fare & Payment Verification**

---

## Prerequisites
1. Backend running (`http://localhost:3000` or local network IP for real device).
2. Connected Android device (`ZXCI59SGINRCS4RK`) or emulator running debug APK.
3. Registered student account (`USER` role).
4. An active ride in an eligible payable state (`CREATED`, `DRIVER_ARRIVING`, `PICKED_UP`, `IN_PROGRESS`, or `COMPLETED`).

---

## Test Scenarios

### TEST 1 — Fare Display & Formatting
* **Action**: Open ride payment screen for a ride with 2500 paise fare.
* **Expected Result**: Amount is formatted as `₹25.00` using Indian currency convention. No raw paise integer displayed to the user.

### TEST 2 — Order Session Creation
* **Action**: Tap "Pay ₹25.00" button.
* **Expected Result**: UI transitions to loading state ("Preparing payment..."). Calls `POST /api/v1/rides/:rideId/payment`. Session details (`paymentId`, `providerOrderId`, `qrPayload`, `grossAmountMinor`) are received.

### TEST 3 — Duplicate Tap Guard
* **Action**: Rapidly double tap "Pay ₹25.00".
* **Expected Result**: Exactly one network request is dispatched. The second tap is ignored by the in-flight guard.

### TEST 4 — UPI / Provider Launch
* **Action**: Proceed with checkout session.
* **Expected Result**: PaymentLauncher opens installed UPI app via `qrPayload` intent, or shows provider checkout sheet.

### TEST 5 — Payment Cancellation by User
* **Action**: Dismiss provider checkout without completing payment.
* **Expected Result**: UI returns to `OrderReady` state with an informative banner: "Payment was cancelled." The user can re-attempt payment.

### TEST 6 — Authoritative Backend Verification
* **Action**: Complete checkout with valid provider payment ID and signature.
* **Expected Result**: UI displays "Confirming payment...". Calls `POST /api/v1/payments/:paymentId/verify`. On HTTP 200, state transitions to `Paid` / Receipt.

### TEST 7 — Never Trust Client Alone
* **Action**: Simulate corrupted/invalid signature in verification request.
* **Expected Result**: Backend returns 400 (`PAYMENT_SIGNATURE_INVALID`). Android app does NOT show success; displays error banner and allows retry.

### TEST 8 — Payment Receipt Rendering
* **Action**: Successfully verify payment.
* **Expected Result**: Receipt card renders ride details, amount (`₹25.00`), payment ID, capture timestamp, and "Track Ride" CTA.

### TEST 9 — Handoff to Live Ride Tracking
* **Action**: Tap "Track Ride" on the receipt screen.
* **Expected Result**: Navigates cleanly into Phase 11 `RideTrackingScreen`.

### TEST 10 — Already-Paid Ride Re-entry
* **Action**: Re-open payment screen for a ride whose payment is already `CAPTURED`.
* **Expected Result**: Checks `GET /api/v1/rides/:rideId/payment`. Immediately displays `Paid` receipt state; does not prompt the user to pay again.

### TEST 11 — Network Disconnection / Offline Guard
* **Action**: Put device in airplane mode and tap Pay.
* **Expected Result**: Shows offline banner: "You're offline. Connect to the internet to complete payment." Does not fabricate payment success.

### TEST 12 — App Process Death / Resume
* **Action**: Initiate payment order, background app, kill process via ADB, relaunch.
* **Expected Result**: App reconciles payment status via backend and restores the correct state.

### TEST 13 — 401 Session Expiration
* **Action**: Expire JWT token and attempt payment.
* **Expected Result**: Session coordinator invalidates session; user is safely redirected to Login.

### TEST 14 — Driver Role Access Prevention
* **Action**: Log in with `DRIVER_CONDUCTOR` credentials.
* **Expected Result**: Payment screen is not accessible in driver flow. Backend would return 403.

### TEST 15 — Regression Check (Phases 00–12)
* **Action**: Run complete test suite (`./gradlew testDebugUnitTest`).
* **Expected Result**: 100% of all unit and contract tests pass.
