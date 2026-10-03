package com.ishara.app.domain.usecase

import com.ishara.app.domain.repository.PaymentRepository

/**
 * Use case to clear cached payment state upon account switching or session termination.
 */
class ClearPaymentStateUseCase(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke() {
        paymentRepository.clearPaymentCache()
    }
}
