package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Payment
import com.ishara.app.domain.repository.PaymentRepository

/**
 * Use case to retrieve the authoritative payment status for a ride.
 */
class GetRidePaymentUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend fun execute(rideId: String): IshaaraResult<Payment> {
        return paymentRepository.getPaymentByRideId(rideId)
    }
}
