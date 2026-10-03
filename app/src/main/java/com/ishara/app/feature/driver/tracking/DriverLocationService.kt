package com.ishara.app.feature.driver.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ishara.app.IshaaraApplication
import com.ishara.app.R
import com.ishara.app.core.common.IshaaraLogger

/**
 * Android Foreground Service adapter hosting driver location tracking during active passenger trips.
 *
 * Guarantees:
 * 1. Complies with Android 14+ (API 34+) [ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION].
 * 2. Displays persistent ongoing notification to satisfy OS foreground execution rules.
 * 3. Contains zero business logic; delegates completely to [DriverTrackingCoordinator].
 * 4. Safely releases resources and unregisters GPS callbacks upon stopping.
 */
class DriverLocationService : Service() {

    private val coordinator: DriverTrackingCoordinator by lazy {
        val app = application as IshaaraApplication
        app.appContainer.driverTrackingCoordinator
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        IshaaraLogger.i(TAG, "DriverLocationService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_TRACKING

        when (action) {
            ACTION_START_TRACKING -> {
                val tripId = intent?.getStringExtra(EXTRA_TRIP_ID)
                if (tripId.isNullOrBlank()) {
                    IshaaraLogger.w(TAG, "DriverLocationService received START without tripId; stopping.")
                    stopSelf()
                    return START_NOT_STICKY
                }

                val notification = buildForegroundNotification(tripId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                        } else {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                        }
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                coordinator.startTracking(tripId)
                isServiceRunning = true
                IshaaraLogger.i(TAG, "DriverLocationService started foreground tracking for trip: $tripId")
            }

            ACTION_STOP_TRACKING -> {
                IshaaraLogger.i(TAG, "DriverLocationService stopping tracking.")
                coordinator.stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isServiceRunning = false
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinator.stopTracking()
        isServiceRunning = false
        IshaaraLogger.i(TAG, "DriverLocationService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Driver Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays persistent status while driver live location is being transmitted."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(tripId: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ishaara Driver Tracking Active")
            .setContentText("Sharing live location for active transit trip.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val TAG = "DriverLocationService"
        const val CHANNEL_ID = "ishaara_driver_tracking_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_TRACKING = "com.ishara.app.action.START_DRIVER_TRACKING"
        const val ACTION_STOP_TRACKING = "com.ishara.app.action.STOP_DRIVER_TRACKING"
        const val EXTRA_TRIP_ID = "extra_trip_id"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        /**
         * Starts the foreground tracking service safely across all Android versions.
         */
        fun start(context: Context, tripId: String) {
            val intent = Intent(context, DriverLocationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Stops the foreground tracking service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, DriverLocationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
