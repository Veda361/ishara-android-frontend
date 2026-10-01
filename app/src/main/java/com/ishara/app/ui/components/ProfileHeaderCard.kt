package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun ProfileHeaderCard() {

    IshaaraCard {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "👤",
                style = MaterialTheme.typography.displaySmall
            )

            Text(
                text = "Apoorva Sahu",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Student",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }

    }

}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderCardPreview() {
    IshaaraTheme {
        ProfileHeaderCard()
    }
}