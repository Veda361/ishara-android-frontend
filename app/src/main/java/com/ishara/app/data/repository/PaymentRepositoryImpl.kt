package com.ishara.app.data.repository

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.PaymentMapper
import com.ishara.app.data.remote.datasource.PaymentRemoteDataSource
import com.ishara.app.data.remote.dto.CreatePaymentOrderRequestDto
import com.ishara.app.data.remote.dto.VerifyPaymentRequestDto
import com.ishara.app.domain.model.Payment
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.PaymentVerificationRequest
import com.ishara.app.domain.repository.PaymentRepository
import kotlinx.coroutines.withContext

/**
 * Production implementation of [PaymentRepository].
 * Resolves session tokens, dispatches network operations, and maps DTOs to Domain models.
 */
class PaymentRepositoryImpl(
    private val remoteDataSource: PaymentRemoteDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : PaymentRepository {

    override suspend fun createPaymentOrder(
        rideId: String,
        idempotencyKey: String?
    ): IshaaraResult<PaymentCheckoutSession> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val requestDto = CreatePaymentOrderRequestDto(
            idempotencyKey = idempotencyKey
        )

        remoteDataSource.createPaymentOrder(rideId, requestDto, idempotencyKey, token).map { dto ->
            PaymentMapper.toDomain(dto)
        }
    }

    override suspend fun getPaymentByRideId(
        rideId: String
    ): IshaaraResult<Payment> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.getPaymentByRideId(rideId, token).map { dto ->
            PaymentMapper.toDomain(dto)
        }
    }

    override suspend fun verifyPayment(
        paymentId: String,
        request: PaymentVerificationRequest
    ): IshaaraResult<Payment> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val requestDto = VerifyPaymentRequestDto(
            providerOrderId = request.providerOrderId,
            providerPaymentId = request.providerPaymentId,
            signature = request.signature
        )

        remoteDataSource.verifyPayment(paymentId, requestDto, token).map { dto ->
            PaymentMapper.toDomain(dto)
        }
    }

    override fun clearPaymentCache() {
        // Stateless remote payment repository: session-clearing invalidates all payment authorization
    }
}
