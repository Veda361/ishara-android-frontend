package com.ishara.app.data.repository

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.local.datasource.TransitLocalDataSource
import com.ishara.app.data.mapper.TransitMapper
import com.ishara.app.data.remote.datasource.TransitRemoteDataSource
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitCacheMetadata
import com.ishara.app.domain.model.TransitCachePolicy
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.repository.TransitRepository
import com.ishara.app.domain.repository.TransitResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TransitRepositoryImpl(
    private val remoteDataSource: TransitRemoteDataSource,
    private val localDataSource: TransitLocalDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val cachePolicy: TransitCachePolicy = TransitCachePolicy()
) : TransitRepository {

    private val refreshMutex = Mutex()

    override fun getRoutes(forceRefresh: Boolean): Flow<TransitResource<List<TransitRoute>>> = flow {
        val cachedRoutes = localDataSource.getRoutes()
        val metadata = localDataSource.getCacheMetadata()
        val lastRefreshedAt = metadata?.lastRefreshedAtMillis ?: 0L
        val freshness = cachePolicy.evaluateFreshness(lastRefreshedAt)

        if (cachedRoutes.isNotEmpty()) {
            // Immediate cache delivery (Stale-While-Revalidate)
            emit(
                TransitResource.Success(
                    data = cachedRoutes,
                    freshness = freshness,
                    isOffline = false,
                    lastRefreshedAt = lastRefreshedAt
                )
            )

            // Revalidate if forced or stale
            if (forceRefresh || freshness != TransitCacheFreshness.FRESH) {
                emit(TransitResource.Loading(cachedData = cachedRoutes, isOffline = false))
                val refreshResult = refreshRoutesInternal()
                when (refreshResult) {
                    is IshaaraResult.Success -> {
                        emit(
                            TransitResource.Success(
                                data = refreshResult.data,
                                freshness = TransitCacheFreshness.FRESH,
                                isOffline = false,
                                lastRefreshedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    is IshaaraResult.Failure -> {
                        val isOffline = isNetworkFailure(refreshResult.error)
                        emit(
                            TransitResource.Error(
                                message = refreshResult.error.message ?: "Failed to refresh transit routes",
                                cachedData = cachedRoutes,
                                isOffline = isOffline
                            )
                        )
                    }
                }
            }
        } else {
            // No cache available; must fetch from remote
            emit(TransitResource.Loading(cachedData = null, isOffline = false))
            val refreshResult = refreshRoutesInternal()
            when (refreshResult) {
                is IshaaraResult.Success -> {
                    emit(
                        TransitResource.Success(
                            data = refreshResult.data,
                            freshness = TransitCacheFreshness.FRESH,
                            isOffline = false,
                            lastRefreshedAt = System.currentTimeMillis()
                        )
                    )
                }
                is IshaaraResult.Failure -> {
                    val isOffline = isNetworkFailure(refreshResult.error)
                    emit(
                        TransitResource.Error(
                            message = refreshResult.error.message ?: "Failed to load transit routes",
                            cachedData = null,
                            isOffline = isOffline
                        )
                    )
                }
            }
        }
    }.flowOn(dispatchers.io)

    override fun getRouteById(
        routeId: String,
        forceRefresh: Boolean
    ): Flow<TransitResource<TransitRoute>> = flow {
        val cached = localDataSource.getRouteById(routeId)
        val metadata = localDataSource.getCacheMetadata()
        val lastRefreshedAt = metadata?.lastRefreshedAtMillis ?: 0L
        val freshness = cachePolicy.evaluateFreshness(lastRefreshedAt)

        if (cached != null) {
            emit(
                TransitResource.Success(
                    data = cached,
                    freshness = freshness,
                    isOffline = false,
                    lastRefreshedAt = lastRefreshedAt
                )
            )

            if (forceRefresh || freshness != TransitCacheFreshness.FRESH) {
                emit(TransitResource.Loading(cachedData = cached, isOffline = false))
                val refreshResult = refreshRoutesInternal()
                when (refreshResult) {
                    is IshaaraResult.Success -> {
                        val updated = refreshResult.data.find { it.id == routeId }
                        if (updated != null) {
                            emit(
                                TransitResource.Success(
                                    data = updated,
                                    freshness = TransitCacheFreshness.FRESH,
                                    isOffline = false,
                                    lastRefreshedAt = System.currentTimeMillis()
                                )
                            )
                        } else {
                            emit(
                                TransitResource.Error(
                                    message = "Route is no longer active",
                                    cachedData = cached,
                                    isOffline = false
                                )
                            )
                        }
                    }
                    is IshaaraResult.Failure -> {
                        emit(
                            TransitResource.Error(
                                message = refreshResult.error.message ?: "Failed to refresh route",
                                cachedData = cached,
                                isOffline = isNetworkFailure(refreshResult.error)
                            )
                        )
                    }
                }
            }
        } else {
            emit(TransitResource.Loading(cachedData = null, isOffline = false))
            val refreshResult = refreshRoutesInternal()
            when (refreshResult) {
                is IshaaraResult.Success -> {
                    val found = refreshResult.data.find { it.id == routeId }
                    if (found != null) {
                        emit(
                            TransitResource.Success(
                                data = found,
                                freshness = TransitCacheFreshness.FRESH,
                                isOffline = false,
                                lastRefreshedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        emit(
                            TransitResource.Error(
                                message = "Route not found",
                                cachedData = null,
                                isOffline = false
                            )
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    emit(
                        TransitResource.Error(
                            message = refreshResult.error.message ?: "Failed to load route",
                            cachedData = null,
                            isOffline = isNetworkFailure(refreshResult.error)
                        )
                    )
                }
            }
        }
    }.flowOn(dispatchers.io)

    override suspend fun refreshRoutes(): IshaaraResult<List<TransitRoute>> = withContext(dispatchers.io) {
        refreshRoutesInternal()
    }

    override suspend fun getCacheMetadata(): TransitCacheMetadata? = withContext(dispatchers.io) {
        localDataSource.getCacheMetadata()
    }

    override suspend fun clearCache() = withContext(dispatchers.io) {
        localDataSource.clear()
    }

    private suspend fun refreshRoutesInternal(): IshaaraResult<List<TransitRoute>> = refreshMutex.withLock {
        val session = sessionStore.getSession()
        val token = session?.token

        val remoteResult = remoteDataSource.listActiveTrips(token)
        when (remoteResult) {
            is IshaaraResult.Success -> {
                val now = System.currentTimeMillis()
                val routes = remoteResult.data.map { TransitMapper.toDomain(it, cachedAtMillis = now) }
                localDataSource.saveRoutes(routes, refreshedAtMillis = now)
                IshaaraResult.success(routes)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(remoteResult.error)
            }
        }
    }

    private fun isNetworkFailure(error: IshaaraError): Boolean {
        return error is IshaaraError.Network ||
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connect", ignoreCase = true) == true ||
            error.message?.contains("offline", ignoreCase = true) == true
    }
}
