package com.ishara.app.ui.screen.passenger.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.ui.components.RecentTripsCard
import com.ishara.app.ui.components.UpcomingTripCard

@Composable
fun TripsScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "My Trips",
            style = MaterialTheme.typography.headlineMedium
        )

        UpcomingTripCard()

        RecentTripsCard()

        // Later:
        // EmptyTripsState()

    }

}

@Preview(showBackground = true)
@Composable
private fun TripsScreenPreview() {
    IshaaraTheme {
        TripsScreen()
    }
}