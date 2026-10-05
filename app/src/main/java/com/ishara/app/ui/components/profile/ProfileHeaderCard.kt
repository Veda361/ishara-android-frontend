package com.ishara.app.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun ProfileHeaderCard() {

    IshaaraCard {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Color(0xFFECE8FF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    "A",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750F6)
                )

            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Apoorva Sahu",
                style = IshaaraTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Rounded.EmojiEvents,
                    null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    "Passenger since 2025",
                    color = Color(0xFF16A34A)
                )

            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                ProfileStat(
                    Icons.Rounded.DirectionsCar,
                    "126",
                    "Rides"
                )

                ProfileStat(
                    Icons.Rounded.Star,
                    "4.9",
                    "Rating"
                )

                ProfileStat(
                    Icons.Rounded.EmojiEvents,
                    "₹620",
                    "Saved"
                )

            }

        }

    }

}

@Composable
private fun ProfileStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            icon,
            null,
            tint = Color(0xFF6750F6)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Text(
            label,
            color = Color.Gray
        )

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ProfileHeaderCardPreview() {

    IshaaraTheme {

        ProfileHeaderCard()

    }

}