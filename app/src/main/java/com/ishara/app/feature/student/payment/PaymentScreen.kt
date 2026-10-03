package com.ishara.app.feature.student.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraDivider
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraStatusChip
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.PaymentCheckoutSession
import com.ishara.app.domain.model.RideReceipt

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            IshaaraTopBar(
                title = stringResource(R.string.payment_screen_title),
                onBackClick = onNavigateBack
            )
        },
        containerColor = IshaaraTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is PaymentUiState.Loading -> {
                    IshaaraLoadingState(
                        message = stringResource(R.string.payment_loading_order),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PaymentUiState.OrderReady -> {
                    OrderReadyContent(
                        session = state.session,
                        pickupAddress = state.pickupAddress,
                        destinationAddress = state.destinationAddress,
                        isProcessing = state.isProcessing,
                        errorMessage = state.errorMessage,
                        noticeMessage = state.noticeMessage,
                        onPayClick = { viewModel.initiateCheckout(context) }
                    )
                }

                is PaymentUiState.Verifying -> {
                    IshaaraLoadingState(
                        message = stringResource(R.string.payment_verifying_with_bank),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PaymentUiState.Paid -> {
                    PaidReceiptContent(
                        receipt = state.receipt,
                        onTrackRideClick = { viewModel.onTrackRideClicked() }
                    )
                }

                is PaymentUiState.Refunded -> {
                    RefundedContent(
                        payment = state.payment,
                        isPartial = state.isPartial,
                        onBack = onNavigateBack
                    )
                }

                is PaymentUiState.PaymentFailed -> {
                    PaymentFailedContent(
                        message = state.message,
                        canRetry = state.canRetry,
                        onRetry = { viewModel.retryPayment() },
                        onBack = onNavigateBack
                    )
                }

                is PaymentUiState.Error -> {
                    IshaaraErrorState(
                        title = stringResource(R.string.payment_error_title),
                        message = state.message,
                        onRetryClick = { viewModel.loadPaymentState() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderReadyContent(
    session: PaymentCheckoutSession,
    pickupAddress: String?,
    destinationAddress: String?,
    isProcessing: Boolean,
    errorMessage: String?,
    noticeMessage: String?,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // Notice / Error Banner
        if (!errorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IshaaraTheme.shapes.sm)
                    .background(colors.dangerSubtle)
                    .border(IshaaraTheme.borders.hairline, colors.danger.copy(alpha = 0.4f), IshaaraTheme.shapes.sm)
                    .padding(spacing.sm)
            ) {
                Text(
                    text = errorMessage,
                    style = typography.bodySmall,
                    color = colors.danger
                )
            }
        } else if (!noticeMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IshaaraTheme.shapes.sm)
                    .background(colors.surfaceSubtle)
                    .border(IshaaraTheme.borders.hairline, colors.borderSubtle, IshaaraTheme.shapes.sm)
                    .padding(spacing.sm)
            ) {
                Text(
                    text = noticeMessage,
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }

        // Ride Context Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.payment_ride_details_header),
                style = typography.titleSmall,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(spacing.sm))

            // Origin
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.success)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Column {
                    Text(
                        text = stringResource(R.string.payment_pickup_label),
                        style = typography.labelSmall,
                        color = colors.foregroundSubtle
                    )
                    Text(
                        text = pickupAddress ?: stringResource(R.string.payment_origin_default),
                        style = typography.bodyMedium,
                        color = colors.foreground
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Column {
                    Text(
                        text = stringResource(R.string.payment_destination_label),
                        style = typography.labelSmall,
                        color = colors.foregroundSubtle
                    )
                    Text(
                        text = destinationAddress ?: stringResource(R.string.payment_destination_default),
                        style = typography.bodyMedium,
                        color = colors.foreground
                    )
                }
            }
        }

        // Fare Summary Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.payment_fare_title),
                    style = typography.titleMedium,
                    color = colors.foreground
                )
                Text(
                    text = session.fare.formatDisplay(),
                    style = typography.headlineSmall,
                    color = colors.foreground,
                    modifier = Modifier.semantics {
                        contentDescription = "Fare amount: ${session.fare.formatDisplay()}"
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))
            IshaaraDivider()
            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.payment_method_label),
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                IshaaraBadge(
                    text = stringResource(R.string.payment_method_upi_online),
                    variant = IshaaraBadgeVariant.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // Primary Pay CTA Button
        val payButtonText = stringResource(R.string.payment_pay_button, session.fare.formatDisplay())
        IshaaraButton(
            text = payButtonText,
            onClick = onPayClick,
            size = IshaaraButtonSize.Large,
            variant = IshaaraButtonVariant.Primary,
            loading = isProcessing,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = payButtonText
                }
        )
    }
}

