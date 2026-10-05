package com.ishara.app.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun RoleSelectionScreen(
    onPassengerSelected: () -> Unit,
    onDriverSelected: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Who are you today?",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Choose how you'd like to use Ishaara.",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(36.dp))

        RoleCard(
            title = "Passenger",
            subtitle = "Book rides around campus",
            icon = Icons.Rounded.Person,
            onClick = onPassengerSelected
        )

        Spacer(modifier = Modifier.height(18.dp))

        RoleCard(
            title = "Driver",
            subtitle = "Give rides and earn",
            icon = Icons.Rounded.DirectionsCar,
            onClick = onDriverSelected
        )

    }

}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(
                        Color(0xFFEDE9FE),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    icon,
                    null,
                    tint = Color(0xFF635BFF)
                )

            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {

                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    subtitle,
                    color = Color.Gray
                )

            }

        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun RoleSelectionScreenPreview() {
    IshaaraTheme {
        RoleSelectionScreen(
            onPassengerSelected = {},
            onDriverSelected = {}
        )
    }
}