package com.ishara.app.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Safe recovery screen shown when an unrecognized or corrupted role is received.
 * Prevents application crashes and silent incorrect defaulting while providing clear user recovery.
 */
@Composable
fun SafeRecoveryScreen(
    message: String = "We couldn't determine your account setup.",
    onRetryClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.xl))

            // Message Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = spacing.md)
            ) {
                Text(
                    text = "⚠️",
                    style = typography.displayLarge
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Text(
                    text = "Account Setup Needed",
                    style = typography.headlineLarge,
                    color = colors.foreground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = message,
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                IshaaraPrimaryAction(
                    text = "Try again",
                    onClick = onRetryClick
                )

                IshaaraButton(
                    text = "Sign out",
                    onClick = onSignOutClick,
                    variant = IshaaraButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
