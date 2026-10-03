package com.ishara.app.data.repository

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.DriverEarningsMapper
import com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSource
import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.DriverEarningsRepository
import kotlinx.coroutines.withContext

class DriverEarningsRepositoryImpl(
    private val remoteDataSource: DriverEarningsRemoteDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider
) : DriverEarningsRepository {

    override suspend fun getDriverEarnings(
        period: EarningsPeriodType,
        from: String?,
        to: String?,
        timezone: String,
        page: Int,
        limit: Int
    ): IshaaraResult<DriverEarnings> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
            ?: return@withContext IshaaraResult.failure(
                IshaaraError.Authentication(message = "Driver authentication required")
            )

        if (session.role != null && session.role != UserRole.DRIVER_CONDUCTOR) {
            return@withContext IshaaraResult.failure(
                IshaaraError.Forbidden(message = "Access restricted to DRIVER_CONDUCTOR role")
            )
        }

        val token = session.token
        if (token.isBlank()) {
            return@withContext IshaaraResult.failure(
                IshaaraError.Authentication(message = "Driver session token is missing")
            )
        }

        remoteDataSource.getDriverEarnings(
            period = period.queryParam,
            from = from,
            to = to,
            timezone = timezone,
            page = page,
            limit = limit,
            token = token
        ).map { dto ->
            DriverEarningsMapper.toDomain(dto)
        }
    }

    override suspend fun getDriverRideHistory(
        tripId: String?,
        period: EarningsPeriodType?,
        from: String?,
        to: String?,
        timezone: String,
        page: Int,
        limit: Int
    ): IshaaraResult<DriverEarnings> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
            ?: return@withContext IshaaraResult.failure(
                IshaaraError.Authentication(message = "Driver authentication required")
            )

        if (session.role != null && session.role != UserRole.DRIVER_CONDUCTOR) {
            return@withContext IshaaraResult.failure(
                IshaaraError.Forbidden(message = "Access restricted to DRIVER_CONDUCTOR role")
            )
        }

        val token = session.token
        if (token.isBlank()) {
            return@withContext IshaaraResult.failure(
                IshaaraError.Authentication(message = "Driver session token is missing")
            )
        }

        remoteDataSource.getDriverRidesWithFinancials(
            status = "COMPLETED",
            tripId = tripId,
            period = period?.queryParam,
            from = from,
            to = to,
            timezone = timezone,
            page = page,
            limit = limit,
            token = token
        ).map { dto ->
            DriverEarningsMapper.toDomain(dto)
        }
    }
}
