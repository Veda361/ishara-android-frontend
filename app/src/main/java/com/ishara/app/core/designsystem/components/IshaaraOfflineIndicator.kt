package com.ishara.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * Subtle, non-intrusive offline indicator.
 * Informs the user that cached data is being displayed without blocking interactions.
 */
@Composable
fun IshaaraOfflineIndicator(
    modifier: Modifier = Modifier,
    message: String = "You're offline • Showing cached routes"
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val borders = IshaaraTheme.borders

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSubtle)
            .border(width = borders.hairline, color = colors.borderSubtle)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = message
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.warning)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )
        }
    }
}
