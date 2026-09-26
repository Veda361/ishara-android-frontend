package com.ishara.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ishara.app.ui.screen.auth.LoginScreen
import com.ishara.app.ui.screen.auth.RoleSelectionScreen
import com.ishara.app.ui.screen.passenger.discover.DiscoverScreen
import com.ishara.app.ui.screen.passenger.home.HomeScreen
import com.ishara.app.ui.screen.passenger.profile.ProfileScreen
import com.ishara.app.ui.screen.splash.SplashScreen
import com.ishara.app.ui.screen.passenger.trips.TripsScreen

@Composable
fun AppNavHost(
    navController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = Graph.ROOT
    ) {

        /*
         * Root Graph
         */
        navigation(
            startDestination = Destination.Splash,
            route = Graph.ROOT
        ) {

            composable(Destination.Splash) {
                SplashScreen()
            }
        }

        /*
         * Auth Graph
         */
        navigation(
            startDestination = Destination.Login,
            route = Graph.AUTH
        ) {

            composable(Destination.Login) {
                LoginScreen()
            }

            composable(Destination.RoleSelection) {
                RoleSelectionScreen()
            }
        }

        /*
         * Passenger Graph
         */
        navigation(
            startDestination = Destination.Home,
            route = Graph.PASSENGER
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

        /*
         * Driver Graph
         */
        navigation(
            startDestination = Destination.Dashboard,
            route = Graph.DRIVER
        ) {

            // Placeholder until we create driver screens.
        }
    }
}