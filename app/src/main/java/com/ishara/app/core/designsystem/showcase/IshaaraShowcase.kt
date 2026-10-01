package com.ishara.app.core.designsystem.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.components.IshaaraBadge
import com.ishara.app.core.designsystem.components.IshaaraButton
import com.ishara.app.core.designsystem.components.IshaaraButtonSize
import com.ishara.app.core.designsystem.components.IshaaraButtonVariant
import com.ishara.app.core.designsystem.components.IshaaraCard
import com.ishara.app.core.designsystem.components.IshaaraDivider
import com.ishara.app.core.designsystem.components.IshaaraDriverCard
import com.ishara.app.core.designsystem.components.IshaaraOfflineIndicator
import com.ishara.app.core.designsystem.components.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.components.IshaaraSearchField
import com.ishara.app.core.designsystem.components.IshaaraSectionHeader
import com.ishara.app.core.designsystem.components.IshaaraSkeleton
import com.ishara.app.core.designsystem.components.IshaaraStatusChip
import com.ishara.app.core.designsystem.components.IshaaraStatusIndicator
import com.ishara.app.core.designsystem.components.IshaaraTextField
import com.ishara.app.core.designsystem.components.IshaaraTopBar
import com.ishara.app.core.designsystem.components.IshaaraTransitStatus
import com.ishara.app.core.designsystem.components.IshaaraTripCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Human-Centered Component Showcase demonstrating real-world Indian mobility UI patterns.
 */
@Composable
fun IshaaraDesignSystemShowcase(
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    var sampleSearchText by remember { mutableStateOf("") }
    var sampleInputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        IshaaraTopBar(
            title = "Design system showcase",
            subtitle = "Human-centered mobility foundation"
        )

        IshaaraOfflineIndicator()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            // Section 1: Trip & Route Cards (Student Experience)
            IshaaraSectionHeader(title = "Available trips", number = "2 found")
            IshaaraTripCard(
                route = "Bus 24 • Express",
                originToDestination = "Jhansi → Orchha",
                etaText = "8 min away",
                fareText = "₹20",
                seatsAvailable = 12,
                onRequestClick = {},
                status = IshaaraTransitStatus.COMING
            )

            IshaaraTripCard(
                route = "Bus 12 • Campus Shuttle",
                originToDestination = "Railway Station → Campus Gate",
                etaText = "3 min away",
                fareText = "₹15",
                seatsAvailable = 3,
                onRequestClick = {},
                status = IshaaraTransitStatus.ARRIVING
            )

            // Section 2: Driver Information Card
            IshaaraSectionHeader(title = "Assigned driver")
            IshaaraDriverCard(
                driverName = "Rajesh Sharma",
                rating = 4.8,
                vehicleDetails = "Tata Starbus • MH 12 AB 9999",
                onCallClick = {},
                status = IshaaraTransitStatus.ONLINE
            )

            // Section 3: Status & Natural Language
            IshaaraSectionHeader(title = "Status language")
            IshaaraCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IshaaraStatusChip(status = IshaaraTransitStatus.ONLINE)
                        IshaaraStatusChip(status = IshaaraTransitStatus.ARRIVING)
                        IshaaraStatusChip(status = IshaaraTransitStatus.IN_PROGRESS)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IshaaraStatusChip(status = IshaaraTransitStatus.PENDING)
                        IshaaraStatusChip(status = IshaaraTransitStatus.OFFLINE)
                        IshaaraBadge(text = "AC Bus")
                    }
                    IshaaraDivider()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IshaaraStatusIndicator(status = IshaaraTransitStatus.ONLINE)
                        IshaaraStatusIndicator(status = IshaaraTransitStatus.IN_PROGRESS)
                        IshaaraStatusIndicator(status = IshaaraTransitStatus.OFFLINE)
                    }
                }
            }

            // Section 4: Primary & Secondary Actions
            IshaaraSectionHeader(title = "Action buttons")
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                // One primary action (54dp large for driver/student thumb reach)
                IshaaraPrimaryAction(
                    text = "Request ride",
                    onClick = {}
                )

                IshaaraButton(
                    text = "Start trip",
                    onClick = {},
                    variant = IshaaraButtonVariant.Primary,
                    size = IshaaraButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )

                IshaaraButton(
                    text = "View route stops",
                    onClick = {},
                    variant = IshaaraButtonVariant.Outlined,
                    size = IshaaraButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IshaaraButton(
                        text = "Emergency SOS",
                        onClick = {},
                        variant = IshaaraButtonVariant.Danger,
                        size = IshaaraButtonSize.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IshaaraButton(
                        text = "Cancel",
                        onClick = {},
                        variant = IshaaraButtonVariant.Text,
                        size = IshaaraButtonSize.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 5: Search & Input
            IshaaraSectionHeader(title = "Search & inputs")
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                IshaaraSearchField(
                    query = sampleSearchText,
                    onQueryChange = { sampleSearchText = it },
                    placeholder = "Where are you going?",
                    onClearClick = { sampleSearchText = "" }
                )

                IshaaraTextField(
                    value = sampleInputText,
                    onValueChange = { sampleInputText = it },
                    label = "Pickup stop",
                    placeholder = "e.g. Library Gate"
                )
            }

            // Section 6: Loading Placeholder
            IshaaraSectionHeader(title = "Loading placeholder")
            IshaaraCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    IshaaraSkeleton(height = 20.dp)
                    IshaaraSkeleton(height = 16.dp)
                    IshaaraSkeleton(height = 40.dp)
                }
            }

            Spacer(modifier = Modifier.height(spacing.xxxl))
        }
    }
}

@Preview(name = "Ishaara Showcase - Light Mode", showBackground = true)
@Composable
fun IshaaraShowcaseLightPreview() {
    IshaaraTheme(darkTheme = false) {
        IshaaraDesignSystemShowcase()
    }
}

@Preview(name = "Ishaara Showcase - Dark Mode", showBackground = true)
@Composable
fun IshaaraShowcaseDarkPreview() {
    IshaaraTheme(darkTheme = true) {
        IshaaraDesignSystemShowcase()
    }
}
