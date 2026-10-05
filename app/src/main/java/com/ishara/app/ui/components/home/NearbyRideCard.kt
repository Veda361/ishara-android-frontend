package com.ishara.app.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun NearbyRideCard() {

    IshaaraCard(
        modifier = Modifier.shadow(
            10.dp,
            RoundedCornerShape(22.dp)
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color(0xFFF1EDFF)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            Icons.Rounded.Person,
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
                        "Rahul Sharma",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Text(
                        "Leaving in 4 min",
                        color = IshaaraTheme.colors.foregroundMuted,
                        fontSize = 13.sp
                    )

                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF2EEFF)
                ) {

                    Text(
                        "₹20",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = Color(0xFF6D5EF9),
                        fontWeight = FontWeight.Bold
                    )

                }

            }

            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    "Girls Hostel",
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.Rounded.ArrowForward,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "Library",
                    fontWeight = FontWeight.SemiBold
                )

            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F8EC)
                ) {

                    Text(
                        "2 Seats Left",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = Color(0xFF1B8E3E),
                        fontSize = 13.sp
                    )

                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF2EEFF)
                ) {

                    Text(
                        "Nearby",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = Color(0xFF6D5EF9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                }

            }

        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_7"
)
@Composable
private fun NearbyRideCardPreview() {

    IshaaraTheme {
        NearbyRideCard()
    }

}