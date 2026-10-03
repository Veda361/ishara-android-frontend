package com.ishara.app

import android.content.Context
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.payment.PaymentCheckoutParams
import com.ishara.app.core.payment.PaymentLauncher
import com.ishara.app.core.payment.PaymentLauncherResult
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.model.Payment
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.PaymentVerificationRequest
import com.ishara.app.domain.repository.PaymentRepository
import com.ishara.app.domain.usecase.CreatePaymentOrderUseCase
import com.ishara.app.domain.usecase.GetRidePaymentUseCase
import com.ishara.app.domain.usecase.VerifyPaymentUseCase
import com.ishara.app.feature.student.payment.PaymentUiState
import com.ishara.app.feature.student.payment.PaymentViewModel
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentViewModelTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private class FakePaymentRepository : PaymentRepository {
        var existingPaymentResult: IshaaraResult<Payment> = IshaaraResult.failure(IshaaraError.NotFound("No payment"))
        var createOrderResult: IshaaraResult<PaymentCheckoutSession> = IshaaraResult.success(
            PaymentCheckoutSession(
                paymentId = "pay_001",
                rideId = "ride_001",
                fare = Money(2500L, "INR"),
                provider = "razorpay",
                providerOrderId = "order_001",
                keyId = "rzp_test_123",
                qrPayload = "upi://pay?pa=test@icici&am=25.00",
                expiresAt = "2026-09-27T17:00:00.000Z"
            )
        )
        var verifyResult: IshaaraResult<Payment> = IshaaraResult.success(
            Payment(
                id = "pay_001",
                rideId = "ride_001",
                userId = "user_001",
                driverId = "driver_001",
                grossFare = Money(2500L, "INR"),
                platformFee = Money(250L, "INR"),
                driverShare = Money(2250L, "INR"),
                refundedAmount = Money(0L, "INR"),
                status = DomainPaymentStatus.CAPTURED,
                provider = "razorpay",
                providerOrderId = "order_001",
                providerPaymentId = "pay_tx_123",
                capturedAt = "2026-09-27T16:30:00.000Z",
                expiresAt = "2026-09-27T17:00:00.000Z",
                createdAt = "2026-09-27T16:25:00.000Z"
            )
        )

        var createOrderCalls = 0
        var verifyCalls = 0

        override suspend fun createPaymentOrder(
            rideId: String,
            idempotencyKey: String?
        ): IshaaraResult<PaymentCheckoutSession> {
            createOrderCalls++
            return createOrderResult
        }

        override suspend fun getPaymentByRideId(rideId: String): IshaaraResult<Payment> {
            return existingPaymentResult
        }

        override suspend fun verifyPayment(
            paymentId: String,
            request: PaymentVerificationRequest
        ): IshaaraResult<Payment> {
            verifyCalls++
            return verifyResult
        }

        override fun clearPaymentCache() {}
    }

    private class FakePaymentLauncher : PaymentLauncher {
        var launcherResult: PaymentLauncherResult = PaymentLauncherResult.Success(
            providerOrderId = "order_001",
            providerPaymentId = "pay_tx_123",
            signature = "sig_123"
        )
        var launchCount = 0

        override suspend fun launchCheckout(
            context: Context,
            params: PaymentCheckoutParams
        ): PaymentLauncherResult {
            launchCount++
            return launcherResult
        }
    }

    @Test
    fun `Initial load transitions to OrderReady when no captured payment exists`() = runBlocking {
        val repository = FakePaymentRepository()
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            pickupAddress = "Jhansi Station",
            destinationAddress = "SRGI College",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val state = viewModel.uiState.value
        assertTrue("Expected OrderReady state, got $state", state is PaymentUiState.OrderReady)
        val ready = state as PaymentUiState.OrderReady
        assertEquals("pay_001", ready.session.paymentId)
        assertEquals("₹25.00", ready.session.fare.formatDisplay())
        assertEquals("Jhansi Station", ready.pickupAddress)
        assertEquals("SRGI College", ready.destinationAddress)
        assertEquals(1, repository.createOrderCalls)
    }

    @Test
    fun `Initial load transitions directly to Paid receipt when payment is already captured`() = runBlocking {
        val repository = FakePaymentRepository().apply {
            existingPaymentResult = IshaaraResult.success(
                Payment(
                    id = "pay_already_captured",
                    rideId = "ride_001",
                    userId = "user_001",
                    driverId = "driver_001",
                    grossFare = Money(2500L, "INR"),
                    platformFee = Money(250L, "INR"),
                    driverShare = Money(2250L, "INR"),
                    refundedAmount = Money(0L, "INR"),
                    status = DomainPaymentStatus.CAPTURED,
                    provider = "razorpay",
                    providerOrderId = "order_001",
                    providerPaymentId = "pay_tx_existing",
                    capturedAt = "2026-09-27T16:00:00.000Z",
                    expiresAt = "2026-09-27T16:30:00.000Z",
                    createdAt = "2026-09-27T15:55:00.000Z"
                )
            )
        }
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            pickupAddress = "Jhansi Station",
            destinationAddress = "SRGI College",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Paid state, got $state", state is PaymentUiState.Paid)
        val paid = state as PaymentUiState.Paid
        assertEquals("pay_already_captured", paid.receipt.paymentId)
        assertEquals("₹25.00", paid.receipt.amount.formatDisplay())
        assertEquals(0, repository.createOrderCalls) // Should NOT create duplicate order
    }

    @Test
    fun `Initial load transitions to Error when backend fails to create payment order`() = runBlocking {
        val repository = FakePaymentRepository().apply {
            createOrderResult = IshaaraResult.failure(IshaaraError.Network("Connection timeout"))
        }
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Error state, got $state", state is PaymentUiState.Error)
        val error = state as PaymentUiState.Error
        assertTrue(error.canRetry)
    }

    @Test
    fun `initiateCheckout executes provider launch and verifies captured payment`() = runBlocking {
        val repository = FakePaymentRepository()
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            pickupAddress = "Jhansi Station",
            destinationAddress = "SRGI College",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        assertTrue(viewModel.uiState.value is PaymentUiState.OrderReady)

        val mockContext = ContextWrapperFake()
        viewModel.initiateCheckout(mockContext)

        assertEquals(1, launcher.launchCount)
        assertEquals(1, repository.verifyCalls)

        val state = viewModel.uiState.value
        assertTrue("Expected Paid state after verification, got $state", state is PaymentUiState.Paid)
        val paid = state as PaymentUiState.Paid
        assertEquals("pay_001", paid.receipt.paymentId)
        assertEquals("pay_tx_123", paid.receipt.providerPaymentId)
    }

    @Test
    fun `initiateCheckout when user cancels checkout returns to OrderReady with notice`() = runBlocking {
        val repository = FakePaymentRepository()
        val launcher = FakePaymentLauncher().apply {
            launcherResult = PaymentLauncherResult.Cancelled
        }
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val mockContext = ContextWrapperFake()
        viewModel.initiateCheckout(mockContext)

        assertEquals(0, repository.verifyCalls)
        val state = viewModel.uiState.value
        assertTrue(state is PaymentUiState.OrderReady)
        val ready = state as PaymentUiState.OrderReady
        assertNotNull(ready.noticeMessage)
        assertEquals("Payment was cancelled.", ready.noticeMessage)
    }

    @Test
    fun `initiateCheckout when verification fails returns to OrderReady with error`() = runBlocking {
        val repository = FakePaymentRepository().apply {
            verifyResult = IshaaraResult.failure(IshaaraError.Server(400, "Cryptographic signature verification failed"))
        }
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val mockContext = ContextWrapperFake()
        viewModel.initiateCheckout(mockContext)

        assertEquals(1, repository.verifyCalls)
        val state = viewModel.uiState.value
        assertTrue(state is PaymentUiState.OrderReady)
        val ready = state as PaymentUiState.OrderReady
        assertNotNull(ready.errorMessage)
        assertTrue(ready.errorMessage!!.contains("Cryptographic signature verification failed"))
    }

    @Test
    fun `onTrackRideClicked navigates to student live tracking route`() = runBlocking {
        val repository = FakePaymentRepository()
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_xyz_123",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        viewModel.onTrackRideClicked()
        val command = navManager.commands.first()
        assertTrue(command is NavigationCommand.NavigateTo)
        assertEquals("student/ride/ride_xyz_123", (command as NavigationCommand.NavigateTo).route)
    }

    @Test
    fun `Initial load transitions to Refunded state when payment is fully refunded`() = runBlocking {
        val repository = FakePaymentRepository().apply {
            existingPaymentResult = IshaaraResult.success(
                Payment(
                    id = "pay_refunded",
                    rideId = "ride_001",
                    userId = "user_001",
                    driverId = "driver_001",
                    grossFare = Money(2500L, "INR"),
                    platformFee = Money(250L, "INR"),
                    driverShare = Money(2250L, "INR"),
                    refundedAmount = Money(2500L, "INR"),
                    status = DomainPaymentStatus.REFUNDED,
                    provider = "razorpay",
                    providerOrderId = "order_001",
                    providerPaymentId = "pay_tx_refunded",
                    capturedAt = "2026-09-27T16:00:00.000Z",
                    expiresAt = "2026-09-27T16:30:00.000Z",
                    createdAt = "2026-09-27T15:55:00.000Z"
                )
            )
        }
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val state = viewModel.uiState.value
        assertTrue("Expected Refunded state, got $state", state is PaymentUiState.Refunded)
        val refunded = state as PaymentUiState.Refunded
        assertEquals(false, refunded.isPartial)
        assertEquals(2500L, refunded.payment.refundedAmount.amountMinor)
    }

    @Test
    fun `Initial load transitions to PaymentFailed state when backend reports CANCELLED payment`() = runBlocking {
        val repository = FakePaymentRepository().apply {
            existingPaymentResult = IshaaraResult.success(
                Payment(
                    id = "pay_cancelled",
                    rideId = "ride_001",
                    userId = "user_001",
                    driverId = "driver_001",
                    grossFare = Money(2500L, "INR"),
                    platformFee = Money(250L, "INR"),
                    driverShare = Money(2250L, "INR"),
                    refundedAmount = Money(0L, "INR"),
                    status = DomainPaymentStatus.CANCELLED,
                    provider = "razorpay",
                    providerOrderId = "order_001",
                    expiresAt = "2026-09-27T16:30:00.000Z",
                    createdAt = "2026-09-27T15:55:00.000Z"
                )
            )
        }
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        val state = viewModel.uiState.value
        assertTrue("Expected PaymentFailed state, got $state", state is PaymentUiState.PaymentFailed)
        val failed = state as PaymentUiState.PaymentFailed
        assertEquals(DomainPaymentStatus.CANCELLED, failed.status)
        assertTrue(failed.canRetry)
    }

    @Test
    fun `retryPayment re-invokes loadPaymentState safely`() = runBlocking {
        val repository = FakePaymentRepository().apply {
            createOrderResult = IshaaraResult.failure(IshaaraError.Network("Temporary timeout"))
        }
        val launcher = FakePaymentLauncher()
        val navManager = NavigationManager()

        val viewModel = PaymentViewModel(
            rideId = "ride_001",
            getRidePaymentUseCase = GetRidePaymentUseCase(repository),
            createPaymentOrderUseCase = CreatePaymentOrderUseCase(repository),
            verifyPaymentUseCase = VerifyPaymentUseCase(repository),
            paymentLauncher = launcher,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider(),
            externalScope = this
        )

        assertTrue(viewModel.uiState.value is PaymentUiState.Error)
        assertEquals(1, repository.createOrderCalls)

        // Resolve network error and call retryPayment()
        repository.createOrderResult = IshaaraResult.success(
            PaymentCheckoutSession(
                paymentId = "pay_002",
                rideId = "ride_001",
                fare = Money(3000L, "INR"),
                provider = "razorpay",
                providerOrderId = "order_002",
                expiresAt = "2026-09-27T18:00:00.000Z"
            )
        )

        viewModel.retryPayment()

        val state = viewModel.uiState.value
        assertTrue("Expected OrderReady state after retry, got $state", state is PaymentUiState.OrderReady)
        assertEquals(2, repository.createOrderCalls)
    }
}

/**
 * Minimal stub Context for unit testing without Android runtime.
 */
private class ContextWrapperFake : android.content.ContextWrapper(null)
