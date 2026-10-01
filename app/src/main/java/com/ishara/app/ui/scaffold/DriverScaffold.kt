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
import com.ishara.app.ui.screen.driver.active_trip.ActiveTripScreen
import com.ishara.app.ui.screen.driver.dashboard.DashboardScreen
import com.ishara.app.ui.screen.driver.earnings.EarningsScreen
import com.ishara.app.ui.screen.driver.profile.DriverProfileScreen
import com.ishara.app.ui.screen.driver.requests.RequestsScreen
import com.ishara.app.ui.screen.driver.settings.DriverSettingsScreen
import com.ishara.app.ui.screen.driver.vehicle.VehicleScreen
import com.ishara.app.ui.screen.driver.voice.VoiceAssistantScreen

@Composable
fun DriverScaffold(
    @Suppress("UNUSED_PARAMETER")
    rootNavController: NavHostController
) {
    val driverNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            DriverBottomBar(
                navController = driverNavController
            )
        }
    ) { padding ->

        NavHost(
            navController = driverNavController,
            startDestination = Destination.Dashboard,
            modifier = Modifier.padding(padding)
        ) {

            composable(Destination.Dashboard) {
                DashboardScreen()
            }

            composable(Destination.Requests) {
                RequestsScreen()
            }

            composable(Destination.ActiveTrip) {
                ActiveTripScreen()
            }

            composable(Destination.Earnings) {
                EarningsScreen()
            }

            composable(Destination.Vehicle) {
                VehicleScreen()
            }

            composable(Destination.DriverProfile) {
                DriverProfileScreen()
            }

            composable(Destination.DriverSettings) {
                DriverSettingsScreen()
            }

            composable(Destination.VoiceAssistant) {
                VoiceAssistantScreen()
            }
        }
    }
}