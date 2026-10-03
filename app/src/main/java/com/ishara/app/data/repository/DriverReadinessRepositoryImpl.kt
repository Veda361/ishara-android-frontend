package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.DriverReadinessMapper
import com.ishara.app.data.remote.datasource.DriverReadinessRemoteDataSource
import com.ishara.app.domain.model.DriverOperationalReadiness
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.DriverReadinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of [DriverReadinessRepository].
 *
 * Enforces:
 * 1. Authoritative backend source of truth for readiness evaluation.
 * 2. Role boundary check: Only DRIVER_CONDUCTOR sessions may evaluate readiness.
 * 3. Cache policy: Memory caching for transient rendering; cache never acts as local permanent authority.
 * 4. Account isolation: Immediate flush of cache upon sign-out / account switch.
 * 5. Concurrency safety: Mutex protection prevents duplicate parallel network executions.
 */
class DriverReadinessRepositoryImpl(
    private val remoteDataSource: DriverReadinessRemoteDataSource,
    private val localDataSource: SessionLocalDataSource
) : DriverReadinessRepository {

    private val mutex = Mutex()
    private val _readinessFlow = MutableStateFlow<DriverOperationalReadiness?>(null)

    override fun observeDriverReadiness(): StateFlow<DriverOperationalReadiness?> = _readinessFlow.asStateFlow()

    override suspend fun getDriverReadiness(forceRefresh: Boolean): IshaaraResult<DriverOperationalReadiness> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Operational readiness is restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        if (!forceRefresh) {
            val cached = _readinessFlow.value
            if (cached != null) {
                return IshaaraResult.success(cached)
            }
        }

        return fetchAuthoritativeReadiness(session.token)
    }

    override suspend fun refreshDriverReadiness(): IshaaraResult<DriverOperationalReadiness> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Operational readiness is restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        return fetchAuthoritativeReadiness(session.token)
    }

    private suspend fun fetchAuthoritativeReadiness(token: String): IshaaraResult<DriverOperationalReadiness> {
        val result = remoteDataSource.getOperationalReadiness(token)
        return when (result) {
            is IshaaraResult.Success -> {
                val domainModel = DriverReadinessMapper.toDomain(result.data)
                _readinessFlow.value = domainModel
                IshaaraLogger.i(
                    TAG,
                    "Authoritative driver readiness evaluated: status=${domainModel.status}, authorized=${domainModel.authorized}, blockersCount=${domainModel.reasons.size}"
                )
                IshaaraResult.success(domainModel)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to evaluate driver readiness: code=${result.error.code}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override fun clearReadinessState() {
        _readinessFlow.value = null
        IshaaraLogger.i(TAG, "Cached driver readiness state cleared.")
    }

    companion object {
        private const val TAG = "DriverReadinessRepo"
    }
}
