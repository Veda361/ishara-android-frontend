package com.ishara.app.ui.components.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun TripHistoryCard() {

    IshaaraCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "Yesterday",
                style = MaterialTheme.typography.titleMedium
            )

            Text("Hostel → Cafeteria")

            Text(
                text = "Completed",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripHistoryCardPreview() {
    IshaaraTheme {
        TripHistoryCard()
    }
}