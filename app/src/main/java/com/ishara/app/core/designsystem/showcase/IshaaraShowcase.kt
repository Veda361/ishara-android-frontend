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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ishara.app.core.designsystem.component.IshaaraAvatar
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraDivider
import com.ishara.app.core.designsystem.component.IshaaraEmptyState
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraIconButton
import com.ishara.app.core.designsystem.component.IshaaraOfflineIndicator
import com.ishara.app.core.designsystem.component.IshaaraOutlinedCard
import com.ishara.app.core.designsystem.component.IshaaraSearchField
import com.ishara.app.core.designsystem.component.IshaaraSectionHeader
import com.ishara.app.core.designsystem.component.IshaaraSkeleton
import com.ishara.app.core.designsystem.component.IshaaraStatusBadge
import com.ishara.app.core.designsystem.component.IshaaraStatusIndicator
import com.ishara.app.core.designsystem.component.IshaaraTechnicalLabel
import com.ishara.app.core.designsystem.component.IshaaraTechnicalValue
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme

/**
 * Development-only Showcase demonstrating all Ishaara Design System tokens and primitives.
 */
@Composable
fun IshaaraDesignSystemShowcase(
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    var sampleInputText by remember { mutableStateOf("") }
    var sampleSearchText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        IshaaraTopBar(
            title = "Design System Showcase",
            systemTag = "TELEMETRY // SHOWCASE V1.0"
        )

        IshaaraOfflineIndicator()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            // Section 01: Technical Labels & Values
            IshaaraSectionHeader(number = "01", title = "Instrumentation & Technical Labels")
            IshaaraCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IshaaraTechnicalLabel(text = "SYSTEM LIVE", bordered = true)
                        IshaaraTechnicalLabel(text = "ROUTE 01", bordered = true)
                        IshaaraTechnicalLabel(text = "ETA 06 MIN", bordered = true, color = colors.accent)
                    }
                    IshaaraDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IshaaraTechnicalValue(value = "₹25", unit = "base fare")
                        IshaaraTechnicalValue(value = "08", unit = "stops left", highlight = true)
                        IshaaraTechnicalValue(value = "34", unit = "seats open")
                    }
                }
            }

            // Section 02: Status System
            IshaaraSectionHeader(number = "02", title = "Status System")
            IshaaraCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        IshaaraStatusBadge(status = IshaaraTransitStatus.ONLINE)
                        IshaaraStatusBadge(status = IshaaraTransitStatus.ARRIVING)
                        IshaaraStatusBadge(status = IshaaraTransitStatus.IN_PROGRESS)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        IshaaraStatusBadge(status = IshaaraTransitStatus.OFFLINE)
                        IshaaraStatusBadge(status = IshaaraTransitStatus.CANCELLED)
                        IshaaraBadge(text = "VOLVO AC 9400")
                    }
                    IshaaraDivider()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IshaaraStatusIndicator(status = IshaaraTransitStatus.ONLINE)
                        IshaaraStatusIndicator(status = IshaaraTransitStatus.ARRIVING)
                        IshaaraStatusIndicator(status = IshaaraTransitStatus.OFFLINE)
                    }
                }
            }

            // Section 03: Button Hierarchy
            IshaaraSectionHeader(number = "03", title = "Button System")
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                IshaaraButton(
                    text = "CONFIRM TRIP ROUTE",
                    onClick = {},
                    variant = IshaaraButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                IshaaraButton(
                    text = "SECONDARY ACTION",
                    onClick = {},
                    variant = IshaaraButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
                IshaaraButton(
                    text = "OUTLINED BOARDING ACTION",
                    onClick = {},
                    variant = IshaaraButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IshaaraButton(
                        text = "EMERGENCY SOS",
                        onClick = {},
                        variant = IshaaraButtonVariant.Danger,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                    IshaaraButton(
                        text = "SYNCING...",
                        onClick = {},
                        loading = true,
                        size = IshaaraButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 04: Form & Search Inputs
            IshaaraSectionHeader(number = "04", title = "Input & Transit Search")
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                IshaaraSearchField(
                    query = sampleSearchText,
                    onQueryChange = { sampleSearchText = it },
                    onClearClick = { sampleSearchText = "" }
                )
                IshaaraTextField(
                    value = sampleInputText,
                    onValueChange = { sampleInputText = it },
                    label = "Pickup Landmark",
                    placeholder = "e.g. Campus Gate No. 2"
                )
                IshaaraTextField(
                    value = "MH 12 AB 9999",
                    onValueChange = {},
                    label = "Vehicle Registration",
                    readOnly = true,
                    helperText = "Verified Fleet Bus"
                )
            }

            // Section 05: Cards, Skeletons & Avatars
            IshaaraSectionHeader(number = "05", title = "Cards & Telemetry Elements")
            IshaaraOutlinedCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IshaaraAvatar(name = "Rajesh Sharma", status = IshaaraTransitStatus.ONLINE)
                            Column {
                                Text(text = "Rajesh Sharma", style = typography.titleMedium, color = colors.foreground)
                                Text(text = "Driver • 8 yrs exp", style = typography.bodySmall, color = colors.foregroundMuted)
                            }
                        }
                        IshaaraIconButton(
                            onClick = {},
                            contentDescription = "Contact driver"
                        ) {
                            Text(text = "📞", style = typography.titleMedium)
                        }
                    }
                    IshaaraDivider()
                    IshaaraSkeleton(height = 14.dp)
                    IshaaraSkeleton(height = 14.dp)
                }
            }

            Spacer(modifier = Modifier.height(spacing.xxl))
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
