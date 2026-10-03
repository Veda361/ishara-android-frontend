package com.ishara.app.feature.driver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.ishara.app.core.designsystem.component.IshaaraStatusChip
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DriverAccessState
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverAssignedVehicle
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverTodayStats
import com.ishara.app.domain.model.DriverTripStatus
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.feature.driver.DriverHomeStage
import com.ishara.app.feature.driver.DriverHomeUiState
import com.ishara.app.feature.driver.DriverStatusActionState
import com.ishara.app.feature.driver.DriverViewModel
import com.ishara.app.feature.driver.TripActionState
import com.ishara.app.feature.driver.TripActionType
import java.util.Locale

/**
 * Production Driver / Conductor Operational Home Screen.
 *
 * Adheres strictly to:
 * - Glanceable, distraction-free transit operational hierarchy.
 * - Minimum 48dp touch targets, dominant 54dp primary CTA.
 * - Authoritative backend state: Online / Offline, Assigned Vehicle, Active Trip.
 * - Explicit lifecycle actions: Start, Complete, Cancel (with safety confirmation dialogs).
 * - Immediate feedback, duplicate tap protection, zero cyberpunk or decorative clutter.
 */
@Composable
fun DriverHomeScreen(
    viewModel: DriverViewModel,
    onSignOut: () -> Unit,
    onNavigateToRequests: () -> Unit = {},
    onNavigateToEarnings: () -> Unit = {},
    onNavigateToVerification: () -> Unit = {},
    onNavigateToAgency: () -> Unit = {},
    onNavigateToReadiness: () -> Unit = {},
    onNavigateToVehicle: () -> Unit = {},
    onNavigateToTrips: () -> Unit = {},
    onNavigateToTripDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCompleteConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var tripIdPendingConfirmation by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.loadOperationalContext(silent = true)
        }
    }

    val onRequestPermissions: () -> Unit = {
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(permissionsToRequest)
    }

    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (val stage = uiState.stage) {
            is DriverHomeStage.Loading -> {
                IshaaraLoadingState(message = "Synchronizing driver operational status...")
            }
            is DriverHomeStage.Error -> {
                IshaaraErrorState(
                    title = if (!stage.canRetry) "Access Denied" else "Connection Issue",
                    message = stage.error.message,
                    retryLabel = if (stage.canRetry) "Try again" else "Sign Out",
                    onRetryClick = {
                        if (stage.canRetry) {
                            viewModel.retry()
                        } else {
                            onSignOut()
                        }
                    }
                )
            }
            is DriverHomeStage.Offline -> {
                DriverHomeContent(
                    driverName = viewModel.driverName,
                    context = stage.context,
                    uiState = uiState,
                    isOfflineStage = true,
                    onGoOnline = { viewModel.goOnline() },
                    onGoOffline = { viewModel.goOffline() },
                    onStartTrip = { tripId -> viewModel.startTrip(tripId) },
                    onInitiateCompleteTrip = { tripId ->
                        tripIdPendingConfirmation = tripId
                        showCompleteConfirmation = true
                    },
                    onInitiateCancelTrip = { tripId ->
                        tripIdPendingConfirmation = tripId
                        showCancelConfirmation = true
                    },
                    onDismissMessage = { viewModel.dismissNotification() },
                    onRefresh = { viewModel.refresh() },
                    onNavigateToRequests = onNavigateToRequests,
                    onNavigateToEarnings = onNavigateToEarnings,
                    onNavigateToVerification = onNavigateToVerification,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToReadiness = onNavigateToReadiness,
                    onNavigateToVehicle = onNavigateToVehicle,
                    onNavigateToTrips = onNavigateToTrips,
                    onNavigateToTripDetail = onNavigateToTripDetail,
                    onSignOut = onSignOut
                )
            }
            is DriverHomeStage.Empty -> {
                DriverHomeContent(
                    driverName = viewModel.driverName,
                    context = stage.context,
                    uiState = uiState,
                    isOfflineStage = false,
                    onGoOnline = { viewModel.goOnline() },
                    onGoOffline = { viewModel.goOffline() },
                    onStartTrip = { tripId -> viewModel.startTrip(tripId) },
                    onInitiateCompleteTrip = { tripId ->
                        tripIdPendingConfirmation = tripId
                        showCompleteConfirmation = true
                    },
                    onInitiateCancelTrip = { tripId ->
                        tripIdPendingConfirmation = tripId
                        showCancelConfirmation = true
                    },
                    onDismissMessage = { viewModel.dismissNotification() },
                    onRefresh = { viewModel.refresh() },
                    onNavigateToRequests = onNavigateToRequests,
                    onNavigateToEarnings = onNavigateToEarnings,
                    onNavigateToVerification = onNavigateToVerification,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToReadiness = onNavigateToReadiness,
                    onNavigateToVehicle = onNavigateToVehicle,
                    onNavigateToTrips = onNavigateToTrips,
                    onNavigateToTripDetail = onNavigateToTripDetail,
                    onSignOut = onSignOut
                )
            }
            is DriverHomeStage.Content -> {
                DriverHomeContent(
                    driverName = viewModel.driverName,
                    context = stage.context,
                    uiState = uiState,
                    isOfflineStage = false,
                    onGoOnline = { viewModel.goOnline() },
                    onGoOffline = { viewModel.goOffline() },
                    onStartTrip = { tripId -> viewModel.startTrip(tripId) },
                    onInitiateCompleteTrip = { tripId ->
                        tripIdPendingConfirmation = tripId
                        showCompleteConfirmation = true
                    },
                    onInitiateCancelTrip = { tripId ->
                        tripIdPendingConfirmation = tripId
                        showCancelConfirmation = true
                    },
                    onRequestPermissions = onRequestPermissions,
                    onDismissMessage = { viewModel.dismissNotification() },
                    onRefresh = { viewModel.refresh() },
                    onNavigateToRequests = onNavigateToRequests,
                    onNavigateToEarnings = onNavigateToEarnings,
                    onNavigateToVerification = onNavigateToVerification,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToReadiness = onNavigateToReadiness,
                    onNavigateToVehicle = onNavigateToVehicle,
                    onNavigateToTrips = onNavigateToTrips,
                    onNavigateToTripDetail = onNavigateToTripDetail,
                    onSignOut = onSignOut
                )
            }
        }

        // Safety Confirmation Dialog for Completing Trip
        if (showCompleteConfirmation) {
            val tripId = tripIdPendingConfirmation
            IshaaraDialog(
                onDismissRequest = {
                    showCompleteConfirmation = false
                    tripIdPendingConfirmation = null
                },
                title = "Complete Trip",
                message = "Are you sure you want to mark this trip as completed? This will finalize all passenger drops on this route.",
                confirmLabel = "Complete Trip",
                onConfirm = {
                    showCompleteConfirmation = false
                    if (tripId != null) {
                        viewModel.completeTrip(tripId)
                    }
                    tripIdPendingConfirmation = null
                },
                dismissLabel = "Keep Active",
                systemTag = "LIFECYCLE // COMPLETE TRIP"
            )
        }

        // Safety Confirmation Dialog for Cancelling Trip
        if (showCancelConfirmation) {
            val tripId = tripIdPendingConfirmation
            IshaaraDialog(
                onDismissRequest = {
                    showCancelConfirmation = false
                    tripIdPendingConfirmation = null
                },
                title = "Cancel Trip",
                message = "Are you sure you want to cancel this trip? Any booked passengers will be notified.",
                confirmLabel = "Cancel Trip",
                onConfirm = {
                    showCancelConfirmation = false
                    if (tripId != null) {
                        viewModel.cancelTrip(tripId)
                    }
                    tripIdPendingConfirmation = null
                },
                dismissLabel = "Go Back",
                isDanger = true,
                systemTag = "LIFECYCLE // CANCEL TRIP"
            )
        }
    }
}

