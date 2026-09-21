package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

enum class IshaaraIconButtonVariant {
    Standard,
    Outlined,
    Filled
}

/**
 * Accessible minimal icon button with mandatory 48dp minimum touch target.
 */
@Composable
fun IshaaraIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    variant: IshaaraIconButtonVariant = IshaaraIconButtonVariant.Standard,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    val colors = IshaaraTheme.colors
    val shapes = IshaaraTheme.shapes
    val borders = IshaaraTheme.borders

    val backgroundColor = when (variant) {
        IshaaraIconButtonVariant.Standard -> Color.Transparent
        IshaaraIconButtonVariant.Outlined -> colors.surface
        IshaaraIconButtonVariant.Filled -> colors.surfaceSubtle
    }

    val borderModifier = if (variant == IshaaraIconButtonVariant.Outlined) {
        Modifier.border(borders.thin, colors.border, shapes.xs)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp) // WCAG 2.1 touch target
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shapes.xs)
                .then(borderModifier)
                .background(backgroundColor)
                .clickable(enabled = enabled, role = Role.Button) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
