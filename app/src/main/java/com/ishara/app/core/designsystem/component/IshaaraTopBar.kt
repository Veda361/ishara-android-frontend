package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Editorial Technical Top Bar for Ishaara.
 */
@Composable
fun IshaaraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    systemTag: String? = null,
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
        if (systemTag != null) {
            IshaaraTechnicalLabel(
                text = systemTag,
                color = colors.foregroundSubtle,
                small = true,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = title,
                style = typography.titleLarge,
                color = colors.foreground,
                modifier = Modifier.weight(1f)
            )

            if (actions != null) {
                actions()
            }
        }
    }
}

/**
 * Technical Section Header (e.g. "01 // ACTIVE TRIPS", "02 // RECENT BOARDINGS").
 */
@Composable
fun IshaaraSectionHeader(
    number: String,
    title: String,
    modifier: Modifier = Modifier,
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
            text = "$number //",
            style = typography.technicalSmall,
            color = colors.accent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title.uppercase(),
            style = typography.labelLarge,
            color = colors.foreground
        )
        Spacer(modifier = Modifier.weight(1f))
        if (action != null) {
            action()
        }
    }
}
