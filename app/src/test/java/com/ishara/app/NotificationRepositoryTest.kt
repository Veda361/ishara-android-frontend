package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.remote.datasource.NotificationRemoteDataSource
import com.ishara.app.data.remote.dto.MarkAllAsReadResponseDto
import com.ishara.app.data.remote.dto.NotificationItemDto
import com.ishara.app.data.remote.dto.PaginatedNotificationsResponseDto
import com.ishara.app.data.remote.dto.UnreadCountResponseDto
import com.ishara.app.data.repository.NotificationRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NotificationRepositoryImpl.
 * Verifies pagination, category filtering, unread count, read operations, auth gating, and error propagation.
 */
class NotificationRepositoryTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private class FakeNotificationRemoteDataSource : NotificationRemoteDataSource {
        var lastPage: Int? = null
        var lastLimit: Int? = null
        var lastCategory: String? = null
        var lastStatus: String? = null
        var lastToken: String? = null

        var getNotificationsResult: IshaaraResult<PaginatedNotificationsResponseDto> = IshaaraResult.success(
            PaginatedNotificationsResponseDto(
                items = listOf(
                    NotificationItemDto(
                        id = "notif_100",
                        userId = "usr_1",
                        type = "RIDE_COMPLETED",
                        title = "Ride Finished",
                        body = "You have reached your destination.",
                        data = mapOf("rideId" to "ride_555"),
                        sourceEventId = "evt_1",
                        aggregateType = "Ride",
                        aggregateId = "ride_555",
                        status = "UNREAD",
                        priority = "NORMAL",
                        readAt = null,
                        createdAt = "2026-10-01T10:00:00Z",
                        updatedAt = "2026-10-01T10:00:00Z",
                        category = "rideUpdates"
                    )
                ),
                total = 1,
                page = 1,
                limit = 20,
                hasMore = false,
                unreadCount = 1
            )
        )

        var getUnreadCountResult: IshaaraResult<UnreadCountResponseDto> = IshaaraResult.success(
            UnreadCountResponseDto(unreadCount = 4)
        )

        var markAsReadId: String? = null
        var markAsReadResult: IshaaraResult<NotificationItemDto> = IshaaraResult.success(
            NotificationItemDto(
                id = "notif_100",
                userId = "usr_1",
                type = "RIDE_COMPLETED",
                title = "Ride Finished",
                body = "You have reached your destination.",
                data = mapOf("rideId" to "ride_555"),
                sourceEventId = "evt_1",
                aggregateType = "Ride",
                aggregateId = "ride_555",
                status = "READ",
                priority = "NORMAL",
                readAt = "2026-10-01T10:05:00Z",
                createdAt = "2026-10-01T10:00:00Z",
                updatedAt = "2026-10-01T10:05:00Z",
                category = "rideUpdates"
            )
        )

        var markAllAsReadResult: IshaaraResult<MarkAllAsReadResponseDto> = IshaaraResult.success(
            MarkAllAsReadResponseDto(markedCount = 3)
        )

        override suspend fun getNotifications(
            page: Int,
            limit: Int,
            category: String?,
            status: String?,
            token: String?
        ): IshaaraResult<PaginatedNotificationsResponseDto> {
            lastPage = page
            lastLimit = limit
            lastCategory = category
            lastStatus = status
            lastToken = token
            return getNotificationsResult
        }

        override suspend fun getUnreadCount(token: String?): IshaaraResult<UnreadCountResponseDto> {
            lastToken = token
            return getUnreadCountResult
        }

        override suspend fun markAsRead(notificationId: String, token: String?): IshaaraResult<NotificationItemDto> {
            markAsReadId = notificationId
            lastToken = token
            return markAsReadResult
        }

