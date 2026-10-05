package com.ishara.app.ui.screen.passenger.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.ui.components.home.RecentTripSection
import com.ishara.app.ui.components.trips.UpcomingTripCard

@Composable
fun TripsScreen() {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(IshaaraTheme.spacing.lg),

        verticalArrangement = Arrangement.spacedBy(
            IshaaraTheme.spacing.lg
        )

    ) {

        UpcomingTripCard()

        RecentTripSection()

        // EmptyTripsState()

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun TripsScreenPreview() {

    IshaaraTheme {

        TripsScreen()

    }

}