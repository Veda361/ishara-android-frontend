package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Technical Avatar displaying user or driver initials with optional live status indicator.
 */
@Composable
fun IshaaraAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    status: IshaaraTransitStatus? = null
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val borders = IshaaraTheme.borders

    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "U" }
        .uppercase()

    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = "Avatar for $name"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.surfaceSubtle)
                .border(borders.thin, colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = typography.labelLarge,
                color = colors.foreground
            )
        }

        if (status != null) {
            val statusColor = when (status) {
                IshaaraTransitStatus.ONLINE, IshaaraTransitStatus.ACTIVE -> colors.success
                IshaaraTransitStatus.ARRIVING, IshaaraTransitStatus.IN_PROGRESS -> colors.accent
                IshaaraTransitStatus.OFFLINE -> colors.foregroundSubtle
                else -> colors.danger
            }

            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(colors.background)
                    .padding(1.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }
    }
}
