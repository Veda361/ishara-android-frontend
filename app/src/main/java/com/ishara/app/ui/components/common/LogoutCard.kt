package com.ishara.app.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@Composable
fun LogoutCard(
    onLogout: () -> Unit = {}
) {

    IshaaraCard {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    Color(0xFFD92D20),
                    RoundedCornerShape(28.dp)
                )
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Rounded.Logout,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Log Out",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
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
private fun LogoutCardPreview() {

    IshaaraTheme {

        LogoutCard()

    }

}