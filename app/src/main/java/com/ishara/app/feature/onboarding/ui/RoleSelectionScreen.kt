package com.ishara.app.feature.onboarding.ui

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.UserRole
import com.ishara.app.feature.onboarding.OnboardingUiState
import com.ishara.app.feature.onboarding.OnboardingViewModel

/**
 * Human-centered Role Selection Screen for Ishaara.
 * Strictly adheres to Phase 02 and Phase 04 UX guidelines:
 * - Simple, clean, high-readability typography
 * - Plain language: "Student / Passenger" vs "Driver / Conductor"
 * - Large touch targets (>54dp) and clear selection states
 * - Inline validation ("Choose how you'll use Ishaara.")
 * - TalkBack accessibility labels
 */
@Composable
fun RoleSelectionScreen(
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RoleSelectionContent(
        uiState = uiState,
        onRoleSelected = { viewModel.selectRole(it) },
        onSubmitClick = { viewModel.submitOnboarding() },
        onRetryClick = { viewModel.retry() },
        modifier = modifier
    )
}

@Composable
fun RoleSelectionContent(
    uiState: OnboardingUiState,
    onRoleSelected: (UserRole) -> Unit,
    onSubmitClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.md)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IshaaraBadge(
                    text = "STEP 1 OF 1 • ACCOUNT SETUP",
                    modifier = Modifier.padding(bottom = spacing.sm)
                )

                Text(
                    text = "How will you use Ishaara?",
                    style = typography.headlineLarge,
                    color = colors.foreground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Choose the option that best describes you.",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Role Options Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                // Role 1: Student / Passenger
                RoleCard(
                    title = "Student / Passenger",
                    subtitle = "Find available trips, request rides, and track your journey.",
                    badgeText = "COMMUTER",
                    isSelected = uiState.selectedRole == UserRole.USER,
                    onClick = { onRoleSelected(UserRole.USER) }
                )

                // Role 2: Driver / Conductor
                RoleCard(
                    title = "Driver / Conductor",
                    subtitle = "Manage trips, passengers, and vehicle operations.",
                    badgeText = "TRANSIT OPERATOR",
                    isSelected = uiState.selectedRole == UserRole.DRIVER_CONDUCTOR,
                    onClick = { onRoleSelected(UserRole.DRIVER_CONDUCTOR) }
                )

                // Validation Error Alert
                if (uiState.validationError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xs)
                            .background(colors.warningSubtle, shape = IshaaraTheme.shapes.md)
                            .padding(spacing.md)
                    ) {
                        Text(
                            text = uiState.validationError,
                            style = typography.bodyMedium,
                            color = colors.warning,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Submission Error Alert
                if (uiState.errorMessage != null) {
                    IshaaraErrorState(
                        title = "Couldn't complete setup",
                        message = uiState.errorMessage,
                        retryLabel = "Try again",
                        onRetryClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom Continue Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IshaaraPrimaryAction(
                    text = if (uiState.isSubmitting) "Saving..." else "Continue",
                    onClick = onSubmitClick,
                    enabled = !uiState.isSubmitting,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                Text(
                    text = "Your role determines your workspace layout and transit features.",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.sm)
                )
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    badgeText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    val borderColor = if (isSelected) colors.accent else colors.border
    val backgroundColor = if (isSelected) colors.surfaceElevated else colors.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(IshaaraTheme.shapes.md)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = IshaaraTheme.shapes.md
            )
            .clickable(onClick = onClick)
            .padding(spacing.md)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "$title. $subtitle"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) colors.accent else colors.foregroundSubtle,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.accent, shape = CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.md))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = typography.headlineSmall,
                        color = colors.foreground
                    )
                    IshaaraBadge(text = badgeText)
                }

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = subtitle,
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
            }
        }
    }
}
