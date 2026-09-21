package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Minimal accessible inline status indicator with pulsating/solid dot and text label.
 */
@Composable
fun IshaaraStatusIndicator(
    status: IshaaraTransitStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    val dotColor = when (status) {
        IshaaraTransitStatus.ONLINE,
        IshaaraTransitStatus.ACTIVE,
        IshaaraTransitStatus.ACCEPTED,
        IshaaraTransitStatus.COMPLETED -> colors.success

        IshaaraTransitStatus.ARRIVING,
        IshaaraTransitStatus.IN_PROGRESS,
        IshaaraTransitStatus.PENDING -> colors.accent

        IshaaraTransitStatus.REJECTED,
        IshaaraTransitStatus.CANCELLED,
        IshaaraTransitStatus.EXPIRED -> colors.danger

        IshaaraTransitStatus.OFFLINE,
        IshaaraTransitStatus.INACTIVE -> colors.foregroundSubtle
    }

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Status: ${status.label}"
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.label,
                style = typography.technicalSmall,
                color = colors.foregroundMuted
            )
        }
    }
}
