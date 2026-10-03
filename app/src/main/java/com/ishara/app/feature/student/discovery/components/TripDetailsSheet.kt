package com.ishara.app.feature.student.discovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraBottomSheet
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.TripCompatibility

/**
 * Bottom Sheet displaying full verified details for the selected trip.
 *
 * Provides a clean hand-off boundary to Phase 07 (Ride Request).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsSheet(
    trip: DiscoveredTrip,
    onDismissRequest: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header: Vehicle Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = trip.vehicle.displayTitle,
                        style = typography.titleLarge,
                        color = colors.foreground
                    )
                    Text(
                        text = "Registration: ${trip.vehicle.registrationNumber}",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }

                when (trip.compatibility) {
                    TripCompatibility.HIGH -> IshaaraBadge(text = "High Match", variant = IshaaraBadgeVariant.Success)
                    TripCompatibility.MEDIUM -> IshaaraBadge(text = "Direct Route", variant = IshaaraBadgeVariant.Neutral)
                    TripCompatibility.LOW -> IshaaraBadge(text = "Detour Route", variant = IshaaraBadgeVariant.Warning)
                    TripCompatibility.UNKNOWN -> Unit
                }
            }

            // Route Details Card
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    // Origin
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colors.primary, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Pickup Location",
                                style = typography.labelSmall,
                                color = colors.foregroundMuted
                            )
                            Text(
                                text = trip.originName ?: trip.originAddress,
                                style = typography.bodyMedium,
                                color = colors.foreground
                            )
                        }
                    }

                    // Destination
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colors.accent, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Dropoff Destination",
                                style = typography.labelSmall,
                                color = colors.foregroundMuted
                            )
                            Text(
                                text = trip.destinationName ?: trip.destinationAddress,
                                style = typography.bodyMedium,
                                color = colors.foreground
                            )
                        }
                    }
                }
            }

            // Metrics row: Duration, Distance, Pickup walk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Est. Duration", style = typography.labelSmall, color = colors.foregroundMuted)
                    Text(text = trip.formattedDuration ?: "Active", style = typography.titleSmall, color = colors.foreground)
                }
                Column {
                    Text(text = "Distance", style = typography.labelSmall, color = colors.foregroundMuted)
                    Text(text = trip.formattedDistance ?: "Direct", style = typography.titleSmall, color = colors.foreground)
                }
                Column {
                    Text(text = "Walk to Pickup", style = typography.labelSmall, color = colors.foregroundMuted)
                    Text(text = trip.formattedPickupDistance, style = typography.titleSmall, color = colors.foreground)
                }
            }

            // Driver attribution
            Text(
                text = "Driver: ${trip.driver.name} (Verified Operator)",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            // Estimated fare if present in discovery metadata
            if (trip.estimatedFare != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Fare",
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                    Text(
                        text = trip.estimatedFare.formatted,
                        style = typography.titleSmall,
                        color = colors.primary
                    )
                }
            }

            // Notice regarding shared transit and fare calculation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, IshaaraTheme.shapes.card)
                    .padding(spacing.sm)
            ) {
                Text(
                    text = "Shared campus transit. Authoritative fare is confirmed upon booking request in Phase A10.",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // Primary 54dp CTA Button: "Select Trip for Ride Request"
            IshaaraButton(
                text = "Select Trip for Ride Request",
                onClick = onContinueClick,
                variant = IshaaraButtonVariant.Primary,
                size = IshaaraButtonSize.Large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

        }
    }
}
