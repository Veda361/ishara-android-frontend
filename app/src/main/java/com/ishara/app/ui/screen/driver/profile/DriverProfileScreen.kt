package com.ishara.app.ui.screen.driver.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DriverProfileScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FA))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        Text(
            "Driver Profile",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )

        Text(
            "Your driver identity",
            color = Color.Gray
        )

        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = Color(0xFFEDE9FE)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            "A",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C63FF)
                        )

                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Apoorva Sahu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Verified,
                        null,
                        tint = Color(0xFF34A853)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "Verified Driver",
                        color = Color(0xFF34A853),
                        fontWeight = FontWeight.SemiBold
                    )

                }

            }

        }

        Card(
            shape = RoundedCornerShape(24.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                ProfileItem(Icons.Default.Call, "Phone", "+91 9876543210")
                Divider()

                ProfileItem(Icons.Default.Email, "Email", "apoorva@ishaara.app")
                Divider()

                ProfileItem(Icons.Default.Badge, "Driver ID", "DRV-10452")
                Divider()

                ProfileItem(Icons.Default.DirectionsCar, "Vehicle", "Honda Activa 6G")
                Divider()

                ProfileItem(Icons.Default.Star, "Rating", "4.9 ★")

            }

        }

    }

}

@Composable
private fun ProfileItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            null,
            tint = Color(0xFF6C63FF)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {

            Text(
                title,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                value,
                fontWeight = FontWeight.SemiBold
            )

        }

    }

}

@Preview(
    showSystemUi = true,
    showBackground = true
)
@Composable
private fun DriverProfileScreenPreview() {

    MaterialTheme {

        DriverProfileScreen()

    }

}