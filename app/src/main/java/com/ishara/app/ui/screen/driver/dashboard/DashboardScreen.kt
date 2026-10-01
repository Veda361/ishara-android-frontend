package com.ishara.app.ui.screen.driver.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen() {

    var online by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Driver Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Text("You're signed in as a driver.")

                Switch(
                    checked = online,
                    onCheckedChange = {
                        online = it
                    }
                )

                Text(
                    if (online)
                        "Status: Online"
                    else
                        "Status: Offline"
                )

                Button(
                    onClick = {
                        // TODO connect UpdateDriverLocationUseCase
                    }
                ) {
                    Text("Update Location")
                }

                Button(
                    onClick = {
                        // TODO open trip requests
                    }
                ) {
                    Text("View Requests")
                }
            }
        }
    }
}