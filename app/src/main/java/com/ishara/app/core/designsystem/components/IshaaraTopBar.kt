package com.ishara.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Clean, human-centered top app bar for Ishaara.
 */
@Composable
fun IshaaraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    systemTag: String? = null, // Backward compatible optional meta tag
    onBackClick: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val borders = IshaaraTheme.borders

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .border(
                width = borders.hairline,
                color = colors.borderSubtle
            )
            .padding(horizontal = IshaaraTheme.spacing.md, vertical = IshaaraTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(role = Role.Button) { onBackClick() }
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        style = typography.titleLarge,
                        color = colors.foreground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.titleLarge,
                    color = colors.foreground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                } else if (systemTag != null) {
                    Text(
                        text = systemTag,
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            if (actions != null) {
                actions()
            }
        }
    }
}

/**
 * Natural Section Header (e.g. "Available trips", "Upcoming stops", "Recent rides").
 * Clean typography and hierarchy without confusing technical prefixes.
 */
@Composable
fun IshaaraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    number: String? = null, // Optional badge/count, e.g. "4 available"
    action: (@Composable () -> Unit)? = null
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = IshaaraTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = typography.headlineSmall,
            color = colors.foreground
        )
        if (number != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IshaaraBadge(text = number)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (action != null) {
            action()
        }
    }
}
