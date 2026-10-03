package com.ishara.app.feature.notifications

import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem

enum class NotificationFilterTab {
    ALL,
    UNREAD
}

data class NotificationUiState(
    val items: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val selectedTab: NotificationFilterTab = NotificationFilterTab.ALL,
    val selectedCategory: NotificationCategory? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val markingReadId: String? = null,
    val isMarkingAllRead: Boolean = false,
    val isSessionExpired: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && !isRefreshing && items.isEmpty()
}
