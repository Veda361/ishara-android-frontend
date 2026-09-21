package com.ishara.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Ishaara Design System Theme Root.
 * Provides custom Ishaara tokens and sets up Material 3 theme compatibility.
 */
@Composable
fun IshaaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val ishaaraColors = if (darkTheme) darkIshaaraColors() else lightIshaaraColors()
    val ishaaraTypography = IshaaraTypography()
    val ishaaraSpacing = IshaaraSpacing()
    val ishaaraShapes = IshaaraShapes()
    val ishaaraBorders = IshaaraBorders()
    val ishaaraElevation = IshaaraElevation()
    val ishaaraMotion = IshaaraMotion(isReducedMotion = reducedMotion)

    // Map Ishaara tokens into Material 3 ColorScheme for seamless ecosystem interop
    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = ishaaraColors.foreground,
            onPrimary = ishaaraColors.background,
            secondary = ishaaraColors.accent,
            onSecondary = ishaaraColors.accentForeground,
            background = ishaaraColors.background,
            onBackground = ishaaraColors.foreground,
            surface = ishaaraColors.surface,
            onSurface = ishaaraColors.foreground,
            surfaceVariant = ishaaraColors.surfaceSubtle,
            onSurfaceVariant = ishaaraColors.foregroundMuted,
            outline = ishaaraColors.border,
            error = ishaaraColors.danger,
            onError = ishaaraColors.onDanger
        )
    } else {
        lightColorScheme(
            primary = ishaaraColors.foreground,
            onPrimary = ishaaraColors.background,
            secondary = ishaaraColors.accent,
            onSecondary = ishaaraColors.accentForeground,
            background = ishaaraColors.background,
            onBackground = ishaaraColors.foreground,
            surface = ishaaraColors.surface,
            onSurface = ishaaraColors.foreground,
            surfaceVariant = ishaaraColors.surfaceSubtle,
            onSurfaceVariant = ishaaraColors.foregroundMuted,
            outline = ishaaraColors.border,
            error = ishaaraColors.danger,
            onError = ishaaraColors.onDanger
        )
    }

    CompositionLocalProvider(
        LocalIshaaraColors provides ishaaraColors,
        LocalIshaaraTypography provides ishaaraTypography,
        LocalIshaaraSpacing provides ishaaraSpacing,
        LocalIshaaraShapes provides ishaaraShapes,
        LocalIshaaraBorders provides ishaaraBorders,
        LocalIshaaraElevation provides ishaaraElevation,
        LocalIshaaraMotion provides ishaaraMotion
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}

/**
 * Direct static accessor for Ishaara design tokens.
 * Usage:
 *   IshaaraTheme.colors.background
 *   IshaaraTheme.typography.technical
 *   IshaaraTheme.spacing.md
 */
object IshaaraTheme {
    val colors: IshaaraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraColors.current

    val typography: IshaaraTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraTypography.current

    val spacing: IshaaraSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraSpacing.current

    val shapes: IshaaraShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraShapes.current

    val borders: IshaaraBorders
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraBorders.current

    val elevation: IshaaraElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraElevation.current

    val motion: IshaaraMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalIshaaraMotion.current
}
