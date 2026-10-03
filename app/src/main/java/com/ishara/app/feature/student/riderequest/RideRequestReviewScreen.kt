package com.ishara.app.feature.student.riderequest

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.RideRequestResult

/**
 * Production Ride Request Review Screen.
 *
 * Provides a clean, calm, transportation-focused interface where the passenger:
 * 1. Reviews physical pickup and destination addresses.
 * 2. Confirms verified vehicle and driver details.
 * 3. Submits the request with duplicate-protection.
 * 4. Views factual PENDING confirmation upon backend success.
 */
@Composable
fun RideRequestReviewScreen(
    viewModel: RideRequestViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            IshaaraTopBar(
                title = if (uiState.stage is RideRequestStage.Submitted) "Request Sent" else "Review Ride",
                onBackClick = {
                    if (uiState.stage is RideRequestStage.Submitted) {
                        viewModel.onDoneClicked()
                    } else {
                        viewModel.onBackClicked()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val stage = uiState.stage) {
                is RideRequestStage.Submitted -> {
                    RideRequestSuccessContent(
                        result = stage.result,
                        onDoneClick = { viewModel.onDoneClicked() },
                        onViewStatusClick = { viewModel.onViewStatusClicked(it) }
                    )
                }
                else -> {
                    RideRequestReviewContent(
                        uiState = uiState,
                        onSubmitClick = { viewModel.submitRideRequest() },
                        onRetryClick = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RideRequestReviewContent(
    uiState: RideRequestUiState,
    onSubmitClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val trip = uiState.trip

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Text(
            text = "Request this ride",
            style = typography.headlineSmall,
            color = colors.foreground
        )

        // Route Summary Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = "JOURNEY DETAILS",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )

                // Pickup
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.primary, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Pickup Location",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = uiState.pickupAddress,
                            style = typography.bodyMedium,
                            color = colors.foreground
                        )
                    }
                }

                // Connecting line
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(2.dp)
                        .height(14.dp)
                        .background(colors.border)
                )

                // Destination
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.accent, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Destination",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = uiState.destinationAddress,
                            style = typography.bodyMedium,
                            color = colors.foreground
                        )
                    }
                }
            }
        }

        // Vehicle & Operator Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = "TRANSIT VEHICLE",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )

                Text(
                    text = trip.vehicle.displayTitle,
                    style = typography.titleLarge,
                    color = colors.foreground
                )

                Text(
                    text = "Plate: ${trip.vehicle.registrationNumber}",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Driver", style = typography.labelSmall, color = colors.foregroundMuted)
                        Text(text = trip.driver.name, style = typography.titleSmall, color = colors.foreground)
                    }
                    Column {
                        Text(text = "Est. Travel", style = typography.labelSmall, color = colors.foregroundMuted)
                        Text(text = trip.formattedDuration ?: "Active", style = typography.titleSmall, color = colors.foreground)
                    }
                    Column {
                        Text(text = "Walk to Pickup", style = typography.labelSmall, color = colors.foregroundMuted)
                        Text(text = trip.formattedPickupDistance, style = typography.titleSmall, color = colors.foreground)
                    }
                }
            }
        }

        // Notice Note
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, IshaaraTheme.shapes.card)
                .padding(spacing.sm)
        ) {
            Text(
                text = "Your request will be submitted to the operator. The request will remain pending until confirmed.",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )
        }

        // Error message if submission failed
        if (uiState.stage is RideRequestStage.Error) {
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = "Submission Failed",
                        style = typography.titleSmall,
                        color = colors.danger
                    )
                    Text(
                        text = uiState.errorMessage ?: "Please check your network connection and try again.",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    IshaaraButton(
                        text = "Retry request",
                        onClick = onRetryClick,
                        variant = IshaaraButtonVariant.Secondary,
                        size = IshaaraButtonSize.Small
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // Primary 54dp CTA Button
        IshaaraButton(
            text = if (uiState.isSubmitting) "Requesting…" else "Request ride",
            onClick = onSubmitClick,
            enabled = !uiState.isSubmitting,
            variant = IshaaraButtonVariant.Primary,
            size = IshaaraButtonSize.Large,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
    }
}

@Composable
private fun RideRequestSuccessContent(
    result: RideRequestResult,
    onDoneClick: () -> Unit,
    onViewStatusClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Spacer(modifier = Modifier.height(spacing.lg))

        // Success Indicator
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.primarySubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", style = typography.headlineMedium, color = colors.primary)
        }

        Text(
            text = "Ride Request Sent",
            style = typography.headlineMedium,
            color = colors.foreground
        )

        Text(
            text = "Your request has been submitted to the driver. The status is currently pending confirmation.",
            style = typography.bodyMedium,
            color = colors.foregroundMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        // Status Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS",
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    IshaaraBadge(
                        text = result.status.name,
                        variant = IshaaraBadgeVariant.Neutral
                    )
                }

                Text(
                    text = "Request ID",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
                Text(
                    text = result.id,
                    style = typography.bodySmall,
                    color = colors.foreground
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Pickup: ${result.pickupAddress}",
                    style = typography.bodySmall,
                    color = colors.foreground
                )
                Text(
                    text = "Destination: ${result.destinationAddress}",
                    style = typography.bodySmall,
                    color = colors.foreground
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Note: Requests expire in 2 minutes if unconfirmed by the operator.",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Primary 54dp CTA Button: Track Status
        IshaaraButton(
            text = "Track request status",
            onClick = { onViewStatusClick(result.id) },
            variant = IshaaraButtonVariant.Primary,
            size = IshaaraButtonSize.Large,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )

        // Secondary CTA: Done
        IshaaraButton(
            text = "Back to home",
            onClick = onDoneClick,
            variant = IshaaraButtonVariant.Secondary,
            size = IshaaraButtonSize.Large,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
    }
}
