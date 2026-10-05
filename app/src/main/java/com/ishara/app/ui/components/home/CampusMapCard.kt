package com.ishara.app.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
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
import com.ishara.app.ui.map.IshaaraMap

@Composable
fun CampusMapCard(
    modifier: Modifier = Modifier
) {

    IshaaraCard(
        modifier = modifier.fillMaxWidth()
    ) {

        Column {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        "Live Campus Map",
                        style = IshaaraTheme.typography.titleMedium
                    )

                    Text(
                        "See rides moving around you",
                        color = IshaaraTheme.colors.foregroundMuted
                    )

                }

                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFFE9F8EC),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {

                    Text(
                        "LIVE",
                        color = Color(0xFF16A34A)
                    )

                }

            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {

                IshaaraMap()

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    SmallMapChip("📍 Library")

                    SmallMapChip("🚗 8 Rides Nearby")

                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(
                            Color.White,
                            CircleShape
                        )
                        .padding(12.dp)
                ) {

                    Icon(
                        Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF6750F6)
                    )

                }

            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                QuickActionChip(
                    Icons.Rounded.Navigation,
                    "Navigate"
                )

                QuickActionChip(
                    Icons.Rounded.LocationOn,
                    "Nearest Ride"
                )

            }

        }

    }

}

@Composable
private fun SmallMapChip(
    text: String
) {

    Box(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = .95f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {

        Text(text)

    }

}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {

    Row(
        modifier = Modifier
            .background(
                Color(0xFFF3F0FF),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            null,
            tint = Color(0xFF6750F6)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text,
            color = Color(0xFF6750F6)
        )

    }

}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun CampusMapCardPreview() {

    IshaaraTheme {

        CampusMapCard()

    }

}