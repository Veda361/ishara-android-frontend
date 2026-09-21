package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Editorial technical dialog for confirmations and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IshaaraDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    systemTag: String = "CONFIRMATION // ACTION",
    dismissLabel: String? = "CANCEL",
    onDismiss: (() -> Unit)? = onDismissRequest,
    isDanger: Boolean = false
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val shapes = IshaaraTheme.shapes
    val borders = IshaaraTheme.borders

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.sm)
                .background(colors.surfaceElevated)
                .border(borders.thin, colors.border, shapes.sm)
                .padding(IshaaraTheme.spacing.lg)
        ) {
            IshaaraTechnicalLabel(
                text = systemTag,
                color = if (isDanger) colors.danger else colors.foregroundSubtle,
                small = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = typography.titleLarge,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = typography.bodyMedium,
                color = colors.foregroundMuted
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                if (dismissLabel != null && onDismiss != null) {
                    IshaaraButton(
                        text = dismissLabel,
                        onClick = onDismiss,
                        variant = IshaaraButtonVariant.Text,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IshaaraButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    variant = if (isDanger) IshaaraButtonVariant.Danger else IshaaraButtonVariant.Primary,
                    size = IshaaraButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
