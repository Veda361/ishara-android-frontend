package com.ishara.app.core.network

import com.ishara.app.core.common.IshaaraLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Centralized coordinator for HTTP 401 Unauthorized responses and session invalidation.
 *
 * Architectural Guarantees:
 * 1. Coalesces concurrent 401 responses from parallel network calls into a SINGLE invalidation event.
 * 2. Debounces rapid successive 401 triggers (default window: 2000ms) to prevent infinite loops.
 * 3. Notifies subscribers (AuthRepository, ViewModels) without UI freezes.
 */
class SessionInvalidationCoordinator(
    private val debounceWindowMillis: Long = 2000L
) {
    private val _invalidationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val invalidationEvents: SharedFlow<String> = _invalidationEvents.asSharedFlow()

    private val lastInvalidatedTime = AtomicLong(0L)

    /**
     * Reports an HTTP 401 Unauthorized response from any network request.
     * @param reason Human-friendly explanation of the invalidation
     * @return true if this call triggered a new invalidation event, false if coalesced/debounced
     */
    fun notifyUnauthorized(reason: String = "Your session has expired. Please sign in again."): Boolean {
        val now = System.currentTimeMillis()
        val last = lastInvalidatedTime.get()

        if (now - last < debounceWindowMillis) {
            IshaaraLogger.d(TAG, "Concurrent/subsequent 401 coalesced; ignoring redundant invalidation event.")
            return false
        }

        if (lastInvalidatedTime.compareAndSet(last, now)) {
            IshaaraLogger.w(TAG, "Session invalidation triggered: $reason")
            _invalidationEvents.tryEmit(reason)
            return true
        }

        return false
    }

    /**
     * Resets the debounce timestamp (e.g. upon new successful authentication).
     */
    fun reset() {
        lastInvalidatedTime.set(0L)
    }

    companion object {
        private const val TAG = "SessionCoordinator"
    }
}
