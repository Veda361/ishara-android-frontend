package com.ishara.app.feature.notifications.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraIconButton
import com.ishara.app.core.designsystem.component.IshaaraIconButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.model.NotificationPriority
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.UserRole
import com.ishara.app.feature.notifications.NotificationFilterTab
import com.ishara.app.feature.notifications.NotificationUiState
import com.ishara.app.feature.notifications.NotificationViewModel

@Composable
fun NotificationInboxScreen(
    viewModel: NotificationViewModel,
    userRole: UserRole,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top App Bar
        NotificationTopBar(
            unreadCount = uiState.unreadCount,
            isMarkingAllRead = uiState.isMarkingAllRead,
            onBackClicked = { onNavigateBack?.invoke() ?: viewModel.onNavigateBack() },
            onMarkAllAsReadClicked = { viewModel.onMarkAllAsRead() }
        )

        // Filter Tabs (ALL / UNREAD)
        NotificationFilterTabs(
            selectedTab = uiState.selectedTab,
            unreadCount = uiState.unreadCount,
            onTabSelected = { viewModel.onFilterTabSelected(it) }
        )

        // Category Filter Chips
        NotificationCategoryChips(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.onCategorySelected(it) }
        )

        // Error Banner
        AnimatedVisibility(
            visible = !uiState.errorMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.errorMessage?.let { errorMsg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.dangerSubtle)
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMsg,
                        style = typography.bodySmall,
                        color = colors.danger,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.danger,
                        modifier = Modifier
                            .clickable { viewModel.onDismissError() }
                            .padding(spacing.xxs)
                    )
                }
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                uiState.isSessionExpired -> {
                    SessionExpiredNotificationView(onRetry = { viewModel.onRetry() })
                }

                uiState.isLoading && uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IshaaraLoadingState(message = "Loading notifications...")
                    }
                }

                uiState.isEmpty -> {
                    EmptyNotificationView(
                        filterTab = uiState.selectedTab,
                        selectedCategory = uiState.selectedCategory,
                        onRefresh = { viewModel.onRefresh() }
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
                        }

                        items(
                            items = uiState.items,
                            key = { it.id }
                        ) { notification ->
                            NotificationCardItem(
                                item = notification,
                                isMarkingRead = uiState.markingReadId == notification.id,
                                onClick = { viewModel.onNotificationClicked(notification, userRole) },
                                onMarkRead = { viewModel.onMarkAsRead(notification) }
                            )
                        }

                        if (uiState.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = spacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = colors.accent,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IshaaraButton(
                                            text = "Load More",
                                            onClick = { viewModel.onLoadMore() },
                                            variant = IshaaraButtonVariant.Outlined,
                                            size = IshaaraButtonSize.Small
                                        )
                                    }
                                }
                            }
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
private fun NotificationTopBar(
    unreadCount: Int,
    isMarkingAllRead: Boolean,
    onBackClicked: () -> Unit,
    onMarkAllAsReadClicked: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(
                width = IshaaraTheme.borders.hairline,
                color = colors.borderSubtle
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            IshaaraIconButton(
                onClick = onBackClicked,
                contentDescription = "Back",
                variant = IshaaraIconButtonVariant.Standard
            ) {
                Text(
                    text = "←",
                    style = typography.titleMedium,
                    color = colors.foreground
                )
            }

            Column {
                Text(
                    text = "Notifications",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Text(
                    text = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                    style = typography.labelSmall,
                    color = if (unreadCount > 0) colors.accent else colors.foregroundSubtle
                )
            }
        }

        if (unreadCount > 0) {
            IshaaraButton(
                text = if (isMarkingAllRead) "Marking..." else "Mark all read",
                onClick = onMarkAllAsReadClicked,
                variant = IshaaraButtonVariant.Text,
                size = IshaaraButtonSize.Small,
                enabled = !isMarkingAllRead,
                loading = isMarkingAllRead
            )
        }
    }
}

