package com.ishara.app.ui.components.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun UpcomingTripCard() {

    IshaaraCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Text(
                "Upcoming Ride",
                style = IshaaraTheme.typography.headlineSmall
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        null,
                        tint = Color(0xFF635BFF)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        "Rahul Sharma",
                        style = IshaaraTheme.typography.titleMedium
                    )

                    Text(
                        "Driver • 4.9 ★",
                        color = IshaaraTheme.colors.foregroundMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFDFF7E8))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {

                    Text(
                        "On Time",
                        color = Color(0xFF179C52)
                    )

                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    Icons.Rounded.LocationOn,
                    null,
                    tint = Color(0xFF635BFF)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {

                    Text(
                        "Pickup",
                        color = IshaaraTheme.colors.foregroundMuted
                    )

                    Text("Girls Hostel")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    Icons.Rounded.DirectionsBus,
                    null,
                    tint = Color(0xFF635BFF)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {

                    Text(
                        "Destination",
                        color = IshaaraTheme.colors.foregroundMuted
                    )

                    Text("Central Library")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    Icons.Rounded.AccessTime,
                    null,
                    tint = Color(0xFF635BFF)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Today • 10:30 AM")

            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun UpcomingTripCardPreview() {
    IshaaraTheme {
        UpcomingTripCard()
    }
}