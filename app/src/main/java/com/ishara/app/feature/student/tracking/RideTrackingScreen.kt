package com.ishara.app.feature.student.tracking

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraIconButton
import com.ishara.app.core.designsystem.component.IshaaraIconButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraStatusChip
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.model.TrackingState
import com.ishara.app.feature.student.tracking.components.IshaaraTrackingCanvasMap

@Composable
fun RideTrackingScreen(
    viewModel: RideTrackingViewModel,
    onNavigateBack: () -> Unit,
    onRateTripClick: () -> Unit = {},
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
            is RideTrackingUiState.Loading -> {
                IshaaraLoadingState(message = stringResource(R.string.location_loading))
            }
            is RideTrackingUiState.Content -> {
                TrackingContent(
                    state = state,
                    onBackClick = {
                        viewModel.onNavigateBack()
                        onNavigateBack()
                    },
                    onRefreshClick = { viewModel.refresh() },
                    onPayFareClick = { viewModel.onPayFareClicked() },
                    onSafetyClick = { viewModel.onSafetyClicked() }
                )
            }
            is RideTrackingUiState.Terminal -> {
                TrackingTerminalScreen(
                    state = state,
                    onDoneClick = {
                        viewModel.onNavigateBack()
                        onNavigateBack()
                    },
                    onRateTripClick = onRateTripClick
                )
            }
            is RideTrackingUiState.Error -> {
                IshaaraErrorState(
                    title = stringResource(R.string.tracking_error_title),
                    message = state.message,
                    retryLabel = stringResource(R.string.retry_button),
                    onRetryClick = { viewModel.loadInitialTracking() }
                )
            }
        }
    }
}

@Composable
private fun TrackingContent(
    state: RideTrackingUiState.Content,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPayFareClick: () -> Unit,
    onSafetyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IshaaraIconButton(
                onClick = onBackClick,
                contentDescription = stringResource(R.string.tracking_back),
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
                    text = stringResource(R.string.tracking_title),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground
                )
                Text(
                    text = state.statusMessage,
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
                    contentDescription = stringResource(R.string.tracking_refresh_button),
                    variant = IshaaraIconButtonVariant.Standard
                ) {
                    Text(
                        text = "↻",
                        style = typography.titleLarge,
                        color = colors.foregroundMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.xs))

            IshaaraIconButton(
                onClick = onSafetyClick,
                contentDescription = "Safety and Emergency Assistance",
                variant = IshaaraIconButtonVariant.Standard
            ) {
                Text(
                    text = "🛡",
                    style = typography.titleMedium,
                    color = IshaaraPalette.Crimson500
                )
            }
        }

        // Connection / degraded banner
        AnimatedVisibility(
            visible = state.connectionNotice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (state.isDegraded) IshaaraPalette.Amber100 else colors.surfaceSubtle)
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
            ) {
                Text(
                    text = state.connectionNotice ?: "",
                    style = typography.labelSmall,
                    color = if (state.isDegraded) IshaaraPalette.Amber900 else colors.foregroundMuted
                )
            }
        }

        // Interactive Map
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            IshaaraTrackingCanvasMap(
                driverLocation = state.snapshot.driverLocation,
                pickupCoordinates = state.snapshot.pickupCoordinates,
                destinationCoordinates = state.snapshot.destinationCoordinates,
                userLocation = state.userLocation
            )
        }

        // Bottom Ride Info Panel
        RideTrackingInfoPanel(
            snapshot = state.snapshot,
            onPayFareClick = onPayFareClick,
            onSafetyClick = onSafetyClick
        )
    }
}

