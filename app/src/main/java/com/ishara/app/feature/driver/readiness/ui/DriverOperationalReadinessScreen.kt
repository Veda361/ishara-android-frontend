package com.ishara.app.feature.driver.readiness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraStatusChip
import com.ishara.app.core.designsystem.component.IshaaraTransitStatus
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DriverOperationalReadiness
import com.ishara.app.domain.model.DriverReadinessBlocker
import com.ishara.app.domain.model.DriverReadinessBlockerAction
import com.ishara.app.domain.model.DriverReadinessReason
import com.ishara.app.domain.model.DriverReadinessRequirements
import com.ishara.app.domain.model.DriverReadinessStatus
import com.ishara.app.feature.driver.readiness.DriverOperationalReadinessViewModel
import com.ishara.app.feature.driver.readiness.DriverReadinessUiStage

/**
 * Dedicated Driver Operational Readiness Screen (Phase A06).
 *
 * Clearly answers: "Can this driver operate right now?"
 * Consumes authoritative backend evaluation without recreating business logic.
 */
@Composable
fun DriverOperationalReadinessScreen(
    viewModel: DriverOperationalReadinessViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToAgency: () -> Unit,
    onNavigateToVehicle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (val stage = uiState.stage) {
            is DriverReadinessUiStage.Loading -> {
                IshaaraLoadingState(message = "Evaluating operational readiness prerequisites...")
            }
            is DriverReadinessUiStage.Error -> {
                IshaaraErrorState(
                    title = if (!stage.canRetry) "Authorization Error" else "Evaluation Failed",
                    message = stage.error.message,
                    retryLabel = if (stage.canRetry) "Retry evaluation" else "Return to Dashboard",
                    onRetryClick = {
                        if (stage.canRetry) {
                            viewModel.retry()
                        } else {
                            onNavigateBack()
                        }
                    }
                )
            }
            is DriverReadinessUiStage.Offline -> {
                IshaaraErrorState(
                    title = "Network Offline",
                    message = "Unable to evaluate authoritative readiness while offline. Please verify your connection.",
                    retryLabel = "Retry",
                    onRetryClick = { viewModel.retry() }
                )
            }
            is DriverReadinessUiStage.Ready -> {
                ReadinessContent(
                    readiness = stage.readiness,
                    blockers = emptyList(),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    onNavigateBack = onNavigateBack,
                    onNavigateToVerification = onNavigateToVerification,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToVehicle = onNavigateToVehicle
                )
            }
            is DriverReadinessUiStage.NotReady -> {
                ReadinessContent(
                    readiness = stage.readiness,
                    blockers = stage.blockers,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    onNavigateBack = onNavigateBack,
                    onNavigateToVerification = onNavigateToVerification,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToVehicle = onNavigateToVehicle
                )
            }
            is DriverReadinessUiStage.Suspended -> {
                ReadinessContent(
                    readiness = stage.readiness,
                    blockers = listOf(
                        DriverReadinessBlocker(
                            reason = DriverReadinessReason.DRIVER_SUSPENDED,
                            title = "Account Suspended",
                            description = stage.reason ?: "Your operational privileges have been restricted by platform administration.",
                            actionType = DriverReadinessBlockerAction.ADMIN_RESTRICTED
                        )
                    ),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    onNavigateBack = onNavigateBack,
                    onNavigateToVerification = onNavigateToVerification,
                    onNavigateToAgency = onNavigateToAgency,
                    onNavigateToVehicle = onNavigateToVehicle
                )
            }
        }
    }
}

