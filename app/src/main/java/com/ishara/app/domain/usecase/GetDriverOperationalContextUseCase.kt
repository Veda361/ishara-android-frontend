package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.repository.DriverRepository

/**
 * Use case to retrieve the driver's current operational snapshot.
 */
class GetDriverOperationalContextUseCase(
    private val driverRepository: DriverRepository
) {
    suspend fun execute(timezone: String? = null): IshaaraResult<DriverOperationalContext> {
        return driverRepository.getOperationalContext(timezone)
    }
}
