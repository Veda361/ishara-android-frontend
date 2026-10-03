package com.ishara.app.data.remote.dto

/**
 * Request payload for POST /api/v1/devices/push-token
 * Strict validation: token (required), platform ("ANDROID"|"IOS"|"WEB"), deviceId?, appVersion?
 */
data class RegisterPushTokenRequestDto(
    val token: String,
    val platform: String = "android",
    val deviceId: String? = null,
    val appVersion: String? = null
)

/**
 * Response payload for POST /api/v1/devices/push-token
 */
data class RegisterPushTokenResponseDto(
    val tokenMasked: String = "",
    val platform: String = "android",
    val isActive: Boolean = true,
    val lastSeenAt: String = "",
    val success: Boolean = true,
    val registered: Boolean = true
)

/**
 * Request payload for DELETE /api/v1/devices/push-token
 */
data class RemovePushTokenRequestDto(
    val token: String
)

/**
 * Response payload for DELETE /api/v1/devices/push-token
 */
data class RemovePushTokenResponseDto(
    val success: Boolean = true,
    val removed: Boolean = true
)

/**
 * DTO for single notification item returned by backend
 */
data class NotificationItemDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val sourceEventId: String,
    val aggregateType: String,
    val aggregateId: String,
    val status: String,
    val priority: String,
    val readAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val category: String? = null
)

/**
 * Paginated response data for GET /api/v1/notifications
 */
data class PaginatedNotificationsResponseDto(
    val items: List<NotificationItemDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
    val unreadCount: Int
)

/**
 * Response data for GET /api/v1/notifications/unread-count
 */
data class UnreadCountResponseDto(
    val unreadCount: Int
)

/**
 * Response data for POST /api/v1/notifications/read-all
 */
data class MarkAllAsReadResponseDto(
    val markedCount: Int
)
