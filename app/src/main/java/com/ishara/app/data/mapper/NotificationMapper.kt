package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.NotificationItemDto
import com.ishara.app.data.remote.dto.PaginatedNotificationsResponseDto
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.model.NotificationPriority
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.PaginatedNotifications

object NotificationMapper {

    fun toDomain(dto: NotificationItemDto): NotificationItem {
        return NotificationItem(
            id = dto.id,
            userId = dto.userId,
            type = dto.type,
            title = dto.title,
            body = dto.body,
            data = dto.data,
            sourceEventId = dto.sourceEventId,
            aggregateType = dto.aggregateType,
            aggregateId = dto.aggregateId,
            status = NotificationStatus.fromBackend(dto.status),
            priority = NotificationPriority.fromBackend(dto.priority),
            category = NotificationCategory.fromBackend(dto.category ?: ""),
            readAt = dto.readAt,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toDomain(dto: PaginatedNotificationsResponseDto): PaginatedNotifications {
        return PaginatedNotifications(
            items = dto.items.map { toDomain(it) },
            total = dto.total,
            page = dto.page,
            limit = dto.limit,
            hasMore = dto.hasMore,
            unreadCount = dto.unreadCount
        )
    }
}
