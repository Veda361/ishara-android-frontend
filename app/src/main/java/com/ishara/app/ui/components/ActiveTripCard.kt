package com.ishara.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ishara.app.core.designsystem.components.IshaaraButton
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.ui.theme.IsharaTheme

@Composable
fun ActiveTripCard() {

    IshaaraCard {

        Text("Route 5")

        IshaaraButton(
            text = "Track Bus",
            onClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveTripCardPreview() {
    IsharaTheme {
        ActiveTripCard()
    }
}