/**
 * Scrollable operational content for Driver Home.
 */
@Composable
private fun DriverHomeContent(
    driverName: String,
    context: DriverOperationalContext,
    uiState: DriverHomeUiState,
    isOfflineStage: Boolean,
    onGoOnline: () -> Unit,
    onGoOffline: () -> Unit,
    onStartTrip: (String) -> Unit,
    onInitiateCompleteTrip: (String) -> Unit,
    onInitiateCancelTrip: (String) -> Unit,
    onRequestPermissions: () -> Unit = {},
    onDismissMessage: () -> Unit,
    onRefresh: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onNavigateToEarnings: () -> Unit = {},
    onNavigateToVerification: () -> Unit = {},
    onNavigateToAgency: () -> Unit = {},
    onNavigateToReadiness: () -> Unit = {},
    onNavigateToVehicle: () -> Unit = {},
    onNavigateToTrips: () -> Unit = {},
    onNavigateToTripDetail: (String) -> Unit = {},
    onSignOut: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.md, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // Top Bar: Driver Identity & Sign Out & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Driver Workspace",
                    style = typography.titleSmall,
                    color = colors.foregroundMuted
                )
                Text(
                    text = driverName,
                    style = typography.headlineSmall,
                    color = colors.foreground
                )
                val license = context.driver.licenseNumberMasked ?: uiState.driverProfile?.licenseNumberMasked
                if (!license.isNullOrBlank()) {
                    Text(
                        text = "License: $license",
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IshaaraButton(
                    text = if (uiState.isRefreshing) "Refreshing..." else "Refresh",
                    onClick = onRefresh,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small,
                    enabled = !uiState.isRefreshing && !uiState.isActionInProgress
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                IshaaraButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small,
                    enabled = !uiState.isActionInProgress
                )
            }
        }

        // Notification / Feedback Banner
        if (uiState.userFacingNotification != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.sm)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.success, shapes.sm)
                    .clickable { onDismissMessage() }
                    .padding(spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.userFacingNotification,
                        style = typography.bodyMedium,
                        color = colors.success,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        style = typography.labelMedium,
                        color = colors.foregroundMuted
                    )
                }
            }
        }

        // Error Banner
        if (uiState.userFacingError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.sm)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.danger, shapes.sm)
                    .clickable { onDismissMessage() }
                    .padding(spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.userFacingError,
                        style = typography.bodyMedium,
                        color = colors.danger,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "✕",
                        style = typography.labelMedium,
                        color = colors.foregroundMuted
                    )
                }
            }
        }

        // Section 1: Verification Overview Card (Phase 3 & Phase 4)
        DriverVerificationOverviewCard(
            accessState = uiState.accessState,
            onCheckVerification = onNavigateToVerification
        )

        // Section 2: Fleet & Agency Affiliation Overview Card (Phase 5, Phase 9, Phase 10)
        DriverFleetAffiliationOverviewCard(
            accessState = uiState.accessState,
            membershipState = uiState.membershipState,
            onManageFleet = onNavigateToAgency
        )

        // Section 3: Transit Operations (Guarded by Platform Verification)
        if (!uiState.accessState.operationalAccess) {
            DriverOperationsLockedCard(accessState = uiState.accessState)
        } else {
            // Operational Status & Availability Control
            DriverStatusCard(
                identity = context.driver,
                isActionInProgress = uiState.isActionInProgress,
                statusActionState = uiState.statusActionState,
                onGoOnline = onGoOnline,
                onGoOffline = onGoOffline
            )

            // Operational Readiness Gate Card (Phase A06)
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OPERATIONAL READINESS",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        IshaaraButton(
                            text = "Check Eligibility",
                            onClick = onNavigateToReadiness,
                            variant = IshaaraButtonVariant.Text,
                            size = IshaaraButtonSize.Small
                        )
                    }
                    Text(
                        text = "Operational Prerequisites",
                        style = typography.titleSmall,
                        color = colors.foreground
                    )
                    Text(
                        text = "Authoritative platform gate evaluating verification, agency affiliation, profile completeness, and standing before entering operational mode.",
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )
                }
            }

            // Assigned Vehicle Card
            DriverVehicleCard(
                vehicle = context.vehicle,
                onManageVehicle = onNavigateToVehicle
            )

            // Current Operational Trip & Primary Lifecycle Action
            DriverTripOperationalCard(
                activeTrip = context.activeTrip,
                isActionInProgress = uiState.isActionInProgress,
                tripActionState = uiState.tripActionState,
                telemetryStatus = uiState.telemetryStatus,
                isDriverOnline = context.driver.status != DriverProfileStatus.OFFLINE,
                onStartTrip = onStartTrip,
                onInitiateCompleteTrip = onInitiateCompleteTrip,
                onInitiateCancelTrip = onInitiateCancelTrip,
                onRequestPermissions = onRequestPermissions,
                onViewTripDetails = onNavigateToTripDetail,
                onNavigateToTrips = onNavigateToTrips
            )

            // Passenger Ride Requests & Boarding CTA
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Text(
                        text = "PASSENGER WORKFLOW",
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = "Ride Requests & Boarding",
                        style = typography.titleMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = "Manage incoming passenger ride requests, confirm boarding at pickup, and monitor operational trip segments.",
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    IshaaraButton(
                        text = "Open Requests & Boarding",
                        onClick = onNavigateToRequests,
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Verified Daily Summary Stats
            DriverTodayStatsCard(
                stats = context.todayStats,
                activeRidesCount = context.activeRidesCount
            )

            // Driver Earnings & Historical Analytics CTA
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Text(
                        text = "FINANCIAL SUMMARY",
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = "Driver Earnings & Ride History",
                        style = typography.titleMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = "View authoritative earnings, platform deductions, completed ride breakdown, and payout settlement status.",
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    IshaaraButton(
                        text = "View Earnings & History",
                        onClick = onNavigateToEarnings,
                        variant = IshaaraButtonVariant.Secondary,
                        size = IshaaraButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Driver Rating Summary
            DriverRatingSummaryCard(ratingSummary = uiState.ratingSummary)
        }

        // Section 4: Quick Actions Section (Phase 3 & Phase 11)
        DriverQuickActionsCard(
            accessState = uiState.accessState,
            onNavigateToVerification = onNavigateToVerification,
            onNavigateToAgency = onNavigateToAgency,
            onNavigateToReadiness = onNavigateToReadiness,
            onNavigateToVehicle = onNavigateToVehicle,
            onNavigateToTrips = onNavigateToTrips,
            onNavigateToEarnings = onNavigateToEarnings
        )

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

/**
 * Driver Availability & Status Card.
 */
@Composable
private fun DriverStatusCard(
    identity: DriverIdentity,
    isActionInProgress: Boolean,
    statusActionState: DriverStatusActionState,
    onGoOnline: () -> Unit,
    onGoOffline: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val isVerified = identity.verificationStatus == DriverVerificationStatus.VERIFIED
    val isOnline = identity.status == DriverProfileStatus.ONLINE
    val isOnRide = identity.status == DriverProfileStatus.ON_RIDE
    val isSubmitting = statusActionState is DriverStatusActionState.Submitting

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operational Status",
                    style = typography.labelLarge,
                    color = colors.foregroundMuted
                )

                // Verification Badge
                when (identity.verificationStatus) {
                    DriverVerificationStatus.VERIFIED -> {
                        IshaaraBadge(text = "VERIFIED", variant = IshaaraBadgeVariant.Success)
                    }
                    DriverVerificationStatus.PENDING -> {
                        IshaaraBadge(text = "PENDING APPROVAL", variant = IshaaraBadgeVariant.Warning)
                    }
                    DriverVerificationStatus.REJECTED -> {
                        IshaaraBadge(text = "REJECTED", variant = IshaaraBadgeVariant.Danger)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Chip
                when (identity.status) {
                    DriverProfileStatus.ONLINE -> {
                        IshaaraStatusChip(status = IshaaraTransitStatus.ONLINE)
                    }
                    DriverProfileStatus.ON_RIDE -> {
                        IshaaraStatusChip(status = IshaaraTransitStatus.IN_PROGRESS, overrideLabel = "On Ride")
                    }
                    DriverProfileStatus.OFFLINE -> {
                        IshaaraStatusChip(status = IshaaraTransitStatus.OFFLINE)
                    }
                }

                // Availability Action Button
                if (identity.status == DriverProfileStatus.OFFLINE) {
                    IshaaraButton(
                        text = "Go Online",
                        onClick = onGoOnline,
                        variant = IshaaraButtonVariant.Primary,
                        size = IshaaraButtonSize.Medium,
                        enabled = isVerified && !isActionInProgress,
                        loading = isSubmitting
                    )
                } else if (identity.status == DriverProfileStatus.ONLINE) {
                    IshaaraButton(
                        text = "Go Offline",
                        onClick = onGoOffline,
                        variant = IshaaraButtonVariant.Outlined,
                        size = IshaaraButtonSize.Medium,
                        enabled = !isActionInProgress,
                        loading = isSubmitting
                    )
                } else if (isOnRide) {
                    Text(
                        text = "Operating Active Ride",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            if (!isVerified) {
                Text(
                    text = "Account verification is required by platform administration before going online.",
                    style = typography.bodySmall,
                    color = colors.warning
                )
            }
        }
    }
}

/**
 * Assigned Vehicle Card.
 */
@Composable
private fun DriverVehicleCard(
    vehicle: DriverAssignedVehicle?,
    onManageVehicle: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = "Assigned Vehicle",
                style = typography.labelLarge,
                color = colors.foregroundMuted
            )

            if (vehicle != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = vehicle.registrationNumber,
                        style = typography.headlineSmall,
                        color = colors.foreground
                    )
                    if (vehicle.isVerified) {
                        IshaaraBadge(text = "VERIFIED", variant = IshaaraBadgeVariant.Success)
                    }
                }

                val makeModel = listOfNotNull(vehicle.make, vehicle.model).joinToString(" ")
                val vehicleSubtitle = if (makeModel.isNotBlank()) {
                    "${vehicle.vehicleType} • $makeModel"
                } else {
                    vehicle.vehicleType
                }

                Text(
                    text = vehicleSubtitle,
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )

                Spacer(modifier = Modifier.height(spacing.xs))
                IshaaraButton(
                    text = "View Vehicle Details",
                    onClick = onManageVehicle,
                    variant = IshaaraButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "No vehicle currently assigned",
                    style = typography.titleMedium,
                    color = colors.foreground
                )
                Text(
                    text = "Your fleet operator has not assigned an active vehicle to your profile.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )

                Spacer(modifier = Modifier.height(spacing.xs))
                IshaaraButton(
                    text = "Manage Vehicle Assignment",
                    onClick = onManageVehicle,
                    variant = IshaaraButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Current Operational Trip Card with Start / Complete / Cancel Lifecycle Actions.
 */
@Composable
private fun DriverTripOperationalCard(
    activeTrip: DriverActiveTrip?,
    isActionInProgress: Boolean,
    tripActionState: TripActionState,
    telemetryStatus: com.ishara.app.feature.driver.tracking.DriverTrackingStatus = com.ishara.app.feature.driver.tracking.DriverTrackingStatus.Idle,
    isDriverOnline: Boolean,
    onStartTrip: (String) -> Unit,
    onInitiateCompleteTrip: (String) -> Unit,
    onInitiateCancelTrip: (String) -> Unit,
    onRequestPermissions: () -> Unit = {},
    onViewTripDetails: (String) -> Unit = {},
    onNavigateToTrips: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val isSubmittingTrip = tripActionState is TripActionState.Submitting

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Trip",
                    style = typography.labelLarge,
                    color = colors.foregroundMuted
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IshaaraButton(
                        text = "All Trips",
                        onClick = onNavigateToTrips,
                        variant = IshaaraButtonVariant.Text,
                        size = IshaaraButtonSize.Small
                    )

                    if (activeTrip != null) {
                        Spacer(modifier = Modifier.width(spacing.xs))
                        when (activeTrip.status) {
                            DriverTripStatus.CREATED -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.COMING, overrideLabel = "Created")
                            }
                            DriverTripStatus.SCHEDULED -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.PENDING, overrideLabel = "Scheduled")
                            }
                            DriverTripStatus.ASSIGNED -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.ACCEPTED, overrideLabel = "Assigned")
                            }
                            DriverTripStatus.READY -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.AVAILABLE, overrideLabel = "Ready")
                            }
                            DriverTripStatus.ACTIVE -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.IN_PROGRESS, overrideLabel = "Active Trip")
                            }
                            DriverTripStatus.COMPLETED -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.COMPLETED)
                            }
                            DriverTripStatus.CANCELLED -> {
                                IshaaraStatusChip(status = IshaaraTransitStatus.CANCELLED)
                            }
                        }
                    }
                }
            }

            if (activeTrip != null && activeTrip.status == DriverTripStatus.ACTIVE) {
                when (telemetryStatus) {
                    is com.ishara.app.feature.driver.tracking.DriverTrackingStatus.Active -> {
                        IshaaraBadge(text = "Location active", variant = IshaaraBadgeVariant.Success)
                    }
                    is com.ishara.app.feature.driver.tracking.DriverTrackingStatus.PermissionRequired -> {
                        Box(modifier = Modifier.clickable { onRequestPermissions() }) {
                            IshaaraBadge(text = "Turn on location to continue", variant = IshaaraBadgeVariant.Warning)
                        }
                    }
                    is com.ishara.app.feature.driver.tracking.DriverTrackingStatus.GpsDisabled -> {
                        Box(modifier = Modifier.clickable { onRequestPermissions() }) {
                            IshaaraBadge(text = "Turn on location to continue", variant = IshaaraBadgeVariant.Warning)
                        }
                    }
                    is com.ishara.app.feature.driver.tracking.DriverTrackingStatus.NetworkUnavailable -> {
                        IshaaraBadge(text = "Connection lost. Trying again…", variant = IshaaraBadgeVariant.Neutral)
                    }
                    is com.ishara.app.feature.driver.tracking.DriverTrackingStatus.Error -> {
                        IshaaraBadge(text = "Location temporarily unavailable", variant = IshaaraBadgeVariant.Danger)
                    }
                    is com.ishara.app.feature.driver.tracking.DriverTrackingStatus.Idle -> {
                        // Inactive/Idle
                    }
                }
            }

            if (activeTrip != null) {
                val originLabel = activeTrip.origin.name ?: activeTrip.origin.formattedAddress
                val destinationLabel = activeTrip.destination.name ?: activeTrip.destination.formattedAddress

                Text(
                    text = "$originLabel → $destinationLabel",
                    style = typography.headlineSmall,
                    color = colors.foreground
                )

                // Detailed Waypoints
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "From: ${activeTrip.origin.formattedAddress}",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = "To: ${activeTrip.destination.formattedAddress}",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }

                // Route Distance & Duration
                if (activeTrip.distanceMeters != null || activeTrip.durationSeconds != null) {
                    val distanceKm = activeTrip.distanceMeters?.let { String.format(Locale.US, "%.1f km", it / 1000.0) }
                    val durationMin = activeTrip.durationSeconds?.let { "${(it / 60).toInt()} mins" }
                    val routeSummary = listOfNotNull(distanceKm, durationMin).joinToString(" • ")

                    Text(
                        text = routeSummary,
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )
                }

                Spacer(modifier = Modifier.height(spacing.xs))

                // Primary Dominant Lifecycle Action
                when (activeTrip.status) {
                    DriverTripStatus.CREATED,
                    DriverTripStatus.SCHEDULED,
                    DriverTripStatus.ASSIGNED,
                    DriverTripStatus.READY -> {
                        // Dominant Primary 54dp CTA
                        IshaaraButton(
                            text = "Start Trip",
                            onClick = { onStartTrip(activeTrip.id) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Large,
                            enabled = !isActionInProgress && isDriverOnline,
                            loading = isSubmittingTrip && (tripActionState as? TripActionState.Submitting)?.actionType == TripActionType.START
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            IshaaraButton(
                                text = "Trip Details",
                                onClick = { onViewTripDetails(activeTrip.id) },
                                modifier = Modifier.weight(1f),
                                variant = IshaaraButtonVariant.Secondary,
                                size = IshaaraButtonSize.Medium
                            )

                            IshaaraButton(
                                text = "Cancel Trip",
                                onClick = { onInitiateCancelTrip(activeTrip.id) },
                                modifier = Modifier.weight(1f),
                                variant = IshaaraButtonVariant.Outlined,
                                size = IshaaraButtonSize.Medium,
                                enabled = !isActionInProgress
                            )
                        }
                    }
                    DriverTripStatus.ACTIVE -> {
                        // Dominant Primary 54dp CTA
                        IshaaraButton(
                            text = "Complete Trip",
                            onClick = { onInitiateCompleteTrip(activeTrip.id) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Large,
                            enabled = !isActionInProgress,
                            loading = isSubmittingTrip && (tripActionState as? TripActionState.Submitting)?.actionType == TripActionType.COMPLETE
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            IshaaraButton(
                                text = "Trip Details",
                                onClick = { onViewTripDetails(activeTrip.id) },
                                modifier = Modifier.weight(1f),
                                variant = IshaaraButtonVariant.Secondary,
                                size = IshaaraButtonSize.Medium
                            )

                            IshaaraButton(
                                text = "Cancel Trip",
                                onClick = { onInitiateCancelTrip(activeTrip.id) },
                                modifier = Modifier.weight(1f),
                                variant = IshaaraButtonVariant.Danger,
                                size = IshaaraButtonSize.Medium,
                                enabled = !isActionInProgress
                            )
                        }
                    }
                    DriverTripStatus.COMPLETED -> {
                        Text(
                            text = "Trip has been completed.",
                            style = typography.bodyMedium,
                            color = colors.success
                        )
                        IshaaraButton(
                            text = "View Trip Details",
                            onClick = { onViewTripDetails(activeTrip.id) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = IshaaraButtonVariant.Secondary,
                            size = IshaaraButtonSize.Small
                        )
                    }
                    DriverTripStatus.CANCELLED -> {
                        Text(
                            text = "Trip was cancelled.",
                            style = typography.bodyMedium,
                            color = colors.danger
                        )
                        IshaaraButton(
                            text = "View Trip Details",
                            onClick = { onViewTripDetails(activeTrip.id) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = IshaaraButtonVariant.Secondary,
                            size = IshaaraButtonSize.Small
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "No active trip scheduled",
                    style = typography.titleMedium,
                    color = colors.foreground
                )
                Text(
                    text = "When a trip is scheduled or assigned to your vehicle, it will appear here ready to start.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                IshaaraButton(
                    text = "View All Trips & Dispatch",
                    onClick = onNavigateToTrips,
                    modifier = Modifier.fillMaxWidth(),
                    variant = IshaaraButtonVariant.Secondary,
                    size = IshaaraButtonSize.Medium
                )
            }
        }
    }
}

/**
 * Secondary Verified Daily Summary Stats.
 */
@Composable
private fun DriverTodayStatsCard(
    stats: DriverTodayStats,
    activeRidesCount: Int
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = "Today's Activity",
                style = typography.labelLarge,
                color = colors.foregroundMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${stats.completedRidesCount}",
                        style = typography.headlineSmall,
                        color = colors.foreground
                    )
                    Text(
                        text = "Completed Rides",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$activeRidesCount",
                        style = typography.headlineSmall,
                        color = colors.foreground
                    )
                    Text(
                        text = "Active In-Flight Rides",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }
        }
    }
}

