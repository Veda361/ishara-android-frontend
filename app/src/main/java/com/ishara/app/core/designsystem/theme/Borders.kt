package com.ishara.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class IshaaraBorders(
    val none: Dp = 0.dp,
    val hairline: Dp = 0.5.dp,
    val thin: Dp = 1.dp,
    val default: Dp = 1.dp,
    val thick: Dp = 2.dp
)

val LocalIshaaraBorders = staticCompositionLocalOf { IshaaraBorders() }
