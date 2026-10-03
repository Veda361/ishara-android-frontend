# Phase A15 Implementation Changelog — Notifications, In-App Alerts & Device Tokens

## Overview
Phase A15 implements the complete, production-grade Android frontend architecture for Notifications, In-App Inbox Alerts, FCM Device Token Management, and Role-Isolated Navigation Routing on the ISHAARA Android mobility platform, strictly aligned with backend capabilities (`ishara-backend/src/modules/notifications/` and `ishara-backend/src/modules/events/`).

---

## 1. Compilation & Architecture Repairs
Resolved 17 compilation errors and architectural mismatches found in preliminary drafts:
- **`IshaaraError` Hierarchy Alignment**:
  - Replaced illegal references to `IshaaraError.Unauthorized` with authoritative `IshaaraError.Authentication(message = ...)` in [`DeviceTokenRepositoryImpl.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/repository/DeviceTokenRepositoryImpl.kt) and [`NotificationRepositoryImpl.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/repository/NotificationRepositoryImpl.kt).
- **Navigation Coordinator Integration**:
  - Fixed [`NotificationRouter.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/core/notifications/NotificationRouter.kt) to invoke `navigationManager.navigate(...)` instead of non-existent `navigateTo`.
- **ViewModel Construction & DI**:
  - Wired `navigationManager = appContainer.navigationManager` into `driverNotificationViewModel` and `studentNotificationViewModel` instantiations in [`MainActivity.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt).
- **Screen Integration & Role Propagation**:
  - Updated [`NotificationInboxScreen.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/notifications/ui/NotificationInboxScreen.kt) to accept `userRole: UserRole` and optional `onNavigateBack: (() -> Unit)?`.
  - Updated call sites in [`MainActivity.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt) for `DriverScreenState.Notifications` (`UserRole.DRIVER_CONDUCTOR`) and `StudentScreenState.Notifications` (`UserRole.USER`).
- **Design System & Layout Repairs**:
  - Replaced non-existent `IshaaraIconButtonVariant.Ghost` with `IshaaraIconButtonVariant.Standard`.
  - Replaced non-existent `IshaaraButtonVariant.Ghost` with `IshaaraButtonVariant.Text`.
  - Fixed inverted Compose parameters in `Column` from `verticalAlignment`/`horizontalArrangement` to `verticalArrangement = Arrangement.Center` and `horizontalAlignment = Alignment.CenterHorizontally`.
  - Updated `IshaaraCard` usage to use `containerColor` and resolved `IshaaraPalette.PureWhite`.

---

## 2. Domain & Data Enhancements
- **Authoritative Category Mapping**:
  - Updated [`NotificationCategory`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/domain/model/NotificationModels.kt) enum to represent backend values: `rideUpdates`, `account`, `system`.
  - Added query parameter delegation in [`NotificationRemoteDataSource.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/datasource/NotificationRemoteDataSource.kt) (`category`, `status`, `page`, `limit`).
- **Device Token Contract**:
  - Enforced lowercase `platform = "android"` in [`DeviceTokenRemoteDataSource.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/datasource/DeviceTokenRemoteDataSource.kt) and [`NotificationDtos.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/data/remote/dto/NotificationDtos.kt).
  - Sanitized registration and removal response handling supporting both envelope and direct boolean shapes.
- **Robust State Machine & Lifecycle**:
  - Enhanced [`NotificationViewModel.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/feature/notifications/NotificationViewModel.kt) with duplicate request protection, stale response generation tracking via `AtomicLong`, unread count synchronization, session expiration state (`isSessionExpired`), retry logic, and `onLogoutCleanup()` to guarantee zero state leakage across account switches.

---

## 3. UI & Design System
- **Notification Inbox Screen**:
  - Supported states: `LOADING`, `CONTENT`, `EMPTY`, `REFRESHING`, `PAGINATION_LOADING`, `SESSION_EXPIRED`.
  - Filter Tabs (`ALL` vs `UNREAD`) with unread badge counter.
  - Horizontal Category Chips (`All Categories`, `Ride Updates`, `Account`, `System`) mapped cleanly without leaking display labels to backend queries.
  - Interactive Mark-as-Read on individual cards and top bar "Mark all read" action.
  - Load More pagination button with inline spinner.

---

## 4. Test Verification
Created 5 comprehensive test suites covering 50 unit test cases (100% passing):
1. [`NotificationStrictContractTest.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/NotificationStrictContractTest.kt) (11 tests):
   - Serialization and deserialization of notification items, pagination envelopes, unread count, mark-read responses, category mapping, and FCM token request DTOs.
2. [`DeviceTokenRepositoryTest.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/DeviceTokenRepositoryTest.kt) (9 tests):
   - Auth gating, validation rules, token store persistence, remote delegation, platform `"android"` verification, and account-switch token cache clearance.
3. [`NotificationRepositoryTest.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/NotificationRepositoryTest.kt) (8 tests):
   - Pagination, category filtering, unread count retrieval, mark read / mark all read, auth token injection, and comprehensive HTTP/network error propagation (400, 401, 403, 404, 409, 429, 500, network).
4. [`NotificationViewModelTest.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/NotificationViewModelTest.kt) (15 tests):
   - State machine verification covering initial load, empty state, errors, refresh, pagination, unread count sync, mark single read, mark all read, duplicate tap prevention, stale response protection, category filtering, session expiration, retry, and logout cleanup.
5. [`NotificationRouterTest.kt`](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/NotificationRouterTest.kt) (7 tests):
   - Role isolation, unauthenticated blocking, passenger deep link routing (`rating`, `tracking`, `payment`), driver deep link routing (`requests`, `active_trip`, `home`), and graceful fallback for unknown event types without crash.

**Full Regression Result:**
- Total tests executed: 682
- Total tests passed: 682
- Failures: 0
- Regressions: 0
