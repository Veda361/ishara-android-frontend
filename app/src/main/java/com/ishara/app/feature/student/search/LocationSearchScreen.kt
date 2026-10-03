package com.ishara.app.feature.student.search

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraEmptyState
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraIconButton
import com.ishara.app.core.designsystem.component.IshaaraIconButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraSearchField
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination

/**
 * Destination Search Screen.
 * Enables students to search campus stops, landmarks, and city locations.
 * Adheres strictly to Phase 02 and Phase 05 design guidelines:
 * - Clean white/light surfaces
 * - Immediate keyboard-friendly autofocus
 * - Inline debounce with cancellation
 * - Never shows raw coordinates, internal IDs, or database flags
 */
@Composable
fun LocationSearchScreen(
    viewModel: LocationSearchViewModel,
    onDestinationSelected: (StudentDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LocationSearchContent(
        uiState = uiState,
        onQueryChange = { viewModel.onQueryChange(it) },
        onClearClick = { viewModel.onClearQuery() },
        onRetryClick = { viewModel.onRetry() },
        onBackClick = { viewModel.onBackClick() },
        onLocationClick = { viewModel.onLocationSelected(it, onDestinationSelected) },
        onRecentClick = { viewModel.onRecentSelected(it, onDestinationSelected) },
        modifier = modifier
    )
}

@Composable
fun LocationSearchContent(
    uiState: LocationSearchUiState,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    onLocationClick: (SearchResultLocation) -> Unit,
    onRecentClick: (StudentDestination) -> Unit,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.md)
        ) {
            Spacer(modifier = Modifier.height(spacing.sm))

            // Search Header: Back Button + Search Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IshaaraIconButton(
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.close_search),
                    variant = IshaaraIconButtonVariant.Standard
                ) {
                    Text(
                        text = "←",
                        style = typography.headlineMedium,
                        color = colors.foreground
                    )
                }

                Spacer(modifier = Modifier.width(spacing.xs))

                IshaaraSearchField(
                    query = uiState.query,
                    onQueryChange = onQueryChange,
                    placeholder = stringResource(R.string.search_destination_placeholder),
                    loading = uiState.isLoading,
                    onClearClick = onClearClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Content Area: Results, Empty State, Error State, or Recents
            when {
                uiState.errorMessage != null -> {
                    IshaaraErrorState(
                        title = stringResource(R.string.search_error_title),
                        message = uiState.errorMessage,
                        retryLabel = stringResource(R.string.retry_button),
                        onRetryClick = onRetryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xl)
                    )
                }

                uiState.isEmptyResult -> {
                    IshaaraEmptyState(
                        title = stringResource(R.string.search_empty_title),
                        message = stringResource(R.string.search_empty_subtitle),
                        actionLabel = stringResource(R.string.search_again_button),
                        onActionClick = onClearClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xl)
                    )
                }

                uiState.results.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        items(uiState.results, key = { it.id }) { location ->
                            SearchResultItem(
                                location = location,
                                onClick = { onLocationClick(location) }
                            )
                        }
                    }
                }

                else -> {
                    // Empty or query < 2 chars: Show Recent Searches if available
                    if (uiState.recentDestinations.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.recent_destinations_title),
                                    style = typography.titleSmall,
                                    color = colors.foregroundMuted,
                                    modifier = Modifier.padding(vertical = spacing.xs)
                                )
                            }

                            items(uiState.recentDestinations) { recent ->
                                RecentSearchItem(
                                    destination = recent,
                                    onClick = { onRecentClick(recent) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    location: SearchResultLocation,
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
            .background(colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md)
            .semantics {
                role = Role.Button
                contentDescription = "${location.name}. ${location.formattedAddress}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.surfaceSubtle, shape = shapes.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📍", style = typography.bodyMedium)
        }

        Spacer(modifier = Modifier.width(spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name,
                style = typography.titleMedium,
                color = colors.foreground
            )
            if (location.formattedAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = location.formattedAddress,
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }
    }
}

@Composable
private fun RecentSearchItem(
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
        Text(text = "🕒", style = typography.bodyMedium)
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
