package com.ishara.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    data object Home : BottomNavItem(
        "home",
        "Home",
        Icons.Default.Home
    )

    data object Discover : BottomNavItem(
        "discover",
        "Discover",
        Icons.Default.Map
    )

    data object Trips : BottomNavItem(
        "trips",
        "Trips",
        Icons.Default.DirectionsBus
    )

    data object Profile : BottomNavItem(
        "profile",
        "Profile",
        Icons.Default.Person
    )
} 