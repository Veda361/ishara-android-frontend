package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.repository.DriverEarningsRepository

/**
 * Use case to retrieve paginated completed ride history operated by driver with financial enrichment.
 */
class GetDriverRideHistoryUseCase(
    private val repository: DriverEarningsRepository
) {
    suspend operator fun invoke(
        tripId: String? = null,
        period: EarningsPeriodType? = null,
        from: String? = null,
        to: String? = null,
        timezone: String = "Asia/Kolkata",
        page: Int = 1,
        limit: Int = 20
    ): IshaaraResult<DriverEarnings> {
        return repository.getDriverRideHistory(
            tripId = tripId,
            period = period,
            from = from,
            to = to,
            timezone = timezone,
            page = page,
            limit = limit
        )
    }
}
