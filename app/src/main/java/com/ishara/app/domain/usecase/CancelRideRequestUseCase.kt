package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.repository.RideRepository

/**
 * Use case to cancel an active pending ride request by the passenger.
 * Enforces valid 24-character hexadecimal ObjectId format and maximum 250 character reason limit.
 */
class CancelRideRequestUseCase(
    private val rideRepository: RideRepository
) {
    suspend fun execute(
        requestId: String,
        reason: String? = null
    ): IshaaraResult<RideRequestResult> {
        if (!requestId.matches(Regex("^[0-9a-fA-F]{24}$"))) {
            return IshaaraResult.failure(
                IshaaraError.Validation("requestId", "Invalid requestId format: must be a 24-character hexadecimal ObjectId")
            )
        }
        if (reason != null && reason.trim().length > 250) {
            return IshaaraResult.failure(
                IshaaraError.Validation("reason", "Cancellation reason cannot exceed 250 characters")
            )
        }
        return rideRepository.cancelRideRequest(requestId, reason?.trim())
    }
}
