package com.ishara.app.feature.student.transit

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.core.utils.TransitFormatters
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.model.TransitStop
import com.ishara.app.feature.student.tracking.components.IshaaraTrackingCanvasMap
import java.util.Locale

@Composable
fun RouteDetailsScreen(
    viewModel: RouteDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = IshaaraTheme.colors
    val spacing = IshaaraTheme.spacing
    val typography = IshaaraTheme.typography

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            IshaaraTopBar(
                title = stringResource(R.string.transit_route_details_title),
                subtitle = uiState.route?.name,
                onBackClick = onNavigateBack,
                actions = {
                    IshaaraButton(
                        text = stringResource(R.string.transit_refresh_button),
                        onClick = { viewModel.refresh() },
                        variant = IshaaraButtonVariant.Text,
                        size = IshaaraButtonSize.Small,
                        loading = uiState.isRefreshing
                    )
                }
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.route == null -> {
                    IshaaraLoadingState(message = "LOADING ROUTE DETAILS...")
                }
                uiState.errorMessage != null && uiState.route == null -> {
                    IshaaraErrorState(
                        title = stringResource(R.string.transit_error_title),
                        message = uiState.errorMessage ?: stringResource(R.string.transit_error_subtitle),
                        onRetryClick = { viewModel.refresh() }
                    )
                }
                uiState.route != null -> {
                    val route = uiState.route!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        // Offline or Stale Banner
                        if (uiState.isOffline) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceElevated, shape = IshaaraTheme.shapes.sm)
                                    .border(1.dp, colors.borderSubtle, IshaaraTheme.shapes.sm)
                                    .padding(horizontal = spacing.md, vertical = spacing.sm)
                            ) {
                                Text(
                                    text = stringResource(R.string.transit_offline_banner),
                                    style = typography.labelMedium,
                                    color = colors.foregroundMuted
                                )
                            }
                        } else if (uiState.freshness == TransitCacheFreshness.STALE) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceElevated, shape = IshaaraTheme.shapes.sm)
                                    .border(1.dp, colors.borderSubtle, IshaaraTheme.shapes.sm)
                                    .padding(horizontal = spacing.md, vertical = spacing.sm)
                            ) {
                                Text(
                                    text = "Showing saved schedule • ${TransitFormatters.formatRelativeTime(uiState.lastRefreshedAtMillis)}",
                                    style = typography.labelMedium,
                                    color = colors.foregroundMuted
                                )
                            }
                        }

                        // Map View Card (reusing Phase 11 IshaaraTrackingCanvasMap)
                        IshaaraCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            IshaaraTrackingCanvasMap(
                                driverLocation = null,
                                pickupCoordinates = route.originStop.coordinates,
                                destinationCoordinates = route.destinationStop.coordinates,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Route Header Card
                        RouteHeaderCard(route = route)

                        // Ordered Stops Timeline Card
                        RouteStopsTimelineCard(stops = route.stops)

                        // Schedule & Operating Notes Card
                        ScheduleCard(
                            schedule = uiState.schedule,
                            lastRefreshedAtMillis = uiState.lastRefreshedAtMillis,
                            freshness = uiState.freshness
                        )

                        // Vehicle & Driver Details Card
                        VehicleDetailsCard(route = route)

                        Spacer(modifier = Modifier.height(spacing.xl))
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteHeaderCard(
    route: TransitRoute,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val distanceText = TransitFormatters.formatDistance(route.distanceMeters)
    val durationText = TransitFormatters.formatDuration(route.durationSeconds)

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.name,
                    style = typography.titleLarge,
                    color = colors.foreground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IshaaraBadge(
                    text = route.operatingStatus.name,
                    variant = if (route.operatingStatus == TransitOperatingStatus.ACTIVE) {
                        IshaaraBadgeVariant.Success
                    } else {
                        IshaaraBadgeVariant.Neutral
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (distanceText != null) {
                    Text(
                        text = "Distance: $distanceText",
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted
                    )
                }
                if (durationText != null) {
                    Text(
                        text = "Est. Time: $durationText",
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteStopsTimelineCard(
    stops: List<TransitStop>,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Text(
                text = stringResource(R.string.transit_stops_title),
                style = typography.titleMedium,
                color = colors.foreground,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            stops.forEachIndexed { index, stop ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${stop.sequence + 1}",
                                style = typography.labelSmall,
                                color = colors.background,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (index < stops.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(36.dp)
                                    .background(colors.borderSubtle)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stop.name,
                            style = typography.bodyLarge,
                            color = colors.foreground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stop.formattedAddress,
                            style = typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Lat: %.4f, Lng: %.4f",
                                stop.coordinates.latitude,
                                stop.coordinates.longitude
                            ),
                            style = typography.labelSmall,
                            color = colors.foregroundSubtle
                        )
                    }
                }

                if (index < stops.size - 1) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: com.ishara.app.domain.model.TransitSchedule?,
    lastRefreshedAtMillis: Long?,
    freshness: TransitCacheFreshness,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Text(
                text = stringResource(R.string.transit_schedule_title),
                style = typography.titleMedium,
                color = colors.foreground,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = schedule?.operatingNote ?: stringResource(R.string.transit_schedule_no_static_timetables),
                style = typography.bodyMedium,
                color = colors.foregroundMuted
            )

            Spacer(modifier = Modifier.height(spacing.sm))
            IshaaraDivider()
            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cache Status",
                    style = typography.labelMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = TransitFormatters.formatRelativeTime(lastRefreshedAtMillis),
                    style = typography.labelMedium,
                    color = if (freshness == TransitCacheFreshness.FRESH) colors.primary else colors.foregroundSubtle,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VehicleDetailsCard(
    route: TransitRoute,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Text(
                text = stringResource(R.string.transit_vehicle_details),
                style = typography.titleMedium,
                color = colors.foreground,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Type",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = route.vehicleType.name,
                    style = typography.bodyMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Plate Number",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = route.vehiclePlate,
                    style = typography.bodyMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Driver",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Text(
                    text = route.driverName,
                    style = typography.bodyMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
