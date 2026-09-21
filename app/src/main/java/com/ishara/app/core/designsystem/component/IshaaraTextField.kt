package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Clean, accessible text input with natural sentence-case labels and clear feedback.
 */
@Composable
fun IshaaraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = IshaaraTheme.colors
    val shapes = IshaaraTheme.shapes
    val typography = IshaaraTheme.typography
    val isError = !errorText.isNullOrBlank()

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = typography.titleSmall,
                color = if (isError) colors.danger else colors.foreground
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = typography.bodyLarge.copy(color = colors.foreground),
            placeholder = if (placeholder != null) {
                { Text(text = placeholder, style = typography.bodyLarge, color = colors.foregroundSubtle) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = shapes.xs,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceElevated,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.surfaceSubtle,
                errorContainerColor = colors.surface,
                focusedBorderColor = colors.foreground,
                unfocusedBorderColor = colors.border,
                disabledBorderColor = colors.borderSubtle,
                errorBorderColor = colors.danger,
                cursorColor = colors.foreground
            )
        )

        val captionText = errorText ?: helperText
        if (captionText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = captionText,
                style = typography.bodySmall,
                color = if (isError) colors.danger else colors.foregroundMuted
            )
        }
    }
}
