package com.ishara.app.ui.screen.passenger.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.ui.components.home.HeroSearchCard
import com.ishara.app.ui.components.home.NearbyRideCard
import com.ishara.app.ui.components.home.RecentTripSection
import com.ishara.app.ui.components.trips.UpcomingTripCard
import com.ishara.app.ui.map.IshaaraMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {}
) {

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(

        scaffoldState = scaffoldState,

        sheetPeekHeight = 170.dp,

        sheetShape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        ),

        sheetContainerColor = MaterialTheme.colorScheme.surface,

        sheetContent = {

            LazyColumn(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {

                item {
                    HeroSearchCard(onClick = onSearchClick)
                }

                item {
                    NearbyRideCard()
                }

                item {
                    UpcomingTripCard()
                }

                item {
                    RecentTripSection()
                }

            }

        }

    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {

            IshaaraMap()

            Text(
                text = "Explore Campus",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(24.dp)
            )

        }

    }

}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_7"
)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onSearchClick = {}
        )
    }
}