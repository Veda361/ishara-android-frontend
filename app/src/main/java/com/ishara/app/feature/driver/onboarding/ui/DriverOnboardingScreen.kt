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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraDivider
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.feature.driver.onboarding.DriverOnboardingViewModel

/**
 * Driver Onboarding & Registration Screen.
 * Collects authoritative driver credentials required to provision the DriverProfile.
 */
@Composable
fun DriverOnboardingScreen(
    viewModel: DriverOnboardingViewModel,
    onSignOut: () -> Unit,
    onOnboardingComplete: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "←", color = colors.foreground, style = typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Driver Registration",
                        style = typography.headlineSmall,
                        color = colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Profile & Commercial Credentials",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }
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

        Spacer(modifier = Modifier.height(16.dp))

        // Info Banner
        IshaaraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IshaaraBadge(
                        text = "DRIVER ONBOARDING",
                        variant = IshaaraBadgeVariant.Primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Provide your commercial driving license and operational details. Once submitted, your credentials will be submitted for platform verification.",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Credentials Form
        Text(
            text = "License Details",
            style = typography.titleMedium,
            color = colors.foreground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        IshaaraTextField(
            value = uiState.licenseNumber,
            onValueChange = { viewModel.onLicenseNumberChanged(it) },
            label = "Driving License Number *",
            placeholder = "e.g. MH1220260012345",
            errorText = uiState.licenseNumberError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        IshaaraTextField(
            value = uiState.yearsOfExperience,
            onValueChange = { viewModel.onYearsOfExperienceChanged(it) },
            label = "Years of Driving Experience",
            placeholder = "e.g. 5",
            errorText = uiState.yearsOfExperienceError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        IshaaraDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // Operating Type Selection
        Text(
            text = "Operating Type *",
            style = typography.titleMedium,
            color = colors.foreground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose your operating mode on the platform",
            style = typography.bodySmall,
            color = colors.foregroundMuted
        )
        Spacer(modifier = Modifier.height(10.dp))

        val isIndividual = uiState.selectedOperatingType == com.ishara.app.feature.driver.onboarding.DriverOperatingTypeChoice.INDIVIDUAL
        val isAgency = uiState.selectedOperatingType == com.ishara.app.feature.driver.onboarding.DriverOperatingTypeChoice.AGENCY

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isIndividual) 2.dp else 1.dp,
                        color = if (isIndividual) colors.primary else colors.border,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(if (isIndividual) colors.primary.copy(alpha = 0.08f) else colors.surface)
                    .clickable { viewModel.selectOperatingType(com.ishara.app.feature.driver.onboarding.DriverOperatingTypeChoice.INDIVIDUAL) }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Individual",
                        style = typography.titleSmall,
                        color = if (isIndividual) colors.primary else colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Independent driver",
                        style = typography.labelSmall,
                        color = colors.foregroundMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isAgency) 2.dp else 1.dp,
                        color = if (isAgency) colors.primary else colors.border,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(if (isAgency) colors.primary.copy(alpha = 0.08f) else colors.surface)
                    .clickable { viewModel.selectOperatingType(com.ishara.app.feature.driver.onboarding.DriverOperatingTypeChoice.AGENCY) }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Fleet / Agency",
                        style = typography.titleSmall,
                        color = if (isAgency) colors.primary else colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Under transport agency",
                        style = typography.labelSmall,
                        color = colors.foregroundMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (uiState.operatingTypeError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.operatingTypeError ?: "",
                style = typography.bodySmall,
                color = colors.danger
            )
        } else if (isAgency) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "For drivers who operate under a transport agency. You will be able to apply to join your agency fleet after profile submission.",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )
        } else if (isIndividual) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "For independent drivers operating your own vehicle directly.",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        IshaaraDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // Emergency Contact Form (Optional)
        Text(
            text = "Emergency Contact (Optional)",
            style = typography.titleMedium,
            color = colors.foreground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        IshaaraTextField(
            value = uiState.emergencyContactName,
            onValueChange = { viewModel.onEmergencyContactNameChanged(it) },
            label = "Contact Name",
            placeholder = "e.g. Rajesh Kumar",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        IshaaraTextField(
            value = uiState.emergencyContactPhone,
            onValueChange = { viewModel.onEmergencyContactPhoneChanged(it) },
            label = "Phone Number",
            placeholder = "e.g. +919876543210",
            errorText = uiState.emergencyPhoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        IshaaraTextField(
            value = uiState.emergencyContactRelationship,
            onValueChange = { viewModel.onEmergencyContactRelationshipChanged(it) },
            label = "Relationship",
            placeholder = "e.g. Spouse, Brother, Parent",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error message banner
        if (uiState.errorMessage != null) {
            IshaaraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = typography.bodyMedium,
                    color = colors.danger,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        IshaaraButton(
            text = if (uiState.isLoading) "Submitting..." else "Submit Registration",
            onClick = {
                viewModel.submitOnboarding(onSuccess = onOnboardingComplete)
            },
            enabled = uiState.isFormValid && !uiState.isLoading,
            variant = IshaaraButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