@Composable
private fun PaidReceiptContent(
    receipt: RideReceipt,
    onTrackRideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Confirmation Status Chip
        IshaaraStatusChip(
            status = IshaaraTransitStatus.COMPLETED,
            overrideLabel = stringResource(R.string.payment_status_confirmed)
        )

        Text(
            text = stringResource(R.string.payment_receipt_title),
            style = typography.headlineSmall,
            color = colors.foreground
        )

        // Receipt Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.payment_receipt_amount_paid),
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = receipt.amount.formatDisplay(),
                    style = typography.titleLarge,
                    color = colors.success
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))
            IshaaraDivider()
            Spacer(modifier = Modifier.height(spacing.xs))

            ReceiptRow(
                label = stringResource(R.string.payment_receipt_payment_id),
                value = receipt.paymentId.takeLast(12)
            )

            if (!receipt.providerPaymentId.isNullOrBlank()) {
                ReceiptRow(
                    label = stringResource(R.string.payment_receipt_reference),
                    value = receipt.providerPaymentId
                )
            }

            if (!receipt.pickupAddress.isNullOrBlank()) {
                ReceiptRow(
                    label = stringResource(R.string.payment_pickup_label),
                    value = receipt.pickupAddress
                )
            }

            if (!receipt.destinationAddress.isNullOrBlank()) {
                ReceiptRow(
                    label = stringResource(R.string.payment_destination_label),
                    value = receipt.destinationAddress
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // CTA: Track Ride in Phase 11
        IshaaraButton(
            text = stringResource(R.string.payment_track_ride_button),
            onClick = onTrackRideClick,
            size = IshaaraButtonSize.Large,
            variant = IshaaraButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.bodySmall,
            color = colors.foregroundMuted
        )
        Text(
            text = value,
            style = typography.bodySmall,
            color = colors.foreground
        )
    }
}

@Composable
private fun RefundedContent(
    payment: com.ishara.app.domain.model.Payment,
    isPartial: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IshaaraStatusChip(
            status = IshaaraTransitStatus.PENDING,
            overrideLabel = if (isPartial) {
                stringResource(R.string.payment_status_partially_refunded)
            } else {
                stringResource(R.string.payment_status_refunded)
            }
        )

        Text(
            text = stringResource(R.string.payment_refund_title),
            style = typography.headlineSmall,
            color = colors.foreground
        )

        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.payment_refunded_amount),
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = payment.refundedAmount.formatDisplay(),
                    style = typography.titleLarge,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))
            IshaaraDivider()
            Spacer(modifier = Modifier.height(spacing.xs))

            ReceiptRow(
                label = stringResource(R.string.payment_gross_amount),
                value = payment.grossFare.formatDisplay()
            )

            ReceiptRow(
                label = stringResource(R.string.payment_receipt_payment_id),
                value = payment.id.takeLast(12)
            )

            if (!payment.providerPaymentId.isNullOrBlank()) {
                ReceiptRow(
                    label = stringResource(R.string.payment_receipt_reference),
                    value = payment.providerPaymentId
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        IshaaraButton(
            text = stringResource(R.string.payment_back_to_home),
            onClick = onBack,
            size = IshaaraButtonSize.Large,
            variant = IshaaraButtonVariant.Outlined,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PaymentFailedContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IshaaraStatusChip(
            status = IshaaraTransitStatus.CANCELLED,
            overrideLabel = stringResource(R.string.payment_failed_title)
        )

        Text(
            text = stringResource(R.string.payment_failed_title),
            style = typography.headlineSmall,
            color = colors.foreground
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(IshaaraTheme.shapes.sm)
                .background(colors.dangerSubtle)
                .border(IshaaraTheme.borders.hairline, colors.danger.copy(alpha = 0.4f), IshaaraTheme.shapes.sm)
                .padding(spacing.md)
        ) {
            Text(
                text = message,
                style = typography.bodyMedium,
                color = colors.danger
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))

        if (canRetry) {
            IshaaraButton(
                text = stringResource(R.string.payment_retry_button),
                onClick = onRetry,
                size = IshaaraButtonSize.Large,
                variant = IshaaraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IshaaraButton(
            text = stringResource(R.string.payment_back_to_home),
            onClick = onBack,
            size = IshaaraButtonSize.Large,
            variant = IshaaraButtonVariant.Outlined,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