        override suspend fun markAllAsRead(token: String?): IshaaraResult<MarkAllAsReadResponseDto> {
            lastToken = token
            return markAllAsReadResult
        }
    }

    private val testDispatchers = TestDispatcherProvider()

    @Test
    fun `getNotifications fails with Authentication error when not signed in`() = runBlocking {
        val remoteDataSource = FakeNotificationRemoteDataSource()
        val sessionStore = InMemorySessionStore(null)
        val repository = NotificationRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)

        val result = repository.getNotifications()

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun `getNotifications delegates query params and maps domain objects`() = runBlocking {
        val remoteDataSource = FakeNotificationRemoteDataSource()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token_123", userId = "u1", role = UserRole.USER)
        )
        val repository = NotificationRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)

        val result = repository.getNotifications(
            page = 2,
            limit = 15,
            category = NotificationCategory.RIDE_UPDATES,
            status = NotificationStatus.UNREAD
        )

        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertEquals(2, remoteDataSource.lastPage)
        assertEquals(15, remoteDataSource.lastLimit)
        assertEquals("rideUpdates", remoteDataSource.lastCategory)
        assertEquals("UNREAD", remoteDataSource.lastStatus)
        assertEquals("session_token_123", remoteDataSource.lastToken)

        assertEquals(1, data.items.size)
        assertEquals("notif_100", data.items[0].id)
        assertEquals(NotificationCategory.RIDE_UPDATES, data.items[0].category)
        assertFalse(data.items[0].isRead)
    }

    @Test
    fun `getUnreadCount returns count for authenticated user`() = runBlocking {
        val remoteDataSource = FakeNotificationRemoteDataSource()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token_123", userId = "u1", role = UserRole.USER)
        )
        val repository = NotificationRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)

        val result = repository.getUnreadCount()

        assertTrue(result is IshaaraResult.Success)
        assertEquals(4, (result as IshaaraResult.Success).data)
    }

    @Test
    fun `getUnreadCount fails when unauthenticated`() = runBlocking {
        val repository = NotificationRepositoryImpl(
            FakeNotificationRemoteDataSource(),
            InMemorySessionStore(null),
            testDispatchers
        )

        val result = repository.getUnreadCount()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun `markAsRead rejects blank ID with validation error`() = runBlocking {
        val repository = NotificationRepositoryImpl(
            FakeNotificationRemoteDataSource(),
            InMemorySessionStore(AuthSession(token = "t", userId = "u", role = UserRole.USER)),
            testDispatchers
        )

        val result = repository.markAsRead("   ")
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Validation)
    }

    @Test
    fun `markAsRead marks notification and returns updated domain item`() = runBlocking {
        val remoteDataSource = FakeNotificationRemoteDataSource()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token_123", userId = "u1", role = UserRole.USER)
        )
        val repository = NotificationRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)

        val result = repository.markAsRead("notif_100")

        assertTrue(result is IshaaraResult.Success)
        val item = (result as IshaaraResult.Success).data
        assertEquals("notif_100", remoteDataSource.markAsReadId)
        assertEquals(NotificationStatus.READ, item.status)
        assertTrue(item.isRead)
        assertEquals("2026-10-01T10:05:00Z", item.readAt)
    }

    @Test
    fun `markAllAsRead marks all notifications and returns marked count`() = runBlocking {
        val remoteDataSource = FakeNotificationRemoteDataSource()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token_123", userId = "u1", role = UserRole.USER)
        )
        val repository = NotificationRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)

        val result = repository.markAllAsRead()

        assertTrue(result is IshaaraResult.Success)
        assertEquals(3, (result as IshaaraResult.Success).data)
    }

    @Test
    fun `repository propagates 400, 401, 403, 404, 409, 429, 500 and network errors`() = runBlocking {
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "valid_tok", userId = "u", role = UserRole.USER)
        )

        val errorCases = listOf(
            IshaaraError.Validation("id", "Invalid notification id"),
            IshaaraError.Authentication(message = "Session expired"),
            IshaaraError.Forbidden("Access denied"),
            IshaaraError.NotFound("Notification not found"),
            IshaaraError.Conflict("Already marked"),
            IshaaraError.RateLimited(message = "Too many requests"),
            IshaaraError.Server(code = 500, message = "Internal error"),
            IshaaraError.Network("DNS error")
        )

        for (expectedError in errorCases) {
            val remote = FakeNotificationRemoteDataSource().apply {
                getNotificationsResult = IshaaraResult.failure(expectedError)
            }
            val repo = NotificationRepositoryImpl(remote, sessionStore, testDispatchers)
            val result = repo.getNotifications()

            assertTrue(result is IshaaraResult.Failure)
            val err = (result as IshaaraResult.Failure).error
            assertEquals(expectedError.message, err.message)
        }
    }
}