@Composable
private fun NotificationFilterTabs(
    selectedTab: NotificationFilterTab,
    unreadCount: Int,
    onTabSelected: (NotificationFilterTab) -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        FilterTabButton(
            title = "All",
            count = null,
            isSelected = selectedTab == NotificationFilterTab.ALL,
            onClick = { onTabSelected(NotificationFilterTab.ALL) },
            modifier = Modifier.weight(1f)
        )

        FilterTabButton(
            title = "Unread",
            count = if (unreadCount > 0) unreadCount else null,
            isSelected = selectedTab == NotificationFilterTab.UNREAD,
            onClick = { onTabSelected(NotificationFilterTab.UNREAD) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NotificationCategoryChips(
    selectedCategory: NotificationCategory?,
    onCategorySelected: (NotificationCategory?) -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes
    val scrollState = rememberScrollState()

    val categories = listOf<Pair<String, NotificationCategory?>>(
        "All Categories" to null,
        NotificationCategory.RIDE_UPDATES.displayLabel to NotificationCategory.RIDE_UPDATES,
        NotificationCategory.ACCOUNT.displayLabel to NotificationCategory.ACCOUNT,
        NotificationCategory.SYSTEM.displayLabel to NotificationCategory.SYSTEM
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = spacing.md, vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        categories.forEach { (label, category) ->
            val isSelected = selectedCategory == category
            val bg = if (isSelected) colors.accentSubtle else colors.surfaceElevated
            val textCol = if (isSelected) colors.accent else colors.foregroundMuted
            val borderCol = if (isSelected) colors.accent else colors.borderSubtle

            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(bg)
                    .border(1.dp, borderCol, shapes.pill)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textCol
                )
            }
        }
    }
}

@Composable
private fun FilterTabButton(
    title: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val shapes = IshaaraTheme.shapes

    val bg = if (isSelected) colors.surfaceElevated else colors.background
    val borderCol = if (isSelected) colors.accent else colors.borderSubtle
    val textCol = if (isSelected) colors.foreground else colors.foregroundMuted

    Box(
        modifier = modifier
            .clip(shapes.sm)
            .background(bg)
            .border(1.dp, borderCol, shapes.sm)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textCol
            )
            if (count != null && count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accent)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        style = typography.labelSmall,
                        color = IshaaraPalette.PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    item: NotificationItem,
    isMarkingRead: Boolean,
    onClick: () -> Unit,
    onMarkRead: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing
    val shapes = IshaaraTheme.shapes

    val isUnread = item.status == NotificationStatus.UNREAD
    val cardBg = if (isUnread) colors.surfaceElevated else colors.surface
    val borderCol = if (isUnread) colors.accent.copy(alpha = 0.35f) else colors.borderSubtle

    IshaaraCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "${if (isUnread) "Unread" else "Read"} notification: ${item.title}"
            },
        containerColor = cardBg,
        borderColor = borderCol,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                        )
                    }

                    val badgeVariant = when {
                        item.priority == NotificationPriority.HIGH -> IshaaraBadgeVariant.Danger
                        isUnread -> IshaaraBadgeVariant.Primary
                        else -> IshaaraBadgeVariant.Neutral
                    }
                    IshaaraBadge(
                        text = formatEventTypeLabel(item.type),
                        variant = badgeVariant
                    )

                    IshaaraBadge(
                        text = item.category.displayLabel,
                        variant = IshaaraBadgeVariant.Neutral
                    )
                }

                Text(
                    text = formatFriendlyTimestamp(item.createdAt),
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = item.title,
                style = typography.titleSmall,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                color = colors.foreground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = item.body,
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isUnread) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (isMarkingRead) "Marking..." else "Mark as read",
                        style = typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(enabled = !isMarkingRead) { onMarkRead() }
                            .padding(vertical = spacing.xxs, horizontal = spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationView(
    filterTab: NotificationFilterTab,
    selectedCategory: NotificationCategory?,
    onRefresh: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔔",
                style = typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))

        val titleText = when {
            filterTab == NotificationFilterTab.UNREAD -> "No unread alerts"
            selectedCategory != null -> "No ${selectedCategory.displayLabel} notifications"
            else -> "You're all caught up"
        }

        Text(
            text = titleText,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(spacing.xxs))

        Text(
            text = if (filterTab == NotificationFilterTab.UNREAD)
                "All unread ride and payment updates have been acknowledged."
            else
                "No notifications right now. Trip updates and messages will appear here.",
            style = typography.bodyMedium,
            color = colors.foregroundMuted,
            modifier = Modifier.padding(horizontal = spacing.md)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        IshaaraButton(
            text = "Refresh",
            onClick = onRefresh,
            variant = IshaaraButtonVariant.Outlined,
            size = IshaaraButtonSize.Small
        )
    }
}

@Composable
private fun SessionExpiredNotificationView(
    onRetry: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔒",
            style = typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(spacing.md))

        Text(
            text = "Session Expired",
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.danger
        )

        Spacer(modifier = Modifier.height(spacing.xxs))

        Text(
            text = "Your session has expired. Please sign in again to access notifications.",
            style = typography.bodyMedium,
            color = colors.foregroundMuted,
            modifier = Modifier.padding(horizontal = spacing.md)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        IshaaraButton(
            text = "Retry",
            onClick = onRetry,
            variant = IshaaraButtonVariant.Outlined,
            size = IshaaraButtonSize.Small
        )
    }
}

private fun formatEventTypeLabel(eventType: String): String {
    return when (eventType) {
        "RIDE_REQUEST_CREATED" -> "RIDE REQUEST"
        "RIDE_REQUEST_ACCEPTED" -> "ACCEPTED"
        "RIDE_REQUEST_REJECTED" -> "DECLINED"
        "RIDE_REQUEST_CANCELLED" -> "CANCELLED"
        "RIDE_REQUEST_EXPIRED" -> "EXPIRED"
        "RIDE_DRIVER_ARRIVING" -> "DRIVER ARRIVING"
        "RIDE_PICKED_UP" -> "PICKED UP"
        "RIDE_STARTED" -> "TRIP STARTED"
        "RIDE_COMPLETED" -> "COMPLETED"
        "RIDE_CANCELLED" -> "CANCELLED"
        "PAYMENT_CAPTURED" -> "PAYMENT"
        "REFUND_PROCESSED" -> "REFUND"
        "SETTLEMENT_PROCESSED" -> "PAYOUT"
        else -> eventType.replace("_", " ")
    }
}

private fun formatFriendlyTimestamp(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    return try {
        if (isoTimestamp.contains("T")) {
            val timePart = isoTimestamp.substringAfter("T").substringBefore(".")
            val parts = timePart.split(":")
            if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart
        } else {
            isoTimestamp
        }
    } catch (_: Exception) {
        isoTimestamp
    }
}
