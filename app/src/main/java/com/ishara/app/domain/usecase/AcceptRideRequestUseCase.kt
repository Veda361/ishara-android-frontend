package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.repository.DriverRideRequestRepository

class AcceptRideRequestUseCase(
    private val repository: DriverRideRequestRepository
) {
    suspend operator fun invoke(requestId: String): IshaaraResult<DriverRideRequest> {
        if (requestId.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Validation(message = "Request ID cannot be blank"))
        }
        return repository.acceptRideRequest(requestId)
    }
}
