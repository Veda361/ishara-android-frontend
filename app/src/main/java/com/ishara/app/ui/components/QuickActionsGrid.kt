package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraButton
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun QuickActionsGrid() {

    IshaaraCard {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                IshaaraButton(
                    modifier = Modifier.weight(1f),
                    text = "Discover",
                    onClick = {}
                )

                IshaaraButton(
                    modifier = Modifier.weight(1f),
                    text = "Trips",
                    onClick = {}
                )

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickActionsGridPreview() {
    IshaaraTheme {
        QuickActionsGrid()
    }
}