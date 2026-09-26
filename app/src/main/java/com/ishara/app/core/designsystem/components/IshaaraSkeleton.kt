package com.ishara.app.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Subtle pulse skeleton loader for cards and text blocks.
 * Automatically respects reduced motion settings.
 */
@Composable
fun IshaaraSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    shape: Shape = IshaaraTheme.shapes.xs
) {
    val motion = IshaaraTheme.motion
    val colors = IshaaraTheme.colors

    val alpha = if (motion.isReducedMotion) {
        0.5f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "skeleton_pulse")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_alpha"
        )
        animatedAlpha
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(colors.surfaceSubtle.copy(alpha = alpha))
    )
}
