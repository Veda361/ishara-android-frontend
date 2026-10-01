package com.ishara.app.ui.screen.passenger.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.ui.components.ActiveTripCard
import com.ishara.app.ui.components.GreetingCard
import com.ishara.app.ui.components.QuickActionsGrid
import com.ishara.app.ui.components.RecentTripsCard
import com.ishara.app.ui.components.SearchDestinationCard

@Composable
fun HomeScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        GreetingCard()

        SearchDestinationCard()

        ActiveTripCard()

        QuickActionsGrid()

        RecentTripsCard()
    }
}