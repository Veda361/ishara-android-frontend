package com.ishara.app.feature.driver.trip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraEmptyState
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraStatusChip
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.feature.driver.trip.DriverTripListViewModel
import com.ishara.app.feature.driver.trip.TripFilterTab
import com.ishara.app.feature.driver.trip.TripListContentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTripListScreen(
    viewModel: DriverTripListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTripDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    var cancelTripTargetId by remember { mutableStateOf<String?>(null) }
    var cancelReasonText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IshaaraTopBar(
            title = "Trips & Dispatch",
            subtitle = "Authoritative Driver Operations",
            onBackClick = onNavigateBack,
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IshaaraButton(
                        text = "New Trip",
                        onClick = { viewModel.openCreateDialog() },
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Small
                    )
                }
            }
        )

        // Action error banner
        if (uiState.actionErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.dangerSubtle)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.actionErrorMessage ?: "",
                        style = IshaaraTheme.typography.bodySmall,
                        color = colors.danger,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = IshaaraTheme.typography.labelSmall,
                        color = colors.danger,
                        modifier = Modifier
                            .clickable { viewModel.dismissActionError() }
                            .padding(start = spacing.sm)
                    )
                }
            }
        }

        // Filter Tabs
        TripFilterTabsRow(
            selectedTab = uiState.selectedTab,
            onSelectTab = { viewModel.selectTab(it) }
        )

        // Main Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState.contentState) {
                is TripListContentState.Loading -> {
                    IshaaraLoadingState(message = "Loading driver trips…")
                }
                is TripListContentState.Empty -> {
                    val emptyMessage = when (state.filter) {
                        TripFilterTab.ALL -> "No trips assigned or created yet."
                        TripFilterTab.ACTIVE -> "No active trip in progress."
                        TripFilterTab.UPCOMING -> "No upcoming or scheduled trips."
                        TripFilterTab.PAST -> "No completed or cancelled trips."
                    }
                    IshaaraEmptyState(
                        title = "No Trips Found",
                        message = emptyMessage,
                        actionLabel = if (state.filter == TripFilterTab.ALL) "Create Trip" else "Show All Trips",
                        onActionClick = {
                            if (state.filter == TripFilterTab.ALL) {
                                viewModel.openCreateDialog()
                            } else {
                                viewModel.selectTab(TripFilterTab.ALL)
                            }
                        }
                    )
                }
                is TripListContentState.Error -> {
                    IshaaraErrorState(
                        title = if (state.isForbidden) "Access Denied" else "Unable to Load Trips",
                        message = state.message,
                        retryLabel = "Retry",
                        onRetryClick = { viewModel.refresh() }
                    )
                }
                is TripListContentState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(spacing.xs))
                        }

                        // Prominent Active Trip Card if on ALL or ACTIVE tab and active trip present
                        if (state.activeTrip != null && (state.filter == TripFilterTab.ALL || state.filter == TripFilterTab.ACTIVE)) {
                            item(key = "active_trip_card") {
                                ActiveTripHighlightCard(
                                    trip = state.activeTrip,
                                    isActionInFlight = uiState.actionInFlightTripId == state.activeTrip.id,
                                    onViewDetails = { onNavigateToTripDetail(state.activeTrip.id) },
                                    onStartTrip = { viewModel.startTrip(state.activeTrip.id) },
                                    onCompleteTrip = { viewModel.completeTrip(state.activeTrip.id) },
                                    onCancelTrip = { cancelTripTargetId = state.activeTrip.id }
                                )
                            }
                        }

                        items(
                            items = state.trips,
                            key = { it.id }
                        ) { trip ->
                            TripItemCard(
                                trip = trip,
                                isActionInFlight = uiState.actionInFlightTripId == trip.id,
                                onViewDetails = { onNavigateToTripDetail(trip.id) },
                                onStartTrip = { viewModel.startTrip(trip.id) },
                                onCompleteTrip = { viewModel.completeTrip(trip.id) },
                                onCancelTrip = { cancelTripTargetId = trip.id }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(spacing.xl))
                        }
                    }
                }
            }
        }
    }

    // Cancel Trip Confirmation Dialog
    if (cancelTripTargetId != null) {
        val shapes = IshaaraTheme.shapes
        val borders = IshaaraTheme.borders
        val typography = IshaaraTheme.typography

        BasicAlertDialog(
            onDismissRequest = {
                cancelTripTargetId = null
                cancelReasonText = ""
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
                    text = "Cancel Trip",
                    style = typography.titleLarge,
                    color = colors.foreground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Are you sure you want to cancel this trip? This operation is permanent.",
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
                    placeholder = "e.g., Vehicle inspection, weather delay"
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    IshaaraButton(
                        text = "Keep Trip",
                        onClick = {
                            cancelTripTargetId = null
                            cancelReasonText = ""
                        },
                        variant = IshaaraButtonVariant.Text,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IshaaraButton(
                        text = "Cancel Trip",
                        onClick = {
                            val targetId = cancelTripTargetId
                            val reason = cancelReasonText.ifBlank { null }
                            cancelTripTargetId = null
                            cancelReasonText = ""
                            if (targetId != null) {
                                viewModel.cancelTrip(targetId, reason)
                            }
                        },
                        variant = IshaaraButtonVariant.Danger,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Create Trip Dialog
    if (uiState.isCreateDialogOpen) {
        CreateTripModalDialog(
            assignedVehicleReg = uiState.assignedVehicle?.registrationNumber,
            isCreating = uiState.isCreatingTrip,
            onDismiss = { viewModel.dismissCreateDialog() },
            onSubmit = { origAddr, origLat, origLng, destAddr, destLat, destLng, departureTime ->
                viewModel.createTrip(origAddr, origLat, origLng, destAddr, destLat, destLng, departureTime)
            }
        )
    }
}

@Composable
private fun TripFilterTabsRow(
    selectedTab: TripFilterTab,
    onSelectTab: (TripFilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        TripFilterTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val bgColor = if (isSelected) colors.accent else colors.surfaceElevated
            val textColor = if (isSelected) colors.background else colors.foregroundMuted
            val borderColor = if (isSelected) colors.accent else colors.borderSubtle

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .clickable(role = Role.Tab) { onSelectTab(tab) }
                    .padding(horizontal = spacing.sm, vertical = 6.dp)
            ) {
                Text(
                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = IshaaraTheme.typography.labelSmall,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ActiveTripHighlightCard(
    trip: Trip,
    isActionInFlight: Boolean,
    onViewDetails: () -> Unit,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    onCancelTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = colors.accent,
        borderWidth = 2.dp,
        containerColor = colors.accentSubtle
    ) {
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
                IshaaraBadge(text = "CURRENT ACTIVE", variant = IshaaraBadgeVariant.Success)
                TripStatusBadge(status = trip.status)
            }

            Text(
                text = "${trip.origin.address} → ${trip.destination.address}",
                style = IshaaraTheme.typography.titleMedium,
                color = colors.foreground
            )

            if (trip.scheduledDepartureAt != null) {
                Text(
                    text = "Scheduled: ${trip.scheduledDepartureAt}",
                    style = IshaaraTheme.typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                if (trip.canBeCompleted) {
                    IshaaraButton(
                        text = "Complete Trip",
                        onClick = onCompleteTrip,
                        enabled = !isActionInFlight,
                        modifier = Modifier.weight(1f),
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Small
                    )
                } else if (trip.canBeStarted) {
                    IshaaraButton(
                        text = "Start Trip",
                        onClick = onStartTrip,
                        enabled = !isActionInFlight,
                        modifier = Modifier.weight(1f),
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Small
                    )
                }

                IshaaraButton(
                    text = "Details",
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    variant = IshaaraButtonVariant.Secondary,
                    size = IshaaraButtonSize.Small
                )

                if (trip.canBeCancelled) {
                    IshaaraButton(
                        text = "Cancel",
                        onClick = onCancelTrip,
                        enabled = !isActionInFlight,
                        variant = IshaaraButtonVariant.Danger,
                        size = IshaaraButtonSize.Small
                    )
                }
            }
        }
    }
}

@Composable
private fun TripItemCard(
    trip: Trip,
    isActionInFlight: Boolean,
    onViewDetails: () -> Unit,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    onCancelTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
    ) {
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
                    text = "Trip #${trip.id.takeLast(6).uppercase()}",
                    style = IshaaraTheme.typography.labelSmall,
                    color = colors.foregroundSubtle
                )
                TripStatusBadge(status = trip.status)
            }

            // Origin / Destination Waypoints
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.success)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = trip.origin.address,
                    style = IshaaraTheme.typography.bodyMedium,
                    color = colors.foreground
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.danger)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = trip.destination.address,
                    style = IshaaraTheme.typography.bodyMedium,
                    color = colors.foreground
                )
            }

            if (trip.cancellationReason != null) {
                Text(
                    text = "Cancelled: ${trip.cancellationReason}",
                    style = IshaaraTheme.typography.bodySmall,
                    color = colors.danger
                )
            }

            // Action row for uncompleted trips
            if (!trip.isTerminal) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    if (trip.canBeStarted) {
                        IshaaraButton(
                            text = "Start",
                            onClick = onStartTrip,
                            enabled = !isActionInFlight,
                            modifier = Modifier.weight(1f),
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Small
                        )
                    } else if (trip.canBeCompleted) {
                        IshaaraButton(
                            text = "Complete",
                            onClick = onCompleteTrip,
                            enabled = !isActionInFlight,
                            modifier = Modifier.weight(1f),
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Small
                        )
                    }

                    IshaaraButton(
                        text = "Cancel",
                        onClick = onCancelTrip,
                        enabled = !isActionInFlight,
                        variant = IshaaraButtonVariant.Outlined,
                        size = IshaaraButtonSize.Small
                    )
                }
            }
        }
    }
}

