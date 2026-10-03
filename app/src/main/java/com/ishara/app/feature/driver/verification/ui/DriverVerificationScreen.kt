package com.ishara.app.feature.driver.verification.ui

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
import androidx.compose.ui.text.font.FontWeight
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
import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.feature.driver.verification.DriverVerificationViewModel

/**
 * Driver Verification Review & Status Screen.
 * Reflects authoritative verification state machine from backend.
 * Distinguishes: PENDING, VERIFIED, REJECTED, and SUSPENDED states.
 */
@Composable
fun DriverVerificationScreen(
    viewModel: DriverVerificationViewModel,
    onSignOut: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToAgency: () -> Unit = {},
    onNavigateBack: () -> Unit = onNavigateToHome,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    val profile = uiState.profile
    val state = uiState.onboardingState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "←", color = colors.foreground, style = typography.titleSmall)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Verification Status",
                        style = typography.titleMedium,
                        color = colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Platform KYC & Regulatory Review",
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

        Spacer(modifier = Modifier.height(20.dp))

        // State-Specific Card
        when (state) {
            is DriverOnboardingState.Loading -> {
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Checking verification records...",
                            style = typography.bodyMedium,
                            color = colors.foregroundMuted
                        )
                    }
                }
            }

            is DriverOnboardingState.NeedsOnboarding,
            is DriverOnboardingState.NeedsOperatingTypeSelection -> {
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        IshaaraBadge(
                            text = "NOT REGISTERED",
                            variant = IshaaraBadgeVariant.Neutral
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Driver Profile Incomplete",
                            style = typography.titleMedium,
                            color = colors.foreground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You have not completed driver onboarding yet. Please provide your license and operating credentials.",
                            style = typography.bodyMedium,
                            color = colors.foregroundMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IshaaraButton(
                            text = "Complete Driver Onboarding",
                            onClick = onNavigateToOnboarding,
                            variant = IshaaraButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            is DriverOnboardingState.PendingVerification -> {
                PendingVerificationCard(
                    profile = state.profile,
                    onRefresh = { viewModel.refreshStatus() },
                    isRefreshing = uiState.isRefreshing,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToHome = onNavigateToHome
                )
            }

            is DriverOnboardingState.Verified -> {
                VerifiedCard(
                    profile = state.profile,
                    onProceed = onNavigateToHome
                )
            }

            is DriverOnboardingState.Rejected -> {
                RejectedCard(
                    profile = state.profile,
                    rejectionReason = state.reason,
                    notes = uiState.resubmissionNotes,
                    onNotesChange = { viewModel.onResubmissionNotesChanged(it) },
                    onSubmitResubmission = { viewModel.submitResubmission() },
                    isSubmitting = uiState.isSubmitting,
                    onNavigateToHome = onNavigateToHome
                )
            }

            is DriverOnboardingState.Suspended -> {
                SuspendedCard(
                    profile = state.profile,
                    reason = state.reason,
                    onNavigateToHome = onNavigateToHome
                )
            }

            is DriverOnboardingState.Error -> {
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        IshaaraBadge(
                            text = "ERROR",
                            variant = IshaaraBadgeVariant.Danger
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Unable to retrieve status",
                            style = typography.titleMedium,
                            color = colors.danger,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.error.message ?: "An unexpected error occurred.",
                            style = typography.bodyMedium,
                            color = colors.foregroundMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IshaaraButton(
                            text = "Retry",
                            onClick = { viewModel.refreshStatus() },
                            variant = IshaaraButtonVariant.Outlined,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            is DriverOnboardingState.Submitting -> {
                IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Submitting verification details...",
                            style = typography.bodyMedium,
                            color = colors.foregroundMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Success / Error Banners
        if (uiState.userFacingMessage != null) {
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.userFacingMessage ?: "",
                    style = typography.bodyMedium,
                    color = colors.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.userFacingError != null) {
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.userFacingError ?: "",
                    style = typography.bodyMedium,
                    color = colors.danger,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Informational Notice on Backend Verification Process
        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Verification Information",
                    style = typography.titleSmall,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Platform KYC verification reviews commercial driving credentials against platform guidelines. Physical document upload workflows will be enabled in an upcoming release (Phase A05/A06).",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                if (profile != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    IshaaraDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Masked License:",
                            style = typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = profile.licenseNumberMasked ?: "N/A",
                            style = typography.bodySmall,
                            color = colors.foreground,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Operating Type:",
                            style = typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = when {
                                profile.operatingType.equals("INDIVIDUAL", ignoreCase = true) -> "Individual"
                                profile.operatingType.equals("AGENCY", ignoreCase = true) -> "Fleet / Agency"
                                else -> profile.operatingType.ifEmpty { "Not set" }
                            },
                            style = typography.bodySmall,
                            color = colors.foreground,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (profile.operatingType.equals("INDIVIDUAL", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your operating type is already set to Individual.",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                    } else if (profile.operatingType.equals("AGENCY", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your operating type is set to Fleet / Agency.",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Refresh Button
        IshaaraButton(
            text = if (uiState.isRefreshing) "Refreshing..." else "Check for Updates",
            onClick = { viewModel.refreshStatus() },
            enabled = !uiState.isRefreshing,
            variant = IshaaraButtonVariant.Outlined,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PendingVerificationCard(
    profile: DriverProfile,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onNavigateToAgency: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            IshaaraBadge(
                text = "UNDER REVIEW",
                variant = IshaaraBadgeVariant.Warning
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Verification in Progress",
                style = typography.titleLarge,
                color = colors.foreground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your driver credentials have been submitted and are awaiting administrative verification. You will be notified once review is complete.",
                style = typography.bodyMedium,
                color = colors.foregroundMuted
            )
            if (profile.submittedAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Submitted: ${profile.submittedAt.take(10)}",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
            }

            if (profile.operatingType.equals("AGENCY", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(16.dp))
                IshaaraDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fleet & Agency Affiliation",
                    style = typography.titleSmall,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "As an agency driver, you can search for your transport fleet and submit an affiliation request while platform verification is pending.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                IshaaraButton(
                    text = "Join a Transport Fleet Agency",
                    onClick = onNavigateToAgency,
                    variant = IshaaraButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            IshaaraButton(
                text = "Go to Driver Home",
                onClick = onNavigateToHome,
                variant = IshaaraButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VerifiedCard(
    profile: DriverProfile,
    onProceed: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            IshaaraBadge(
                text = "VERIFIED",
                variant = IshaaraBadgeVariant.Success
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Platform Verification Approved",
                style = typography.titleLarge,
                color = colors.foreground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your commercial driving credentials have been verified by the platform administration. You can now access driver operations.",
                style = typography.bodyMedium,
                color = colors.foregroundMuted
            )
            if (profile.licenseVerifiedAt != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verified on: ${profile.licenseVerifiedAt.take(10)}",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            IshaaraButton(
                text = "Proceed to Driver Dashboard",
                onClick = onProceed,
                variant = IshaaraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RejectedCard(
    profile: DriverProfile,
    rejectionReason: String?,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSubmitResubmission: () -> Unit,
    isSubmitting: Boolean,
    onNavigateToHome: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            IshaaraBadge(
                text = "VERIFICATION REJECTED",
                variant = IshaaraBadgeVariant.Danger
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Verification Needs Attention",
                style = typography.titleLarge,
                color = colors.danger,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Rejection reason banner from backend
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.danger.copy(alpha = 0.08f))
                    .border(1.dp, colors.danger.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Reason from Administrator:",
                        style = typography.labelSmall,
                        color = colors.danger,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rejectionReason?.ifBlank { null } ?: "Credential validation failed. Please review your license details.",
                        style = typography.bodyMedium,
                        color = colors.foreground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Resubmit Verification",
                style = typography.titleSmall,
                color = colors.foreground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            IshaaraTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = "Notes / Clarification (Optional)",
                placeholder = "e.g. Corrected license number or updated expiry",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            IshaaraButton(
                text = if (isSubmitting) "Resubmitting..." else "Resubmit for Review",
                onClick = onSubmitResubmission,
                enabled = !isSubmitting,
                variant = IshaaraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            IshaaraButton(
                text = "Go to Driver Home",
                onClick = onNavigateToHome,
                variant = IshaaraButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SuspendedCard(
    profile: DriverProfile,
    reason: String?,
    onNavigateToHome: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            IshaaraBadge(
                text = "ACCOUNT RESTRICTED",
                variant = IshaaraBadgeVariant.Danger
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Driver Privileges Suspended",
                style = typography.titleLarge,
                color = colors.danger,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your driver account has been administratively suspended. Operating privileges and trip dispatch are restricted.",
                style = typography.bodyMedium,
                color = colors.foregroundMuted
            )
            if (!reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.danger.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Reason: $reason",
                        style = typography.bodySmall,
                        color = colors.danger
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please contact platform support to resolve any compliance issues.",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            IshaaraButton(
                text = "Go to Driver Home",
                onClick = onNavigateToHome,
                variant = IshaaraButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
