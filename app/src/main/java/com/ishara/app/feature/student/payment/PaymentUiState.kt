package com.ishara.app.feature.student.payment

import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.RideReceipt

/**
 * Immutable UI State representation for the Student Payment Screen.
 */
sealed interface PaymentUiState {

    /**
     * Fetching existing payment status or initializing payment order.
     */
    data object Loading : PaymentUiState

    /**
     * Active checkout session ready for student interaction.
     */
    data class OrderReady(
        val session: PaymentCheckoutSession,
        val pickupAddress: String? = null,
        val destinationAddress: String? = null,
        val isProcessing: Boolean = false,
        val errorMessage: String? = null,
        val noticeMessage: String? = null
    ) : PaymentUiState

    /**
     * Client checkout completed at provider; awaiting authoritative backend verification.
     */
    data class Verifying(
        val paymentId: String,
        val message: String = "Confirming payment..."
    ) : PaymentUiState

    /**
     * Authoritative payment capture confirmed by backend. Renders digital ride receipt.
     */
    data class Paid(
        val receipt: RideReceipt
    ) : PaymentUiState

    /**
     * Authoritative backend refund status.
     */
    data class Refunded(
        val payment: com.ishara.app.domain.model.Payment,
        val isPartial: Boolean
    ) : PaymentUiState

    /**
     * Explicit payment failure or cancellation reported authoritatively by backend.
     */
    data class PaymentFailed(
        val message: String,
        val canRetry: Boolean = true,
        val status: com.ishara.app.domain.model.DomainPaymentStatus = com.ishara.app.domain.model.DomainPaymentStatus.FAILED
    ) : PaymentUiState

    /**
     * Terminal or actionable error state.
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val isUnauthorized: Boolean = false
    ) : PaymentUiState
}
