package com.ishara.app.feature.student.fare

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraIconButton
import com.ishara.app.core.designsystem.component.IshaaraIconButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.FareBreakdownDetails
import com.ishara.app.domain.model.RideFare

@Composable
fun FareSummaryScreen(
    viewModel: FareSummaryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.onNavigateBack()
        onNavigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IshaaraTheme.colors.background)
    ) {
        when (val state = uiState) {
            is FareUiState.Loading -> {
                IshaaraLoadingState(message = stringResource(R.string.location_loading))
            }
            is FareUiState.Content -> {
                FareContent(
                    state = state,
                    onBackClick = {
                        viewModel.onNavigateBack()
                        onNavigateBack()
                    },
                    onRefreshClick = { viewModel.refresh() },
                    onProceedToPayment = { viewModel.onProceedToPayment() }
                )
            }
            is FareUiState.Error -> {
                IshaaraErrorState(
                    title = "Fare Unavailable",
                    message = state.message,
                    retryLabel = stringResource(R.string.retry_button),
                    onRetryClick = { viewModel.loadFare() }
                )
            }
        }
    }
}

@Composable
private fun FareContent(
    state: FareUiState.Content,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onProceedToPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val fare = state.fare

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IshaaraIconButton(
                onClick = onBackClick,
                contentDescription = "Back",
                variant = IshaaraIconButtonVariant.Standard
            ) {
                Text(
                    text = "←",
                    style = typography.titleLarge,
                    color = colors.foreground
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fare & Billing Summary",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground
                )
                Text(
                    text = "Ride ID: ${fare.rideId.takeLast(8)}",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = spacing.sm),
                    strokeWidth = 2.dp,
                    color = colors.accent
                )
            } else {
                IshaaraIconButton(
                    onClick = onRefreshClick,
                    contentDescription = "Refresh Fare",
                    variant = IshaaraIconButtonVariant.Standard
                ) {
                    Text(
                        text = "↻",
                        style = typography.titleLarge,
                        color = colors.foregroundMuted
                    )
                }
            }
        }

        // Notice Banner (if any)
        AnimatedVisibility(
            visible = state.bannerNotice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IshaaraPalette.Amber100)
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
            ) {
                Text(
                    text = state.bannerNotice ?: "",
                    style = typography.labelSmall,
                    color = IshaaraPalette.Amber900
                )
            }
        }

        Column(modifier = Modifier.padding(spacing.md)) {
            // State Badge
            val isFinal = state.isFinal
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFinal) IshaaraPalette.Emerald100 else IshaaraPalette.Amber100)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isFinal) "AUTHORITATIVE FINAL FARE" else "ESTIMATED FARE PROJECTION",
                    style = typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFinal) IshaaraPalette.Emerald900 else IshaaraPalette.Amber900
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Primary Total Card
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Payable Fare",
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = fare.currentFare.formatDisplay(),
                        style = typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = state.statusDescription,
                        style = typography.labelSmall,
                        color = colors.foregroundSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Breakdown Section
            val breakdown = fare.effectiveBreakdown
            if (breakdown != null) {
                FareBreakdownCard(breakdown = breakdown)
                Spacer(modifier = Modifier.height(spacing.md))
            }

            // Calculation Metadata Card
            if (breakdown != null && breakdown.calculatedAt.isNotBlank()) {
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(spacing.md)) {
                        Text(
                            text = "Pricing Information",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.foreground
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Policy Version", style = typography.bodySmall, color = colors.foregroundMuted)
                            Text(text = breakdown.pricingPolicyVersion, style = typography.bodySmall, color = colors.foreground)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Calculated At", style = typography.bodySmall, color = colors.foregroundMuted)
                            Text(text = breakdown.calculatedAt.take(19).replace("T", " "), style = typography.bodySmall, color = colors.foreground)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacing.lg))
            }

            // Payment Handoff Action Button
            IshaaraButton(
                text = "Proceed to Payment (${fare.currentFare.formatDisplay()})",
                onClick = onProceedToPayment,
                size = IshaaraButtonSize.Large,
                variant = IshaaraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "Payments are processed securely via verified gateways (Phase A13).",
                style = typography.labelSmall,
                color = colors.foregroundSubtle,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun FareBreakdownCard(
    breakdown: FareBreakdownDetails,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = "Itemized Breakdown",
                style = typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(spacing.sm))

            // Base Fare
            BreakdownRow(
                label = "Base Fare",
                detail = "Initial flag drop",
                amount = breakdown.baseFare.formatDisplay()
            )

            // Distance Fare
            BreakdownRow(
                label = "Distance Fare",
                detail = "${breakdown.distanceKmFormatted} traveled",
                amount = breakdown.distanceComponent.formatDisplay()
            )

            // Time Component (if > 0)
            if (breakdown.timeComponent.amountMinor > 0L) {
                BreakdownRow(
                    label = "Time Fare",
                    detail = breakdown.durationMinutesFormatted ?: "Duration charge",
                    amount = breakdown.timeComponent.formatDisplay()
                )
            }

            // Subtotal
            Spacer(modifier = Modifier.height(spacing.xs))
            Divider(color = colors.surfaceSubtle)
            Spacer(modifier = Modifier.height(spacing.xs))

            BreakdownRow(
                label = "Subtotal",
                detail = null,
                amount = breakdown.subtotal.formatDisplay(),
                isBold = true
            )

            // Taxes (GST)
            if (breakdown.tax.amountMinor > 0L) {
                BreakdownRow(
                    label = "Taxes & Cess",
                    detail = "Applicable transit tax",
                    amount = breakdown.tax.formatDisplay()
                )
            }

            // Service Fee
            if (breakdown.serviceFee.amountMinor > 0L) {
                BreakdownRow(
                    label = "Platform Service Fee",
                    detail = "Included in total",
                    amount = breakdown.serviceFee.formatDisplay()
                )
            }

            // Discount (if any)
            if (breakdown.discount.amountMinor > 0L) {
                BreakdownRow(
                    label = "Discount",
                    detail = "Promotional deduction",
                    amount = "-${breakdown.discount.formatDisplay()}",
                    isDiscount = true
                )
            }

            // Total
            Spacer(modifier = Modifier.height(spacing.xs))
            Divider(color = colors.surfaceSubtle)
            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Text(
                    text = breakdown.total.formatDisplay(),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    detail: String?,
    amount: String,
    isBold: Boolean = false,
    isDiscount: Boolean = false
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = if (isBold) typography.bodyMedium else typography.bodySmall,
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.foreground
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = typography.labelSmall,
                    color = colors.foregroundSubtle
                )
            }
        }
        Text(
            text = amount,
            style = if (isBold) typography.bodyMedium else typography.bodySmall,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isDiscount) IshaaraPalette.Emerald500 else colors.foreground
        )
    }
}