@Composable
private fun RideTrackingInfoPanel(
    snapshot: RideTrackingSnapshot,
    onPayFareClick: () -> Unit,
    onSafetyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.md)
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            // Status and ETA row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val transitStatus = when (snapshot.status) {
                    TrackingRideStatus.CREATED -> IshaaraTransitStatus.COMING
                    TrackingRideStatus.DRIVER_ARRIVING -> IshaaraTransitStatus.ARRIVING
                    TrackingRideStatus.PICKED_UP -> IshaaraTransitStatus.BOARDING
                    TrackingRideStatus.IN_PROGRESS -> IshaaraTransitStatus.IN_PROGRESS
                    TrackingRideStatus.COMPLETED -> IshaaraTransitStatus.COMPLETED
                    TrackingRideStatus.CANCELLED -> IshaaraTransitStatus.CANCELLED
                    TrackingRideStatus.UNKNOWN -> IshaaraTransitStatus.ACTIVE
                }

                IshaaraStatusChip(
                    status = transitStatus,
                    overrideLabel = RideTrackingUiState.getStatusDescription(snapshot.status)
                )

                // ETA pill if provided by backend
                val etaText = snapshot.eta.formattedEtaMinutes
                if (etaText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IshaaraPalette.Amber100)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.tracking_eta_prefix)} $etaText",
                            style = typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IshaaraPalette.Amber900
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Remaining Distance if provided by backend
            val remainingDistance = snapshot.routeProgress?.remainingDistanceMeters
                ?: snapshot.distanceToPickupMeters
                ?: snapshot.distanceToDestinationMeters

            if (remainingDistance != null && remainingDistance > 0) {
                val distText = if (remainingDistance >= 1000) {
                    String.format(java.util.Locale.US, "%.1f km", remainingDistance / 1000.0)
                } else {
                    "$remainingDistance m"
                }

                Text(
                    text = "$distText ${stringResource(R.string.tracking_distance_remaining)}",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )

                Spacer(modifier = Modifier.height(spacing.xs))
            }

            // Route Points
            if (!snapshot.pickupAddress.isNullOrBlank() || !snapshot.destinationAddress.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceSubtle)
                        .padding(spacing.sm)
                ) {
                    if (!snapshot.pickupAddress.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(IshaaraPalette.Emerald500)
                            )
                            Spacer(modifier = Modifier.width(spacing.xs))
                            Text(
                                text = snapshot.pickupAddress,
                                style = typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.foreground
                            )
                        }
                    }

                    if (!snapshot.pickupAddress.isNullOrBlank() && !snapshot.destinationAddress.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (!snapshot.destinationAddress.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(IshaaraPalette.Crimson500)
                            )
                            Spacer(modifier = Modifier.width(spacing.xs))
                            Text(
                                text = snapshot.destinationAddress,
                                style = typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.foreground
                            )
                        }
                    }
                }
            }

            // Tracking state freshness indicator
            Spacer(modifier = Modifier.height(spacing.xs))
            val trackingStateText = when (snapshot.trackingState) {
                TrackingState.FRESH -> stringResource(R.string.tracking_state_fresh)
                TrackingState.STALE -> stringResource(R.string.tracking_state_stale)
                TrackingState.OFF_ROUTE -> stringResource(R.string.tracking_state_off_route)
                TrackingState.UNAVAILABLE, TrackingState.UNKNOWN -> stringResource(R.string.tracking_state_unavailable)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "GPS: $trackingStateText",
                    style = typography.labelSmall,
                    color = colors.foregroundSubtle
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))
            com.ishara.app.core.designsystem.component.IshaaraButton(
                text = stringResource(R.string.payment_pay_fare_card_button),
                onClick = onPayFareClick,
                size = com.ishara.app.core.designsystem.component.IshaaraButtonSize.Medium,
                variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.xs))
            com.ishara.app.core.designsystem.component.IshaaraButton(
                text = "Safety & Emergency (SOS)",
                onClick = onSafetyClick,
                size = com.ishara.app.core.designsystem.component.IshaaraButtonSize.Medium,
                variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TrackingTerminalScreen(
    state: RideTrackingUiState.Terminal,
    onDoneClick: () -> Unit,
    onRateTripClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val isCompleted = state.status == TrackingRideStatus.COMPLETED

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) IshaaraPalette.Emerald100 else IshaaraPalette.Crimson100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCompleted) "✓" else "✕",
                    style = typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) IshaaraPalette.Emerald500 else IshaaraPalette.Crimson500
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = if (isCompleted) stringResource(R.string.tracking_status_completed) else stringResource(R.string.tracking_status_cancelled),
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )

            if (!state.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = state.reason,
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            if (isCompleted) {
                IshaaraButton(
                    text = "Rate Trip & Driver",
                    onClick = onRateTripClick,
                    variant = IshaaraButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                IshaaraButton(
                    text = stringResource(R.string.tracking_done_button),
                    onClick = onDoneClick,
                    variant = IshaaraButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                IshaaraButton(
                    text = stringResource(R.string.tracking_done_button),
                    onClick = onDoneClick,
                    variant = IshaaraButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
