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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.model.displayName

/**
 * Production Ride Request Status Tracking Screen.
 *
 * Displays the authoritative state machine for a passenger ride request:
 * - PENDING: Waiting for driver response, live countdown, cancellation action with modal.
 * - ACCEPTED: Driver accepted, route handoff CTA to Phase A11 (Live Ride).
 * - REJECTED: Driver rejected, displays authoritative rejection reason.
 * - CANCELLED: Passenger cancelled, displays authoritative cancellation reason.
 * - EXPIRED: Request timed out without operator confirmation.
 */
@Composable
fun RideRequestStatusScreen(
    viewModel: RideRequestStatusViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReasonText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            IshaaraTopBar(
                title = "Request Status",
                onBackClick = { viewModel.onBackToHome() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Status Hero Card
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val badgeVariant = when (uiState.status) {
                        RideRequestStatus.PENDING -> IshaaraBadgeVariant.Neutral
                        RideRequestStatus.ACCEPTED -> IshaaraBadgeVariant.Success
                        RideRequestStatus.REJECTED -> IshaaraBadgeVariant.Danger
                        RideRequestStatus.CANCELLED -> IshaaraBadgeVariant.Neutral
                        RideRequestStatus.EXPIRED -> IshaaraBadgeVariant.Danger
                    }

                    IshaaraBadge(
                        text = uiState.status.displayName.uppercase(),
                        variant = badgeVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = when (uiState.status) {
                            RideRequestStatus.PENDING -> "Waiting for Driver Confirmation"
                            RideRequestStatus.ACCEPTED -> "Ride Request Accepted!"
                            RideRequestStatus.REJECTED -> "Request Declined"
                            RideRequestStatus.CANCELLED -> "Request Cancelled"
                            RideRequestStatus.EXPIRED -> "Request Expired"
                        },
                        style = typography.titleLarge,
                        color = colors.foreground
                    )

                    Text(
                        text = when (uiState.status) {
                            RideRequestStatus.PENDING -> "Your request was submitted to the operator. Awaiting acceptance."
                            RideRequestStatus.ACCEPTED -> "Your driver has accepted your request. Proceed to join your ride."
                            RideRequestStatus.REJECTED -> uiState.request?.rejectionReason?.let { "Reason: $it" }
                                ?: "The operator was unable to accept this request at this time."
                            RideRequestStatus.CANCELLED -> uiState.request?.cancellationReason?.let { "Reason: $it" }
                                ?: "You cancelled this ride request."
                            RideRequestStatus.EXPIRED -> "The operator did not confirm before the 2-minute expiration window."
                        },
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Error or Success Feedback
            uiState.errorMessage?.let { error ->
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = error,
                        style = typography.bodySmall,
                        color = colors.danger
                    )
                }
            }
            uiState.cancelSuccessMessage?.let { success ->
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = success,
                        style = typography.bodySmall,
                        color = colors.primary
                    )
                }
            }

            // Route Waypoints Card
            val req = uiState.request
            if (req != null) {
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
                                Text(text = "Pickup", style = typography.labelSmall, color = colors.foregroundMuted)
                                Text(text = req.pickupAddress, style = typography.bodyMedium, color = colors.foreground)
                            }
                        }

                        // Connecting line
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .width(2.dp)
                                .height(12.dp)
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
                                Text(text = "Destination", style = typography.labelSmall, color = colors.foregroundMuted)
                                Text(text = req.destinationAddress, style = typography.bodyMedium, color = colors.foreground)
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.xs))

                        Text(
                            text = "Request ID: ${req.id}",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        if (req.expiresAt.isNotBlank()) {
                            Text(
                                text = "Expires at: ${req.expiresAt}",
                                style = typography.labelSmall,
                                color = colors.foregroundMuted
                            )
                        }
                    }
                }
            }

            // Vehicle & Driver Details Card if trip provided
            val trip = uiState.trip
            if (trip != null) {
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = "OPERATOR & VEHICLE",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = trip.vehicle.displayTitle,
                            style = typography.titleMedium,
                            color = colors.foreground
                        )
                        Text(
                            text = "Plate: ${trip.vehicle.registrationNumber} • Driver: ${trip.driver.name}",
                            style = typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Lifecycle Context Actions
            when (uiState.status) {
                RideRequestStatus.PENDING -> {
                    // Refresh status button
                    IshaaraButton(
                        text = if (uiState.isLoading) "Refreshing…" else "Refresh status",
                        onClick = { viewModel.refreshStatus() },
                        enabled = !uiState.isLoading && !uiState.isCancelling,
                        variant = IshaaraButtonVariant.Secondary,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cancel button
                    IshaaraButton(
                        text = if (uiState.isCancelling) "Cancelling…" else "Cancel request",
                        onClick = { showCancelDialog = true },
                        enabled = !uiState.isLoading && !uiState.isCancelling,
                        variant = IshaaraButtonVariant.Danger,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                RideRequestStatus.ACCEPTED -> {
                    IshaaraButton(
                        text = "Continue to Live Ride",
                        onClick = { viewModel.onContinueToLiveRide() },
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )
                }
                RideRequestStatus.REJECTED, RideRequestStatus.EXPIRED -> {
                    IshaaraButton(
                        text = "Find another trip",
                        onClick = { viewModel.onBackToDiscovery() },
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )
                }
                RideRequestStatus.CANCELLED -> {
                    IshaaraButton(
                        text = "Back to home",
                        onClick = { viewModel.onBackToHome() },
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )
                }
            }
        }
    }

    // Cancellation Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(text = "Cancel Ride Request?", style = typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = "Are you sure you want to cancel this ride request? The operator will be notified.",
                        style = typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = cancelReasonText,
                        onValueChange = { if (it.length <= 250) cancelReasonText = it },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                IshaaraButton(
                    text = "Confirm Cancel",
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelRequest(cancelReasonText.takeIf { it.isNotBlank() })
                    },
                    variant = IshaaraButtonVariant.Danger,
                    size = IshaaraButtonSize.Small
                )
            },
            dismissButton = {
                IshaaraButton(
                    text = "Keep Request",
                    onClick = { showCancelDialog = false },
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small
                )
            }
        )
    }
}
