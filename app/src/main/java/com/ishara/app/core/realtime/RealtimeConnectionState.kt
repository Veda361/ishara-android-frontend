package com.ishara.app.core.realtime

import com.ishara.app.core.result.IshaaraError

/**
 * Lifecycle states for WebSocket streams.
 */
sealed interface RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState
    object Connecting : RealtimeConnectionState
    object Connected : RealtimeConnectionState
    data class Reconnecting(val attempt: Int, val delayMillis: Long) : RealtimeConnectionState
    data class Failed(val error: IshaaraError) : RealtimeConnectionState
}
