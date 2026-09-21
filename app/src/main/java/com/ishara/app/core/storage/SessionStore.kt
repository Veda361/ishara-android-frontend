package com.ishara.app.core.storage

import com.ishara.app.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage abstraction for persisting authentication tokens and user session data.
 */
interface SessionStore {
    suspend fun saveSession(session: AuthSession)
    suspend fun getSession(): AuthSession?
    suspend fun clearSession()
    fun observeSession(): Flow<AuthSession?>
}

/**
 * In-memory stateful session store implementation with reactive Flow observation.
 */
class InMemorySessionStore(initialSession: AuthSession? = null) : SessionStore {
    private val sessionFlow = MutableStateFlow(initialSession)

    override suspend fun saveSession(session: AuthSession) {
        sessionFlow.value = session
    }

    override suspend fun getSession(): AuthSession? {
        return sessionFlow.value
    }

    override suspend fun clearSession() {
        sessionFlow.value = null
    }

    override fun observeSession(): Flow<AuthSession?> {
        return sessionFlow.asStateFlow()
    }
}
