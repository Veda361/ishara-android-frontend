package com.ishara.app.feature.driver.trip.ui

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
import androidx.compose.foundation.border
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraDialog
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.feature.driver.trip.DriverTripDetailViewModel
import com.ishara.app.feature.driver.trip.TripDetailContentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTripDetailScreen(
    viewModel: DriverTripDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    var cancelReasonText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IshaaraTopBar(
            title = "Trip Details",
            subtitle = "Authoritative State & Execution",
            onBackClick = onNavigateBack,
            actions = {
                IshaaraButton(
                    text = "Refresh",
                    onClick = { viewModel.refresh() },
                    variant = IshaaraButtonVariant.Secondary,
                    size = IshaaraButtonSize.Small
                )
            }
        )

        // Error message banner
        if (uiState.actionErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.dangerSubtle)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Text(
                    text = uiState.actionErrorMessage ?: "",
                    style = IshaaraTheme.typography.bodySmall,
                    color = colors.danger
                )
            }
        }

        // Success message banner
        if (uiState.actionSuccessMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.successSubtle)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Text(
                    text = uiState.actionSuccessMessage ?: "",
                    style = IshaaraTheme.typography.bodySmall,
                    color = colors.success
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState.contentState) {
                is TripDetailContentState.Loading -> {
                    IshaaraLoadingState(message = "Loading trip details…")
                }
                is TripDetailContentState.Error -> {
                    IshaaraErrorState(
                        title = if (state.isNotFound) "Trip Not Found" else "Error Loading Trip",
                        message = state.message,
                        retryLabel = if (state.canRetry) "Retry" else "Go Back",
                        onRetryClick = {
                            if (state.canRetry) viewModel.refresh() else onNavigateBack()
                        }
                    )
                }
                is TripDetailContentState.Success -> {
                    TripDetailBody(
                        trip = state.trip,
                        vehicle = state.assignedVehicle,
                        isActionLoading = uiState.isActionLoading,
                        onStartTrip = { viewModel.startTrip() },
                        onCompleteTrip = { viewModel.completeTrip() },
                        onOpenCancelDialog = { viewModel.openCancelDialog() }
                    )
                }
            }
        }
    }

    // Cancellation Dialog
    if (uiState.isCancelDialogOpen) {
        val shapes = IshaaraTheme.shapes
        val borders = IshaaraTheme.borders
        val typography = IshaaraTheme.typography

        BasicAlertDialog(
            onDismissRequest = {
                cancelReasonText = ""
                viewModel.dismissCancelDialog()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.sm)
                    .background(colors.surfaceElevated)
                    .border(borders.thin, colors.border, shapes.sm)
                    .padding(spacing.lg)
            ) {
                Text(
                    text = "LIFECYCLE // CANCEL TRIP",
                    style = typography.labelSmall,
                    color = colors.danger
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Confirm Trip Cancellation",
                    style = typography.titleLarge,
                    color = colors.foreground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cancelling this trip will mark it as CANCELLED on the server and notify affected parties.",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reason (optional):",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                IshaaraTextField(
                    value = cancelReasonText,
                    onValueChange = { cancelReasonText = it },
                    placeholder = "e.g., Mechanical trouble, road closure"
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    IshaaraButton(
                        text = "Keep Trip",
                        onClick = {
                            cancelReasonText = ""
                            viewModel.dismissCancelDialog()
                        },
                        variant = IshaaraButtonVariant.Text,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IshaaraButton(
                        text = "Cancel Trip",
                        onClick = {
                            val reason = cancelReasonText.ifBlank { null }
                            cancelReasonText = ""
                            viewModel.confirmCancelTrip(reason)
                        },
                        variant = IshaaraButtonVariant.Danger,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TripDetailBody(
    trip: Trip,
    vehicle: Vehicle?,
    isActionLoading: Boolean,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    onOpenCancelDialog: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // Status & ID Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trip ID",
                        style = IshaaraTheme.typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    TripStatusBadge(status = trip.status)
                }
                Text(
                    text = trip.id,
                    style = IshaaraTheme.typography.titleMedium,
                    color = colors.foreground
                )
                if (trip.agencyId != null) {
                    IshaaraBadge(
                        text = "Agency Fleet Trip: ${trip.agencyId.takeLast(6).uppercase()}",
                        variant = IshaaraBadgeVariant.Neutral
                    )
                }
            }
        }

        // Waypoints & Route Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Text(
                    text = "Route & Waypoints",
                    style = IshaaraTheme.typography.titleSmall,
                    color = colors.foreground
                )

                // Origin
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.success)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Column {
                        Text(
                            text = "Origin",
                            style = IshaaraTheme.typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = trip.origin.address,
                            style = IshaaraTheme.typography.bodyMedium,
                            color = colors.foreground
                        )
                        Text(
                            text = "(${trip.origin.latitude}, ${trip.origin.longitude})",
                            style = IshaaraTheme.typography.labelSmall,
                            color = colors.foregroundSubtle
                        )
                    }
                }

                // Destination
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.danger)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Column {
                        Text(
                            text = "Destination",
                            style = IshaaraTheme.typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = trip.destination.address,
                            style = IshaaraTheme.typography.bodyMedium,
                            color = colors.foreground
                        )
                        Text(
                            text = "(${trip.destination.latitude}, ${trip.destination.longitude})",
                            style = IshaaraTheme.typography.labelSmall,
                            color = colors.foregroundSubtle
                        )
                    }
                }

                if (trip.route?.distanceMeters != null || trip.route?.durationSeconds != null) {
                    val distanceKm = trip.route.distanceMeters?.let { String.format("%.1f km", it / 1000.0) }
                    val durationMin = trip.route.durationSeconds?.let { "${(it / 60).toInt()} mins" }
                    val summary = listOfNotNull(distanceKm, durationMin).joinToString(" • ")

                    Text(
                        text = "Estimated Route: $summary",
                        style = IshaaraTheme.typography.bodySmall,
                        color = colors.accent
                    )
                }
            }
        }

        // Assigned Vehicle Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = "Assigned Vehicle",
                    style = IshaaraTheme.typography.titleSmall,
                    color = colors.foreground
                )
                if (vehicle != null) {
                    Text(
                        text = "${vehicle.registrationNumber} • ${vehicle.make} ${vehicle.model}",
                        style = IshaaraTheme.typography.bodyMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = "Type: ${vehicle.vehicleType.name} • Status: ${if (vehicle.isActive) "Active" else "Inactive"}",
                        style = IshaaraTheme.typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                } else {
                    Text(
                        text = "Vehicle ID: ${trip.vehicleId}",
                        style = IshaaraTheme.typography.bodyMedium,
                        color = colors.foreground
                    )
                }
            }
        }

        // Operational Audit Timestamps Card
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = "Operational Timeline",
                    style = IshaaraTheme.typography.titleSmall,
                    color = colors.foreground
                )
                if (trip.scheduledDepartureAt != null) {
                    AuditRow(label = "Scheduled Departure", value = trip.scheduledDepartureAt)
                }
                if (trip.startedAt != null) {
                    AuditRow(label = "Started At", value = trip.startedAt)
                }
                if (trip.completedAt != null) {
                    AuditRow(label = "Completed At", value = trip.completedAt)
                }
                if (trip.cancelledAt != null) {
                    AuditRow(label = "Cancelled At", value = trip.cancelledAt)
                    if (trip.cancellationReason != null) {
                        AuditRow(label = "Reason", value = trip.cancellationReason)
                    }
                    if (trip.cancelledByRole != null) {
                        AuditRow(label = "Cancelled By", value = trip.cancelledByRole.name)
                    }
                }
                if (trip.createdByRole != null) {
                    AuditRow(label = "Dispatched By", value = trip.createdByRole.name)
                }
                if (trip.createdAt != null) {
                    AuditRow(label = "Created", value = trip.createdAt)
                }
            }
        }

        // Action Buttons Row
        if (!trip.isTerminal) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                if (trip.canBeStarted) {
                    IshaaraButton(
                        text = "Start Trip",
                        onClick = onStartTrip,
                        enabled = !isActionLoading,
                        loading = isActionLoading,
                        modifier = Modifier.fillMaxWidth(),
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Large
                    )
                } else if (trip.canBeCompleted) {
                    IshaaraButton(
                        text = "Complete Trip",
                        onClick = onCompleteTrip,
                        enabled = !isActionLoading,
                        loading = isActionLoading,
                        modifier = Modifier.fillMaxWidth(),
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Large
                    )
                }

                IshaaraButton(
                    text = "Cancel Trip",
                    onClick = onOpenCancelDialog,
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth(),
                    variant = IshaaraButtonVariant.Danger,
                    size = IshaaraButtonSize.Medium
                )
            }
        } else {
            IshaaraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (trip.status == TripStatus.COMPLETED) colors.successSubtle else colors.dangerSubtle
            ) {
                Text(
                    text = if (trip.status == TripStatus.COMPLETED) "This trip has been completed." else "This trip was cancelled.",
                    style = IshaaraTheme.typography.bodyMedium,
                    color = if (trip.status == TripStatus.COMPLETED) colors.success else colors.danger,
                    modifier = Modifier.padding(spacing.md)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun AuditRow(label: String, value: String) {
    val colors = IshaaraTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = IshaaraTheme.typography.bodySmall, color = colors.foregroundMuted)
        Text(text = value, style = IshaaraTheme.typography.bodySmall, color = colors.foreground)
    }
}