/**
 * Driver aggregate rating summary card.
 */
@Composable
private fun DriverRatingSummaryCard(
    ratingSummary: com.ishara.app.domain.model.DriverRatingSummary?,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVICE QUALITY & RATINGS",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )

                IshaaraBadge(
                    text = if (ratingSummary != null && ratingSummary.ratingCount > 0) "ACTIVE REVIEWS" else "NEW DRIVER",
                    variant = if (ratingSummary != null && ratingSummary.ratingCount > 0) IshaaraBadgeVariant.Success else IshaaraBadgeVariant.Neutral
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Text(
                    text = ratingSummary?.formattedScore ?: "New",
                    style = typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Text(
                    text = "★",
                    style = typography.headlineMedium,
                    color = com.ishara.app.core.designsystem.theme.IshaaraPalette.Amber500
                )
                Column {
                    Text(
                        text = if (ratingSummary != null && ratingSummary.ratingCount > 0)
                            "${ratingSummary.ratingCount} passenger ratings"
                        else
                            "No ratings recorded yet",
                        style = typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.foreground
                    )
                    Text(
                        text = "Authoritative rating summary from completed rides",
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )
                }
            }
        }
    }
}

/**
 * Overview card for Platform Verification Status (Phase 3 & Phase 4).
 */
