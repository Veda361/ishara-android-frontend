package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

enum class IshaaraButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Text,
    Danger
}

enum class IshaaraButtonSize(val minHeight: Dp, val horizontalPadding: Dp, val iconSize: Dp) {
    Small(minHeight = 36.dp, horizontalPadding = 12.dp, iconSize = 16.dp),
    Medium(minHeight = 48.dp, horizontalPadding = 16.dp, iconSize = 20.dp),
    Large(minHeight = 56.dp, horizontalPadding = 24.dp, iconSize = 24.dp)
}

/**
 * Standard Production Button for Ishaara.
 * Supports Primary, Secondary, Outlined, Text, and Danger variants with loading states.
 */
@Composable
fun IshaaraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IshaaraButtonVariant = IshaaraButtonVariant.Primary,
    size: IshaaraButtonSize = IshaaraButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = IshaaraTheme.colors
    val shapes = IshaaraTheme.shapes
    val typography = IshaaraTheme.typography

    val textStyle: TextStyle = when (size) {
        IshaaraButtonSize.Small -> typography.labelMedium
        IshaaraButtonSize.Medium -> typography.labelLarge
        IshaaraButtonSize.Large -> typography.titleMedium
    }

    val contentPadding = PaddingValues(
        horizontal = size.horizontalPadding,
        vertical = 0.dp
    )

    val buttonModifier = modifier
        .defaultMinSize(minHeight = size.minHeight)
        .semantics { role = Role.Button }

    when (variant) {
        IshaaraButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shapes.sm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.foreground,
                    contentColor = colors.background,
                    disabledContainerColor = colors.surfaceSubtle,
                    disabledContentColor = colors.foregroundSubtle
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    textStyle = textStyle,
                    textColor = colors.background,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    iconSize = size.iconSize
                )
            }
        }
        IshaaraButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shapes.sm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceSubtle,
                    contentColor = colors.foreground,
                    disabledContainerColor = colors.surfaceSubtle,
                    disabledContentColor = colors.foregroundSubtle
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    textStyle = textStyle,
                    textColor = colors.foreground,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    iconSize = size.iconSize
                )
            }
        }
        IshaaraButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shapes.sm,
                border = BorderStroke(
                    width = IshaaraTheme.borders.thin,
                    color = if (enabled && !loading) colors.borderStrong else colors.borderSubtle
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.foreground,
                    disabledContentColor = colors.foregroundSubtle
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    textStyle = textStyle,
                    textColor = if (enabled && !loading) colors.foreground else colors.foregroundSubtle,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    iconSize = size.iconSize
                )
            }
        }
        IshaaraButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shapes.sm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.foreground,
                    disabledContentColor = colors.foregroundSubtle
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    textStyle = textStyle,
                    textColor = if (enabled && !loading) colors.foreground else colors.foregroundSubtle,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    iconSize = size.iconSize
                )
            }
        }
        IshaaraButtonVariant.Danger -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shapes.sm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.danger,
                    contentColor = colors.onDanger,
                    disabledContainerColor = colors.dangerSubtle,
                    disabledContentColor = colors.foregroundSubtle
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(
                    text = text,
                    textStyle = textStyle,
                    textColor = colors.onDanger,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    iconSize = size.iconSize
                )
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    textStyle: TextStyle,
    textColor: Color,
    loading: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    iconSize: Dp
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(iconSize),
            strokeWidth = 2.dp,
            color = textColor
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = textStyle,
                color = textColor
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    trailingIcon()
                }
            }
        }
    }
}
