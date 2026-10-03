package com.ishara.app.feature.student.transit

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.core.utils.TransitFormatters
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitRoute

@Composable
fun TransitRoutesScreen(
    viewModel: TransitRoutesViewModel,
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
                title = stringResource(R.string.transit_routes_title),
                subtitle = stringResource(R.string.transit_routes_subtitle),
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
                uiState.isLoading && uiState.routes.isEmpty() -> {
                    IshaaraLoadingState(message = "LOADING TRANSIT ROUTES...")
                }
                uiState.errorMessage != null && uiState.routes.isEmpty() -> {
                    IshaaraErrorState(
                        title = stringResource(R.string.transit_error_title),
                        message = uiState.errorMessage ?: stringResource(R.string.transit_error_subtitle),
                        onRetryClick = { viewModel.refresh() }
                    )
                }
                uiState.routes.isEmpty() -> {
                    EmptyTransitState(
                        isOffline = uiState.isOffline,
                        onRefreshClick = { viewModel.refresh() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(spacing.xs))
                            if (uiState.isOffline) {
                                OfflineCacheBanner(message = stringResource(R.string.transit_offline_banner))
                            } else if (uiState.freshness == TransitCacheFreshness.STALE) {
                                StaleCacheBanner(
                                    message = "Showing saved transit schedule • ${TransitFormatters.formatRelativeTime(uiState.lastRefreshedAtMillis)}"
                                )
                            }
                        }

                        items(
                            items = uiState.routes,
                            key = { it.id }
                        ) { route ->
                            TransitRouteCard(
                                route = route,
                                onClick = { viewModel.onRouteSelected(route.id) }
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
}

@Composable
private fun TransitRouteCard(
    route: TransitRoute,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val distanceText = TransitFormatters.formatDistance(route.distanceMeters)
    val durationText = TransitFormatters.formatDuration(route.durationSeconds)
    val metricsSummary = listOfNotNull(distanceText, durationText).joinToString(" • ")

    IshaaraCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            // Top Row: Vehicle badge and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IshaaraBadge(
                        text = route.vehicleType.name,
                        variant = IshaaraBadgeVariant.Primary
                    )
                    Text(
                        text = route.vehiclePlate,
                        style = typography.labelMedium,
                        color = colors.foregroundMuted
                    )
                }

                val statusVariant = when (route.operatingStatus) {
                    TransitOperatingStatus.ACTIVE -> IshaaraBadgeVariant.Success
                    TransitOperatingStatus.SCHEDULED -> IshaaraBadgeVariant.Neutral
                    TransitOperatingStatus.COMPLETED -> IshaaraBadgeVariant.Neutral
                    TransitOperatingStatus.INACTIVE -> IshaaraBadgeVariant.Warning
                }
                IshaaraBadge(
                    text = route.operatingStatus.name,
                    variant = statusVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Origin Stop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
                Text(
                    text = route.originStop.name,
                    style = typography.titleMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Connecting indicator
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                    .width(2.dp)
                    .height(14.dp)
                    .background(colors.borderSubtle)
            )

            // Destination Stop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .border(2.dp, colors.primary, CircleShape)
                )
                Text(
                    text = route.destinationStop.name,
                    style = typography.titleMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Metrics and Driver Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (metricsSummary.isNotBlank()) {
                    Text(
                        text = metricsSummary,
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = "Driver: ${route.driverName}",
                    style = typography.labelSmall,
                    color = colors.foregroundSubtle
                )
            }
        }
    }
}

@Composable
private fun OfflineCacheBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs)
            .background(colors.surfaceElevated, shape = IshaaraTheme.shapes.sm)
            .border(1.dp, colors.borderSubtle, IshaaraTheme.shapes.sm)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Text(
            text = message,
            style = typography.labelMedium,
            color = colors.foregroundMuted
        )
    }
}

@Composable
private fun StaleCacheBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs)
            .background(colors.surfaceElevated, shape = IshaaraTheme.shapes.sm)
            .border(1.dp, colors.borderSubtle, IshaaraTheme.shapes.sm)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Text(
            text = message,
            style = typography.labelMedium,
            color = colors.foregroundMuted
        )
    }
}

@Composable
private fun EmptyTransitState(
    isOffline: Boolean,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

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
            Text(
                text = if (isOffline) {
                    stringResource(R.string.transit_empty_offline)
                } else {
                    stringResource(R.string.transit_empty_title)
                },
                style = typography.titleMedium,
                color = colors.foreground,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = if (isOffline) {
                    stringResource(R.string.transit_error_subtitle)
                } else {
                    stringResource(R.string.transit_empty_subtitle)
                },
                style = typography.bodyMedium,
                color = colors.foregroundMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.md))

            IshaaraButton(
                text = stringResource(R.string.transit_refresh_button),
                onClick = onRefreshClick,
                variant = IshaaraButtonVariant.Primary,
                size = IshaaraButtonSize.Medium
            )
        }
    }
}
