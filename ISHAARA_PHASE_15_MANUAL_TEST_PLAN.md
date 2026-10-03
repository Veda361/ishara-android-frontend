# ISHAARA Phase 15 — Manual Safety & SOS Test Plan

This manual test plan outlines comprehensive verification steps for production readiness of the Safety, SOS, and Emergency Response system.

---

## 1. Basic Flow

1. **Login as Supported Role**: Log in as a passenger (`USER` role).
2. **Start/Create a Valid Ride**: Initiate or accept a ride so that it transitions to `DRIVER_ARRIVING` or `IN_PROGRESS`.
3. **Open Active Ride Screen**: Verify that live tracking displays driver, pickup, destination, and the prominent "Safety & Emergency" entry point.
4. **Open Safety Screen**: Tap the Safety action. Verify navigation to `SafetyScreen` with ride context.
5. **Review SOS Explanation**: Confirm calm, clear explanatory text detailing what the safety alert does without exaggerated or false claims.
6. **Confirm SOS**: Tap or hold the "Send Safety Alert" button.
7. **Observe Loading State**: Observe immediate transition to in-flight progress ("Sending safety alert...") and verification that primary action is disabled.
8. **Verify Backend Response**: Confirm successful creation with `201 Created` and returned `eventId` (`se_...`).
9. **Verify Success UI**: Verify active alert banner/card showing event ID, active status badge, timestamp, and a "Cancel Alert" option.

---

## 2. Duplicate Protection

10. **Tap SOS Repeatedly**: Rapidly tap the "Send Safety Alert" button multiple times.
11. **Verify Deduplication**: Verify that only a single HTTP POST request is dispatched and only one active incident exists.

---

## 3. Network Resilience & Reconciliation

12. **Offline Pre-Flight**: Turn on Airplane mode before tapping SOS.
13. **Verify Offline Notice**: Confirm immediate, calm feedback: "You are offline. Unable to send safety alert." Verify no crash.
14. **Disconnect During Request**: Initiate SOS with network enabled; immediately toggle Airplane mode while request is in-flight.
15. **Reconnection & Reconciliation**: Re-enable network; trigger status check or retry.
16. **State Reconciliation**: Verify client calls `GET /rides/:rideId/safety/active` and adopts authoritative active status if the previous attempt succeeded.

---

## 4. Location Context

17. **Ride in `DRIVER_ARRIVING`**: Trigger SOS before pickup; verify backend records pickup location snapshot.
18. **Ride in `IN_PROGRESS`**: Trigger SOS after pickup; verify backend records vehicle current GPS snapshot.
19. **Location Unavailable on Driver Device**: Trigger SOS when driver GPS is turned off; verify SOS still succeeds with `coordinates: null` (never blocked).
20. **Privacy Check**: Inspect Android logcat to ensure raw GPS coordinates (`[lng, lat]`) are never logged.

---

## 5. App Lifecycle & Configuration Changes

21. **Background App**: Trigger SOS and immediately place app in background.
22. **Return to Foreground**: Reopen app; verify active SOS state is preserved.
23. **Screen Rotation**: Rotate device between portrait and landscape during active alert state; verify no UI flickering or loss of state.
24. **Process Death / Recreation**: Kill process via developer options; reopen ride; verify `GET /rides/:rideId/safety/active` restores the alert state.
25. **Navigation Away**: Navigate back to ride tracking while alert is active; confirm persistent safety alert indicator remains visible on ride tracking screen.

---

## 6. Backend Edge Cases

26. **Already-Active SOS**: Attempt to trigger SOS when an active alert is already open; verify client handles `409 Conflict` gracefully and presents active event.
27. **Invalid Ride ID**: Attempt safety actions on non-existent ride; verify clear `404 Ride Not Found` handling.
28. **Unauthorized Session**: Expire auth token; verify prompt to re-authenticate without crash.
29. **Forbidden User**: Attempt to trigger SOS for another passenger's ride; verify `403 Forbidden` handled cleanly.
30. **Cancellation Flow**: Tap "Cancel Alert" with optional reason; verify transition to `CANCELLED` status and alert dismisses.
31. **Rate Limiting**: Verify appropriate message if `429 Too Many Requests` is returned.

---

## 7. Emergency Contacts Management

32. **View Contacts**: Open Emergency Contacts section; list current contacts.
33. **Add Contact**: Add valid contact with name, phone (`+919876543210`), and relationship (`PARENT`).
34. **Limit Enforcement**: Attempt to add a 6th contact; verify `409 Conflict` ("Maximum of 5 emergency contacts reached") handled with friendly UI.
35. **Delete Contact**: Remove contact; verify immediate UI update.

---

## 8. Regression Suite

36. **Student Home Screen**: Destination selection and search remain operational.
37. **Trip Discovery**: Discovery sheet and results display properly.
38. **Ride Requests**: Creating and submitting ride requests functions normally.
39. **Digital Payment**: Fare estimation and checkout session work without interference.
40. **Digital Ticket**: QR code and boarding pass display properly.
41. **Live Ride Tracking**: Realtime driver GPS tracking, canvas map, and ETA remain smooth.
42. **Voice Trip**: Speech recognition and draft creation remain functional.
43. **Offline Transit**: Offline bus schedule cache remains intact and isolated.
44. **Driver Operations**: Driver home, GPS tracking, and ride requests remain intact.
