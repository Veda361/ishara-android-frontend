package com.ishara.app.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Startup splash screen displayed while resolving persisted authentication status.
 * Eliminates navigation flicker.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = spacing.xl)
        ) {
            Text(
                text = "Ishaara",
                style = typography.displayLarge,
                color = colors.foreground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = "Connecting transit routes...",
                style = typography.bodyMedium,
                color = colors.foregroundMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.xxl))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = colors.accent,
                strokeWidth = 3.dp
            )
        }
    }
}
