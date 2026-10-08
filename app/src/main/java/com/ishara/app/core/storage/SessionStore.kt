package com.ishara.app.core.storage

import android.content.Context
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SessionStore {
    suspend fun saveSession(session: AuthSession)
    suspend fun getSession(): AuthSession?
    suspend fun clearSession()
    fun observeSession(): Flow<AuthSession?>
}

class SharedPreferencesSessionStore(context: Context) : SessionStore {
    
    private val prefs = context.getSharedPreferences("ishara_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val sessionKey = "auth_session"
    
    private val _sessionFlow = MutableStateFlow<AuthSession?>(null)
    
    init {
        val sessionJson = prefs.getString(sessionKey, null)
        if (sessionJson != null) {
            try {
                _sessionFlow.value = json.decodeFromString<AuthSession>(sessionJson)
            } catch (e: Exception) {
                // Ignore corrupted session
            }
        }
    }

    override suspend fun saveSession(session: AuthSession) {
        val sessionJson = json.encodeToString(session)
        prefs.edit().putString(sessionKey, sessionJson).apply()
        _sessionFlow.value = session
    }

    override suspend fun getSession(): AuthSession? {
        return _sessionFlow.value
    }

    override suspend fun clearSession() {
        prefs.edit().remove(sessionKey).apply()
        _sessionFlow.value = null
    }

    override fun observeSession(): Flow<AuthSession?> {
        return _sessionFlow.asStateFlow()
    }
}
