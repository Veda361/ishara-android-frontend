package com.ishara.app.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ishara.app.IshaaraApplication
import com.ishara.app.core.di.ViewModelFactory
import com.ishara.app.feature.auth.LoginViewModel
import com.ishara.app.ui.screen.auth.LoginScreen
import com.ishara.app.ui.screen.auth.RoleSelectionScreen
import com.ishara.app.ui.scaffold.PassengerScaffold
import com.ishara.app.ui.scaffold.DriverScaffold
import com.ishara.app.ui.screen.splash.SplashScreen
import com.ishara.app.ui.screen.splash.SplashViewModel
import com.ishara.app.ui.screen.driver.active_trip.ActiveTripScreen
import com.ishara.app.ui.screen.driver.dashboard.DashboardScreen
import com.ishara.app.ui.screen.driver.earnings.EarningsScreen
import com.ishara.app.ui.screen.driver.profile.DriverProfileScreen
import com.ishara.app.ui.screen.driver.requests.RequestsScreen
import com.ishara.app.ui.screen.driver.settings.DriverSettingsScreen
import com.ishara.app.ui.screen.driver.vehicle.VehicleScreen
import com.ishara.app.ui.screen.driver.voice.VoiceAssistantScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as IshaaraApplication).appContainer
    val viewModelFactory = ViewModelFactory(appContainer)

    // Handle global navigation commands from NavigationManager
    LaunchedEffect(Unit) {
        appContainer.navigationManager.commands.collectLatest { command ->
            Log.d("AUTH_DEBUG", "AppNavHost: Received navigation command: $command")
            when (command) {
                is NavigationCommand.NavigateTo -> {
                    Log.d("AUTH_DEBUG", "AppNavHost: Navigating to ${command.route}")
                    navController.navigate(command.route) {
                        command.popUpToRoute?.let { popUpTo(it) { inclusive = command.inclusive } }
                        launchSingleTop = true
                    }
                }
                is NavigationCommand.NavigateUp -> {
                    Log.d("AUTH_DEBUG", "AppNavHost: Navigating up")
                    navController.navigateUp()
                }
                is NavigationCommand.SwitchToRole -> {
                    val targetGraph = when (command.role.name.lowercase()) {
                        "driver" -> Graph.DRIVER
                        "driver_conductor" -> Graph.DRIVER
                        else -> Graph.PASSENGER
                    }
                    Log.d("AUTH_DEBUG", "AppNavHost: Switching to role ${command.role}, target graph: $targetGraph")
                    navController.navigate(targetGraph) {
                        popUpTo(Graph.ROOT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

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
                val splashViewModel: SplashViewModel = viewModel(factory = viewModelFactory)
                SplashScreen(viewModel = splashViewModel)
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
                val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                LoginScreen(viewModel = loginViewModel)
            }

            composable(Destination.RoleSelection) {
                RoleSelectionScreen(
                    onPassengerSelected = {
                        navController.navigate(Graph.PASSENGER) {
                            popUpTo(Graph.AUTH) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onDriverSelected = {
                        navController.navigate(Graph.DRIVER) {
                            popUpTo(Graph.AUTH) { inclusive = true }
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
            composable(Destination.Requests) { RequestsScreen() }
            composable(Destination.ActiveTrip) { ActiveTripScreen() }
            composable(Destination.Earnings) { EarningsScreen() }
            composable(Destination.Vehicle) { VehicleScreen() }
            composable(Destination.DriverProfile) { DriverProfileScreen() }
            composable(Destination.DriverSettings) { DriverSettingsScreen() }
            composable(Destination.VoiceAssistant) { VoiceAssistantScreen() }
        }
    }
}
