package com.ishara.app.feature.student.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.component.IshaaraStatusIndicator
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.feature.student.tracking.components.IshaaraTrackingCanvasMap

/**
 * Production Student / Passenger Home Screen.
 * Strictly adheres to Phase 02 and Phase 05 UX principles:
 * - Simple, fast, trustworthy, modern, human
 * - Clear visual hierarchy answering the 7 core student mobility questions
 * - Active ride priority: surfaced at top if one exists
 * - Accessible thumb-zone destination search
 * - Human-readable current location and interactive campus map
 * - Discovery entry point (Phase 06 foundation)
 */
@Composable
fun StudentHomeScreen(
    viewModel: StudentHomeViewModel,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = { viewModel.onProfileClicked() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(fineGranted, coarseGranted)
    }

    val onRequestLocationPermissions: () -> Unit = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val onOpenLocationSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // Proactively check and request location permission on launch
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            onRequestLocationPermissions()
        }
    }

    StudentHomeContent(
        uiState = uiState,
        onSearchClick = { viewModel.onSearchClicked() },
        onVoiceClick = { viewModel.onVoiceClicked() },
        onBrowseRoutesClick = { viewModel.onBrowseRoutesClicked() },
        onDestinationSelect = { viewModel.onDestinationSelected(it) },
        onClearDestination = { viewModel.onClearSelectedDestination() },
        onFindTripsClick = { viewModel.onFindTripsClicked() },
        onViewActiveRideClick = { viewModel.onViewActiveRideClicked(it) },
        onPayFareClick = { viewModel.onPayRideClicked(it) },
        onAllowLocationClick = onRequestLocationPermissions,
        onDismissLocationClick = { viewModel.onDismissLocationPermission() },
        onOpenSettingsClick = onOpenLocationSettings,
        onClearRecentClick = { viewModel.onClearRecentDestinations() },
        onProfileClick = onProfileClick,
        modifier = modifier
    )
}

@Composable
fun StudentHomeContent(
    uiState: StudentHomeUiState,
    onSearchClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onBrowseRoutesClick: () -> Unit = {},
    onDestinationSelect: (StudentDestination) -> Unit,
    onClearDestination: () -> Unit,
    onFindTripsClick: () -> Unit,
    onViewActiveRideClick: (String) -> Unit,
    onPayFareClick: (String) -> Unit = {},
    onAllowLocationClick: () -> Unit,
    onDismissLocationClick: () -> Unit,
    onOpenSettingsClick: () -> Unit = {},
    onClearRecentClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                // Header: Greeting & Student Profile
                item {
                    Spacer(modifier = Modifier.height(spacing.md))
                    StudentHomeHeader(
                        greeting = uiState.greeting,
                        userName = uiState.userName,
                        onProfileClick = onProfileClick
                    )
                }

                // Active Ride Priority Card
                when (val activeState = uiState.activeRideState) {
                    is ActiveRideUiState.Active -> {
                        item {
                            ActiveRideCard(
                                ride = activeState.ride,
                                onViewClick = { onViewActiveRideClick(activeState.ride.id) },
                                onPayFareClick = { onPayFareClick(activeState.ride.id) }
                            )
                        }
                    }
                    else -> Unit
                }

                // Destination Search Entry Card, Hands-Free Voice Trip Entry, & Public Transit Routes
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        DestinationSearchEntryCard(
                            onSearchClick = onSearchClick
                        )

                        VoiceTripEntryCard(
                            onVoiceClick = onVoiceClick
                        )

                        TransitRoutesEntryCard(
                            onBrowseRoutesClick = onBrowseRoutesClick
                        )
                    }
                }

                // Current Location Pill / Rationale Banner
                item {
                    CurrentLocationSection(
                        locationState = uiState.locationState,
                        onAllowClick = onAllowLocationClick,
                        onDismissClick = onDismissLocationClick,
                        onOpenSettingsClick = onOpenSettingsClick
                    )
                }

                // Live Interactive Campus & Transit Map
                item {
                    val userCoords = (uiState.locationState as? LocationUiState.Available)?.location?.coordinates
                    val isPermReq = uiState.locationState is LocationUiState.PermissionRequired || uiState.locationState is LocationUiState.PermissionDenied
                    val isLocDisabled = uiState.locationState is LocationUiState.LocationServicesDisabled

                    IshaaraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        IshaaraTrackingCanvasMap(
                            driverLocation = null,
                            pickupCoordinates = null,
                            destinationCoordinates = null,
                            userLocation = userCoords,
                            isPermissionRequired = isPermReq,
                            isLocationDisabled = isLocDisabled,
                            onRequestPermission = onAllowLocationClick,
                            onOpenSettings = onOpenSettingsClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Selected Destination Confirmation (Discovery Entry Point)
                if (uiState.selectedDestination != null) {
                    item {
                        DestinationConfirmationSection(
                            destination = uiState.selectedDestination,
                            onFindTripsClick = onFindTripsClick,
                            onChangeDestinationClick = onSearchClick,
                            onClearClick = onClearDestination
                        )
                    }
                }

                // Recent Destinations Section
                if (uiState.recentDestinations.isNotEmpty() && uiState.selectedDestination == null) {
                    item {
                        RecentDestinationsHeader(onClearClick = onClearRecentClick)
                    }

                    items(uiState.recentDestinations) { recent ->
                        RecentDestinationItem(
                            destination = recent,
                            onClick = { onDestinationSelect(recent) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(spacing.xl))
                }
            }

            // Bottom Navigation Bar
            StudentBottomNavigation(
                onProfileClick = onProfileClick,
                onTripsClick = onFindTripsClick
            )
        }
    }
}

