package com.ishara.app.ui.screen.driver.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun DashboardScreen() {

    var online by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        //-------------------------------------------------------
        // Greeting
        //-------------------------------------------------------

        Text(
            text = "Good Morning 👋",
            fontSize = 18.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Hello, Jatin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Ready to pick up riders?",
            color = Color.Gray
        )

        Spacer(Modifier.height(28.dp))

        //-------------------------------------------------------
        // Online Card
        //-------------------------------------------------------

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (online)
                                Color(0xFFDCFCE7)
                            else
                                Color(0xFFF3F4F6),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        Icons.Default.ToggleOn,
                        contentDescription = null,
                        tint = if (online)
                            Color(0xFF16A34A)
                        else
                            Color.Gray
                    )

                }

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        if (online)
                            "You're Online"
                        else
                            "You're Offline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Text(
                        if (online)
                            "You can now receive ride requests."
                        else
                            "Turn on availability to start earning.",
                        color = Color.Gray
                    )

                }

                Switch(
                    checked = online,
                    onCheckedChange = {
                        online = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF16A34A)
                    )
                )

            }

        }

        Spacer(Modifier.height(22.dp))

        //-------------------------------------------------------
        // Stats
        //-------------------------------------------------------

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            StatCard(
                modifier = Modifier.weight(1f),
                title = "Today's Earnings",
                value = "₹640",
                icon = Icons.Default.DirectionsCar
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = "Trips",
                value = "8",
                icon = Icons.Default.LocationOn
            )

        }

        Spacer(Modifier.height(14.dp))

        StatCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Driver Rating",
            value = "4.9 ★",
            icon = Icons.Default.Star
        )

        Spacer(Modifier.height(30.dp))

        //-------------------------------------------------------
        // Nearby Requests
        //-------------------------------------------------------

        Text(
            "Nearby Ride Requests",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(16.dp))

        RideRequestCard(
            pickup = "Girls Hostel",
            destination = "Library",
            distance = "1.2 km"
        )

        Spacer(Modifier.height(14.dp))

        RideRequestCard(
            pickup = "Main Gate",
            destination = "VIT Square",
            distance = "800 m"
        )

    }

}

@Composable
private fun StatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF6D5EF9)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                title,
                color = Color.Gray
            )

        }

    }

}

@Composable
private fun RideRequestCard(
    pickup: String,
    destination: String,
    distance: String
) {

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                pickup,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                "Pickup",
                color = Color.Gray
            )

            Spacer(Modifier.height(10.dp))

            Divider()

            Spacer(Modifier.height(10.dp))

            Text(
                destination,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                "Destination",
                color = Color.Gray
            )

            Spacer(Modifier.height(14.dp))

            Text(
                distance,
                color = Color(0xFF6D5EF9),
                fontWeight = FontWeight.SemiBold
            )

        }

    }

}


@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_7"
)
@Composable
private fun DashboardScreenPreview() {
    IshaaraTheme {
        DashboardScreen()
    }
}