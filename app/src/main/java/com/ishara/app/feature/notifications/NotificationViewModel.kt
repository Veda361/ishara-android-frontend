package com.ishara.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.notifications.NotificationRouter
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.GetNotificationsUseCase
import com.ishara.app.domain.usecase.GetUnreadNotificationCountUseCase
import com.ishara.app.domain.usecase.MarkAllNotificationsAsReadUseCase
import com.ishara.app.domain.usecase.MarkNotificationAsReadUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class NotificationViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadNotificationCountUseCase: GetUnreadNotificationCountUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val markAllNotificationsAsReadUseCase: MarkAllNotificationsAsReadUseCase,
    private val notificationRouter: NotificationRouter,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // Stale response tracking
    private val requestCounter = AtomicLong(0)

    init {
        loadInitial()
    }

    fun loadInitial() {
        val requestId = requestCounter.incrementAndGet()
        _uiState.update { it.copy(isLoading = true, errorMessage = null, isSessionExpired = false) }
        viewModelScope.launch(dispatchers.main) {
            refreshUnreadCountInternal()
            fetchNotificationsInternal(page = 1, isRefresh = false, requestId = requestId)
        }
    }

    fun onRefresh() {
        if (_uiState.value.isRefreshing) return
        val requestId = requestCounter.incrementAndGet()
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null, isSessionExpired = false) }
        viewModelScope.launch(dispatchers.main) {
            refreshUnreadCountInternal()
            fetchNotificationsInternal(page = 1, isRefresh = true, requestId = requestId)
        }
    }

    fun onRetry() {
        loadInitial()
    }

    fun onFilterTabSelected(tab: NotificationFilterTab) {
        if (_uiState.value.selectedTab == tab) return
        val requestId = requestCounter.incrementAndGet()
        _uiState.update { it.copy(selectedTab = tab, isLoading = true, currentPage = 1, errorMessage = null) }
        viewModelScope.launch(dispatchers.main) {
            fetchNotificationsInternal(page = 1, isRefresh = false, requestId = requestId)
        }
    }

    fun onCategorySelected(category: NotificationCategory?) {
        if (_uiState.value.selectedCategory == category) return
        val requestId = requestCounter.incrementAndGet()
        _uiState.update { it.copy(selectedCategory = category, isLoading = true, currentPage = 1, errorMessage = null) }
        viewModelScope.launch(dispatchers.main) {
            fetchNotificationsInternal(page = 1, isRefresh = false, requestId = requestId)
        }
    }

    fun onLoadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMore || current.isLoading || current.isRefreshing) return

        val nextPage = current.currentPage + 1
        val requestId = requestCounter.get()
        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        viewModelScope.launch(dispatchers.main) {
            val status = if (current.selectedTab == NotificationFilterTab.UNREAD) NotificationStatus.UNREAD else null
            when (val result = getNotificationsUseCase(page = nextPage, limit = 20, category = current.selectedCategory, status = status)) {
                is IshaaraResult.Success -> {
                    if (requestCounter.get() != requestId) return@launch
                    _uiState.update { state ->
                        state.copy(
                            items = state.items + result.data.items,
                            currentPage = result.data.page,
                            hasMore = result.data.hasMore,
                            isLoadingMore = false,
                            unreadCount = result.data.unreadCount
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    if (requestCounter.get() != requestId) return@launch
                    val isExpired = result.error is IshaaraError.Authentication
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            isSessionExpired = isExpired,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun onNotificationClicked(item: NotificationItem, currentRole: UserRole) {
        if (item.status == NotificationStatus.UNREAD) {
            onMarkAsRead(item)
        }

        notificationRouter.routeNotification(
            type = item.type,
            data = item.data,
            authenticatedRole = currentRole
        )
    }

    fun onMarkAsRead(item: NotificationItem) {
        if (item.status == NotificationStatus.READ || _uiState.value.markingReadId == item.id) return

        _uiState.update { it.copy(markingReadId = item.id) }
        viewModelScope.launch(dispatchers.main) {
            when (val result = markNotificationAsReadUseCase(item.id)) {
                is IshaaraResult.Success -> {
                    _uiState.update { state ->
                        val updatedItems = state.items.map {
                            if (it.id == item.id) it.copy(status = NotificationStatus.READ, readAt = result.data.readAt)
                            else it
                        }
                        val newUnread = (state.unreadCount - 1).coerceAtLeast(0)
                        state.copy(
                            items = updatedItems,
                            unreadCount = newUnread,
                            markingReadId = null
                        )
                    }
                    refreshUnreadCountInternal()
                }
                is IshaaraResult.Failure -> {
                    val isExpired = result.error is IshaaraError.Authentication
                    _uiState.update {
                        it.copy(
                            markingReadId = null,
                            isSessionExpired = isExpired,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun onMarkAllAsRead() {
        if (_uiState.value.isMarkingAllRead || _uiState.value.unreadCount == 0) return

        _uiState.update { it.copy(isMarkingAllRead = true) }
        viewModelScope.launch(dispatchers.main) {
            when (val result = markAllNotificationsAsReadUseCase()) {
                is IshaaraResult.Success -> {
                    _uiState.update { state ->
                        val readItems = state.items.map { it.copy(status = NotificationStatus.READ) }
                        state.copy(
                            items = readItems,
                            unreadCount = 0,
                            isMarkingAllRead = false
                        )
                    }
                    refreshUnreadCountInternal()
                }
                is IshaaraResult.Failure -> {
                    val isExpired = result.error is IshaaraError.Authentication
                    _uiState.update {
                        it.copy(
                            isMarkingAllRead = false,
                            isSessionExpired = isExpired,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onNavigateBack() {
        navigationManager.navigateUp()
    }

    fun onLogoutCleanup() {
        requestCounter.incrementAndGet()
        _uiState.value = NotificationUiState(isLoading = false)
    }

    private suspend fun fetchNotificationsInternal(page: Int, isRefresh: Boolean, requestId: Long) {
        val currentTab = _uiState.value.selectedTab
        val currentCategory = _uiState.value.selectedCategory
        val status = if (currentTab == NotificationFilterTab.UNREAD) NotificationStatus.UNREAD else null

        when (val result = getNotificationsUseCase(page = page, limit = 20, category = currentCategory, status = status)) {
            is IshaaraResult.Success -> {
                if (requestCounter.get() != requestId) return
                _uiState.update { state ->
                    state.copy(
                        items = result.data.items,
                        currentPage = result.data.page,
                        hasMore = result.data.hasMore,
                        unreadCount = result.data.unreadCount,
                        isLoading = false,
                        isRefreshing = false,
                        isSessionExpired = false
                    )
                }
            }
            is IshaaraResult.Failure -> {
                if (requestCounter.get() != requestId) return
                val isExpired = result.error is IshaaraError.Authentication
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSessionExpired = isExpired,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private suspend fun refreshUnreadCountInternal() {
        when (val result = getUnreadNotificationCountUseCase()) {
            is IshaaraResult.Success -> {
                _uiState.update { it.copy(unreadCount = result.data) }
            }
            is IshaaraResult.Failure -> {
                if (result.error is IshaaraError.Authentication) {
                    _uiState.update { it.copy(isSessionExpired = true) }
                }
            }
        }
    }
}
