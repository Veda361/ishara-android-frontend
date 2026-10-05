package com.ishara.app.ui.screen.passenger.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.ui.components.discover.PopularRoutesCard
import com.ishara.app.ui.components.home.CampusMapCard
import com.ishara.app.ui.components.home.HeroSearchCard
import com.ishara.app.ui.components.home.NearbyRideSection
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun DiscoverScreen() {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(IshaaraTheme.spacing.lg),

        verticalArrangement = Arrangement.spacedBy(
            IshaaraTheme.spacing.lg
        )

    ) {

        HeroSearchCard()

        NearbyRideSection()

        PopularRoutesCard()

        CampusMapCard()

    }

}



@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun DiscoverScreenPreview() {

    IshaaraTheme {

        DiscoverScreen()

    }

}