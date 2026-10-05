package com.ishara.app.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.ishara.app.core.designsystem.components.IshaaraSectionHeader
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun RecentTripSection() {

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        IshaaraSectionHeader(
            title = "Recent Trips"
        )

        RecentTripCard(
            driver = "Rahul Sharma",
            route = "Library → Girls Hostel",
            fare = "₹20",
            date = "Yesterday"
        )

        RecentTripCard(
            driver = "Apoorva",
            route = "Main Gate → Auditorium",
            fare = "₹15",
            date = "Monday"
        )

        RecentTripCard(
            driver = "Vikram",
            route = "Hostel → Sports Complex",
            fare = "₹30",
            date = "Last Week"
        )
    }
}

@Composable
private fun RecentTripCard(
    driver: String,
    route: String,
    fare: String,
    date: String
) {

    IshaaraCard {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    driver,
                    style = IshaaraTheme.typography.titleMedium
                )

                Text(route)

                Text(
                    date,
                    color = IshaaraTheme.colors.foregroundMuted
                )

            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    fare,
                    style = IshaaraTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = Color(0xFF179C52)
                )

            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun RecentTripSectionPreview() {
    IshaaraTheme {
        RecentTripSection()
    }
}