package com.ishara.app.ui.components.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PopularRoutesCard() {

    IshaaraCard {

        Column {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "Trending Today",
                    style = IshaaraTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

            }

            Spacer(Modifier.padding(top = 8.dp))

            Text(
                "Most travelled campus routes",
                color = IshaaraTheme.colors.foregroundMuted
            )

            Spacer(Modifier.padding(top = 16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                RouteChip("🏠 Girls Hostel → 📚 Library")
                RouteChip("🚪 Main Gate → 🏛 Auditorium")
                RouteChip("📚 Library → 🍽 Cafeteria")
                RouteChip("🏢 Academic Block → 🚌 Bus Stop")
                RouteChip("🏸 Sports Complex → 🏠 Hostel")

            }

        }

    }

}

@Composable
private fun RouteChip(
    text: String
) {

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFF2EEFF)
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Rounded.TrendingUp,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
                tint = Color(0xFF6D5EF9)
            )

            Text(
                text = text,
                color = Color(0xFF6D5EF9)
            )

        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_7"
)
@Composable
private fun PopularRoutesCardPreview() {

    IshaaraTheme {

        PopularRoutesCard()

    }

}