@Composable
fun TripStatusBadge(status: TripStatus, modifier: Modifier = Modifier) {
    val transitStatus = when (status) {
        TripStatus.CREATED -> IshaaraTransitStatus.COMING
        TripStatus.SCHEDULED -> IshaaraTransitStatus.PENDING
        TripStatus.ASSIGNED -> IshaaraTransitStatus.ACCEPTED
        TripStatus.READY -> IshaaraTransitStatus.AVAILABLE
        TripStatus.ACTIVE -> IshaaraTransitStatus.ACTIVE
        TripStatus.COMPLETED -> IshaaraTransitStatus.COMPLETED
        TripStatus.CANCELLED -> IshaaraTransitStatus.CANCELLED
    }
    IshaaraStatusChip(status = transitStatus, overrideLabel = status.name, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripModalDialog(
    assignedVehicleReg: String?,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (origAddr: String, origLat: Double, origLng: Double, destAddr: String, destLat: Double, destLng: Double, departure: String?) -> Unit
) {
    var originAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }

    // Coordinates default to Varanasi transit hubs if not manually edited
    var originLat by remember { mutableStateOf(25.2799) }
    var originLng by remember { mutableStateOf(82.9995) }
    var destLat by remember { mutableStateOf(25.2899) }
    var destLng by remember { mutableStateOf(83.0068) }

    val colors = IshaaraTheme.colors
    val shapes = IshaaraTheme.shapes
    val borders = IshaaraTheme.borders
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    BasicAlertDialog(
        onDismissRequest = onDismiss
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
                text = "TRIP // CREATE",
                style = typography.labelSmall,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Create New Trip",
                style = typography.titleLarge,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (assignedVehicleReg != null) {
                    "Vehicle: $assignedVehicleReg (Authoritative Assignment)"
                } else {
                    "Warning: No vehicle assigned. You must have an active vehicle assignment."
                },
                style = typography.bodyMedium,
                color = if (assignedVehicleReg != null) colors.foregroundMuted else colors.danger
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Origin", style = typography.labelSmall, color = colors.foregroundMuted)
            Spacer(modifier = Modifier.height(spacing.xs))
            IshaaraTextField(
                value = originAddress,
                onValueChange = { originAddress = it },
                placeholder = "e.g., BHU Main Gate"
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(text = "Destination", style = typography.labelSmall, color = colors.foregroundMuted)
            Spacer(modifier = Modifier.height(spacing.xs))
            IshaaraTextField(
                value = destinationAddress,
                onValueChange = { destinationAddress = it },
                placeholder = "e.g., Assi Ghat, Varanasi"
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                IshaaraButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IshaaraButton(
                    text = if (isCreating) "Creating…" else "Create Trip",
                    onClick = {
                        if (originAddress.isNotBlank() && destinationAddress.isNotBlank()) {
                            onSubmit(originAddress, originLat, originLng, destinationAddress, destLat, destLng, null)
                        }
                    },
                    enabled = !isCreating && originAddress.isNotBlank() && destinationAddress.isNotBlank(),
                    variant = IshaaraButtonVariant.Primary,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
