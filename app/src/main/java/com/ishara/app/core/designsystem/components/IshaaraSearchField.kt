package com.ishara.app.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Clean, natural search input for transit routes, stops, and destinations.
 * Example placeholder: "Where are you going?"
 */
@Composable
fun IshaaraSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Where are you going?",
    loading: Boolean = false,
    enabled: Boolean = true,
    onClearClick: (() -> Unit)? = null,
    onSearchAction: (() -> Unit)? = null
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val shapes = IshaaraTheme.shapes

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = typography.bodyLarge.copy(color = colors.foreground),
        placeholder = {
            Text(
                text = placeholder,
                style = typography.bodyLarge,
                color = colors.foregroundSubtle
            )
        },
        leadingIcon = {
            Text(
                text = "🔍",
                style = typography.titleMedium,
                color = colors.foregroundMuted
            )
        },
        trailingIcon = {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.foreground
                )
            } else if (query.isNotEmpty() && onClearClick != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(role = Role.Button) { onClearClick() }
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        style = typography.titleSmall,
                        color = colors.foregroundMuted
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction?.invoke() }),
        shape = shapes.sm,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceElevated,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.foreground,
            unfocusedBorderColor = colors.border,
            cursorColor = colors.foreground
        )
    )
}
