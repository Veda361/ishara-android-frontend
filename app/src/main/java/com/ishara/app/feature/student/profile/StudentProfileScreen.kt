package com.ishara.app.feature.student.profile

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraPrimaryAction
import com.ishara.app.core.designsystem.component.IshaaraTopBar
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole

/**
 * Production Student / Passenger Profile Screen.
 * Displays authoritative user attributes from GET /api/v1/users/me and allows editing supported fields via PATCH.
 */
@Composable
fun StudentProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            IshaaraTopBar(
                title = "Passenger Profile",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Header: Avatar & Primary Identity
            val profile = uiState.userProfile
            ProfileIdentityCard(
                profile = profile,
                modifier = Modifier.fillMaxWidth()
            )

            // Success feedback banner
            if (uiState.successMessage != null) {
                IshaaraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.successMessage ?: "",
                        style = typography.bodyMedium,
                        color = colors.success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Error feedback banner
            if (uiState.userFacingError != null) {
                IshaaraCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.userFacingError ?: "",
                        style = typography.bodyMedium,
                        color = colors.danger,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Profile Attributes / Edit Section
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Account Details",
                            style = typography.titleMedium,
                            color = colors.foreground,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!uiState.isEditing) {
                            IshaaraButton(
                                text = "Edit Profile",
                                onClick = { viewModel.startEditing() },
                                variant = IshaaraButtonVariant.Text
                            )
                        }
                    }

                    if (!uiState.isEditing) {
                        // Display Mode
                        ProfileField(label = "Full Name", value = profile?.name ?: "Not provided")
                        ProfileField(label = "Email Address", value = profile?.email ?: "Not linked")
                        ProfileField(label = "Phone Number", value = profile?.phoneNumber ?: "Not provided")
                        ProfileField(label = "Application Role", value = if (profile?.role == UserRole.USER) "Passenger / Student" else profile?.role?.name ?: "Unknown")
                        ProfileField(label = "Onboarding Status", value = if (profile?.hasCompletedOnboarding == true) "Completed" else "Incomplete")
                    } else {
                        // Edit Mode
                        OutlinedTextField(
                            value = uiState.editName,
                            onValueChange = { viewModel.onNameChanged(it) },
                            label = { Text("Full Name") },
                            isError = uiState.nameError != null,
                            supportingText = uiState.nameError?.let { { Text(it, color = colors.danger) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border
                            )
                        )

                        OutlinedTextField(
                            value = uiState.editPhone,
                            onValueChange = { viewModel.onPhoneChanged(it) },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+919876543210") },
                            isError = uiState.phoneError != null,
                            supportingText = uiState.phoneError?.let { { Text(it, color = colors.danger) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border
                            )
                        )

                        Spacer(modifier = Modifier.height(spacing.xs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            IshaaraButton(
                                text = "Cancel",
                                onClick = { viewModel.cancelEditing() },
                                variant = IshaaraButtonVariant.Outlined,
                                enabled = !uiState.isSaving,
                                modifier = Modifier.weight(1f)
                            )

                            IshaaraPrimaryAction(
                                text = if (uiState.isSaving) "Saving..." else "Save Changes",
                                onClick = { viewModel.saveProfile() },
                                enabled = uiState.canSave,
                                loading = uiState.isSaving,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Security & Session Section
            IshaaraCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Text(
                        text = "Session & Security",
                        style = typography.titleMedium,
                        color = colors.foreground,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Signed in as an authenticated passenger on the Ishaara mobility network.",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    IshaaraButton(
                        text = "Sign Out",
                        onClick = onSignOut,
                        variant = IshaaraButtonVariant.Danger,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primarySubtle)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = profile?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                Text(
                    text = initial,
                    style = typography.titleLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile?.name ?: "Passenger",
                    style = typography.titleMedium,
                    color = colors.foreground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = profile?.email ?: "No email linked",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            IshaaraBadge(text = "PASSENGER")
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xxs)
    ) {
        Text(
            text = label,
            style = typography.labelSmall,
            color = colors.foregroundMuted
        )
        Text(
            text = value,
            style = typography.bodyMedium,
            color = colors.foreground,
            fontWeight = FontWeight.Medium
        )
    }
}
