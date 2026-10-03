package com.ishara.app.data.local.datasource

import com.ishara.app.domain.model.TransitCacheMetadata
import com.ishara.app.domain.model.TransitRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Local cache abstraction for public transit routes, stops, and sync metadata.
 */
interface TransitLocalDataSource {
    fun observeRoutes(): Flow<List<TransitRoute>>
    suspend fun getRoutes(): List<TransitRoute>
    suspend fun getRouteById(routeId: String): TransitRoute?
    suspend fun saveRoutes(routes: List<TransitRoute>, refreshedAtMillis: Long)
    suspend fun getCacheMetadata(): TransitCacheMetadata?
    suspend fun clear()
}

/**
 * Pure Kotlin in-memory implementation of TransitLocalDataSource.
 * Used for unit testing and fast in-process fallback without Android SQLite framework stubbing.
 */
class InMemoryTransitLocalDataSource : TransitLocalDataSource {

    private val mutex = Mutex()
    private val routesFlow = MutableStateFlow<List<TransitRoute>>(emptyList())
    private var metadata: TransitCacheMetadata? = null

    override fun observeRoutes(): Flow<List<TransitRoute>> = routesFlow.asStateFlow()

    override suspend fun getRoutes(): List<TransitRoute> = mutex.withLock {
        routesFlow.value
    }

    override suspend fun getRouteById(routeId: String): TransitRoute? = mutex.withLock {
        routesFlow.value.find { it.id == routeId }
    }

    override suspend fun saveRoutes(routes: List<TransitRoute>, refreshedAtMillis: Long) {
        mutex.withLock {
            routesFlow.value = routes
            metadata = TransitCacheMetadata(
                lastRefreshedAtMillis = refreshedAtMillis,
                itemCount = routes.size,
                freshness = com.ishara.app.domain.model.TransitCacheFreshness.FRESH
            )
        }
    }

    override suspend fun getCacheMetadata(): TransitCacheMetadata? = mutex.withLock {
        metadata
    }

    override suspend fun clear() {
        mutex.withLock {
            routesFlow.value = emptyList()
            metadata = null
        }
    }
}
