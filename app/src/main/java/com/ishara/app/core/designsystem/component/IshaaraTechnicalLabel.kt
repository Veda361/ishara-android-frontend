package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Clean metadata label for secondary information such as "Live", "ETA 6 min", or route badges.
 * Softened from harsh brackets into clean, modern transit tags.
 */
@Composable
fun IshaaraTechnicalLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = IshaaraTheme.colors.foregroundMuted,
    bordered: Boolean = false,
    small: Boolean = false
) {
    val typography = IshaaraTheme.typography
    val borders = IshaaraTheme.borders
    val shapes = IshaaraTheme.shapes
    val style = if (small) typography.labelSmall else typography.labelMedium

    // Clean up text if it had legacy brackets
    val cleanText = text.removePrefix("[").removeSuffix("]").trim()

    val boxModifier = if (bordered) {
        modifier
            .border(
                width = borders.hairline,
                color = color.copy(alpha = 0.4f),
                shape = shapes.xs
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    } else {
        modifier
    }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Text(
            text = cleanText,
            style = style,
            color = color
        )
    }
}

/**
 * Natural, highly legible transit value readout (e.g. "₹20", "8 min away", "12 seats").
 */
@Composable
fun IshaaraTechnicalValue(
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = typography.headlineMedium,
            color = if (highlight) colors.accent else colors.foreground
        )
        if (unit != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = typography.bodySmall,
                color = colors.foregroundMuted,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
