# ISHAARA Frontend Phase A15 — Forensic Audit & Backend Contract

**Date:** 2026-10-01  
**Target:** Notifications, Push Messaging & Device Tokens  
**Backend Source:** `ishara-backend/src/modules/notifications/` and `ishara-backend/src/modules/events/`

---

## PART 1 — DEVICE TOKEN FORENSIC AUDIT

### 1. What endpoint registers a device token?
- `POST /api/v1/devices/push-token`

### 2. HTTP method?
- `POST`

### 3. Required fields?
- `token` (string, min 1, trimmed)

### 4. Optional fields?
- `platform` (enum: `"ANDROID"`, `"IOS"`, `"WEB"`, defaults to `"ANDROID"`)
- `appVersion` (string, trimmed, optional)
- `deviceId` (string, trimmed, optional)

### 5. Is platform required?
- No, it is optional in the schema with a default of `"ANDROID"`. Valid values: `"ANDROID"`, `"IOS"`, `"WEB"`.

### 6. Is device ID required?
- No, optional. Stored as string or null.

### 7. Is app version supported?
- Yes, optional. Stored as string or null.

### 8. Is OS version supported?
- No. Neither the Zod schema (`registerPushTokenSchema`) nor Mongoose model (`DeviceTokenModel`) accept or store OS version.

### 9. Is device name supported?
- No. Not in the schema or model.

### 10. Does the backend support multiple devices per account?
- **Yes.** The backend enforces an active token quota defined by `PUSH_TOKEN_MAX_PER_USER` (defaults to 5).
- If an authenticated user already has 5 active tokens, the backend deactivates the oldest active token (`isActive = false`) sorted by `lastSeenAt: 1, _id: 1` before creating a new one.

### 11. Can one token belong to only one user?
- **Yes.** The `token` field has a unique index.
- If a device token is already registered to user A, and user B registers with the same token (e.g. account switch or reassignment), `DeviceTokenService.registerToken` updates `userId` to user B, sets `isActive = true`, and updates `lastSeenAt = now`. The token is transferred atomically.

### 12. Can the same user have multiple active tokens?
- **Yes**, up to 5 concurrent active tokens.

### 13. What happens when a token is registered twice?
- Re-registering the same token for the same user refreshes `isActive = true`, updates `lastSeenAt = now`, updates optional metadata (`deviceId`, `appVersion`), and returns HTTP 200 without creating a duplicate document.

### 14. Is registration idempotent?
- **Yes.** Atomic lookup by `{ token }` with update, plus E11000 duplicate key race catch block.

### 15. How is token refresh handled?
- The client obtains a new FCM push token and invokes `POST /api/v1/devices/push-token`.

### 16. How is token invalidation handled?
- When the push provider (FCM) reports `INVALID_TOKEN` or `UNREGISTERED` during delivery, the backend orchestrator calls `DeviceTokenService.deactivateToken(token)` setting `isActive = false`.

### 17. How are stale tokens removed?
- Stale tokens are deactivated on delivery failure, de-prioritized by FIFO quota when a user registers new devices, or deactivated via the removal endpoint.

### 18. Is there an unregister endpoint?
- **Yes:** `DELETE /api/v1/devices/push-token` with request body `{"token": "<token>"}`.

### 19. Is device-token deletion performed during logout?
- The frontend must call `DELETE /api/v1/devices/push-token` on explicit logout to deactivate the token association for that account.

### 20. Does the backend distinguish USER and DRIVER_CONDUCTOR devices?
- Tokens are associated with `userId`. The user's role is authoritative in Better Auth (`role: "USER" | "DRIVER_CONDUCTOR"`). Domain event mapping specifically resolves passenger `userId` or driver `driverProfile.userId` before dispatching.

### 21. What authorization is required?
- Bearer session token (`requireAuth`).
- Rate limited via `pushTokenRateLimiter`.

### 22. What response does registration return?
- HTTP 200 OK:
  ```json
  {
    "status": "success",
    "data": {
      "tokenMasked": "dK3x9...",
      "platform": "ANDROID",
      "isActive": true,
      "lastSeenAt": "2026-10-01T16:00:00.000Z"
    },
    "message": "Push token registered successfully."
  }
  ```

---

## PART 2 — PUSH PROVIDER AUDIT

