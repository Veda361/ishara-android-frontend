package com.ishara.app.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun SavedPlacesCard() {

    IshaaraCard {

        Column {

            Text(
                "Saved Places",
                style = IshaaraTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            SavedPlaceRow(
                Icons.Rounded.Home,
                "Girls Hostel",
                "Home"
            )

            Spacer(Modifier.height(10.dp))

            SavedPlaceRow(
                Icons.Rounded.School,
                "Central Library",
                "Study"
            )

            Spacer(Modifier.height(10.dp))

            SavedPlaceRow(
                Icons.Rounded.LocationOn,
                "Main Gate",
                "Campus"
            )

        }

    }

}

@Composable
private fun SavedPlaceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF7F5FF),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    Color(0xFFECE8FF),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                icon,
                null,
                tint = Color(0xFF6750F6)
            )

        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                title,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                subtitle,
                color = Color.Gray
            )

        }

        Text(
            "›",
            color = Color.Gray
        )

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun SavedPlacesCardPreview() {

    IshaaraTheme {

        SavedPlacesCard()

    }

}