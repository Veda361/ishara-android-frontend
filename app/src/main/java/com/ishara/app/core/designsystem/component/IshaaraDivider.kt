package com.ishara.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Clean hairline divider.
 */
@Composable
fun IshaaraDivider(
    modifier: Modifier = Modifier,
    color: Color = IshaaraTheme.colors.borderSubtle,
    thickness: Dp = IshaaraTheme.borders.hairline
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}
