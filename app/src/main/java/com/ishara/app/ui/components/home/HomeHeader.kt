package com.ishara.app.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun HomeHeader() {

    Column {

        Text(
            text = "Good Morning 👋",
            style = IshaaraTheme.typography.bodyMedium,
            color = IshaaraTheme.colors.foregroundMuted
        )

        Text(
            text = "Welcome to Ishaara",
            style = IshaaraTheme.typography.headlineLarge
        )

    }

}