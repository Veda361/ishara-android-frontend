package com.ishara.app.ui.screen.driver.active_trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActiveTripScreen() {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Temporary Map Placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F7))
        )

        // Bottom Sheet
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {

            Column(
                modifier = Modifier.padding(24.dp)
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .background(
                            Color.LightGray,
                            CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Current Trip",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEDE9FE)
                    ) {

                        Icon(
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.padding(16.dp),
                            tint = Color(0xFF6C63FF)
                        )

                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            "Apoorva",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            "4.9 ★"
                        )

                    }

                    FilledIconButton(
                        onClick = {}
                    ) {

                        Icon(
                            Icons.Default.Call,
                            null
                        )

                    }

                }

                Spacer(modifier = Modifier.height(24.dp))

                Row {

                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = Color(0xFF6C63FF)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {

                        Text(
                            "Pickup",
                            color = Color.Gray
                        )

                        Text(
                            "Girls Hostel",
                            fontWeight = FontWeight.SemiBold
                        )

                    }

                }

                Spacer(modifier = Modifier.height(18.dp))

                Row {

                    Icon(
                        Icons.Default.Navigation,
                        null,
                        tint = Color(0xFF6C63FF)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {

                        Text(
                            "Destination",
                            color = Color.Gray
                        )

                        Text(
                            "VIT Main Gate",
                            fontWeight = FontWeight.SemiBold
                        )

                    }

                }

                Spacer(modifier = Modifier.height(26.dp))

                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {

                    Text("Navigate")

                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {

                    Text("Complete Trip")

                }

            }

        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ActiveTripScreenPreview() {

    MaterialTheme {

        ActiveTripScreen()

    }

}