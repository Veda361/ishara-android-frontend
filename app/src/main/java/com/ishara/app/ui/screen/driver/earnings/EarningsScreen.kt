package com.ishara.app.ui.screen.driver.earnings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.TrendingUp
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
fun EarningsScreen() {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FA))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        item {

            Text(
                "Earnings",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Your driving income",
                color = Color.Gray
            )

        }

        item {

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF6C63FF)
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Text(
                        "Today's Earnings",
                        color = Color.White.copy(alpha = .8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "₹640",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row {

                        Icon(
                            Icons.Default.TrendingUp,
                            null,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            "+18% from yesterday",
                            color = Color.White
                        )

                    }

                }

            }

        }

        item {

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Trips",
                    value = "8",
                    icon = Icons.Default.AccountBalanceWallet
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Hours",
                    value = "5.2",
                    icon = Icons.Default.TrendingUp
                )

            }

        }

        item {

            Text(
                "Recent Payments",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

        }

        items(5) { index ->

            Card(
                shape = RoundedCornerShape(22.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEDE9FE)
                    ) {

                        Icon(
                            Icons.Default.ArrowDownward,
                            null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.padding(12.dp)
                        )

                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            "Trip #${1024 + index}",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Girls Hostel → Library",
                            color = Color.Gray
                        )

                    }

                    Text(
                        "₹${80 + index * 10}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C63FF)
                    )

                }

            }

        }

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
        shape = RoundedCornerShape(22.dp)
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Icon(
                icon,
                null,
                tint = Color(0xFF6C63FF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Text(
                title,
                color = Color.Gray
            )

        }

    }

}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun EarningsScreenPreview() {
    MaterialTheme {
        EarningsScreen()
    }
}