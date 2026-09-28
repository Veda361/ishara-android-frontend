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
import com.ishara.app.ui.theme.IsharaTheme

@Composable
fun RecentTripsCard() {

    IshaaraCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                "Recent Trips",
                style = MaterialTheme.typography.titleMedium
            )

            Text("Yesterday")
            Text(
                text = "Hostel → Library",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            Text("2 Days Ago")
            Text(
                text = "Main Gate → Academic Block",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentTripsCardPreview() {
    IsharaTheme {
        RecentTripsCard()
    }
}