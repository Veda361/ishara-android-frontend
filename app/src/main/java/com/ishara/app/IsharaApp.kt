package com.ishara.app


import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.ishara.app.navigation.AppNavHost


@Composable
fun IsharaApp() {

    val navController = rememberNavController()

    AppNavHost(
        navController = navController
    )
}