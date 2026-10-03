package com.ishara.app.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage abstraction for persisting local device push token across process lifecycle.
 */
interface DeviceTokenStore {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
    fun observeToken(): Flow<String?>
}

/**
 * In-memory fallback implementation of DeviceTokenStore.
 */
class InMemoryDeviceTokenStore(initialToken: String? = null) : DeviceTokenStore {
    private val tokenFlow = MutableStateFlow(initialToken)

    override suspend fun saveToken(token: String) {
        tokenFlow.value = token
    }

    override suspend fun getToken(): String? {
        return tokenFlow.value
    }

    override suspend fun clearToken() {
        tokenFlow.value = null
    }

    override fun observeToken(): Flow<String?> {
        return tokenFlow.asStateFlow()
    }
}
