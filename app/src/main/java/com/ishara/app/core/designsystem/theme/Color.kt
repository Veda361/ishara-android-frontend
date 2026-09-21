package com.ishara.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Ishaara Color Palette
 *
 * Visual Direction:
 * - High-contrast Light & Dark modes
 * - Minimalist monochrome foundation (White, Jet Charcoal, Slate Grays)
 * - Restrained transit accent (Amber / Marigold: #F59E0B)
 * - Semantic status colors (Emerald, Amber, Crimson, Cobalt)
 */
object IshaaraPalette {
    // Pure Monochrome
    val PureWhite = Color(0xFFFFFFFF)
    val PureBlack = Color(0xFF000000)

    // Light Neutrals
    val Neutral50 = Color(0xFFF9FAFB)
    val Neutral100 = Color(0xFFF3F4F6)
    val Neutral200 = Color(0xFFE5E7EB)
    val Neutral300 = Color(0xFFD1D5DB)
    val Neutral400 = Color(0xFF9CA3AF)
    val Neutral500 = Color(0xFF6B7280)
    val Neutral600 = Color(0xFF4B5563)
    val Neutral700 = Color(0xFF374151)
    val Neutral800 = Color(0xFF1F2937)
    val Neutral900 = Color(0xFF111827)
    val Neutral950 = Color(0xFF0B0F17)

    // Dark Neutrals
    val DarkBackground = Color(0xFF0D0F12)
    val DarkSurface = Color(0xFF14171D)
    val DarkSurfaceElevated = Color(0xFF1A1E26)
    val DarkSurfaceSubtle = Color(0xFF101318)
    val DarkBorder = Color(0xFF262C38)
    val DarkBorderSubtle = Color(0xFF1C212B)

    // Transit Accent — Amber / Electric Marigold
    val Amber500 = Color(0xFFF59E0B)
    val Amber600 = Color(0xFFD97706)
    val Amber400 = Color(0xFFFBBF24)
    val Amber100 = Color(0xFFFEF3C7)
    val Amber900 = Color(0xFF78350F)

    // Semantic Status Colors
    val Emerald500 = Color(0xFF10B981) // Online / Active / Success
    val Emerald100 = Color(0xFFD1FAE5)
    val Emerald900 = Color(0xFF064E3B)

    val Crimson500 = Color(0xFFEF4444) // Error / Cancelled / SOS
    val Crimson100 = Color(0xFFFEE2E2)
    val Crimson900 = Color(0xFF7F1D1D)

    val Cobalt500 = Color(0xFF3B82F6) // Information / Routing
    val Cobalt100 = Color(0xFFDBEAFE)
    val Cobalt900 = Color(0xFF1E3A8A)
}

@Immutable
data class IshaaraColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val foreground: Color,
    val foregroundMuted: Color,
    val foregroundSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentForeground: Color,
    val accentSubtle: Color,
    val success: Color,
    val onSuccess: Color,
    val successSubtle: Color,
    val warning: Color,
    val onWarning: Color,
    val warningSubtle: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerSubtle: Color,
    val info: Color,
    val onInfo: Color,
    val infoSubtle: Color
)

fun lightIshaaraColors(): IshaaraColors = IshaaraColors(
    isDark = false,
    background = IshaaraPalette.PureWhite,
    surface = IshaaraPalette.Neutral50,
    surfaceElevated = IshaaraPalette.PureWhite,
    surfaceSubtle = IshaaraPalette.Neutral100,
    foreground = IshaaraPalette.Neutral900,
    foregroundMuted = IshaaraPalette.Neutral600,
    foregroundSubtle = IshaaraPalette.Neutral400,
    border = IshaaraPalette.Neutral300,
    borderSubtle = IshaaraPalette.Neutral200,
    borderStrong = IshaaraPalette.Neutral900,
    accent = IshaaraPalette.Amber500,
    accentForeground = IshaaraPalette.PureBlack,
    accentSubtle = IshaaraPalette.Amber100,
    success = IshaaraPalette.Emerald500,
    onSuccess = IshaaraPalette.PureWhite,
    successSubtle = IshaaraPalette.Emerald100,
    warning = IshaaraPalette.Amber600,
    onWarning = IshaaraPalette.PureWhite,
    warningSubtle = IshaaraPalette.Amber100,
    danger = IshaaraPalette.Crimson500,
    onDanger = IshaaraPalette.PureWhite,
    dangerSubtle = IshaaraPalette.Crimson100,
    info = IshaaraPalette.Cobalt500,
    onInfo = IshaaraPalette.PureWhite,
    infoSubtle = IshaaraPalette.Cobalt100
)

fun darkIshaaraColors(): IshaaraColors = IshaaraColors(
    isDark = true,
    background = IshaaraPalette.DarkBackground,
    surface = IshaaraPalette.DarkSurface,
    surfaceElevated = IshaaraPalette.DarkSurfaceElevated,
    surfaceSubtle = IshaaraPalette.DarkSurfaceSubtle,
    foreground = IshaaraPalette.Neutral50,
    foregroundMuted = IshaaraPalette.Neutral400,
    foregroundSubtle = IshaaraPalette.Neutral500,
    border = IshaaraPalette.DarkBorder,
    borderSubtle = IshaaraPalette.DarkBorderSubtle,
    borderStrong = IshaaraPalette.Neutral100,
    accent = IshaaraPalette.Amber400,
    accentForeground = IshaaraPalette.PureBlack,
    accentSubtle = IshaaraPalette.Amber900,
    success = IshaaraPalette.Emerald500,
    onSuccess = IshaaraPalette.PureBlack,
    successSubtle = IshaaraPalette.Emerald900,
    warning = IshaaraPalette.Amber400,
    onWarning = IshaaraPalette.PureBlack,
    warningSubtle = IshaaraPalette.Amber900,
    danger = IshaaraPalette.Crimson500,
    onDanger = IshaaraPalette.PureWhite,
    dangerSubtle = IshaaraPalette.Crimson900,
    info = IshaaraPalette.Cobalt500,
    onInfo = IshaaraPalette.PureBlack,
    infoSubtle = IshaaraPalette.Cobalt900
)

val LocalIshaaraColors = staticCompositionLocalOf { lightIshaaraColors() }
