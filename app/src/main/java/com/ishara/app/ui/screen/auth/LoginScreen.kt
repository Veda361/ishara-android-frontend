package com.ishara.app.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ishara.app.R

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.ishara_bus),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "Welcome to",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Text(
            "Ishaara",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Smart, safe and effortless campus rides.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF635BFF)
            ),
            onClick = onLoginClick
        ) {

            Text(
                "Continue with Google",
                color = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Rounded.ArrowForward,
                null
            )

        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            "Secure sign in powered by Google",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun LoginScreenPreview() {
    IshaaraTheme {
        LoginScreen(
            onLoginClick = {}
        )
    }
}