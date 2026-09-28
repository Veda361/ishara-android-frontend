package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun PopularRoutesCard() {

    IshaaraCard {

        Column {

            Text("Popular Routes")

            Text("Main Gate → Library")

            Text("Hostel → Academic Block")

            Text("Cafeteria → Sports Complex")

        }

    }

}

@Preview(showBackground = true)
@Composable
private fun PopularRoutesCardPreview() {
    IshaaraTheme {
        PopularRoutesCard()
    }
}