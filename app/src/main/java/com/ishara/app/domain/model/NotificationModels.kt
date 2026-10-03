package com.ishara.app.domain.model

/**
 * Status of an in-app notification.
 * Authoritative on backend: UNREAD -> READ
 */
enum class NotificationStatus {
    UNREAD,
    READ;

    companion object {
        fun fromBackend(value: String): NotificationStatus {
            return when (value.uppercase()) {
                "READ" -> READ
                else -> UNREAD
            }
        }
    }
}

/**
 * Notification delivery priority.
 */
enum class NotificationPriority {
    NORMAL,
    HIGH;

    companion object {
        fun fromBackend(value: String): NotificationPriority {
            return when (value.uppercase()) {
                "HIGH" -> HIGH
                else -> NORMAL
            }
        }
    }
}

/**
 * Notification category mapped from domain events.
 * Authoritative on backend: rideUpdates, account, system
 */
enum class NotificationCategory(val backendValue: String, val displayLabel: String) {
    RIDE_UPDATES("rideUpdates", "Ride Updates"),
    ACCOUNT("account", "Account"),
    SYSTEM("system", "System");

    companion object {
        fun fromBackend(value: String): NotificationCategory {
            return when (value.trim()) {
                "rideUpdates" -> RIDE_UPDATES
                "account" -> ACCOUNT
                "system" -> SYSTEM
                else -> SYSTEM
            }
        }
    }
}

/**
 * Authoritative in-app notification domain item.
 */
data class NotificationItem(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val sourceEventId: String,
    val aggregateType: String,
    val aggregateId: String,
    val status: NotificationStatus,
    val priority: NotificationPriority,
    val category: NotificationCategory = NotificationCategory.SYSTEM,
    val readAt: String?,
    val createdAt: String,
    val updatedAt: String
) {
    val isRead: Boolean
        get() = status == NotificationStatus.READ

    val rideId: String?
        get() = data["rideId"] ?: if (aggregateType == "Ride") aggregateId else null

    val requestId: String?
        get() = data["requestId"] ?: if (aggregateType == "RideRequest") aggregateId else null

    val paymentId: String?
        get() = data["paymentId"] ?: if (aggregateType == "Payment") aggregateId else null

    val tripId: String?
        get() = data["tripId"]
}

/**
 * Paginated list response for notifications.
 */
data class PaginatedNotifications(
    val items: List<NotificationItem>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
    val unreadCount: Int
)

/**
 * Response for push token registration.
 */
data class DeviceTokenRegistration(
    val tokenMasked: String = "",
    val platform: String = "android",
    val isActive: Boolean = true,
    val lastSeenAt: String = "",
    val success: Boolean = true,
    val registered: Boolean = true
)