@Composable
private fun StudentHomeHeader(
    greeting: String,
    userName: String,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, $userName",
                style = typography.headlineMedium,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = "Ready to explore campus transit",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )
        }

        IshaaraBadge(
            text = "STUDENT",
            modifier = Modifier.clickable(onClick = onProfileClick)
        )
    }
}

/**
 * Main tap target for destination search in the thumb zone.
 */
@Composable
private fun DestinationSearchEntryCard(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.md)
            .background(colors.surfaceElevated)
            .border(width = 1.dp, color = colors.border, shape = shapes.md)
            .clickable(onClick = onSearchClick)
            .padding(spacing.md)
            .semantics {
                role = Role.Button
                contentDescription = "Search destination. Where are you going?"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surfaceSubtle, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔍",
                    style = typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            Column {
                Text(
                    text = stringResource(R.string.student_home_title),
                    style = typography.titleLarge,
                    color = colors.foreground
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = "Search campus stops, hostels, and city places",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
            }
        }
    }
}

/**
 * Main tap target for voice trip creation in the thumb zone.
 */
@Composable
private fun VoiceTripEntryCard(
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.md)
            .background(colors.surfaceElevated)
            .border(width = 1.dp, color = colors.border, shape = shapes.md)
            .clickable(onClick = onVoiceClick)
            .padding(spacing.md)
            .semantics {
                role = Role.Button
                contentDescription = "Create a trip using your voice"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎙",
                    style = typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.voice_entry_button),
                    style = typography.titleMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = "Hands-free trip creation. Just tap and speak.",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
            }
        }
    }
}

/**
 * Entry card for public transit routes and schedules browsing (Phase 14).
 */
@Composable
private fun TransitRoutesEntryCard(
    onBrowseRoutesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.md)
            .background(colors.surfaceElevated)
            .border(width = 1.dp, color = colors.border, shape = shapes.md)
            .clickable(onClick = onBrowseRoutesClick)
            .padding(spacing.md)
            .semantics {
                role = Role.Button
                contentDescription = "Browse public transit routes and schedules"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🚌",
                    style = typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.transit_browse_card_title),
                    style = typography.titleMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(R.string.transit_browse_card_subtitle),
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
            }
        }
    }
}

/**
 * Displays human-readable location and handles permissions gracefully.
 */
@Composable
private fun CurrentLocationSection(
    locationState: LocationUiState,
    onAllowClick: () -> Unit,
    onDismissClick: () -> Unit,
    onOpenSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    when (locationState) {
        is LocationUiState.Available -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colors.surface, shape = shapes.sm)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IshaaraStatusIndicator(
                    status = IshaaraTransitStatus.AVAILABLE,
                    showLabel = false
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Column {
                    Text(
                        text = stringResource(R.string.current_location_label),
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = locationState.location.subtitle,
                        style = typography.bodyMedium,
                        color = colors.foreground
                    )
                }
            }
        }
        is LocationUiState.Loading -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = stringResource(R.string.location_loading),
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }
        is LocationUiState.PermissionRequired -> {
            IshaaraCard(modifier = modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = stringResource(R.string.location_permission_title),
                        style = typography.titleMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = stringResource(R.string.location_permission_subtitle),
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        IshaaraButton(
                            text = stringResource(R.string.location_permission_allow),
                            onClick = onAllowClick,
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Small,
                            modifier = Modifier.weight(1f)
                        )
                        IshaaraButton(
                            text = stringResource(R.string.location_permission_not_now),
                            onClick = onDismissClick,
                            variant = IshaaraButtonVariant.Text,
                            size = IshaaraButtonSize.Small,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        is LocationUiState.PermissionDenied -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle, shape = shapes.sm)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.location_permission_denied),
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                IshaaraButton(
                    text = stringResource(R.string.location_permission_enable),
                    onClick = onAllowClick,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small
                )
            }
        }
        is LocationUiState.LocationServicesDisabled -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle, shape = shapes.sm)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Location Services are turned off.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                IshaaraButton(
                    text = "Settings",
                    onClick = onOpenSettingsClick,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small
                )
            }
        }
        is LocationUiState.Unavailable -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = locationState.message,
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
                IshaaraButton(
                    text = stringResource(R.string.retry_button),
                    onClick = onAllowClick,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small
                )
            }
        }
        is LocationUiState.Error -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = locationState.message,
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
                IshaaraButton(
                    text = stringResource(R.string.retry_button),
                    onClick = onAllowClick,
                    variant = IshaaraButtonVariant.Text,
                    size = IshaaraButtonSize.Small
                )
            }
        }
    }
}

