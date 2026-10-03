package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.repository.RideRepository

/**
 * Use case to retrieve the authoritative state of a specific ride request.
 * Enforces valid 24-character hexadecimal ObjectId format prior to dispatching network call.
 */
class GetRideRequestUseCase(
    private val rideRepository: RideRepository
) {
    suspend fun execute(requestId: String): IshaaraResult<RideRequestResult> {
        if (!requestId.matches(Regex("^[0-9a-fA-F]{24}$"))) {
            return IshaaraResult.failure(
                IshaaraError.Validation("requestId", "Invalid requestId format: must be a 24-character hexadecimal ObjectId")
            )
        }
        return rideRepository.getRideRequest(requestId)
    }
}
