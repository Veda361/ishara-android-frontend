package com.ishara.app.ui.screen.driver.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
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
fun VehicleScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FA))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            "My Vehicle",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )

        Text(
            "Manage your registered vehicle",
            color = Color.Gray
        )

        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(
                modifier = Modifier.padding(24.dp)
            ) {

                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color(0xFFEDE9FE)
                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(36.dp)
                        )

                    }

                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Honda Activa 6G",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "MH12 AB 4582",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(24.dp))

                VehicleInfoRow("Color", "White")
                VehicleInfoRow("Model Year", "2023")
                VehicleInfoRow("Fuel", "Petrol")
                VehicleInfoRow("Seats", "2")

            }

        }

        Card(
            shape = RoundedCornerShape(24.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Verified,
                        null,
                        tint = Color(0xFF34A853)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        "Vehicle Verification",
                        fontWeight = FontWeight.Bold
                    )

                }

                Spacer(Modifier.height(16.dp))

                StatusChip("Registration Verified")
                Spacer(Modifier.height(10.dp))
                StatusChip("Insurance Active")
                Spacer(Modifier.height(10.dp))
                StatusChip("PUC Valid")

            }

        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {

            Icon(Icons.Default.Edit, null)

            Spacer(Modifier.width(8.dp))

            Text("Edit Vehicle Details")

        }

    }

}

@Composable
private fun VehicleInfoRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        Text(
            title,
            modifier = Modifier.weight(1f),
            color = Color.Gray
        )

        Text(
            value,
            fontWeight = FontWeight.SemiBold
        )

    }

}

@Composable
private fun StatusChip(text: String) {

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFE8F5E9)
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = Color(0xFF34A853),
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text,
                color = Color(0xFF2E7D32)
            )

        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun VehicleScreenPreview() {

    MaterialTheme {

        VehicleScreen()

    }

}