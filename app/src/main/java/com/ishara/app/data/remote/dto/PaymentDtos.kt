package com.ishara.app.data.remote.dto

/**
 * Strict DTO for POST /api/v1/rides/:rideId/payment request body.
 * Matches backend `createPaymentOrderSchema`.
 */
data class CreatePaymentOrderRequestDto(
    val idempotencyKey: String? = null
)

/**
 * DTO representing CheckoutSessionDetails returned by POST /api/v1/rides/:rideId/payment.
 */
data class CheckoutSessionDto(
    val paymentId: String,
    val rideId: String,
    val grossAmountMinor: Long,
    val currency: String,
    val provider: String,
    val providerOrderId: String,
    val keyId: String? = null,
    val qrPayload: String? = null,
    val expiresAt: String
)

/**
 * Strict DTO for POST /api/v1/payments/:paymentId/verify request body.
 * Matches backend `verifyPaymentSchema`.
 */
data class VerifyPaymentRequestDto(
    val providerOrderId: String,
    val providerPaymentId: String,
    val signature: String
)

/**
 * Authoritative PaymentRecord DTO returned by:
 * - GET /api/v1/rides/:rideId/payment
 * - POST /api/v1/payments/:paymentId/verify
 */
data class PaymentRecordDto(
    val id: String,
    val rideId: String,
    val userId: String,
    val driverId: String,
    val grossAmountMinor: Long,
    val platformFeeMinor: Long,
    val providerAmountMinor: Long,
    val refundedAmountMinor: Long = 0L,
    val currency: String,
    val status: String,
    val provider: String,
    val providerOrderId: String,
    val providerPaymentId: String? = null,
    val providerSignature: String? = null,
    val idempotencyKey: String? = null,
    val capturedAt: String? = null,
    val expiresAt: String,
    val createdAt: String,
    val updatedAt: String
)
