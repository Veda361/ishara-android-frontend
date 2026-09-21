package com.ishara.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class IshaaraShapes(
    val none: Shape = RoundedCornerShape(0.dp),
    val xs: Shape = RoundedCornerShape(4.dp),
    val sm: Shape = RoundedCornerShape(8.dp),
    val md: Shape = RoundedCornerShape(12.dp),
    val lg: Shape = RoundedCornerShape(16.dp),
    val pill: Shape = RoundedCornerShape(999.dp)
)

val LocalIshaaraShapes = staticCompositionLocalOf { IshaaraShapes() }
