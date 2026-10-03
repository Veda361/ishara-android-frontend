package com.ishara.app.data.repository

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.FareMapper
import com.ishara.app.data.remote.datasource.FareRemoteDataSource
import com.ishara.app.domain.model.RideFare
import com.ishara.app.domain.repository.FareRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FareRepositoryImpl(
    private val remoteDataSource: FareRemoteDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : FareRepository {

    private val tag = "FareRepository"
    private val cacheMutex = Mutex()
    private val fareCache = mutableMapOf<String, RideFare>()

    override suspend fun getRideFare(rideId: String): IshaaraResult<RideFare> = withContext(dispatchers.io) {
        val session = sessionLocalDataSource.getSession()
            ?: return@withContext IshaaraResult.failure(
                IshaaraError.Authentication(message = "Authentication required to view fare.")
            )

        when (val result = remoteDataSource.getRideFare(rideId, session.token)) {
            is IshaaraResult.Success -> {
                val domainFare = FareMapper.toDomain(result.data)
                cacheMutex.withLock {
                    fareCache[rideId] = domainFare
                }
                IshaaraResult.success(domainFare)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(tag, "Failed to retrieve fare for ride $rideId: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun clearFareState() = withContext(dispatchers.io) {
        cacheMutex.withLock {
            fareCache.clear()
        }
        IshaaraLogger.i(tag, "Fare repository cache cleared.")
    }
}
