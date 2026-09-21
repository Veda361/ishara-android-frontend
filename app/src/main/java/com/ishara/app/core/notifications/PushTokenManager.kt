package com.ishara.app.core.notifications

import com.ishara.app.core.result.IshaaraResult

/**
 * Boundary abstraction for FCM device token registration and un-registration.
 */
interface PushTokenManager {
    suspend fun getDeviceToken(): String?
    suspend fun registerDeviceToken(token: String): IshaaraResult<Unit>
    suspend fun unregisterDeviceToken(token: String): IshaaraResult<Unit>
}
