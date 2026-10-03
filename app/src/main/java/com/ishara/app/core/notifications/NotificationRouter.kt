package com.ishara.app.core.notifications

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.domain.model.UserRole
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager

/**
 * Authoritative notification routing engine.
 * Enforces strict deep link security and role isolation:
 * - Rejects any navigation if the user is unauthenticated
 * - Rejects passenger destinations when the active role is DRIVER_CONDUCTOR
 * - Rejects driver destinations when the active role is USER
 * - Validates entity identifiers (rideId, requestId) before constructing routes
 */
class NotificationRouter(
    private val navigationManager: NavigationManager
) {
    private val tag = "NotificationRouter"

    /**
     * Resolves and executes a secure navigation command based on the notification payload and caller role.
     * Returns true if route was safely dispatched; false if rejected due to missing auth, role mismatch, or invalid entity.
     */
    fun routeNotification(
        type: String,
        data: Map<String, String>,
        authenticatedRole: UserRole?
    ): Boolean {
        if (authenticatedRole == null) {
            IshaaraLogger.w(tag, "Blocked navigation: user is unauthenticated")
            return false
        }

        val targetRoute = resolveRoute(type, data, authenticatedRole)
        if (targetRoute == null) {
            IshaaraLogger.w(tag, "Blocked navigation: type=$type incompatible with role=$authenticatedRole or invalid entity")
            return false
        }

        IshaaraLogger.i(tag, "Dispatched secure notification route: $targetRoute for role: $authenticatedRole")
        navigationManager.navigate(targetRoute)
        return true
    }

    /**
     * Resolves target route string. Returns null if role validation fails or required entity IDs are missing.
     */
    fun resolveRoute(
        type: String,
        data: Map<String, String>,
        authenticatedRole: UserRole
    ): String? {
        val rideId = data["rideId"]?.trim()?.ifBlank { null }
        val requestId = data["requestId"]?.trim()?.ifBlank { null }
        val paymentId = data["paymentId"]?.trim()?.ifBlank { null }

        return when (authenticatedRole) {
            UserRole.USER, UserRole.ADMIN -> resolvePassengerRoute(type, rideId, requestId, paymentId)
            UserRole.DRIVER_CONDUCTOR -> resolveDriverRoute(type, rideId, requestId, paymentId)
        }
    }

    private fun resolvePassengerRoute(
        type: String,
        rideId: String?,
        requestId: String?,
        paymentId: String?
    ): String? {
        return when (type) {
            "RIDE_REQUEST_CREATED", "RIDE_REQUEST_CANCELLED", "SETTLEMENT_PROCESSED" -> {
                // Strictly driver events; reject for passenger
                null
            }
            "RIDE_COMPLETED" -> {
                if (rideId != null) "student/ride/$rideId/rating" else "student/notifications"
            }
            "RIDE_DRIVER_ARRIVING", "RIDE_PICKED_UP", "RIDE_STARTED" -> {
                if (rideId != null) "student/ride/$rideId" else "student/notifications"
            }
            "RIDE_REQUEST_ACCEPTED" -> {
                if (rideId != null) "student/ride/$rideId"
                else if (requestId != null) "student/ride/request/status/$requestId"
                else "student/notifications"
            }
            "RIDE_REQUEST_REJECTED", "RIDE_REQUEST_EXPIRED" -> {
                if (requestId != null) "student/ride/request/status/$requestId" else "student/home"
            }
            "PAYMENT_CAPTURED" -> {
                if (rideId != null) "student/ride/$rideId/payment" else "student/notifications"
            }
            "REFUND_PROCESSED" -> {
                "student/notifications"
            }
            "RIDE_CANCELLED" -> {
                if (rideId != null) "student/ride/$rideId" else "student/home"
            }
            else -> {
                "student/notifications"
            }
        }
    }

    private fun resolveDriverRoute(
        type: String,
        rideId: String?,
        requestId: String?,
        paymentId: String?
    ): String? {
        return when (type) {
            "RIDE_REQUEST_REJECTED", "RIDE_REQUEST_EXPIRED", "REFUND_PROCESSED" -> {
                // Strictly passenger events; reject for driver
                null
            }
            "RIDE_REQUEST_CREATED", "RIDE_REQUEST_CANCELLED" -> {
                "driver/requests"
            }
            "RIDE_DRIVER_ARRIVING", "RIDE_PICKED_UP", "RIDE_STARTED" -> {
                if (rideId != null) "driver/active_trip" else "driver/home"
            }
            "RIDE_COMPLETED" -> {
                "driver/home"
            }
            "PAYMENT_CAPTURED", "SETTLEMENT_PROCESSED" -> {
                "driver/home"
            }
            "RIDE_CANCELLED" -> {
                "driver/home"
            }
            else -> {
                "driver/notifications"
            }
        }
    }
}