@Composable
private fun ReadinessContent(
    readiness: DriverOperationalReadiness,
    blockers: List<DriverReadinessBlocker>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToAgency: () -> Unit,
    onNavigateToVehicle: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // Top Navigation & Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IshaaraButton(
                text = "← Back",
                onClick = onNavigateBack,
                variant = IshaaraButtonVariant.Text,
                size = IshaaraButtonSize.Small
            )

            IshaaraButton(
                text = if (isRefreshing) "Refreshing..." else "Refresh",
                onClick = onRefresh,
                variant = IshaaraButtonVariant.Outlined,
                size = IshaaraButtonSize.Small,
                enabled = !isRefreshing,
                loading = isRefreshing
            )
        }

        // Section Title
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = "OPERATIONAL READINESS",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )
            Text(
                text = "Driver Eligibility & Status",
                style = typography.headlineSmall,
                color = colors.foreground
            )
            Text(
                text = "Authoritative platform gate determining whether you may enter operational mode and accept transit rides.",
                style = typography.bodySmall,
                color = colors.foregroundSubtle
            )
        }

        // 1. Hero Status Card
        HeroReadinessCard(readiness = readiness)

        // 2. Actionable Blockers (if any)
        if (blockers.isNotEmpty()) {
            BlockersSection(
                blockers = blockers,
                onNavigateToVerification = onNavigateToVerification,
                onNavigateToAgency = onNavigateToAgency
            )
        }

        // 3. Platform Prerequisites Checklist Card
        RequirementsChecklistCard(
            requirements = readiness.requirements,
            operatingType = readiness.operatingType,
            agency = readiness.agency
        )

        // 4. Vehicle Telemetry & Informational Section
        VehicleTelemetryCard(
            activeVehicle = readiness.activeVehicle,
            onNavigateToVehicle = onNavigateToVehicle
        )

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun HeroReadinessCard(readiness: DriverOperationalReadiness) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val (badgeText, badgeVariant, titleText, descText) = when {
        readiness.isSuspended -> {
            Quad(
                "SUSPENDED",
                IshaaraBadgeVariant.Danger,
                "Operational Access Restricted",
                "Your account is currently suspended from operations by platform administration. Operational mode cannot be enabled."
            )
        }
        readiness.isReady -> {
            Quad(
                "READY TO OPERATE",
                IshaaraBadgeVariant.Success,
                "Eligible for Operational Mode",
                "You meet all platform requirements. You are fully authorized to go online and accept transit operations."
            )
        }
        else -> {
            Quad(
                "NOT READY",
                IshaaraBadgeVariant.Warning,
                "Action Required Before Operating",
                "You cannot enter operational mode yet. Complete the pending requirements listed below to become eligible."
            )
        }
    }

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATUS GATE",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
                IshaaraBadge(text = badgeText, variant = badgeVariant)
            }

            Text(
                text = titleText,
                style = typography.titleLarge,
                color = colors.foreground
            )

            Text(
                text = descText,
                style = typography.bodyMedium,
                color = colors.foregroundSubtle
            )
        }
    }
}

