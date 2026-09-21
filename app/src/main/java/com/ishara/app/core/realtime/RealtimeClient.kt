package com.ishara.app.core.realtime

import com.ishara.app.core.result.IshaaraResult
import kotlinx.coroutines.flow.Flow

/**
 * Architectural abstraction for bidirectional WebSocket communication.
 */
interface RealtimeClient {
    /**
     * Connects to a specific realtime streaming endpoint with bearer token authentication.
     */
    suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit>

    /**
     * Disconnects active stream.
     */
    suspend fun disconnect()

    /**
     * Sends message / audio payload over the active socket.
     */
    suspend fun send(message: String): IshaaraResult<Unit>

    /**
     * Observes socket lifecycle connection states.
     */
    fun observeConnectionState(): Flow<RealtimeConnectionState>

    /**
     * Observes typed realtime events arriving from the backend.
     */
    fun observeEvents(): Flow<RealtimeEvent>
}
