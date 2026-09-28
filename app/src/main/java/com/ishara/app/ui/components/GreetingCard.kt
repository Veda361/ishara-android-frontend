package com.ishara.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.ui.theme.IsharaTheme

@Composable
fun GreetingCard() {

    Text(
        text = "👋 Good Morning",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Apoorva",
        style = MaterialTheme.typography.headlineMedium
    )

}

@Preview(showBackground = true)
@Composable
private fun GreetingCardPreview() {
    IsharaTheme {
        GreetingCard()
    }
}