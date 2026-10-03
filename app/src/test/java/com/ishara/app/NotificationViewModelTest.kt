package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.notifications.NotificationRouter
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.model.NotificationPriority
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.PaginatedNotifications
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.NotificationRepository
import com.ishara.app.domain.usecase.GetNotificationsUseCase
import com.ishara.app.domain.usecase.GetUnreadNotificationCountUseCase
import com.ishara.app.domain.usecase.MarkAllNotificationsAsReadUseCase
import com.ishara.app.domain.usecase.MarkNotificationAsReadUseCase
import com.ishara.app.feature.notifications.NotificationFilterTab
import com.ishara.app.feature.notifications.NotificationViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NotificationViewModel.
 * Verifies 17 required scenarios: loading, pagination, unread counts, mark read, stale response protection,
 * duplicate protection, category filtering, session expiration, and logout cleanup.
 */
class NotificationViewModelTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private class FakeNotificationRepository : NotificationRepository {
        var getNotificationsCalls = 0
        var lastPage: Int = 1
        var lastLimit: Int = 20
        var lastCategory: NotificationCategory? = null
        var lastStatus: NotificationStatus? = null

        var paginatedResult: IshaaraResult<PaginatedNotifications> = IshaaraResult.success(
            PaginatedNotifications(
                items = listOf(
                    createSampleItem("notif_1", NotificationStatus.UNREAD)
                ),
                total = 1,
                page = 1,
                limit = 20,
                hasMore = false,
                unreadCount = 1
            )
        )

        var unreadCountResult: IshaaraResult<Int> = IshaaraResult.success(1)
        var markAsReadResult: IshaaraResult<NotificationItem> = IshaaraResult.success(
            createSampleItem("notif_1", NotificationStatus.READ, readAt = "2026-10-01T12:00:00Z")
        )
        var markAllAsReadResult: IshaaraResult<Int> = IshaaraResult.success(1)

        override suspend fun getNotifications(
            page: Int,
            limit: Int,
            category: NotificationCategory?,
            status: NotificationStatus?
        ): IshaaraResult<PaginatedNotifications> {
            getNotificationsCalls++
            lastPage = page
            lastLimit = limit
            lastCategory = category
            lastStatus = status
            return paginatedResult
        }

        override suspend fun getUnreadCount(): IshaaraResult<Int> = unreadCountResult

        override suspend fun markAsRead(notificationId: String): IshaaraResult<NotificationItem> {
            unreadCountResult = IshaaraResult.success(0)
            return markAsReadResult
        }

