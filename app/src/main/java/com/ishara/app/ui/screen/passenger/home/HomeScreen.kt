package com.ishara.app.ui.screen.passenger.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishara.app.ui.map.IshaaraMap

@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Full-screen map
        IshaaraMap()

        // Bottom Search Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .align(Alignment.BottomCenter)
        ) {

            // Purple glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 22.dp, vertical = 10.dp)
                    .blur(35.dp)
                    .background(
                        Color(0xFF8B5CF6).copy(alpha = 0.10f),
                        RoundedCornerShape(26.dp)
                    )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFDFDFE)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = Color(0xFFF2F0FF)
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                Icons.Default.Search,
                                contentDescription = null
                            )

                        }

                    }

                    Spacer(Modifier.width(14.dp))

                    Column {

                        Text(
                            text = "Where are you going?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            "Search rides or campus stops",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                    }

                }

            }

        }

    }

}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_7"
)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onMenuClick = {},
            onSearchClick = {}
        )
    }
}