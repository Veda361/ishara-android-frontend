package com.ishara.app.feature.student.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraEmptyState
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraSkeleton
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.feature.student.discovery.components.DiscoveryHeader
import com.ishara.app.feature.student.discovery.components.TripDetailsSheet
import com.ishara.app.feature.student.discovery.components.TripResultCard

/**
 * Production Trip Discovery screen for Student / Passenger users.
 *
 * Provides immediate answers to:
 * 1. WHERE FROM & WHERE TO (DiscoveryHeader)
 * 2. WHAT TRANSIT OPTIONS ARE ACTIVE (Verified TripResultCards)
 * 3. HOW FAR & HOW LONG (Factual travel duration & distance)
 * 4. WHAT TO DO NEXT (Trip selection -> Phase 07 navigation boundary)
 */
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    modifier: Modifier = Modifier,
    onNavigateToRideRequest: ((com.ishara.app.domain.model.DiscoveredTrip) -> Unit)? = null
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
                title = "Available Trips",
                onBackClick = { viewModel.onChangeSearchClicked() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Origin & Destination Header
            DiscoveryHeader(
                query = uiState.query,
                onChangeClick = { viewModel.onChangeSearchClicked() },
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
            )

            // Stage rendering
            when (val stage = uiState.stage) {
                is DiscoveryStage.Idle -> {
                    IshaaraEmptyState(
                        title = "Discover Trips",
                        message = "Enter your pickup location and destination to search for active transit trips running along your corridor.",
                        actionLabel = "Search Route",
                        onActionClick = { viewModel.onChangeSearchClicked() }
                    )
                }

                is DiscoveryStage.Loading -> {
                    DiscoveryLoadingContent(modifier = Modifier.fillMaxSize())
                }

                is DiscoveryStage.ValidationError -> {
                    IshaaraErrorState(
                        title = "Invalid Search Route",
                        message = stage.message,
                        retryLabel = "Change Route",
                        onRetryClick = { viewModel.onChangeSearchClicked() }
                    )
                }

                is DiscoveryStage.Empty -> {
                    IshaaraEmptyState(
                        title = "No trips found",
                        message = "No active transport is currently running along this route. Try changing your destination or pickup point.",
                        actionLabel = "Change destination",
                        onActionClick = { viewModel.onChangeSearchClicked() }
                    )
                }

                is DiscoveryStage.Error -> {
                    IshaaraErrorState(
                        title = "Unable to search trips",
                        message = uiState.errorMessage ?: "Please check your connection and try again.",
                        retryLabel = "Try again",
                        onRetryClick = { viewModel.retry() }
                    )
                }

                is DiscoveryStage.Success,
                is DiscoveryStage.LoadingMore -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "${uiState.trips.size} active option(s) found",
                            style = typography.labelMedium,
                            color = colors.foregroundMuted,
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = spacing.md,
                                end = spacing.md,
                                bottom = spacing.xl
                            ),
                            verticalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            items(
                                items = uiState.trips,
                                key = { it.tripId }
                            ) { trip ->
                                TripResultCard(
                                    trip = trip,
                                    isSelected = uiState.selectedTrip?.tripId == trip.tripId,
                                    onSelectClick = { viewModel.onTripSelected(trip) }
                                )
                            }

                            if (uiState.hasMore) {
                                item {
                                    if (stage is DiscoveryStage.LoadingMore) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = spacing.md),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                    } else {
                                        com.ishara.app.core.designsystem.component.IshaaraButton(
                                            text = "Load More Trips",
                                            onClick = { viewModel.loadNextPage() },
                                            variant = com.ishara.app.core.designsystem.component.IshaaraButtonVariant.Outlined,
                                            size = com.ishara.app.core.designsystem.component.IshaaraButtonSize.Medium,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    // Modal Sheet showing full trip details and continuing to Phase 07 boundary
    val selectedTrip = uiState.selectedTrip
    if (uiState.isDetailsSheetVisible && selectedTrip != null) {
        TripDetailsSheet(
            trip = selectedTrip,
            onDismissRequest = { viewModel.onDismissTripDetails() },
            onContinueClick = {
                viewModel.onDismissTripDetails()
                if (onNavigateToRideRequest != null) {
                    onNavigateToRideRequest(selectedTrip)
                } else {
                    viewModel.onContinueToRideRequest()
                }
            }
        )
    }
}

@Composable
private fun DiscoveryLoadingContent(modifier: Modifier = Modifier) {
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        repeat(3) {
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    IshaaraSkeleton(height = 20.dp, modifier = Modifier.fillMaxWidth(0.5f))
                    IshaaraSkeleton(height = 16.dp, modifier = Modifier.fillMaxWidth(0.8f))
                    Spacer(modifier = Modifier.height(spacing.xs))
                    IshaaraSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth(0.4f))
                    IshaaraSkeleton(height = 44.dp, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