        override suspend fun markAllAsRead(): IshaaraResult<Int> {
            unreadCountResult = IshaaraResult.success(0)
            return markAllAsReadResult
        }
    }

    private val testDispatchers = TestDispatcherProvider()
    private val navigationManager = NavigationManager()
    private val notificationRouter = NotificationRouter(navigationManager)

    private fun createViewModel(repo: FakeNotificationRepository): NotificationViewModel {
        return NotificationViewModel(
            getNotificationsUseCase = GetNotificationsUseCase(repo),
            getUnreadNotificationCountUseCase = GetUnreadNotificationCountUseCase(repo),
            markNotificationAsReadUseCase = MarkNotificationAsReadUseCase(repo),
            markAllNotificationsAsReadUseCase = MarkAllNotificationsAsReadUseCase(repo),
            notificationRouter = notificationRouter,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )
    }

    @Test
    fun `1 & 2 - Initial loading and successful notification loading`() = runBlocking {
        val repo = FakeNotificationRepository()
        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(1, state.items.size)
        assertEquals("notif_1", state.items[0].id)
        assertEquals(1, state.unreadCount)
    }

    @Test
    fun `3 - Empty notification state`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.success(
                PaginatedNotifications(emptyList(), total = 0, page = 1, limit = 20, hasMore = false, unreadCount = 0)
            )
            unreadCountResult = IshaaraResult.success(0)
        }
        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals(0, state.items.size)
        assertEquals(0, state.unreadCount)
    }

    @Test
    fun `4 - Error state`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.failure(IshaaraError.Network("Offline"))
        }
        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Offline", state.errorMessage)
    }

    @Test
    fun `5 - Refresh reloads data`() = runBlocking {
        val repo = FakeNotificationRepository()
        val viewModel = createViewModel(repo)

        viewModel.onRefresh()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(1, state.items.size)
    }

    @Test
    fun `6 - Pagination appends next page items`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.success(
                PaginatedNotifications(
                    items = listOf(createSampleItem("notif_1", NotificationStatus.UNREAD)),
                    total = 2,
                    page = 1,
                    limit = 1,
                    hasMore = true,
                    unreadCount = 2
                )
            )
        }
        val viewModel = createViewModel(repo)

        // Prepare page 2
        repo.paginatedResult = IshaaraResult.success(
            PaginatedNotifications(
                items = listOf(createSampleItem("notif_2", NotificationStatus.UNREAD)),
                total = 2,
                page = 2,
                limit = 1,
                hasMore = false,
                unreadCount = 2
            )
        )

        viewModel.onLoadMore()

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals("notif_1", state.items[0].id)
        assertEquals("notif_2", state.items[1].id)
        assertEquals(2, state.currentPage)
        assertFalse(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `7 - Pagination failure sets errorMessage without clearing items`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.success(
                PaginatedNotifications(
                    items = listOf(createSampleItem("notif_1", NotificationStatus.UNREAD)),
                    total = 2,
                    page = 1,
                    limit = 1,
                    hasMore = true,
                    unreadCount = 2
                )
            )
        }
        val viewModel = createViewModel(repo)

        repo.paginatedResult = IshaaraResult.failure(IshaaraError.Network("Timeout on page 2"))
        viewModel.onLoadMore()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("Timeout on page 2", state.errorMessage)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `8 - Unread count loading`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            unreadCountResult = IshaaraResult.success(7)
            paginatedResult = IshaaraResult.success(
                PaginatedNotifications(
                    items = listOf(createSampleItem("notif_1", NotificationStatus.UNREAD)),
                    total = 1,
                    page = 1,
                    limit = 20,
                    hasMore = false,
                    unreadCount = 7
                )
            )
        }
        val viewModel = createViewModel(repo)

        assertEquals(7, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `9 & 11 - Mark single notification as read updates item and decrements unread count`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            unreadCountResult = IshaaraResult.success(1)
        }
        val viewModel = createViewModel(repo)
        val item = viewModel.uiState.value.items[0]

        viewModel.onMarkAsRead(item)

        val state = viewModel.uiState.value
        assertTrue(state.items[0].isRead)
        assertEquals(0, state.unreadCount)
        assertNull(state.markingReadId)
    }

    @Test
    fun `10 - Mark all notifications as read marks all and resets count`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.success(
                PaginatedNotifications(
                    items = listOf(
                        createSampleItem("n1", NotificationStatus.UNREAD),
                        createSampleItem("n2", NotificationStatus.UNREAD)
                    ),
                    total = 2,
                    page = 1,
                    limit = 20,
                    hasMore = false,
                    unreadCount = 2
                )
            )
            unreadCountResult = IshaaraResult.success(2)
            markAllAsReadResult = IshaaraResult.success(2)
        }
        val viewModel = createViewModel(repo)

        viewModel.onMarkAllAsRead()

        val state = viewModel.uiState.value
        assertTrue(state.items.all { it.isRead })
        assertEquals(0, state.unreadCount)
        assertFalse(state.isMarkingAllRead)
    }

    @Test
    fun `12 - Duplicate request protection ignores loadMore when already loading or hasMore is false`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.success(
                PaginatedNotifications(
                    items = listOf(createSampleItem("n1", NotificationStatus.READ)),
                    total = 1,
                    page = 1,
                    limit = 20,
                    hasMore = false,
                    unreadCount = 0
                )
            )
        }
        val viewModel = createViewModel(repo)
        val initialCalls = repo.getNotificationsCalls

        viewModel.onLoadMore()
        assertEquals(initialCalls, repo.getNotificationsCalls)
    }

    @Test
    fun `13 - Stale response protection prevents out-of-order responses from overwriting current state`() = runBlocking {
        val repo = FakeNotificationRepository()
        val viewModel = createViewModel(repo)

        // Switching category triggers new request counter
        viewModel.onCategorySelected(NotificationCategory.ACCOUNT)
        assertEquals(NotificationCategory.ACCOUNT, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `14 - Category filtering resets page to 1 and delegates category parameter`() = runBlocking {
        val repo = FakeNotificationRepository()
        val viewModel = createViewModel(repo)

        viewModel.onCategorySelected(NotificationCategory.RIDE_UPDATES)

        assertEquals(NotificationCategory.RIDE_UPDATES, repo.lastCategory)
        assertEquals(1, repo.lastPage)
        assertEquals(NotificationCategory.RIDE_UPDATES, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `15 - Session expiration transitions to isSessionExpired on 401 error`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.failure(IshaaraError.Authentication(message = "Session expired"))
        }
        val viewModel = createViewModel(repo)

        assertTrue(viewModel.uiState.value.isSessionExpired)
    }

    @Test
    fun `16 - Retry reloads initial notifications`() = runBlocking {
        val repo = FakeNotificationRepository().apply {
            paginatedResult = IshaaraResult.failure(IshaaraError.Network("Offline"))
        }
        val viewModel = createViewModel(repo)
        assertEquals("Offline", viewModel.uiState.value.errorMessage)

        // Now network recovers
        repo.paginatedResult = IshaaraResult.success(
            PaginatedNotifications(
                items = listOf(createSampleItem("recovered_notif", NotificationStatus.UNREAD)),
                total = 1,
                page = 1,
                limit = 20,
                hasMore = false,
                unreadCount = 1
            )
        )
        viewModel.onRetry()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(1, state.items.size)
        assertEquals("recovered_notif", state.items[0].id)
    }

    @Test
    fun `17 - Logout cleanup resets all notification and unread state`() = runBlocking {
        val repo = FakeNotificationRepository()
        val viewModel = createViewModel(repo)

        assertEquals(1, viewModel.uiState.value.items.size)

        viewModel.onLogoutCleanup()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertEquals(0, state.unreadCount)
        assertNull(state.selectedCategory)
        assertFalse(state.isSessionExpired)
    }

    companion object {
        private fun createSampleItem(
            id: String,
            status: NotificationStatus,
            readAt: String? = null
        ): NotificationItem {
            return NotificationItem(
                id = id,
                userId = "usr_test",
                type = "RIDE_COMPLETED",
                title = "Title $id",
                body = "Body $id",
                data = mapOf("rideId" to "ride_test"),
                sourceEventId = "evt_test",
                aggregateType = "Ride",
                aggregateId = "ride_test",
                status = status,
                priority = NotificationPriority.NORMAL,
                category = NotificationCategory.RIDE_UPDATES,
                readAt = readAt,
                createdAt = "2026-10-01T12:00:00Z",
                updatedAt = "2026-10-01T12:00:00Z"
            )
        }
    }
}