@Composable
private fun DriverVerificationOverviewCard(
    accessState: DriverAccessState,
    onCheckVerification: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val badgeVariant = when (accessState.verificationStatus) {
        DriverVerificationStatus.VERIFIED -> IshaaraBadgeVariant.Success
        DriverVerificationStatus.PENDING -> IshaaraBadgeVariant.Warning
        DriverVerificationStatus.REJECTED -> IshaaraBadgeVariant.Danger
    }

    val badgeText = when (accessState.verificationStatus) {
        DriverVerificationStatus.VERIFIED -> "VERIFIED"
        DriverVerificationStatus.PENDING -> "UNDER REVIEW"
        DriverVerificationStatus.REJECTED -> "REJECTED"
    }

    val title = when (accessState.verificationStatus) {
        DriverVerificationStatus.VERIFIED -> "Platform Identity Verified"
        DriverVerificationStatus.PENDING -> "Verification in Progress"
        DriverVerificationStatus.REJECTED -> "Verification Rejected"
    }

    val description = when (accessState.verificationStatus) {
        DriverVerificationStatus.VERIFIED -> "Your driver documents and identity have been verified by Ishaara transport administration. You have platform clearance for commercial transit."
        DriverVerificationStatus.PENDING -> "Your driver verification is currently being reviewed. Operational transit actions (going online, starting trips, receiving passenger requests) will unlock once verified."
        DriverVerificationStatus.REJECTED -> "Your verification submission was not approved. Please review administrator feedback or re-submit your documents to continue."
    }

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLATFORM VERIFICATION",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
                IshaaraBadge(
                    text = badgeText,
                    variant = badgeVariant
                )
            }

            Text(
                text = title,
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )

            Text(
                text = description,
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            IshaaraButton(
                text = "Check Verification Details",
                onClick = onCheckVerification,
                variant = IshaaraButtonVariant.Secondary,
                size = IshaaraButtonSize.Small,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Overview card for Fleet & Agency Affiliation (Phase 5, Phase 9, Phase 10).
 */
@Composable
private fun DriverFleetAffiliationOverviewCard(
    accessState: DriverAccessState,
    membershipState: DriverMembershipState?,
    onManageFleet: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val agency = when (membershipState) {
        is DriverMembershipState.Approved -> membershipState.membership
        is DriverMembershipState.Pending -> membershipState.membership
        is DriverMembershipState.Rejected -> membershipState.membership
        else -> null
    }
    val agencyName = agency?.agencyName ?: accessState.agencyName ?: "No Transport Fleet"
    val agencyCity = agency?.agencyCity ?: ""
    val hasActiveFleet = accessState.membershipActive

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FLEET AFFILIATION",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
                IshaaraBadge(
                    text = if (hasActiveFleet) "ACTIVE" else "NO FLEET",
                    variant = if (hasActiveFleet) IshaaraBadgeVariant.Success else IshaaraBadgeVariant.Neutral
                )
            }

            Text(
                text = agencyName,
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )

            if (agencyCity.isNotBlank()) {
                Text(
                    text = agencyCity,
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            Text(
                text = if (hasActiveFleet) {
                    "Affiliated with $agencyName. Fleet operations, routes, and vehicle assignments are managed through your agency association."
                } else {
                    "You are not currently affiliated with any active transport agency or operator. Join an agency fleet to receive vehicle assignments and operational routes."
                },
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            IshaaraButton(
                text = if (hasActiveFleet) "View Fleet Affiliation" else "Join a Transport Fleet",
                onClick = onManageFleet,
                variant = IshaaraButtonVariant.Secondary,
                size = IshaaraButtonSize.Small,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Card explaining why operational actions are locked (Phase 6, Phase 7, Phase 10).
 */
@Composable
private fun DriverOperationsLockedCard(
    accessState: DriverAccessState
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPERATIONS GATE",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
                IshaaraBadge(
                    text = "LOCKED",
                    variant = IshaaraBadgeVariant.Neutral
                )
            }

            Text(
                text = "Transit Operations Locked",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )

            val reason = accessState.operationalLockReason ?: "Operational features are locked until platform verification is complete."
            Text(
                text = reason,
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            Text(
                text = "While under review or pending verification, you can review your agency fleet membership, update your profile, and check verification status. Vehicle assignments, live GPS telemetry broadcasting, going online, and starting commercial trips are restricted until administrative clearance is granted.",
                style = typography.bodySmall,
                color = colors.foregroundSubtle
            )
        }
    }
}

/**
 * Quick Actions card providing direct navigation to verification, fleet, profile, etc.
 */
@Composable
private fun DriverQuickActionsCard(
    accessState: DriverAccessState,
    onNavigateToVerification: () -> Unit,
    onNavigateToAgency: () -> Unit,
    onNavigateToReadiness: () -> Unit,
    onNavigateToVehicle: () -> Unit,
    onNavigateToTrips: () -> Unit,
    onNavigateToEarnings: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "QUICK ACTIONS & NAVIGATION",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                IshaaraButton(
                    text = "Verification Status",
                    onClick = onNavigateToVerification,
                    variant = IshaaraButtonVariant.Secondary,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
                IshaaraButton(
                    text = "Fleet Affiliation",
                    onClick = onNavigateToAgency,
                    variant = IshaaraButtonVariant.Secondary,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                IshaaraButton(
                    text = "Readiness Gate",
                    onClick = onNavigateToReadiness,
                    variant = IshaaraButtonVariant.Secondary,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
                if (accessState.operationalAccess) {
                    IshaaraButton(
                        text = "Assigned Vehicle",
                        onClick = onNavigateToVehicle,
                        variant = IshaaraButtonVariant.Secondary,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (accessState.operationalAccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    IshaaraButton(
                        text = "Trip History",
                        onClick = onNavigateToTrips,
                        variant = IshaaraButtonVariant.Secondary,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                    IshaaraButton(
                        text = "Earnings",
                        onClick = onNavigateToEarnings,
                        variant = IshaaraButtonVariant.Secondary,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

