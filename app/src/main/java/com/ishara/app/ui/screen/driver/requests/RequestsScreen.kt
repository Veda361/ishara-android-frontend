package com.ishara.app.ui.screen.driver.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

private data class RideRequest(
    val passenger: String,
    val pickup: String,
    val destination: String,
    val distance: String,
    val fare: String
)

@Composable
fun RequestsScreen() {

    val requests = listOf(
        RideRequest(
            "Apoorva",
            "Girls Hostel",
            "VIT Main Gate",
            "1.2 km",
            "₹35"
        ),
        RideRequest(
            "Riya",
            "Library",
            "Boys Hostel",
            "900 m",
            "₹25"
        ),
        RideRequest(
            "Rahul",
            "Auditorium",
            "Cafeteria",
            "1.8 km",
            "₹40"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .padding(20.dp)
    ) {

        Text(
            text = "Ride Requests",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${requests.size} passengers waiting nearby",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(requests) { request ->

                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEDE9FE),
                                modifier = Modifier.size(50.dp)
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF6D5EF9)
                                    )

                                }

                            }

                            Spacer(Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    request.passenger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Text(
                                    request.distance,
                                    color = Color.Gray
                                )

                            }

                            Text(
                                request.fare,
                                color = Color(0xFF6D5EF9),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )

                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = Color(0xFF6D5EF9)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column {

                                Text(
                                    "Pickup",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )

                                Text(
                                    request.pickup,
                                    fontWeight = FontWeight.SemiBold
                                )

                            }

                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Default.Schedule,
                                null,
                                tint = Color(0xFF6D5EF9)
                            )

                            Spacer(Modifier.width(8.dp))

                            Column {

                                Text(
                                    "Destination",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )

                                Text(
                                    request.destination,
                                    fontWeight = FontWeight.SemiBold
                                )

                            }

                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {}
                            ) {

                                Text("Decline")

                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6D5EF9)
                                )
                            ) {

                                Text("Accept")

                            }

                        }

                    }

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
private fun RequestsScreenPreview() {
    IshaaraTheme {
        RequestsScreen()
    }
}