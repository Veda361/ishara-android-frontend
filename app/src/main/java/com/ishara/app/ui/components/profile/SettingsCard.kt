package com.ishara.app.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
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
fun SettingsCard() {

    IshaaraCard {

        Column {

            Text(
                "Settings",
                style = IshaaraTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            SettingRow(
                Icons.Rounded.Notifications,
                "Notifications"
            )

            Spacer(Modifier.height(10.dp))

            SettingRow(
                Icons.Rounded.Palette,
                "Appearance"
            )

            Spacer(Modifier.height(10.dp))

            SettingRow(
                Icons.Rounded.Security,
                "Privacy & Safety"
            )

            Spacer(Modifier.height(10.dp))

            SettingRow(
                Icons.Rounded.Help,
                "Help & Support"
            )

        }

    }

}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
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
                contentDescription = null,
                tint = Color(0xFF6750F6)
            )

        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold
        )

        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun SettingsCardPreview() {

    IshaaraTheme {

        SettingsCard()

    }

}