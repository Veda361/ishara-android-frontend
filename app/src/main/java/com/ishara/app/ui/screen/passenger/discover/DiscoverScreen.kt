package com.ishara.app.ui.screen.passenger.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.ui.components.CampusMapPlaceholder
import com.ishara.app.ui.components.NearbyStopsCard
import com.ishara.app.ui.components.SearchDestinationCard

@Composable
fun DiscoverScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SearchDestinationCard()

        NearbyStopsCard()

        PopularRoutesCard()

        CampusMapPlaceholder()

    }
}

@Composable
fun PopularRoutesCard() {
    TODO("Not yet implemented")
}