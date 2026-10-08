package com.ishara.app.ui.screen.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ishara.app.R
import com.ishara.app.feature.auth.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF635BFF)
            ),
            enabled = !uiState.isLoading,
            onClick = { 
                Log.d("AUTH_DEBUG", "LoginScreen: Google Sign-In button clicked")
                viewModel.signInWithGoogle(context) 
            }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
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
