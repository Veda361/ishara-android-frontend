package com.ishara.app.ui.screen.driver.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DriverSettingsScreen() {

    var notifications by remember { mutableStateOf(true) }
    var sounds by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FA))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Customize your driver experience",
                color = Color.Gray
            )

        }

        item {

            Card(
                shape = RoundedCornerShape(24.dp)
            ) {

                Column {

                    SettingSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        checked = notifications
                    ) {
                        notifications = it
                    }

                    Divider()

                    SettingSwitchRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Ride Request Sounds",
                        checked = sounds
                    ) {
                        sounds = it
                    }

                }

            }

        }

        item {

            Card(
                shape = RoundedCornerShape(24.dp)
            ) {

                Column {

                    SettingRow(Icons.Default.DarkMode, "Appearance")
                    Divider()

                    SettingRow(Icons.Default.Language, "Language")
                    Divider()

                    SettingRow(Icons.Default.Security, "Security")
                    Divider()

                    SettingRow(Icons.Default.PrivacyTip, "Privacy")
                    Divider()

                    SettingRow(Icons.Default.Help, "Help & Support")

                }

            }

        }

        item {

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                onClick = {}
            ) {

                Icon(Icons.Default.Logout, null)

                Spacer(modifier = Modifier.width(8.dp))

                Text("Logout")

            }

        }

    }

}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            null,
            tint = Color(0xFF6C63FF)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            title,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

    }

}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            null,
            tint = Color(0xFF6C63FF)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            title,
            modifier = Modifier.weight(1f)
        )

        Text(
            ">",
            color = Color.Gray
        )

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun DriverSettingsScreenPreview() {

    MaterialTheme {

        DriverSettingsScreen()

    }

}