package com.ishara.app.core.notifications

/**
 * System notification channel identifiers and importance levels for Android 8.0+ (API 26+).
 */
object NotificationChannelConfig {
    const val CHANNEL_RIDE_UPDATES_ID = "ishara_ride_updates"
    const val CHANNEL_RIDE_UPDATES_NAME = "Ride & Trip Updates"

    const val CHANNEL_DRIVER_TRACKING_ID = "ishara_driver_tracking"
    const val CHANNEL_DRIVER_TRACKING_NAME = "Driver Route Tracking Service"

    const val CHANNEL_SAFETY_SOS_ID = "ishara_safety_sos"
    const val CHANNEL_SAFETY_SOS_NAME = "Safety & Emergency Alerts"
}
