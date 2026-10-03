package com.ishara.app.feature.student.payment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.payment.PaymentCheckoutParams
import com.ishara.app.core.payment.PaymentLauncher
import com.ishara.app.core.payment.PaymentLauncherResult
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.PaymentMapper
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.PaymentVerificationRequest
import com.ishara.app.domain.usecase.CreatePaymentOrderUseCase
import com.ishara.app.domain.usecase.GetRidePaymentUseCase
import com.ishara.app.domain.usecase.VerifyPaymentUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Production ViewModel managing the Student Ride Payment and Ticketing flow.
 *
 * Implements:
 * 1. Double-tap and duplicate payment submission prevention.
 * 2. Stable Idempotency-Key per checkout session.
 * 3. Authoritative server verification (never trusts client callback alone).
 * 4. Automatic recovery of already-captured payments.
 * 5. Clean handoff into Phase 11 Live Ride Tracking.
 */
class PaymentViewModel(
    val rideId: String,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null,
    private val getRidePaymentUseCase: GetRidePaymentUseCase,
    private val createPaymentOrderUseCase: CreatePaymentOrderUseCase,
    private val verifyPaymentUseCase: VerifyPaymentUseCase,
    private val paymentLauncher: PaymentLauncher,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val tag = "PaymentViewModel"
    private val scope = externalScope ?: viewModelScope

    val idempotencyKey: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    init {
        loadPaymentState()
    }

    /**
     * Reconciles current ride payment state:
     * 1. Checks if payment is already captured.
     * 2. If not, requests or reuses active payment order from backend.
     */
    fun loadPaymentState() {
        if (activeJob?.isActive == true) return

        _uiState.value = PaymentUiState.Loading

        activeJob = scope.launch(dispatchers.main) {
            // First check if an existing authoritative payment exists for this ride
            when (val existingResult = getRidePaymentUseCase.execute(rideId)) {
                is IshaaraResult.Success -> {
                    val payment = existingResult.data
                    if (payment.status.isCaptured) {
                        IshaaraLogger.d(tag, "Ride $rideId already has captured payment: ${payment.id}")
                        _uiState.value = PaymentUiState.Paid(
                            PaymentMapper.toReceipt(payment, pickupAddress, destinationAddress)
                        )
                        return@launch
                    } else if (payment.status.isRefunded) {
                        IshaaraLogger.d(tag, "Ride $rideId has refunded payment: ${payment.id}, status=${payment.status}")
                        _uiState.value = PaymentUiState.Refunded(
                            payment = payment,
                            isPartial = payment.status == com.ishara.app.domain.model.DomainPaymentStatus.PARTIALLY_REFUNDED
                        )
                        return@launch
                    } else if (payment.status == com.ishara.app.domain.model.DomainPaymentStatus.FAILED ||
                        payment.status == com.ishara.app.domain.model.DomainPaymentStatus.CANCELLED
                    ) {
                        IshaaraLogger.d(tag, "Ride $rideId has failed/cancelled payment: ${payment.id}, status=${payment.status}")
                        _uiState.value = PaymentUiState.PaymentFailed(
                            message = "Payment ${payment.status.name.lowercase()}",
                            canRetry = true,
                            status = payment.status
                        )
                        return@launch
                    }
                }
                is IshaaraResult.Failure -> {
                    // Not found or unpaid is normal; proceed to create order
                }
            }

            // Create or retrieve active checkout session
            when (val orderResult = createPaymentOrderUseCase.execute(rideId = rideId, idempotencyKey = idempotencyKey)) {
                is IshaaraResult.Success -> {
                    val session = orderResult.data
                    _uiState.value = PaymentUiState.OrderReady(
                        session = session,
                        pickupAddress = pickupAddress,
                        destinationAddress = destinationAddress
                    )
                }
                is IshaaraResult.Failure -> {
                    handleError(orderResult.error)
                }
            }
        }
    }

    /**
     * Retries payment initialization or reconciliation.
     */
    fun retryPayment() {
        loadPaymentState()
    }

    /**
     * Initiates external payment provider checkout.
     * Guarded against duplicate / concurrent calls.
     */
    fun initiateCheckout(context: Context) {
        val currentState = _uiState.value as? PaymentUiState.OrderReady ?: return
        if (currentState.isProcessing || activeJob?.isActive == true) return

        val session = currentState.session

        _uiState.update {
            (it as? PaymentUiState.OrderReady)?.copy(
                isProcessing = true,
                errorMessage = null,
                noticeMessage = null
            ) ?: it
        }

        activeJob = scope.launch(dispatchers.main) {
            val params = PaymentCheckoutParams(
                paymentId = session.paymentId,
                rideId = session.rideId,
                providerOrderId = session.providerOrderId,
                amountMinor = session.fare.amountMinor,
                currency = session.fare.currency,
                keyId = session.keyId,
                qrPayload = session.qrPayload
            )

            when (val launcherResult = paymentLauncher.launchCheckout(context, params)) {
                is PaymentLauncherResult.Success -> {
                    // Provider checkout succeeded; now perform authoritative backend verification
                    verifyPayment(
                        paymentId = session.paymentId,
                        providerOrderId = launcherResult.providerOrderId,
                        providerPaymentId = launcherResult.providerPaymentId,
                        signature = launcherResult.signature,
                        session = session
                    )
                }
                is PaymentLauncherResult.Cancelled -> {
                    _uiState.update {
                        (it as? PaymentUiState.OrderReady)?.copy(
                            isProcessing = false,
                            noticeMessage = "Payment was cancelled."
                        ) ?: it
                    }
                }
                is PaymentLauncherResult.Failed -> {
                    _uiState.update {
                        (it as? PaymentUiState.OrderReady)?.copy(
                            isProcessing = false,
                            errorMessage = launcherResult.reason
                        ) ?: it
                    }
                }
            }
        }
    }

    /**
     * Submits client signature to POST /api/v1/payments/:paymentId/verify
     */
    private fun verifyPayment(
        paymentId: String,
        providerOrderId: String,
        providerPaymentId: String,
        signature: String,
        session: PaymentCheckoutSession
    ) {
        _uiState.value = PaymentUiState.Verifying(paymentId = paymentId)

        activeJob = scope.launch(dispatchers.main) {
            val request = PaymentVerificationRequest(
                providerOrderId = providerOrderId,
                providerPaymentId = providerPaymentId,
                signature = signature
            )

            when (val verifyResult = verifyPaymentUseCase.execute(paymentId, request)) {
                is IshaaraResult.Success -> {
                    val payment = verifyResult.data
                    _uiState.value = PaymentUiState.Paid(
                        PaymentMapper.toReceipt(payment, pickupAddress, destinationAddress)
                    )
                }
                is IshaaraResult.Failure -> {
                    _uiState.value = PaymentUiState.OrderReady(
                        session = session,
                        pickupAddress = pickupAddress,
                        destinationAddress = destinationAddress,
                        isProcessing = false,
                        errorMessage = "Payment verification failed: ${verifyResult.error.message}"
                    )
                }
            }
        }
    }

    /**
     * Navigates from confirmed receipt to Live Ride Tracking.
     */
    fun onTrackRideClicked() {
        navigationManager.navigate(IshaaraDestination.StudentRideTracking.createRoute(rideId))
    }

    /**
     * Navigates back.
     */
    fun onBackClicked() {
        navigationManager.navigateUp()
    }

    private fun handleError(error: IshaaraError) {
        val isAuth = error is IshaaraError.Authentication ||
                error is IshaaraError.Forbidden ||
                (error is IshaaraError.Server && error.code in listOf(401, 403))
        val message = when {
            isAuth -> "You do not have permission to pay for this ride."
            error is IshaaraError.Network -> "Network unavailable. Please check your connection."
            error is IshaaraError.Conflict -> error.message
            else -> "Unable to process payment order. Please try again."
        }
        _uiState.value = PaymentUiState.Error(
            message = message,
            canRetry = !isAuth,
            isUnauthorized = isAuth
        )
    }
}
