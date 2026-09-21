package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Reusable visual error state with recovery action (Retry, Settings, etc.).
 */
@Composable
fun IshaaraErrorState(
    title: String = "SIGNAL INTERRUPTED",
    message: String = "Unable to reach transit servers. Please check your network connection.",
    modifier: Modifier = Modifier,
    retryLabel: String = "RETRY CONNECTION",
    onRetryClick: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(IshaaraTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IshaaraTechnicalLabel(
                text = "ERROR // PROTOCOL EXCEPTION",
                color = colors.danger,
                small = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = typography.headlineSmall,
                color = colors.foreground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = typography.bodyMedium,
                color = colors.foregroundMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            IshaaraButton(
                text = retryLabel,
                onClick = onRetryClick,
                variant = IshaaraButtonVariant.Outlined,
                size = IshaaraButtonSize.Medium
            )
        }
    }
}
