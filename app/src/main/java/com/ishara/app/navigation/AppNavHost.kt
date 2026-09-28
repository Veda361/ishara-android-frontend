package com.ishara.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ishara.app.ui.screen.auth.LoginScreen
import com.ishara.app.ui.screen.auth.RoleSelectionScreen
import com.ishara.app.ui.screen.passenger.discover.DiscoverScreen
import com.ishara.app.ui.scaffold.PassengerScaffold
import com.ishara.app.ui.scaffold.DriverScaffold
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

                SplashScreen(

                    onFinished = {

                        navController.navigate(Graph.AUTH) {

                            popUpTo(Graph.ROOT) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }

                    }

                )

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

                LoginScreen(

                    onLoginClick = {

                        navController.navigate(Destination.RoleSelection) {

                            launchSingleTop = true

                        }

                    }

                )

            }

            composable(Destination.RoleSelection) {

                RoleSelectionScreen(

                    onPassengerSelected = {

                        navController.navigate(Graph.PASSENGER) {

                            popUpTo(Graph.AUTH) {
                                inclusive = true
                            }
                            launchSingleTop = true

                        }

                    },

                    onDriverSelected = {

                        navController.navigate(Graph.DRIVER) {

                            popUpTo(Graph.AUTH) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }

                    }

                )

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
                PassengerScaffold(navController)
            }

        }

        /*
         * Driver Graph
         */
        navigation(
            startDestination = Destination.Dashboard,
            route = Graph.DRIVER
        ) {

            composable(Destination.Dashboard) {
                DriverScaffold(navController)
            }

        }
    }
}
