package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Payment
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.PaymentVerificationRequest

/**
 * Domain repository contract for payment operations.
 */
interface PaymentRepository {

    /**
     * Creates or retrieves an active checkout session for an eligible ride.
     * Maps to POST /api/v1/rides/:rideId/payment
     */
    suspend fun createPaymentOrder(
        rideId: String,
        idempotencyKey: String? = null
    ): IshaaraResult<PaymentCheckoutSession>

    /**
     * Retrieves the authoritative payment record and capture status for a ride.
     * Maps to GET /api/v1/rides/:rideId/payment
     */
    suspend fun getPaymentByRideId(
        rideId: String
    ): IshaaraResult<Payment>

    /**
     * Authoritatively verifies provider checkout signature and captures payment.
     * Maps to POST /api/v1/payments/:paymentId/verify
     */
    suspend fun verifyPayment(
        paymentId: String,
        request: PaymentVerificationRequest
    ): IshaaraResult<Payment>

    /**
     * Clears local payment state/cache upon account switching or session termination.
     */
    fun clearPaymentCache()
}
