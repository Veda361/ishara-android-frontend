package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.EarningsPeriodType

/**
 * Domain repository contract for Driver Earnings, Settlement Status, and Ride History.
 * Authoritative financial source is backend; Android client performs zero client-side financial computation.
 */
interface DriverEarningsRepository {

    /**
     * Retrieves authoritative driver earnings and settlement read model for a bounded time window.
     */
    suspend fun getDriverEarnings(
        period: EarningsPeriodType = EarningsPeriodType.TODAY,
        from: String? = null,
        to: String? = null,
        timezone: String = "Asia/Kolkata",
        page: Int = 1,
        limit: Int = 20
    ): IshaaraResult<DriverEarnings>

    /**
     * Retrieves driver completed ride history enriched with per-ride financial breakdown.
     */
    suspend fun getDriverRideHistory(
        tripId: String? = null,
        period: EarningsPeriodType? = null,
        from: String? = null,
        to: String? = null,
        timezone: String = "Asia/Kolkata",
        page: Int = 1,
        limit: Int = 20
    ): IshaaraResult<DriverEarnings>
}
