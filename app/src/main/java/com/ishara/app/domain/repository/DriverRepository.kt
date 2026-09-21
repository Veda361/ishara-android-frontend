package com.ishara.app.domain.repository

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverProfile

/**
 * Domain repository contract for Driver & Conductor operations.
 */
interface DriverRepository {
    suspend fun getDriverProfile(): IshaaraResult<DriverProfile>

    suspend fun setOnline(): IshaaraResult<Unit>

    suspend fun setOffline(): IshaaraResult<Unit>

    /**
     * Ingests high-frequency driver GPS update (PATCH /api/v1/drivers/me/location).
     */
    suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit>
}
