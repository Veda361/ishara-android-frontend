package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.repository.PaymentRepository

/**
 * Use case to create or retrieve an active checkout session for a ride.
 */
class CreatePaymentOrderUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend fun execute(
        rideId: String,
        idempotencyKey: String? = null
    ): IshaaraResult<PaymentCheckoutSession> {
        return paymentRepository.createPaymentOrder(
            rideId = rideId,
            idempotencyKey = idempotencyKey
        )
    }
}
