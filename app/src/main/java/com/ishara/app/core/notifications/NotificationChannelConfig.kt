package com.ishara.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * System notification channel identifiers, configuration, and importance levels for Android 8.0+ (API 26+).
 * Directly maps to backend notification categories:
 * - rideUpdates -> CHANNEL_RIDE_UPDATES_ID
 * - rideRequests -> CHANNEL_RIDE_REQUESTS_ID
 * - safetyAlerts -> CHANNEL_SAFETY_SOS_ID
 * - systemNotifications -> CHANNEL_SYSTEM_ID
 */
object NotificationChannelConfig {
    const val CHANNEL_RIDE_UPDATES_ID = "ishara_ride_updates"
    const val CHANNEL_RIDE_UPDATES_NAME = "Ride & Trip Updates"

    const val CHANNEL_RIDE_REQUESTS_ID = "ishara_ride_requests"
    const val CHANNEL_RIDE_REQUESTS_NAME = "Ride Requests"

    const val CHANNEL_DRIVER_TRACKING_ID = "ishara_driver_tracking"
    const val CHANNEL_DRIVER_TRACKING_NAME = "Driver Route Tracking Service"

    const val CHANNEL_SAFETY_SOS_ID = "ishara_safety_sos"
    const val CHANNEL_SAFETY_SOS_NAME = "Safety & Emergency Alerts"

    const val CHANNEL_SYSTEM_ID = "ishara_system_updates"
    const val CHANNEL_SYSTEM_NAME = "Payments & System Notices"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val rideUpdatesChannel = NotificationChannel(
                CHANNEL_RIDE_UPDATES_ID,
                CHANNEL_RIDE_UPDATES_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates regarding your ongoing or scheduled transit trips."
            }

            val rideRequestsChannel = NotificationChannel(
                CHANNEL_RIDE_REQUESTS_ID,
                CHANNEL_RIDE_REQUESTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming passenger ride requests and confirmations."
            }

            val driverTrackingChannel = NotificationChannel(
                CHANNEL_DRIVER_TRACKING_ID,
                CHANNEL_DRIVER_TRACKING_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for active GPS telemetry."
            }

            val safetySosChannel = NotificationChannel(
                CHANNEL_SAFETY_SOS_ID,
                CHANNEL_SAFETY_SOS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority safety and emergency incident dispatches."
                enableVibration(true)
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM_ID,
                CHANNEL_SYSTEM_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Transaction updates, payment confirmations, and system notices."
            }

            notificationManager.createNotificationChannels(
                listOf(
                    rideUpdatesChannel,
                    rideRequestsChannel,
                    driverTrackingChannel,
                    safetySosChannel,
                    systemChannel
                )
            )
        }
    }
}
