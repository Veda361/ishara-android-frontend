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

enum class IshaaraTransitStatus(val label: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    PENDING("PENDING"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED"),
    ARRIVING("ARRIVING"),
    IN_PROGRESS("IN PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED")
}

/**
 * Technical Status Badge displaying state with both text and semantic indicator.
 * Complies with accessibility guidelines by never communicating status solely via color.
 */
@Composable
fun IshaaraStatusBadge(
    status: IshaaraTransitStatus,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val shapes = IshaaraTheme.shapes
    val borders = IshaaraTheme.borders

    val (indicatorColor, backgroundColor, textColor) = when (status) {
        IshaaraTransitStatus.ONLINE,
        IshaaraTransitStatus.ACTIVE,
        IshaaraTransitStatus.ACCEPTED,
        IshaaraTransitStatus.COMPLETED -> Triple(colors.success, colors.successSubtle, colors.success)

        IshaaraTransitStatus.ARRIVING,
        IshaaraTransitStatus.IN_PROGRESS,
        IshaaraTransitStatus.PENDING -> Triple(colors.accent, colors.accentSubtle, colors.warning)

        IshaaraTransitStatus.REJECTED,
        IshaaraTransitStatus.CANCELLED,
        IshaaraTransitStatus.EXPIRED -> Triple(colors.danger, colors.dangerSubtle, colors.danger)

        IshaaraTransitStatus.OFFLINE,
        IshaaraTransitStatus.INACTIVE -> Triple(colors.foregroundSubtle, colors.surfaceSubtle, colors.foregroundMuted)
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Status: ${status.label}"
            }
            .clip(shapes.xs)
            .background(backgroundColor)
            .border(width = borders.hairline, color = indicatorColor.copy(alpha = 0.4f), shape = shapes.xs)
            .padding(horizontal = 8.dp, vertical = 3.dp),
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
                text = status.label,
                style = typography.technicalSmall,
                color = textColor
            )
        }
    }
}

/**
 * Generic minimal technical badge (e.g. "ROUTE 24A", "SEATS: 12", "AC BUS").
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
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = typography.labelSmall,
            color = contentColor
        )
    }
}
