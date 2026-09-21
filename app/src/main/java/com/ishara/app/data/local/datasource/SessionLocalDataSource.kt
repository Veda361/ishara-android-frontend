package com.ishara.app.data.local.datasource

import com.ishara.app.core.storage.SessionStore
import com.ishara.app.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface SessionLocalDataSource {
    suspend fun saveSession(session: AuthSession)
    suspend fun getSession(): AuthSession?
    suspend fun clearSession()
    fun observeSession(): Flow<AuthSession?>
}

class SessionLocalDataSourceImpl(
    private val sessionStore: SessionStore
) : SessionLocalDataSource {
    override suspend fun saveSession(session: AuthSession) = sessionStore.saveSession(session)
    override suspend fun getSession(): AuthSession? = sessionStore.getSession()
    override suspend fun clearSession() = sessionStore.clearSession()
    override fun observeSession(): Flow<AuthSession?> = sessionStore.observeSession()
}
