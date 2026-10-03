package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.CheckoutSessionDto
import com.ishara.app.data.remote.dto.PaymentRecordDto
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.model.Payment
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.RideReceipt

object PaymentMapper {

    fun toDomain(dto: CheckoutSessionDto): PaymentCheckoutSession {
        return PaymentCheckoutSession(
            paymentId = dto.paymentId,
            rideId = dto.rideId,
            fare = Money(amountMinor = dto.grossAmountMinor, currency = dto.currency),
            provider = dto.provider,
            providerOrderId = dto.providerOrderId,
            keyId = dto.keyId,
            qrPayload = dto.qrPayload,
            expiresAt = dto.expiresAt
        )
    }

    fun toDomain(dto: PaymentRecordDto): Payment {
        val currency = dto.currency
        return Payment(
            id = dto.id,
            rideId = dto.rideId,
            userId = dto.userId,
            driverId = dto.driverId,
            grossFare = Money(amountMinor = dto.grossAmountMinor, currency = currency),
            platformFee = Money(amountMinor = dto.platformFeeMinor, currency = currency),
            driverShare = Money(amountMinor = dto.providerAmountMinor, currency = currency),
            refundedAmount = Money(amountMinor = dto.refundedAmountMinor, currency = currency),
            status = DomainPaymentStatus.fromBackend(dto.status),
            provider = dto.provider,
            providerOrderId = dto.providerOrderId,
            providerPaymentId = dto.providerPaymentId,
            capturedAt = dto.capturedAt,
            expiresAt = dto.expiresAt,
            createdAt = dto.createdAt
        )
    }

    fun toReceipt(
        payment: Payment,
        pickupAddress: String? = null,
        destinationAddress: String? = null
    ): RideReceipt {
        return RideReceipt(
            paymentId = payment.id,
            rideId = payment.rideId,
            amount = payment.grossFare,
            providerPaymentId = payment.providerPaymentId,
            capturedAt = payment.capturedAt ?: payment.createdAt,
            pickupAddress = pickupAddress,
            destinationAddress = destinationAddress
        )
    }
}
