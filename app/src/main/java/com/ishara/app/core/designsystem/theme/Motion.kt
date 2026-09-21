package com.ishara.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class IshaaraMotion(
    val durationFast: Int = 150,
    val durationNormal: Int = 300,
    val durationSlow: Int = 500,
    val standardEasing: Easing = FastOutSlowInEasing,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val exitEasing: Easing = LinearOutSlowInEasing,
    val isReducedMotion: Boolean = false
) {
    /**
     * Resolves animation duration, collapsing to 0ms if reduced motion is enabled.
     */
    fun resolveDuration(baseDuration: Int): Int = if (isReducedMotion) 0 else baseDuration
}

val LocalIshaaraMotion = staticCompositionLocalOf { IshaaraMotion() }
