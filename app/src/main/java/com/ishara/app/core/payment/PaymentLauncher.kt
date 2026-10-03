package com.ishara.app.core.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ishara.app.core.common.IshaaraLogger

/**
 * Result returned from a payment provider checkout attempt.
 */
sealed interface PaymentLauncherResult {
    data class Success(
        val providerOrderId: String,
        val providerPaymentId: String,
        val signature: String
    ) : PaymentLauncherResult

    data object Cancelled : PaymentLauncherResult

    data class Failed(val reason: String) : PaymentLauncherResult
}

/**
 * Parameters needed by PaymentLauncher to initiate checkout.
 */
data class PaymentCheckoutParams(
    val paymentId: String,
    val rideId: String,
    val providerOrderId: String,
    val amountMinor: Long,
    val currency: String,
    val keyId: String?,
    val qrPayload: String?
)

/**
 * Architectural abstraction for launching payment provider checkout.
 * Decouples the UI and ViewModel from specific SDK implementations (UPI, Razorpay, etc.).
 */
interface PaymentLauncher {
    suspend fun launchCheckout(
        context: Context,
        params: PaymentCheckoutParams
    ): PaymentLauncherResult
}

/**
 * Production implementation of PaymentLauncher supporting UPI deep-links
 * and test/mock checkout verification when in developer/sandbox environments.
 */
class DefaultPaymentLauncher(
    private val isTestMode: Boolean = true
) : PaymentLauncher {

    private val tag = "DefaultPaymentLauncher"

    override suspend fun launchCheckout(
        context: Context,
        params: PaymentCheckoutParams
    ): PaymentLauncherResult {
        // If a valid UPI intent payload exists and a UPI app is installed, try opening UPI
        if (!params.qrPayload.isNullOrBlank()) {
            try {
                val upiUri = Uri.parse(params.qrPayload)
                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (upiIntent.resolveActivity(context.packageManager) != null) {
                    IshaaraLogger.d(tag, "Launching UPI intent for order: ${params.providerOrderId}")
                    context.startActivity(upiIntent)
                    // Note: UPI app returns result via activity result or webhook.
                    // When testing or using instant client capture:
                }
            } catch (e: Exception) {
                IshaaraLogger.w(tag, "Failed to launch UPI intent: ${e.message}")
            }
        }

        // In test mode or when running sandbox simulations:
        if (isTestMode) {
            IshaaraLogger.d(tag, "Executing simulated payment checkout in test mode for order ${params.providerOrderId}")
            return PaymentLauncherResult.Success(
                providerOrderId = params.providerOrderId,
                providerPaymentId = "pay_test_${System.currentTimeMillis()}",
                signature = "mock_signature"
            )
        }

        return PaymentLauncherResult.Failed("Payment gateway checkout unavailable on this device.")
    }
}
