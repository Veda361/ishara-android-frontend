package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Human-friendly Transit Statuses:
 * Natural language states that students and drivers understand immediately.
 */
enum class IshaaraTransitStatus(val displayLabel: String) {
    ONLINE("Online"),
    OFFLINE("Offline"),
    AVAILABLE("Available"),
    COMING("Coming"),
    ARRIVING("Arriving"),
    ARRIVED("Driver arrived"),
    BOARDING("Boarding"),
    IN_PROGRESS("Trip started"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    PENDING("Waiting for driver"),
    ACCEPTED("Confirmed"),
    REJECTED("Declined"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    val label: String get() = displayLabel
}

/**
 * Clean Status Chip displaying a color indicator dot and natural language label.
 * Fully accessible — color is always paired with clear text.
 */
@Composable
fun IshaaraStatusChip(
    status: IshaaraTransitStatus,
    modifier: Modifier = Modifier,
    overrideLabel: String? = null
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val shapes = IshaaraTheme.shapes
    val borders = IshaaraTheme.borders

    val label = overrideLabel ?: status.displayLabel

    val (indicatorColor, backgroundColor, textColor) = when (status) {
        IshaaraTransitStatus.ONLINE,
        IshaaraTransitStatus.AVAILABLE,
        IshaaraTransitStatus.ACTIVE,
        IshaaraTransitStatus.ACCEPTED,
        IshaaraTransitStatus.COMPLETED -> Triple(colors.success, colors.successSubtle, colors.foreground)

        IshaaraTransitStatus.COMING,
        IshaaraTransitStatus.ARRIVING,
        IshaaraTransitStatus.ARRIVED,
        IshaaraTransitStatus.BOARDING,
        IshaaraTransitStatus.IN_PROGRESS,
        IshaaraTransitStatus.PENDING -> Triple(colors.accent, colors.accentSubtle, colors.foreground)

        IshaaraTransitStatus.REJECTED,
        IshaaraTransitStatus.CANCELLED,
        IshaaraTransitStatus.EXPIRED -> Triple(colors.danger, colors.dangerSubtle, colors.danger)

        IshaaraTransitStatus.OFFLINE,
        IshaaraTransitStatus.INACTIVE -> Triple(colors.foregroundSubtle, colors.surfaceSubtle, colors.foregroundMuted)
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Status: $label"
            }
            .clip(shapes.xs)
            .background(backgroundColor)
            .border(width = borders.hairline, color = indicatorColor.copy(alpha = 0.35f), shape = shapes.xs)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = typography.labelSmall,
                color = textColor
            )
        }
    }
}

/**
 * Backward-compatible alias for IshaaraStatusChip.
 */
@Composable
fun IshaaraStatusBadge(
    status: IshaaraTransitStatus,
    modifier: Modifier = Modifier
) {
    IshaaraStatusChip(status = status, modifier = modifier)
}

/**
 * Clean metadata badge (e.g., "Bus 24", "12 seats", "Express").
 */
@Composable
fun IshaaraBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = IshaaraTheme.colors.surfaceSubtle,
    contentColor: Color = IshaaraTheme.colors.foreground
) {
    val typography = IshaaraTheme.typography
    val shapes = IshaaraTheme.shapes
    val borders = IshaaraTheme.borders

    Box(
        modifier = modifier
            .clip(shapes.xs)
            .background(containerColor)
            .border(width = borders.hairline, color = IshaaraTheme.colors.borderSubtle, shape = shapes.xs)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = typography.labelSmall,
            color = contentColor
        )
    }
}
