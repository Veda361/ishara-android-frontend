package com.ishara.app.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun HomeMapOverlay() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(IshaaraTheme.spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        HomeHeader()

        Spacer(
            modifier = Modifier.weight(1f)
        )

        HeroSearchCard()

    }
}