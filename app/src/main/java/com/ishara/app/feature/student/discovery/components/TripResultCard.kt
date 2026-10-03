package com.ishara.app.feature.student.discovery.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.TripCompatibility

/**
 * Reusable trip result component for discovered transit options.
 *
 * Strictly adheres to backend DiscoveryItemDto:
 * Displays verified vehicle, driver, route duration/distance, pickup walk distance,
 * and factual match compatibility.
 *
 * NOTE: Seat counts and fare values are NOT displayed, in strict accordance
 * with backend contract verification.
 */
@Composable
fun TripResultCard(
    trip: DiscoveredTrip,
    isSelected: Boolean,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val routeSummary = "${trip.originName ?: trip.originAddress} → ${trip.destinationName ?: trip.destinationAddress}"
    val vehicleTitle = trip.vehicle.displayTitle
    val regNumber = trip.vehicle.registrationNumber
    val durationText = trip.formattedDuration ?: "Active Trip"
    val distanceText = trip.formattedDistance ?: ""
    val pickupWalk = trip.formattedPickupDistance

    val accessibilityDesc = "Vehicle $vehicleTitle, registration $regNumber. " +
            "Route from ${trip.originName ?: trip.originAddress} to ${trip.destinationName ?: trip.destinationAddress}. " +
            "Travel duration $durationText. $pickupWalk to pickup."

    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, colors.primary, IshaaraTheme.shapes.card)
    } else {
        Modifier
    }

    IshaaraCard(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDesc
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            // Header Row: Vehicle details & Compatibility badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicleTitle,
                        style = typography.titleMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = regNumber,
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                }

                when (trip.compatibility) {
                    TripCompatibility.HIGH -> {
                        IshaaraBadge(text = "High Match", variant = IshaaraBadgeVariant.Success)
                    }
                    TripCompatibility.MEDIUM -> {
                        IshaaraBadge(text = "Direct Route", variant = IshaaraBadgeVariant.Neutral)
                    }
                    TripCompatibility.LOW -> {
                        IshaaraBadge(text = "Detour Route", variant = IshaaraBadgeVariant.Warning)
                    }
                    TripCompatibility.UNKNOWN -> Unit
                }
            }

            // Route representation
            Text(
                text = routeSummary,
                style = typography.bodyMedium,
                color = colors.foreground,
                maxLines = 2
            )

            // Metrics row: Duration • Distance • Pickup walk
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = durationText,
                    style = typography.titleSmall,
                    color = colors.primary
                )
                if (distanceText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(text = "•", style = typography.bodySmall, color = colors.foregroundSubtle)
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = distanceText,
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(text = "•", style = typography.bodySmall, color = colors.foregroundSubtle)
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = pickupWalk,
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            // Driver attribution and estimated fare (if returned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Driver: ${trip.driver.name}",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                if (trip.estimatedFare != null) {
                    Text(
                        text = "Est. ${trip.estimatedFare.formatted}",
                        style = typography.labelMedium,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // Selection CTA
            IshaaraButton(
                text = if (isSelected) "Selected" else "View Trip Details",
                onClick = onSelectClick,
                variant = if (isSelected) IshaaraButtonVariant.Secondary else IshaaraButtonVariant.Primary,
                size = IshaaraButtonSize.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
