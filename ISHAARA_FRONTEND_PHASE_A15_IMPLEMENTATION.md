# Phase A15 Implementation Guide — Notifications, In-App Alerts & Device Tokens

## 1. Architecture Overview
Phase A15 establishes the production-grade push and in-app notification infrastructure for the ISHAARA Android application following Clean Architecture principles:
- **Presentation Layer**: [`NotificationInboxScreen.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/notifications/ui/NotificationInboxScreen.kt), [`NotificationViewModel.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/notifications/NotificationViewModel.kt), [`NotificationUiState.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/notifications/NotificationUiState.kt).
- **Core / Routing Layer**: [`NotificationRouter.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/notifications/NotificationRouter.kt), [`NotificationChannelConfig.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/notifications/NotificationChannelConfig.kt), [`DeviceTokenStore.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/storage/DeviceTokenStore.kt).
- **Domain Layer**: [`NotificationModels.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/model/NotificationModels.kt), [`NotificationRepository.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/repository/NotificationRepository.kt), [`DeviceTokenRepository.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/repository/DeviceTokenRepository.kt), and use cases (`GetNotificationsUseCase`, `GetUnreadNotificationCountUseCase`, `MarkNotificationAsReadUseCase`, `MarkAllNotificationsAsReadUseCase`, `RegisterDeviceTokenUseCase`, `RemoveDeviceTokenUseCase`).
- **Data Layer**: [`NotificationRepositoryImpl.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/repository/NotificationRepositoryImpl.kt), [`DeviceTokenRepositoryImpl.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/repository/DeviceTokenRepositoryImpl.kt), [`NotificationRemoteDataSource.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/datasource/NotificationRemoteDataSource.kt), [`DeviceTokenRemoteDataSource.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/datasource/DeviceTokenRemoteDataSource.kt), [`NotificationDtos.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/dto/NotificationDtos.kt), [`NotificationMapper.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/mapper/NotificationMapper.kt), [`DeviceTokenMapper.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/mapper/DeviceTokenMapper.kt).
- **Dependency Injection**: Integrated into [`AppContainer.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/di/AppContainer.kt).

---

## 2. Backend Contract Specifications
Base URL: `https://reposnse-ishaara.onrender.com/api/v1`

| Endpoint | Method | Auth | Query / Body | Response Schema |
|---|---|---|---|---|
| `/notifications` | `GET` | Bearer Token | `page`, `limit`, `category`, `status` | `{ items: NotificationItem[], total, page, limit, hasMore, unreadCount }` |
| `/notifications/unread-count` | `GET` | Bearer Token | None | `{ unreadCount: number }` |
| `/notifications/:id/read` | `POST` | Bearer Token | None | `NotificationItem` (updated readAt) |
| `/notifications/read-all` | `POST` | Bearer Token | None | `{ markedCount: number }` |
| `/devices/push-token` | `POST` | Bearer Token | `{ token, platform: "android" }` | `{ success: true, registered: true }` |
| `/devices/push-token` | `DELETE` | Bearer Token | `{ token }` | `{ success: true, removed: true }` |

Authoritative Notification Categories:
- `rideUpdates` ("Ride Updates")
- `account` ("Account")
- `system` ("System")

---

## 3. Role-Isolated Notification Routing
Deep links from notifications are mediated through [`NotificationRouter.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/notifications/NotificationRouter.kt):
1. **Authentication Requirement**: Navigation is strictly blocked if active user is unauthenticated.
2. **Role Boundaries**:
   - `UserRole.USER`: Routes `RIDE_COMPLETED` to rating, `RIDE_STARTED`/`RIDE_PICKED_UP` to live ride tracking, `PAYMENT_CAPTURED` to ride payment, and requests to request status. Blocked from driver-only events (`RIDE_REQUEST_CREATED`, `SETTLEMENT_PROCESSED`).
   - `UserRole.DRIVER_CONDUCTOR`: Routes `RIDE_REQUEST_CREATED` to driver requests, `RIDE_STARTED` to active driver trip, and completions/payments to driver home. Blocked from passenger-only events (`RIDE_REQUEST_REJECTED`, `REFUND_PROCESSED`).
3. **Graceful Fallback**: Unknown event types or malformed payloads safely navigate to the in-app notification inbox without crashing.

---

## 4. Device Token Lifecycle & Security
1. **Registration**: When an FCM token is acquired and a valid session exists, `registerDeviceToken` posts `{ token, platform: "android" }` with the session auth token.
2. **Removal on Logout**: During logout, the device token is deactivated on the backend via `DELETE /api/v1/devices/push-token` and the local cache in `DeviceTokenStore` is cleared.
3. **Account Switching**: Token caches and in-memory notification states are cleared on logout, preventing cross-account contamination when USER A logs out and USER B logs in.
4. **Sanitized Logging**: FCM tokens, authorization headers, and raw notification payloads are never logged.

---

## 5. Build & Verification
- Unit Tests: 50/50 Phase A15 tests passed.
- Full Unit Test Suite: 682/682 passed.
- Compilation: `compileDebugKotlin` and `compileDebugUnitTestKotlin` clean.
