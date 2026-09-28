package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun SettingsCard() {

    IshaaraCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                "Settings",
                style = MaterialTheme.typography.titleMedium
            )

            Text("🔔 Notifications")

            HorizontalDivider()

            Text("🎨 Theme")

            HorizontalDivider()

            Text("❓ Help & Support")

        }

    }

}

@Preview(showBackground = true)
@Composable
private fun SettingsCardPreview() {
    IshaaraTheme {
        SettingsCard()
    }
}