package com.ishara.app

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.PaymentMapper
import com.ishara.app.data.remote.datasource.PaymentRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.CheckoutSessionDto
import com.ishara.app.data.remote.dto.CreatePaymentOrderRequestDto
import com.ishara.app.data.remote.dto.PaymentRecordDto
import com.ishara.app.data.remote.dto.VerifyPaymentRequestDto
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.Money
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Strict Contract and Domain Tests for Phase 13: Digital Ticketing, Fare & Payment.
 */
class PaymentStrictContractTest {

    // =========================================================================
    // 1. Money Domain Tests
    // =========================================================================

    @Test
    fun `Money correctly calculates major amount and formats Indian currency`() {
        val moneyZero = Money(amountMinor = 0L, currency = "INR")
        assertEquals(0.0, moneyZero.amountMajor, 0.001)
        assertEquals("₹0.00", moneyZero.formatDisplay())

        val money25 = Money(amountMinor = 2500L, currency = "INR")
        assertEquals(25.0, money25.amountMajor, 0.001)
        assertEquals("₹25.00", money25.formatDisplay())

        val money1250 = Money(amountMinor = 125050L, currency = "INR")
        assertEquals(1250.50, money1250.amountMajor, 0.001)
        assertEquals("₹1250.50", money1250.formatDisplay())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Money rejects negative minor amount`() {
        Money(amountMinor = -100L)
    }

    // =========================================================================
    // 2. Domain Payment Status Mapping Tests
    // =========================================================================

    @Test
    fun `DomainPaymentStatus correctly maps all verified backend statuses`() {
        assertEquals(DomainPaymentStatus.CREATED, DomainPaymentStatus.fromBackend("CREATED"))
        assertEquals(DomainPaymentStatus.ORDER_CREATED, DomainPaymentStatus.fromBackend("ORDER_CREATED"))
        assertEquals(DomainPaymentStatus.AUTHORIZED, DomainPaymentStatus.fromBackend("AUTHORIZED"))
        assertEquals(DomainPaymentStatus.CAPTURED, DomainPaymentStatus.fromBackend("CAPTURED"))
        assertEquals(DomainPaymentStatus.FAILED, DomainPaymentStatus.fromBackend("FAILED"))
        assertEquals(DomainPaymentStatus.CANCELLED, DomainPaymentStatus.fromBackend("CANCELLED"))
        assertEquals(DomainPaymentStatus.REFUND_PENDING, DomainPaymentStatus.fromBackend("REFUND_PENDING"))
        assertEquals(DomainPaymentStatus.PARTIALLY_REFUNDED, DomainPaymentStatus.fromBackend("PARTIALLY_REFUNDED"))
        assertEquals(DomainPaymentStatus.REFUNDED, DomainPaymentStatus.fromBackend("REFUNDED"))
    }

    @Test
    fun `DomainPaymentStatus safely falls back to UNKNOWN for unmapped future statuses without crashing`() {
        assertEquals(DomainPaymentStatus.UNKNOWN, DomainPaymentStatus.fromBackend("FUTURE_ARBITRARY_STATUS"))
        assertEquals(DomainPaymentStatus.UNKNOWN, DomainPaymentStatus.fromBackend(null))
        assertEquals(DomainPaymentStatus.UNKNOWN, DomainPaymentStatus.fromBackend(""))
    }

    @Test
    fun `DomainPaymentStatus boolean property helpers function accurately`() {
        assertTrue(DomainPaymentStatus.CAPTURED.isCaptured)
        assertFalse(DomainPaymentStatus.ORDER_CREATED.isCaptured)
        assertFalse(DomainPaymentStatus.FAILED.isCaptured)

        assertTrue(DomainPaymentStatus.ORDER_CREATED.isPending)
        assertTrue(DomainPaymentStatus.AUTHORIZED.isPending)
        assertFalse(DomainPaymentStatus.CAPTURED.isPending)

        assertTrue(DomainPaymentStatus.FAILED.isFailed)
        assertTrue(DomainPaymentStatus.CANCELLED.isFailed)
        assertFalse(DomainPaymentStatus.CAPTURED.isFailed)

        assertTrue(DomainPaymentStatus.REFUNDED.isRefunded)
        assertTrue(DomainPaymentStatus.PARTIALLY_REFUNDED.isRefunded)
        assertFalse(DomainPaymentStatus.CAPTURED.isRefunded)

        assertTrue(DomainPaymentStatus.FAILED.isTerminal)
        assertTrue(DomainPaymentStatus.CANCELLED.isTerminal)
        assertTrue(DomainPaymentStatus.REFUNDED.isTerminal)
        assertFalse(DomainPaymentStatus.ORDER_CREATED.isTerminal)
        assertFalse(DomainPaymentStatus.CAPTURED.isTerminal)
    }

    @Test
    fun `canTransitionTo validates valid transitions and rejects invalid state jumps`() {
        // Legal forward transitions
        assertTrue(DomainPaymentStatus.CREATED.canTransitionTo(DomainPaymentStatus.ORDER_CREATED))
        assertTrue(DomainPaymentStatus.ORDER_CREATED.canTransitionTo(DomainPaymentStatus.CAPTURED))
        assertTrue(DomainPaymentStatus.ORDER_CREATED.canTransitionTo(DomainPaymentStatus.AUTHORIZED))
        assertTrue(DomainPaymentStatus.AUTHORIZED.canTransitionTo(DomainPaymentStatus.CAPTURED))
        assertTrue(DomainPaymentStatus.CAPTURED.canTransitionTo(DomainPaymentStatus.REFUNDED))
        assertTrue(DomainPaymentStatus.CAPTURED.canTransitionTo(DomainPaymentStatus.PARTIALLY_REFUNDED))
        assertTrue(DomainPaymentStatus.CAPTURED.canTransitionTo(DomainPaymentStatus.REFUND_PENDING))
        assertTrue(DomainPaymentStatus.REFUND_PENDING.canTransitionTo(DomainPaymentStatus.REFUNDED))
        assertTrue(DomainPaymentStatus.PARTIALLY_REFUNDED.canTransitionTo(DomainPaymentStatus.REFUNDED))

        // Same-state idempotent transitions
        assertTrue(DomainPaymentStatus.CAPTURED.canTransitionTo(DomainPaymentStatus.CAPTURED))
        assertTrue(DomainPaymentStatus.ORDER_CREATED.canTransitionTo(DomainPaymentStatus.ORDER_CREATED))

        // Illegal / backward transitions
        assertFalse(DomainPaymentStatus.CAPTURED.canTransitionTo(DomainPaymentStatus.ORDER_CREATED))
        assertFalse(DomainPaymentStatus.CAPTURED.canTransitionTo(DomainPaymentStatus.CREATED))
        assertFalse(DomainPaymentStatus.REFUNDED.canTransitionTo(DomainPaymentStatus.CAPTURED))
        assertFalse(DomainPaymentStatus.FAILED.canTransitionTo(DomainPaymentStatus.CAPTURED))
        assertFalse(DomainPaymentStatus.CANCELLED.canTransitionTo(DomainPaymentStatus.ORDER_CREATED))
    }

    // =========================================================================
    // 3. DTO JSON Parsing & Serialization Tests
    // =========================================================================

    @Test
    fun `parseCheckoutSessionResponse parses valid backend JSON payload correctly`() {
        val json = """
            {
                "success": true,
                "statusCode": 201,
                "message": "Payment order created successfully",
                "data": {
                    "paymentId": "6504a1b2c3d4e5f678901234",
                    "rideId": "6504a1b2c3d4e5f678901235",
                    "grossAmountMinor": 2500,
                    "currency": "INR",
                    "provider": "razorpay",
                    "providerOrderId": "order_NXyz1234567890",
                    "keyId": "rzp_test_Public123",
                    "qrPayload": "upi://pay?pa=test@icici&am=25.00",
                    "expiresAt": "2026-09-27T16:30:00.000Z"
                }
            }
        """.trimIndent()

        val dataSource = PaymentRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                    IshaaraResult.success(HttpResponse(201, json))
            },
            networkConfig = NetworkConfig()
        )

        val session = dataSource.parseCheckoutSessionResponse(json)
        assertEquals("6504a1b2c3d4e5f678901234", session.paymentId)
        assertEquals("6504a1b2c3d4e5f678901235", session.rideId)
        assertEquals(2500L, session.grossAmountMinor)
        assertEquals("INR", session.currency)
        assertEquals("razorpay", session.provider)
        assertEquals("order_NXyz1234567890", session.providerOrderId)
        assertEquals("rzp_test_Public123", session.keyId)
        assertEquals("upi://pay?pa=test@icici&am=25.00", session.qrPayload)
        assertEquals("2026-09-27T16:30:00.000Z", session.expiresAt)
    }

    @Test
    fun `parsePaymentRecordResponse parses captured payment record JSON correctly`() {
        val json = """
            {
                "success": true,
                "statusCode": 200,
                "message": "Payment verified and captured successfully",
                "data": {
                    "id": "6504a1b2c3d4e5f678901234",
                    "rideId": "6504a1b2c3d4e5f678901235",
                    "userId": "6504a1b2c3d4e5f678901236",
                    "driverId": "6504a1b2c3d4e5f678901237",
                    "grossAmountMinor": 5000,
                    "platformFeeMinor": 500,
                    "providerAmountMinor": 4500,
                    "refundedAmountMinor": 0,
                    "currency": "INR",
                    "status": "CAPTURED",
                    "provider": "razorpay",
                    "providerOrderId": "order_NXyz1234567890",
                    "providerPaymentId": "pay_test_001",
                    "providerSignature": "mock_signature",
                    "idempotencyKey": "idem_abc",
                    "capturedAt": "2026-09-27T16:05:00.000Z",
                    "expiresAt": "2026-09-27T16:30:00.000Z",
                    "createdAt": "2026-09-27T16:00:00.000Z",
                    "updatedAt": "2026-09-27T16:05:00.000Z"
                }
            }
        """.trimIndent()

        val dataSource = PaymentRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                    IshaaraResult.success(HttpResponse(200, json))
            },
            networkConfig = NetworkConfig()
        )

        val record = dataSource.parsePaymentRecordResponse(json)
        assertEquals("6504a1b2c3d4e5f678901234", record.id)
        assertEquals("6504a1b2c3d4e5f678901235", record.rideId)
        assertEquals(5000L, record.grossAmountMinor)
        assertEquals(500L, record.platformFeeMinor)
        assertEquals(4500L, record.providerAmountMinor)
        assertEquals("CAPTURED", record.status)
        assertEquals("pay_test_001", record.providerPaymentId)
        assertEquals("mock_signature", record.providerSignature)
        assertEquals("2026-09-27T16:05:00.000Z", record.capturedAt)
    }

    // =========================================================================
    // 4. Mapper Tests
    // =========================================================================

    @Test
    fun `PaymentMapper converts DTOs to Domain models and Receipts correctly`() {
        val dto = PaymentRecordDto(
            id = "pay_1",
            rideId = "ride_1",
            userId = "user_1",
            driverId = "driver_1",
            grossAmountMinor = 3000L,
            platformFeeMinor = 300L,
            providerAmountMinor = 2700L,
            refundedAmountMinor = 0L,
            currency = "INR",
            status = "CAPTURED",
            provider = "razorpay",
            providerOrderId = "order_1",
            providerPaymentId = "pay_tx_1",
            providerSignature = "sig_1",
            capturedAt = "2026-09-27T16:00:00.000Z",
            expiresAt = "2026-09-27T16:30:00.000Z",
            createdAt = "2026-09-27T15:55:00.000Z",
            updatedAt = "2026-09-27T16:00:00.000Z"
        )

        val payment = PaymentMapper.toDomain(dto)
        assertEquals("pay_1", payment.id)
        assertEquals("ride_1", payment.rideId)
        assertEquals(3000L, payment.grossFare.amountMinor)
        assertEquals("₹30.00", payment.grossFare.formatDisplay())
        assertEquals(300L, payment.platformFee.amountMinor)
        assertEquals(2700L, payment.driverShare.amountMinor)
        assertEquals(DomainPaymentStatus.CAPTURED, payment.status)
        assertTrue(payment.status.isCaptured)

        val receipt = PaymentMapper.toReceipt(
            payment = payment,
            pickupAddress = "Jhansi Station",
            destinationAddress = "SRGI College"
        )
        assertEquals("pay_1", receipt.paymentId)
        assertEquals("ride_1", receipt.rideId)
        assertEquals("₹30.00", receipt.amount.formatDisplay())
        assertEquals("Jhansi Station", receipt.pickupAddress)
        assertEquals("SRGI College", receipt.destinationAddress)
        assertEquals("pay_tx_1", receipt.providerPaymentId)
    }

    // =========================================================================
    // 5. Remote Data Source Network Execution Tests
    // =========================================================================

    @Test
    fun `createPaymentOrder transmits Idempotency-Key and Bearer headers`() = runBlocking {
        var capturedHeaders: Map<String, String>? = null
        var capturedUrl: String? = null
        var capturedMethod: HttpMethod? = null

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedHeaders = request.headers
                capturedUrl = request.url
                capturedMethod = request.method
                val jsonResponse = """
                    {
                        "data": {
                            "paymentId": "p1",
                            "rideId": "r1",
                            "grossAmountMinor": 2500,
                            "currency": "INR",
                            "provider": "razorpay",
                            "providerOrderId": "ord_1",
                            "expiresAt": "2026-09-27T17:00:00.000Z"
                        }
                    }
                """.trimIndent()
                return IshaaraResult.success(HttpResponse(201, jsonResponse))
            }
        }

        val dataSource = PaymentRemoteDataSourceImpl(fakeClient, NetworkConfig())
        val result = dataSource.createPaymentOrder(
            rideId = "r1",
            request = CreatePaymentOrderRequestDto(),
            idempotencyKey = "test-idem-key-123",
            token = "jwt-secret-token"
        )

        assertTrue(result is IshaaraResult.Success)
        assertEquals(HttpMethod.POST, capturedMethod)
        assertTrue(capturedUrl!!.endsWith("/rides/r1/payment"))
        assertEquals("Bearer jwt-secret-token", capturedHeaders!!["Authorization"])
        assertEquals("test-idem-key-123", capturedHeaders!!["Idempotency-Key"])
    }

    @Test
    fun `verifyPayment transmits exact verified keys providerOrderId, providerPaymentId, and signature`() = runBlocking {
        var capturedBody: String? = null
        var capturedUrl: String? = null

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedBody = request.body
                capturedUrl = request.url
                val jsonResponse = """
                    {
                        "data": {
                            "id": "p1",
                            "rideId": "r1",
                            "grossAmountMinor": 2500,
                            "currency": "INR",
                            "status": "CAPTURED",
                            "provider": "razorpay",
                            "providerOrderId": "ord_1",
                            "providerPaymentId": "pay_1",
                            "providerSignature": "sig_1",
                            "expiresAt": "2026-09-27T17:00:00.000Z",
                            "createdAt": "2026-09-27T16:00:00.000Z",
                            "updatedAt": "2026-09-27T16:05:00.000Z"
                        }
                    }
                """.trimIndent()
                return IshaaraResult.success(HttpResponse(200, jsonResponse))
            }
        }

        val dataSource = PaymentRemoteDataSourceImpl(fakeClient, NetworkConfig())
        val verifyDto = VerifyPaymentRequestDto(
            providerOrderId = "ord_1",
            providerPaymentId = "pay_1",
            signature = "sig_1"
        )
        val result = dataSource.verifyPayment("p1", verifyDto, "token123")

        assertTrue(result is IshaaraResult.Success)
        assertTrue(capturedUrl!!.endsWith("/payments/p1/verify"))
        assertTrue(capturedBody!!.contains("\"providerOrderId\":\"ord_1\""))
        assertTrue(capturedBody!!.contains("\"providerPaymentId\":\"pay_1\""))
        assertTrue(capturedBody!!.contains("\"signature\":\"sig_1\""))
    }

    @Test
    fun `dataSource handles network error and HTTP failures correctly`() = runBlocking {
        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return IshaaraResult.failure(IshaaraError.Network("Connection refused"))
            }
        }

        val dataSource = PaymentRemoteDataSourceImpl(fakeClient, NetworkConfig())
        val result = dataSource.getPaymentByRideId("r1", "token")

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Network)
    }
}
