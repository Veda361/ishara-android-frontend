package com.ishara.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Reusable Transit Trip Card.
 * Clean, uncluttered layout answering:
 * WHAT (Bus 24), WHERE (Jhansi → Sipri), WHEN (8 min away), COST (₹20), SEATS (12 seats), ACTION (Request ride).
 */
@Composable
fun IshaaraTripCard(
    route: String,
    originToDestination: String,
    etaText: String,
    fareText: String,
    seatsAvailable: Int,
    onRequestClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: IshaaraTransitStatus = IshaaraTransitStatus.AVAILABLE
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            // Header Row: Route Number & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route,
                    style = typography.titleLarge,
                    color = colors.foreground
                )
                IshaaraStatusChip(status = status)
            }

            // Origin → Destination
            Text(
                text = originToDestination,
                style = typography.headlineSmall,
                color = colors.foreground
            )

            // Metadata: ETA • Fare • Seats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = etaText,
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(text = "•", style = typography.bodyMedium, color = colors.foregroundSubtle)
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = fareText,
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(text = "•", style = typography.bodyMedium, color = colors.foregroundSubtle)
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = "$seatsAvailable seats available",
                    style = typography.bodyMedium,
                    color = if (seatsAvailable < 5) colors.warning else colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // Primary Action Button
            IshaaraButton(
                text = "Request ride",
                onClick = onRequestClick,
                modifier = Modifier.fillMaxWidth(),
                variant = IshaaraButtonVariant.Primary,
                size = IshaaraButtonSize.Medium
            )
        }
    }
}

/**
 * Reusable Driver Card presenting driver profile, rating, and vehicle in a clean human format.
 */
@Composable
fun IshaaraDriverCard(
    driverName: String,
    rating: Double,
    vehicleDetails: String,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: IshaaraTransitStatus = IshaaraTransitStatus.ONLINE
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IshaaraAvatar(name = driverName, status = status, size = 48.dp)
                Column {
                    Text(
                        text = driverName,
                        style = typography.titleLarge,
                        color = colors.foreground
                    )
                    Text(
                        text = "$vehicleDetails • ★ $rating",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            IshaaraIconButton(
                onClick = onCallClick,
                contentDescription = "Call driver",
                variant = IshaaraIconButtonVariant.Filled
            ) {
                Text(text = "📞", style = typography.titleMedium)
            }
        }
    }
}
