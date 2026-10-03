package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitCacheMetadata
import com.ishara.app.domain.model.TransitRoute
import kotlinx.coroutines.flow.Flow

/**
 * High-level resource state modeling offline-first caching, freshness metadata, and network state.
 */
sealed class TransitResource<out T> {
    data class Success<T>(
        val data: T,
        val freshness: TransitCacheFreshness,
        val isOffline: Boolean,
        val lastRefreshedAt: Long?
    ) : TransitResource<T>()

    data class Loading<T>(
        val cachedData: T? = null,
        val isOffline: Boolean = false
    ) : TransitResource<T>()

    data class Error<T>(
        val message: String,
        val cachedData: T? = null,
        val isOffline: Boolean = false
    ) : TransitResource<T>()
}

/**
 * Public transit repository handling stale-while-revalidate offline-first data synchronization.
 */
interface TransitRepository {
    fun getRoutes(forceRefresh: Boolean = false): Flow<TransitResource<List<TransitRoute>>>
    fun getRouteById(routeId: String, forceRefresh: Boolean = false): Flow<TransitResource<TransitRoute>>
    suspend fun refreshRoutes(): IshaaraResult<List<TransitRoute>>
    suspend fun getCacheMetadata(): TransitCacheMetadata?
    suspend fun clearCache()
}
