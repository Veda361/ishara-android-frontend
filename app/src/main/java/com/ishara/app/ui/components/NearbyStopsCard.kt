package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun NearbyStopsCard() {

    IshaaraCard {

        Column {

            Text("Nearby Stops")

            Text("• Main Gate")

            Text("• Library")

            Text("• Hostel")

        }

    }

}

@Preview(showBackground = true)
@Composable
private fun NearbyStopsCardPreview() {
    IshaaraTheme {
        NearbyStopsCard()
    }
}