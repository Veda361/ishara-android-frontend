package com.ishara.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun IsharaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    IshaaraTheme(
        darkTheme = darkTheme,
        content = content
    )
}