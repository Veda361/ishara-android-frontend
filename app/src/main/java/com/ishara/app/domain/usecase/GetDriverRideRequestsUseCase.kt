package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverRideRequestPage
import com.ishara.app.domain.repository.DriverRideRequestRepository

class GetDriverRideRequestsUseCase(
    private val repository: DriverRideRequestRepository
) {
    suspend operator fun invoke(
        status: String? = null,
        tripId: String? = null,
        page: Int = 1,
        limit: Int = 20,
        cursor: String? = null
    ): IshaaraResult<DriverRideRequestPage> {
        if (page < 1) {
            return IshaaraResult.failure(IshaaraError.Validation(message = "Page must be >= 1"))
        }
        if (limit < 1 || limit > 100) {
            return IshaaraResult.failure(IshaaraError.Validation(message = "Limit must be between 1 and 100"))
        }
        return repository.getDriverRideRequests(status, tripId, page, limit, cursor)
    }
}
