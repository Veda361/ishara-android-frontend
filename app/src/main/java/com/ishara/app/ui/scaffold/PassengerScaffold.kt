package com.ishara.app.ui.scaffold

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ishara.app.navigation.Destination
import com.ishara.app.ui.screen.passenger.discover.DiscoverScreen
import com.ishara.app.ui.screen.passenger.home.HomeScreen
import com.ishara.app.ui.screen.passenger.profile.ProfileScreen
import com.ishara.app.ui.screen.passenger.trips.TripsScreen

@Composable
fun PassengerScaffold(
    @Suppress("UNUSED_PARAMETER")
    rootNavController: NavHostController
) {
    val passengerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            PassengerBottomBar(
                navController = passengerNavController
            )
        }
    ) { padding ->

        NavHost(
            navController = passengerNavController,
            startDestination = Destination.Home,
            modifier = Modifier.padding(padding)
        ) {

            composable(Destination.Home) {
                HomeScreen()
            }

            composable(Destination.Discover) {
                DiscoverScreen()
            }

            composable(Destination.Trips) {
                TripsScreen()
            }

            composable(Destination.Profile) {
                ProfileScreen()
            }
        }
    }
}