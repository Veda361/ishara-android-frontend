package com.ishara.app.feature.driver.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraEmptyState
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.model.DriverRideStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRideRequestsScreen(
    viewModel: DriverRideRequestsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRejectDialogForRequest by remember { mutableStateOf<DriverRideRequest?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }

    var showCancelDialogForRide by remember { mutableStateOf<DriverPassengerRide?>(null) }
    var cancellationReasonText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ride Requests & Boarding",
                            style = IshaaraTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                        val realtimeText = when (uiState.realtimeState) {
                            is RealtimeConnectionState.Connected -> "Live Realtime"
                            is RealtimeConnectionState.Reconnecting -> "Reconnecting..."
                            else -> "Standard Mode"
                        }
                        Text(
                            text = realtimeText,
                            style = IshaaraTheme.typography.labelSmall,
                            color = if (uiState.realtimeState is RealtimeConnectionState.Connected) colors.success else colors.foregroundSubtle
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = spacing.xs)
                    ) {
                        Text("← Back", color = colors.foreground)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isActionInProgress
                    ) {
                        Text("Refresh", color = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = if (uiState.selectedTab == DriverRequestsTab.INCOMING) 0 else 1,
                containerColor = colors.surface,
                contentColor = colors.accent
            ) {
                Tab(
                    selected = uiState.selectedTab == DriverRequestsTab.INCOMING,
                    onClick = { viewModel.selectTab(DriverRequestsTab.INCOMING) },
                    text = {
                        val count = uiState.pendingRequests.size
                        Text(
                            text = if (count > 0) "Incoming Requests ($count)" else "Incoming Requests",
                            fontWeight = if (uiState.selectedTab == DriverRequestsTab.INCOMING) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedTab == DriverRequestsTab.INCOMING) colors.foreground else colors.foregroundMuted
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == DriverRequestsTab.ACTIVE_RIDES,
                    onClick = { viewModel.selectTab(DriverRequestsTab.ACTIVE_RIDES) },
                    text = {
                        val count = uiState.activeRides.size
                        Text(
                            text = if (count > 0) "Active Boarding ($count)" else "Active Boarding",
                            fontWeight = if (uiState.selectedTab == DriverRequestsTab.ACTIVE_RIDES) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedTab == DriverRequestsTab.ACTIVE_RIDES) colors.foreground else colors.foregroundMuted
                        )
                    }
                )
            }

            when (val stage = uiState.stage) {
                is DriverRideRequestsStage.Loading -> {
                    IshaaraLoadingState(message = "Retrieving ride requests...")
                }
                is DriverRideRequestsStage.Error -> {
                    IshaaraErrorState(
                        title = "Unable to load requests",
                        message = stage.error.message,
                        retryLabel = "Try again",
                        onRetryClick = { viewModel.retry() }
                    )
                }
                is DriverRideRequestsStage.Empty -> {
                    val emptyMessage = if (uiState.selectedTab == DriverRequestsTab.INCOMING) {
                        "No pending ride requests at this moment."
                    } else {
                        "No passengers currently boarding or in transit."
                    }
                    IshaaraEmptyState(
                        title = "No Requests Found",
                        message = emptyMessage,
                        actionLabel = "Refresh",
                        onActionClick = { viewModel.refresh() }
                    )
                }
                is DriverRideRequestsStage.Content -> {
                    if (uiState.selectedTab == DriverRequestsTab.INCOMING) {
                        if (uiState.pendingRequests.isEmpty()) {
                            IshaaraEmptyState(
                                title = "No Incoming Requests",
                                message = "There are no pending requests right now.",
                                actionLabel = "Refresh",
                                onActionClick = { viewModel.refresh() }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(spacing.md)
                            ) {
                                items(
                                    items = uiState.pendingRequests,
                                    key = { it.id }
                                ) { request ->
                                    IncomingRequestCard(
                                        request = request,
                                        isActionInProgress = uiState.isActionInProgress,
                                        onAccept = { viewModel.acceptRequest(request.id) },
                                        onReject = {
                                            rejectionReasonText = ""
                                            showRejectDialogForRequest = request
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        if (uiState.activeRides.isEmpty()) {
                            IshaaraEmptyState(
                                title = "No Active Rides",
                                message = "You have no active rides in boarding or transit.",
                                actionLabel = "Refresh",
                                onActionClick = { viewModel.refresh() }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(spacing.md)
                            ) {
                                items(
                                    items = uiState.activeRides,
                                    key = { it.id }
                                ) { ride ->
                                    ActiveRideCard(
                                        ride = ride,
                                        isActionInProgress = uiState.isActionInProgress,
                                        onMarkArrived = { viewModel.markArrived(ride.id) },
                                        onMarkBoarded = { viewModel.markBoarded(ride.id) },
                                        onStartRide = { viewModel.startRide(ride.id) },
                                        onCompleteRide = { viewModel.completeRide(ride.id) },
                                        onCancelRide = {
                                            cancellationReasonText = ""
                                            showCancelDialogForRide = ride
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection confirmation dialog
    showRejectDialogForRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { showRejectDialogForRequest = null },
            title = { Text(text = "Reject Ride Request", fontWeight = FontWeight.Bold, color = colors.foreground) },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to decline this passenger's ride request?",
                        style = IshaaraTheme.typography.bodyMedium,
                        color = colors.foregroundMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("e.g. Vehicle full, off route") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectionReasonText.ifBlank { null }
                        viewModel.rejectRequest(request.id, reason)
                        showRejectDialogForRequest = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Reject Request", color = colors.onDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialogForRequest = null }) {
                    Text("Dismiss", color = colors.foregroundMuted)
                }
            }
        )
    }

    // Cancellation confirmation dialog
    showCancelDialogForRide?.let { ride ->
        AlertDialog(
            onDismissRequest = { showCancelDialogForRide = null },
            title = { Text(text = "Cancel Active Ride", fontWeight = FontWeight.Bold, color = colors.foreground) },
            text = {
                Column {
                    Text(
                        text = "Cancelling an active ride cannot be undone. Please specify a reason:",
                        style = IshaaraTheme.typography.bodyMedium,
                        color = colors.foregroundMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancellationReasonText,
                        onValueChange = { cancellationReasonText = it },
                        label = { Text("Reason (Required)") },
                        placeholder = { Text("e.g. Passenger no-show, vehicle breakdown") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancellationReasonText.isNotBlank()) {
                            viewModel.cancelRide(ride.id, cancellationReasonText)
                            showCancelDialogForRide = null
                        }
                    },
                    enabled = cancellationReasonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Confirm Cancel", color = colors.onDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialogForRide = null }) {
                    Text("Dismiss", color = colors.foregroundMuted)
                }
            }
        )
    }
}

@Composable
private fun IncomingRequestCard(
    request: DriverRideRequest,
    isActionInProgress: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Passenger: ${request.passengerId.takeLast(6)}",
                    style = IshaaraTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accentSubtle)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PENDING",
                        style = IshaaraTheme.typography.labelSmall,
                        color = colors.accentForeground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pickup
            Column {
                Text(
                    text = "PICKUP",
                    style = IshaaraTheme.typography.labelSmall,
                    color = colors.foregroundMuted
                )
                Text(
                    text = request.pickupAddress,
                    style = IshaaraTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.foreground
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Destination
            Column {
                Text(
                    text = "DESTINATION",
                    style = IshaaraTheme.typography.labelSmall,
                    color = colors.foregroundMuted
                )
                Text(
                    text = request.destinationAddress,
                    style = IshaaraTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.foreground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: 54dp large touch targets for driver ergonomic safety
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isActionInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger)
                ) {
                    Text("Decline", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    enabled = !isActionInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold, color = colors.accentForeground)
                }
            }
        }
    }
}

@Composable
private fun ActiveRideCard(
    ride: DriverPassengerRide,
    isActionInProgress: Boolean,
    onMarkArrived: () -> Unit,
    onMarkBoarded: () -> Unit,
    onStartRide: () -> Unit,
    onCompleteRide: () -> Unit,
    onCancelRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Passenger: ${ride.passengerId.takeLast(6)}",
                    style = IshaaraTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                val statusLabel = when (ride.status) {
                    DriverRideStatus.CREATED -> "ARRIVING TO PICKUP"
                    DriverRideStatus.DRIVER_ARRIVING -> "READY FOR BOARDING"
                    DriverRideStatus.PICKED_UP -> "PASSENGER BOARDED"
                    DriverRideStatus.IN_PROGRESS -> "IN TRANSIT"
                    else -> ride.status.name
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accentSubtle)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = IshaaraTheme.typography.labelSmall,
                        color = colors.accentForeground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "To: ${ride.destinationAddress}",
                style = IshaaraTheme.typography.bodyMedium,
                color = colors.foreground,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Operational Action based on verified backend lifecycle
            when (ride.status) {
                DriverRideStatus.CREATED -> {
                    Button(
                        onClick = onMarkArrived,
                        enabled = !isActionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("Arrived at Pickup", fontWeight = FontWeight.Bold, color = colors.accentForeground)
                    }
                }
                DriverRideStatus.DRIVER_ARRIVING -> {
                    // Authoritative Passenger Boarding Step
                    Button(
                        onClick = onMarkBoarded,
                        enabled = !isActionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.success)
                    ) {
                        Text("Confirm Passenger Boarding", fontWeight = FontWeight.Bold, color = colors.onSuccess)
                    }
                }
                DriverRideStatus.PICKED_UP -> {
                    Button(
                        onClick = onStartRide,
                        enabled = !isActionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("Start Transit to Destination", fontWeight = FontWeight.Bold, color = colors.accentForeground)
                    }
                }
                DriverRideStatus.IN_PROGRESS -> {
                    Button(
                        onClick = onCompleteRide,
                        enabled = !isActionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.success)
                    ) {
                        Text("Complete Ride", fontWeight = FontWeight.Bold, color = colors.onSuccess)
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (ride.status in listOf(DriverRideStatus.CREATED, DriverRideStatus.DRIVER_ARRIVING)) {
                OutlinedButton(
                    onClick = onCancelRide,
                    enabled = !isActionInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger)
                ) {
                    Text("Cancel Ride (Passenger No-Show)", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
