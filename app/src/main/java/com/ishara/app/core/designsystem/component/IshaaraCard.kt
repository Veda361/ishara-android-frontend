package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Standard Surface Card for Ishaara.
 * Relies on precise borders and structured background surfaces rather than heavy drop shadows.
 */
@Composable
fun IshaaraCard(
    modifier: Modifier = Modifier,
    containerColor: Color = IshaaraTheme.colors.surfaceElevated,
    borderColor: Color = IshaaraTheme.colors.borderSubtle,
    borderWidth: Dp = IshaaraTheme.borders.thin,
    shape: Shape = IshaaraTheme.shapes.sm,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clickable(role = Role.Button) { onClick() }
            .semantics { role = Role.Button }
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.padding(IshaaraTheme.spacing.md),
            content = content
        )
    }
}

/**
 * Outlined Card primitive for technical data blocks and status panels.
 */
@Composable
fun IshaaraOutlinedCard(
    modifier: Modifier = Modifier,
    borderColor: Color = IshaaraTheme.colors.border,
    shape: Shape = IshaaraTheme.shapes.sm,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    IshaaraCard(
        modifier = modifier,
        containerColor = IshaaraTheme.colors.surface,
        borderColor = borderColor,
        borderWidth = IshaaraTheme.borders.thin,
        shape = shape,
        onClick = onClick,
        content = content
    )
}