@Composable
private fun BlockersSection(
    blockers: List<DriverReadinessBlocker>,
    onNavigateToVerification: () -> Unit,
    onNavigateToAgency: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
            text = "PENDING REQUIREMENTS (${blockers.size})",
            style = typography.labelMedium,
            color = colors.foregroundMuted
        )

        blockers.forEach { blocker ->
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = blocker.title,
                        style = typography.titleSmall,
                        color = colors.foreground
                    )
                    Text(
                        text = blocker.description,
                        style = typography.bodySmall,
                        color = colors.foregroundSubtle
                    )

                    Spacer(modifier = Modifier.height(spacing.xxs))

                    when (blocker.actionType) {
                        DriverReadinessBlockerAction.NAVIGATE_VERIFICATION -> {
                            IshaaraButton(
                                text = "Go to Verification",
                                onClick = onNavigateToVerification,
                                variant = IshaaraButtonVariant.Primary,
                                size = IshaaraButtonSize.Small,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        DriverReadinessBlockerAction.NAVIGATE_AGENCY -> {
                            IshaaraButton(
                                text = "Manage Agency Affiliation",
                                onClick = onNavigateToAgency,
                                variant = IshaaraButtonVariant.Primary,
                                size = IshaaraButtonSize.Small,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        DriverReadinessBlockerAction.VEHICLE_DEFERRED -> {
                            Text(
                                text = "Vehicle registration and assignment is handled under Phase A07.",
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                        DriverReadinessBlockerAction.ADMIN_RESTRICTED -> {
                            Text(
                                text = "Contact support regarding administrative restrictions.",
                                style = typography.bodySmall,
                                color = colors.danger
                            )
                        }
                        DriverReadinessBlockerAction.COMPLETE_PROFILE -> {
                            Text(
                                text = "License number required on profile.",
                                style = typography.bodySmall,
                                color = colors.warning
                            )
                        }
                        DriverReadinessBlockerAction.NONE -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun RequirementsChecklistCard(
    requirements: DriverReadinessRequirements,
    operatingType: String,
    agency: com.ishara.app.domain.model.DriverReadinessAgency?
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "PREREQUISITES BREAKDOWN",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )

            ChecklistItem(
                label = "Platform KYC Verification",
                isSatisfied = requirements.platformVerification,
                detail = if (requirements.platformVerification) "Approved by Administration" else "Verification Pending / Rejected"
            )

            ChecklistItem(
                label = "Agency Fleet Membership",
                isSatisfied = requirements.agencyMembership,
                detail = when {
                    operatingType.equals("INDIVIDUAL", ignoreCase = true) -> "Not required (Individual Driver)"
                    agency?.membershipStatus.equals("APPROVED", ignoreCase = true) -> "Approved (${agency?.agencyName ?: "Agency"})"
                    agency?.membershipStatus.equals("PENDING", ignoreCase = true) -> "Pending Approval (${agency?.agencyName ?: "Agency"})"
                    else -> "Required for Fleet Operations"
                }
            )

            ChecklistItem(
                label = "Driver Profile Completeness",
                isSatisfied = requirements.profileComplete,
                detail = if (requirements.profileComplete) "License details provided" else "License details required"
            )

            ChecklistItem(
                label = "Operational Standing",
                isSatisfied = requirements.notSuspended,
                detail = if (requirements.notSuspended) "Account in Good Standing" else "Account Administratively Suspended"
            )

            ChecklistItem(
                label = "Vehicle Assignment (Phase 08 Telemetry)",
                isSatisfied = requirements.vehicleAssigned,
                detail = if (requirements.vehicleAssigned) "Active Vehicle Assigned" else "No active vehicle assigned (Informational)"
            )
        }
    }
}

@Composable
private fun ChecklistItem(
    label: String,
    isSatisfied: Boolean,
    detail: String
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = typography.bodyMedium,
                color = colors.foreground
            )
            Text(
                text = detail,
                style = typography.bodySmall,
                color = colors.foregroundSubtle
            )
        }

        IshaaraBadge(
            text = if (isSatisfied) "PASSED" else "PENDING",
            variant = if (isSatisfied) IshaaraBadgeVariant.Success else IshaaraBadgeVariant.Warning
        )
    }
}

@Composable
private fun VehicleTelemetryCard(
    activeVehicle: com.ishara.app.domain.model.DriverReadinessVehicle?,
    onNavigateToVehicle: () -> Unit = {}
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = "ASSIGNED VEHICLE TELEMETRY",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )

            if (activeVehicle != null) {
                Text(
                    text = "${activeVehicle.make ?: ""} ${activeVehicle.model ?: ""}".trim().ifEmpty { "Assigned Vehicle" },
                    style = typography.titleSmall,
                    color = colors.foreground
                )
                Text(
                    text = "Reg: ${activeVehicle.registrationNumber}",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
            } else {
                Text(
                    text = "No vehicle currently assigned",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
                Text(
                    text = "Vehicle assignment is managed under Phase A07. Readiness gate does not block on vehicle assignment to avoid circular dependencies.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))
            IshaaraButton(
                text = if (activeVehicle != null) "View Vehicle Assignment" else "Manage Vehicle Assignment",
                onClick = onNavigateToVehicle,
                variant = IshaaraButtonVariant.Outlined,
                size = IshaaraButtonSize.Small,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
