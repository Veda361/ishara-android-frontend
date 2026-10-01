package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun UpcomingTripCard() {

    IshaaraCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "Upcoming Trip",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "🚌 Route 5",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Main Gate → Library")

            Text("Departure: 10:30 AM")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🟢 On Time",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpcomingTripCardPreview() {
    IshaaraTheme {
        UpcomingTripCard()
    }
}