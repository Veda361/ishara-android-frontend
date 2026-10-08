package com.ishara.app.ui.scaffold

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ishara.app.IshaaraApplication
import com.ishara.app.core.designsystem.components.IshaaraIconButton
import com.ishara.app.core.designsystem.components.IshaaraTopBar
import com.ishara.app.navigation.Destination
import com.ishara.app.core.designsystem.components.IshaaraNavigationDrawer
import com.ishara.app.core.di.ViewModelFactory
import com.ishara.app.navigation.Graph
import com.ishara.app.ui.screen.passenger.discover.DiscoverScreen
import com.ishara.app.ui.screen.passenger.home.HomeScreen
import com.ishara.app.ui.screen.passenger.profile.ProfileScreen
import com.ishara.app.ui.screen.passenger.profile.ProfileViewModel
import com.ishara.app.ui.screen.passenger.trips.TripsScreen
import kotlinx.coroutines.launch

@Composable
fun PassengerScaffold(
    rootNavController: NavHostController
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as IshaaraApplication).appContainer
    val viewModelFactory = ViewModelFactory(appContainer)

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            IshaaraNavigationDrawer(
                onHomeClick = {
                    navController.navigate(Destination.Home)
                    scope.launch { drawerState.close() }
                },
                onDiscoverClick = {
                    navController.navigate(Destination.Discover)
                    scope.launch { drawerState.close() }
                },
                onTripsClick = {
                    navController.navigate(Destination.Trips)
                    scope.launch { drawerState.close() }
                },
                onProfileClick = {
                    navController.navigate(Destination.Profile)
                    scope.launch { drawerState.close() }
                },
                onDriverModeClick = {
                    scope.launch {
                        drawerState.close()
                    }
                    rootNavController.navigate(Graph.DRIVER) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                IshaaraTopBar(
                    title = "Ishaara",
                    actions = {
                        IshaaraIconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            contentDescription = "Menu"
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
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
                    val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
                    ProfileScreen(viewModel = profileViewModel)
                }
            }
        }
    }
}
