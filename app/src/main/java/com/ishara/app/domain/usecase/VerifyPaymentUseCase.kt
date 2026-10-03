package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Payment
import com.ishara.app.domain.model.PaymentVerificationRequest
import com.ishara.app.domain.repository.PaymentRepository

/**
 * Use case to verify payment signature with the backend and authoritatively capture the payment.
 */
class VerifyPaymentUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend fun execute(
        paymentId: String,
        request: PaymentVerificationRequest
    ): IshaaraResult<Payment> {
        return paymentRepository.verifyPayment(paymentId, request)
    }
}
