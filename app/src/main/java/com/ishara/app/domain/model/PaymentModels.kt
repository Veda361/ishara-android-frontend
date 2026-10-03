package com.ishara.app.domain.model

import java.util.Locale

/**
 * Authoritative money representation operating in integer minor units (paise in INR).
 * Never uses floating-point values for financial calculations or storage.
 */
data class Money(
    val amountMinor: Long,
    val currency: String = "INR"
) {
    init {
        require(amountMinor >= 0) { "Amount minor cannot be negative: $amountMinor" }
    }

    val amountMajor: Double
        get() = amountMinor.toDouble() / 100.0

    /**
     * Formats the monetary amount for human display according to locale conventions.
     * Example: ₹25.00 for 2500 paise INR.
     */
    fun formatDisplay(): String {
        val major = amountMinor / 100
        val minor = amountMinor % 100
        return when (currency.uppercase(Locale.ROOT)) {
            "INR" -> String.format(Locale.forLanguageTag("en-IN"), "₹%d.%02d", major, minor)
            else -> String.format(Locale.US, "$currency %d.%02d", major, minor)
        }
    }
}

/**
 * Authoritative payment lifecycle statuses verified from the backend contract.
 */
enum class DomainPaymentStatus {
    CREATED,
    ORDER_CREATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    REFUND_PENDING,
    PARTIALLY_REFUNDED,
    REFUNDED,
    UNKNOWN;

    val isCaptured: Boolean get() = this == CAPTURED
    val isPending: Boolean get() = this == CREATED || this == ORDER_CREATED || this == AUTHORIZED
    val isFailed: Boolean get() = this == FAILED || this == CANCELLED
    val isRefunded: Boolean get() = this == REFUNDED || this == PARTIALLY_REFUNDED
    val isTerminal: Boolean get() = this == FAILED || this == CANCELLED || this == REFUNDED

    /**
     * Authoritative payment state machine transitions derived strictly from backend payment.state-machine.ts
     */
    fun canTransitionTo(next: DomainPaymentStatus): Boolean {
        if (this == next) return true // Idempotent same-state
        return when (this) {
            CREATED -> next in listOf(ORDER_CREATED, FAILED, CANCELLED)
            ORDER_CREATED -> next in listOf(AUTHORIZED, CAPTURED, FAILED, CANCELLED)
            AUTHORIZED -> next in listOf(CAPTURED, FAILED, CANCELLED)
            CAPTURED -> next in listOf(REFUND_PENDING, PARTIALLY_REFUNDED, REFUNDED)
            REFUND_PENDING -> next in listOf(PARTIALLY_REFUNDED, REFUNDED, CAPTURED)
            PARTIALLY_REFUNDED -> next in listOf(REFUND_PENDING, REFUNDED)
            FAILED, CANCELLED, REFUNDED, UNKNOWN -> false
        }
    }

    companion object {
        fun fromBackend(value: String?): DomainPaymentStatus {
            return when (value?.uppercase(Locale.ROOT)) {
                "CREATED" -> CREATED
                "ORDER_CREATED" -> ORDER_CREATED
                "AUTHORIZED" -> AUTHORIZED
                "CAPTURED" -> CAPTURED
                "FAILED" -> FAILED
                "CANCELLED" -> CANCELLED
                "REFUND_PENDING" -> REFUND_PENDING
                "PARTIALLY_REFUNDED" -> PARTIALLY_REFUNDED
                "REFUNDED" -> REFUNDED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Active checkout session details returned by POST /api/v1/rides/:rideId/payment.
 */
data class PaymentCheckoutSession(
    val paymentId: String,
    val rideId: String,
    val fare: Money,
    val provider: String,
    val providerOrderId: String,
    val keyId: String? = null,
    val qrPayload: String? = null,
    val expiresAt: String
)

/**
 * Normalized domain model for an authoritative Payment record.
 */
data class Payment(
    val id: String,
    val rideId: String,
    val userId: String,
    val driverId: String,
    val grossFare: Money,
    val platformFee: Money,
    val driverShare: Money,
    val refundedAmount: Money,
    val status: DomainPaymentStatus,
    val provider: String,
    val providerOrderId: String,
    val providerPaymentId: String? = null,
    val capturedAt: String? = null,
    val expiresAt: String,
    val createdAt: String
)

/**
 * Verification input submitted to POST /api/v1/payments/:paymentId/verify.
 */
data class PaymentVerificationRequest(
    val providerOrderId: String,
    val providerPaymentId: String,
    val signature: String
)

/**
 * Authoritative digital payment receipt rendered after confirmed capture.
 */
data class RideReceipt(
    val paymentId: String,
    val rideId: String,
    val amount: Money,
    val providerPaymentId: String?,
    val capturedAt: String,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null
)
