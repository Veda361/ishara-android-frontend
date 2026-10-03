package com.ishara.app.feature.driver.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.feature.driver.onboarding.DriverOnboardingUiState
import com.ishara.app.feature.driver.onboarding.DriverOnboardingViewModel
import com.ishara.app.feature.driver.onboarding.DriverOperatingTypeChoice

/**
 * Production Driver Operational Classification Selection Screen.
 *
 * Requirements:
 * - Clear heading: "How will you operate?"
 * - Explanatory subtitle: "Choose how you want to provide transport services on ISHAARA."
 * - Option 1: INDIVIDUAL ("Operate independently using your own vehicle.")
 * - Option 2: AGENCY / FLEET ("Operate through a registered transport agency/fleet.")
 * - Both choices visually distinct with clear selected states.
 * - [ Continue ] button strictly disabled until an option is explicitly chosen.
 * - Zero silent preselection of INDIVIDUAL.
 */
@Composable
fun DriverOperatingTypeScreen(
    viewModel: DriverOnboardingViewModel,
    onSignOut: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IshaaraBadge(
                text = "DRIVER SETUP",
                variant = IshaaraBadgeVariant.Primary
            )
            Text(
                text = "Sign Out",
                style = typography.labelMedium,
                color = colors.danger,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSignOut() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Screen Title & Subtitle
        Text(
            text = "How will you operate?",
            style = typography.headlineMedium,
            color = colors.foreground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose how you want to provide transport services on ISHAARA.",
            style = typography.bodyMedium,
            color = colors.foregroundMuted
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Operating Type Option 1: INDIVIDUAL
        val isIndividualSelected = uiState.selectedOperatingType == DriverOperatingTypeChoice.INDIVIDUAL
        OperatingTypeOptionCard(
            title = "INDIVIDUAL",
            badgeText = "Independent",
            description = "Operate independently using your own vehicle.",
            detail = "Directly manage your vehicle, trips, and passenger fares without agency oversight.",
            isSelected = isIndividualSelected,
            onClick = { viewModel.selectOperatingType(DriverOperatingTypeChoice.INDIVIDUAL) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Operating Type Option 2: AGENCY / FLEET
        val isAgencySelected = uiState.selectedOperatingType == DriverOperatingTypeChoice.AGENCY
        OperatingTypeOptionCard(
            title = "AGENCY / FLEET",
            badgeText = "Fleet Affiliated",
            description = "Operate through a registered transport agency/fleet.",
            detail = "Associate with an accredited transit organization or fleet operator for vehicle assignments.",
            isSelected = isAgencySelected,
            onClick = { viewModel.selectOperatingType(DriverOperatingTypeChoice.AGENCY) }
        )

        if (uiState.operatingTypeError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.operatingTypeError ?: "",
                style = typography.bodySmall,
                color = colors.danger
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button: Disabled until an explicit choice is made
        IshaaraButton(
            text = "Continue",
            onClick = {
                if (uiState.isOperatingTypeSelected) {
                    onContinue()
                }
            },
            enabled = uiState.isOperatingTypeSelected,
            variant = IshaaraButtonVariant.Primary,
            size = IshaaraButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OperatingTypeOptionCard(
    title: String,
    badgeText: String,
    description: String,
    detail: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.border,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface
            )
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Radio Selection Indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 6.dp else 2.dp,
                                color = if (isSelected) colors.primary else colors.border,
                                shape = CircleShape
                            )
                            .background(if (isSelected) colors.surface else androidx.compose.ui.graphics.Color.Transparent)
                    )

                    Text(
                        text = title,
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.primary else colors.foreground
                    )
                }

                IshaaraBadge(
                    text = badgeText,
                    variant = if (isSelected) IshaaraBadgeVariant.Primary else IshaaraBadgeVariant.Neutral
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.foreground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = detail,
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )
        }
    }
}
