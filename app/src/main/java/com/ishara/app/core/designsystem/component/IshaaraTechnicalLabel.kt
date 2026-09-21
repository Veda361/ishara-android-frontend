package com.ishara.app.core.designsystem.component

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Signature Ishaara Technical Tag:
 * Renders bracketed technical instrument readouts such as:
 * [ SYSTEM LIVE ]
 * [ 01 ]
 * [ ROUTE STATUS ]
 * [ DRIVER STATUS ]
 * [ ETA 06 MIN ]
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
    val style = if (small) typography.technicalSmall else typography.technical

    val formattedText = if (text.startsWith("[") && text.endsWith("]")) {
        text.uppercase()
    } else {
        "[ ${text.trim().uppercase()} ]"
    }

    val boxModifier = if (bordered) {
        modifier
            .border(
                width = borders.hairline,
                color = color.copy(alpha = 0.4f),
                shape = shapes.xs
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    } else {
        modifier
    }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Text(
            text = formattedText,
            style = style,
            color = color
        )
    }
}

/**
 * High-legibility technical measurement readout (e.g. "₹25", "14 MIN", "32 SEATS").
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
            style = typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            ),
            color = if (highlight) colors.accent else colors.foreground
        )
        if (unit != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit.uppercase(),
                style = typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.foregroundMuted,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