/**
 * Surfaced with top priority when the student has an in-flight ride.
 */
@Composable
private fun ActiveRideCard(
    ride: Ride,
    onViewClick: () -> Unit,
    onPayFareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IshaaraBadge(text = stringResource(R.string.active_ride_title))
                IshaaraStatusIndicator(
                    status = IshaaraTransitStatus.IN_PROGRESS,
                    showLabel = true
                )
            }

            Text(
                text = "${ride.pickup.address} → ${ride.dropoff.address}",
                style = typography.headlineSmall,
                color = colors.foreground
            )

            Text(
                text = "${ride.seats} seat • Fare: ₹${ride.farePaise / 100}",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                com.ishara.app.core.designsystem.component.IshaaraButton(
                    text = stringResource(R.string.view_ride_button),
                    onClick = onViewClick,
                    variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Outlined,
                    size = com.ishara.app.core.designsystem.component.IshaaraButtonSize.Medium,
                    modifier = Modifier.weight(1f)
                )
                com.ishara.app.core.designsystem.component.IshaaraButton(
                    text = stringResource(R.string.payment_pay_fare_card_button),
                    onClick = onPayFareClick,
                    variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Primary,
                    size = com.ishara.app.core.designsystem.component.IshaaraButtonSize.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Confirmation card shown once a destination is selected.
 * Acts as the official entry point into Phase 06 — Trip Discovery.
 */
@Composable
private fun DestinationConfirmationSection(
    destination: StudentDestination,
    onFindTripsClick: () -> Unit,
    onChangeDestinationClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.going_to_label),
                    style = typography.labelMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = "Clear",
                    style = typography.labelSmall,
                    color = colors.foregroundSubtle,
                    modifier = Modifier
                        .clickable(role = Role.Button) { onClearClick() }
                        .semantics { role = Role.Button }
                )
            }

            Text(
                text = destination.name,
                style = typography.headlineMedium,
                color = colors.foreground
            )

            if (destination.formattedAddress.isNotBlank()) {
                Text(
                    text = destination.formattedAddress,
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // Large Thumb-Zone Action: Enters Phase 06 Discovery
            IshaaraPrimaryAction(
                text = stringResource(R.string.find_trips_button),
                onClick = onFindTripsClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecentDestinationsHeader(
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.recent_destinations_title),
            style = typography.titleSmall,
            color = colors.foregroundMuted
        )
        Text(
            text = stringResource(R.string.recent_destinations_clear),
            style = typography.bodySmall,
            color = colors.foregroundSubtle,
            modifier = Modifier
                .clickable(role = Role.Button) { onClearClick() }
                .semantics { role = Role.Button }
        )
    }
}

@Composable
private fun RecentDestinationItem(
    destination: StudentDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.sm)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .semantics {
                role = Role.Button
                contentDescription = "Recent destination: ${destination.name}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📍",
            style = typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = destination.name,
                style = typography.titleMedium,
                color = colors.foreground
            )
            if (destination.formattedAddress.isNotBlank()) {
                Text(
                    text = destination.formattedAddress,
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }
    }
}

/**
 * Clean, lightweight Bottom Navigation bar.
 */
@Composable
private fun StudentBottomNavigation(
    onHomeClick: () -> Unit = {},
    onTripsClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(width = 1.dp, color = colors.border)
            .padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = stringResource(R.string.nav_home),
            icon = "🏠",
            selected = true,
            onClick = onHomeClick
        )
        BottomNavItem(
            label = stringResource(R.string.nav_trips),
            icon = "🚌",
            selected = false,
            onClick = onTripsClick
        )
        BottomNavItem(
            label = stringResource(R.string.nav_activity),
            icon = "📋",
            selected = false,
            onClick = onActivityClick
        )
        BottomNavItem(
            label = stringResource(R.string.nav_profile),
            icon = "👤",
            selected = false,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .semantics {
                role = Role.Tab
                this.contentDescription = label
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = typography.titleMedium
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = label,
            style = typography.labelSmall,
            color = if (selected) colors.accent else colors.foregroundSubtle
        )
    }
}
