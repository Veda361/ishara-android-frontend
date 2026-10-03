package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.PaginatedNotifications

/**
 * Repository contract for managing user notifications.
 * Endpoints:
 * - GET  /api/v1/notifications
 * - GET  /api/v1/notifications/unread-count
 * - POST /api/v1/notifications/:notificationId/read
 * - POST /api/v1/notifications/read-all
 */
interface NotificationRepository {

    suspend fun getNotifications(
        page: Int = 1,
        limit: Int = 20,
        category: NotificationCategory? = null,
        status: NotificationStatus? = null
    ): IshaaraResult<PaginatedNotifications>

    suspend fun getUnreadCount(): IshaaraResult<Int>

    suspend fun markAsRead(
        notificationId: String
    ): IshaaraResult<NotificationItem>

    suspend fun markAllAsRead(): IshaaraResult<Int>
}