- **Provider**: Firebase Cloud Messaging (FCM) via server-side `firebase-admin/messaging`.
- **Server Credentials**: `FCM_PROJECT_ID`, `FCM_CLIENT_EMAIL`, `FCM_PRIVATE_KEY` on the backend. When missing, runs in Sandbox mode.
- **Android Security Rule**: Private server credentials must never be bundled into the APK.
- **FCM Message Payload**:
  - `notification`: `{ title: string, body: string }`
  - `data`: `Record<string, string>` (key-value string pairs)
  - `android`: `{ priority: "high" | "normal", notification: { sound: "default", clickAction: "FLUTTER_NOTIFICATION_CLICK" } }`

---

## PART 3 — NOTIFICATION EVENT AUDIT TABLE

| Event Type | Backend Trigger | Recipient | Target Role | Payload Data | Target Destination |
|---|---|---|---|---|---|
| `RIDE_REQUEST_CREATED` | Passenger creates request | Driver (`resolveDriverUserId(driverId)`) | `DRIVER_CONDUCTOR` | `{ type, requestId, tripId }` | Driver Ride Requests |
| `RIDE_REQUEST_ACCEPTED`| Driver accepts request | Passenger (`userId`) | `USER` | `{ type, requestId, tripId }` | Passenger Ride Tracking |
| `RIDE_REQUEST_REJECTED`| Driver declines request | Passenger (`userId`) | `USER` | `{ type, requestId }` | Passenger Home |
| `RIDE_REQUEST_CANCELLED`| Passenger cancels request| Driver (`resolveDriverUserId(driverId)`) | `DRIVER_CONDUCTOR` | `{ type, requestId }` | Driver Requests |
| `RIDE_REQUEST_EXPIRED` | Request expires (timeout)| Passenger (`userId`) | `USER` | `{ type, requestId }` | Passenger Home |
| `RIDE_DRIVER_ARRIVING` | Driver is approaching | Passenger (`userId`) | `USER` | `{ type, rideId }` | Passenger Ride Tracking |
| `RIDE_PICKED_UP`       | Driver confirms pickup | Passenger (`userId`) | `USER` | `{ type, rideId }` | Passenger Ride Tracking |
| `RIDE_STARTED`         | Ride in progress | Passenger (`userId`) | `USER` | `{ type, rideId }` | Passenger Ride Tracking |
| `RIDE_COMPLETED`       | Ride finished | Passenger (`userId`) | `USER` | `{ type, rideId }` | Post-Ride Rating |
| `RIDE_CANCELLED`       | Driver or Passenger cancel | Counterpart | Both | `{ type, rideId }` | Ride Details / Home |
| `PAYMENT_CAPTURED`     | Payment successful | Passenger & Driver | Both | `{ type, paymentId, rideId }` | Payment Details / Driver Earnings |
| `REFUND_PROCESSED`     | Refund issued | Passenger (`userId`) | `USER` | `{ type, refundId, paymentId }` | Payment Details |
| `SETTLEMENT_PROCESSED` | Payout transfer done | Driver (`driverUserId`) | `DRIVER_CONDUCTOR` | `{ type, settlementId }` | Driver Earnings |

*Discrepancy Note:* `SAFETY_SOS_CREATED` is handled in A11 Realtime WebSockets (`SOS_CREATED` / `SOS_CANCELLED`), not in the in-app notification outbox mapper.

---

## PART 4 — NOTIFICATION INBOX & READ/UNREAD APIS

- `GET /api/v1/notifications?page=1&limit=20&status=UNREAD|READ`
  - Query parameters: `page` (default 1), `limit` (max 50, default 20), `status` (optional `UNREAD` or `READ`).
  - Response: `{ items: NotificationItem[], total, page, limit, hasMore, unreadCount }`.
- `GET /api/v1/notifications/unread-count`
  - Response: `{ unreadCount: number }`.
- `POST /api/v1/notifications/:notificationId/read`
  - Enforces IDOR protection: returns 403 `NOTIFICATION_NOT_OWNED` if owned by another user.
  - Response: Updated `NotificationItem`.
- `POST /api/v1/notifications/read-all`
  - Marks all unread notifications for current user as read.
  - Response: `{ markedCount: number }`.
- **Deletion**: Not supported by backend HTTP API.
- **Preferences**: Read by backend orchestrator in MongoDB, but **NO** HTTP routes exist for preferences. No fake settings screens should be built.
