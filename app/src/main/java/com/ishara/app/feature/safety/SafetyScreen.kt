package com.ishara.app.feature.safety

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.R
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraDialog
import com.ishara.app.core.designsystem.component.IshaaraErrorState
import com.ishara.app.core.designsystem.component.IshaaraIconButton
import com.ishara.app.core.designsystem.component.IshaaraIconButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraLoadingState
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyStatus

@Composable
fun SafetyScreen(
    viewModel: SafetyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.onNavigateBack()
        onNavigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IshaaraTheme.colors.background)
    ) {
        when (val state = uiState) {
            is SafetyUiState.Loading -> {
                IshaaraLoadingState(message = "Checking safety status...")
            }
            is SafetyUiState.Content -> {
                SafetyContent(
                    state = state,
                    onBackClick = {
                        viewModel.onNavigateBack()
                        onNavigateBack()
                    },
                    onTriggerSosClick = { viewModel.onOpenConfirmationDialog() },
                    onConfirmSos = { viewModel.onConfirmSos() },
                    onDismissConfirmation = { viewModel.onDismissConfirmationDialog() },
                    onOpenCancelDialog = { viewModel.onOpenCancelDialog() },
                    onDismissCancelDialog = { viewModel.onDismissCancelDialog() },
                    onCancellationReasonChanged = { viewModel.onCancellationReasonChanged(it) },
                    onConfirmCancel = { viewModel.onConfirmCancel() },
                    onDismissBanner = { viewModel.onDismissBanner() },
                    onDismissError = { viewModel.onDismissError() },
                    onOpenAddContactDialog = { viewModel.onOpenAddContactDialog() },
                    onDismissAddContactDialog = { viewModel.onDismissAddContactDialog() },
                    onAddContact = { name, phone, rel -> viewModel.onAddContact(name, phone, rel) },
                    onDeleteContact = { viewModel.onDeleteContact(it) }
                )
            }
            is SafetyUiState.Error -> {
                IshaaraErrorState(
                    title = "Safety System Unavailable",
                    message = state.message,
                    retryLabel = stringResource(R.string.retry_button),
                    onRetryClick = { viewModel.loadInitialState() }
                )
            }
        }
    }
}

@Composable
private fun SafetyContent(
    state: SafetyUiState.Content,
    onBackClick: () -> Unit,
    onTriggerSosClick: () -> Unit,
    onConfirmSos: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onOpenCancelDialog: () -> Unit,
    onDismissCancelDialog: () -> Unit,
    onCancellationReasonChanged: (String) -> Unit,
    onConfirmCancel: () -> Unit,
    onDismissBanner: () -> Unit,
    onDismissError: () -> Unit,
    onOpenAddContactDialog: () -> Unit,
    onDismissAddContactDialog: () -> Unit,
    onAddContact: (String, String, com.ishara.app.domain.model.EmergencyContactRelationship) -> Unit,
    onDeleteContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IshaaraIconButton(
                onClick = onBackClick,
                contentDescription = "Return to ride tracking",
                variant = IshaaraIconButtonVariant.Standard
            ) {
                Text(
                    text = "←",
                    style = typography.titleLarge,
                    color = colors.foreground
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safety & Emergency Assistance",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Ride Reference: ${state.rideId.takeLast(8)}",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }

        // Banners (Success / Status notices)
        AnimatedVisibility(
            visible = state.bannerMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IshaaraPalette.Emerald100)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.bannerMessage ?: "",
                        style = typography.bodySmall,
                        color = IshaaraPalette.Emerald900,
                        modifier = Modifier.weight(1f)
                    )
                    IshaaraButton(
                        text = "OK",
                        onClick = onDismissBanner,
                        size = IshaaraButtonSize.Small,
                        variant = IshaaraButtonVariant.Text
                    )
                }
            }
        }

        // Error Banner
        AnimatedVisibility(
            visible = state.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IshaaraPalette.Crimson100)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = typography.bodySmall,
                        color = IshaaraPalette.Crimson900,
                        modifier = Modifier.weight(1f)
                    )
                    IshaaraButton(
                        text = "Dismiss",
                        onClick = onDismissError,
                        size = IshaaraButtonSize.Small,
                        variant = IshaaraButtonVariant.Text
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = spacing.md)
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.md))

                // Active Alert Card (if active)
                if (state.hasActiveAlert && state.activeEvent != null) {
                    ActiveAlertCard(
                        event = state.activeEvent,
                        isCancelling = state.isCancelling,
                        onCancelClick = onOpenCancelDialog
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                } else {
                    // Trigger SOS Card
                    TriggerSosCard(
                        isSubmitting = state.isSubmitting,
                        onTriggerClick = onTriggerSosClick
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                }
            }

            // Emergency Contacts Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Contacts",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = "Trusted contacts registered to your profile (${state.emergencyContacts.size}/5)",
                            style = typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                    }

                    if (state.emergencyContacts.size < 5) {
                        IshaaraButton(
                            text = "+ Add",
                            onClick = onOpenAddContactDialog,
                            size = IshaaraButtonSize.Small,
                            variant = IshaaraButtonVariant.Outlined
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.sm))
            }

            if (state.emergencyContacts.isEmpty()) {
                item {
                    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No emergency contacts configured.",
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                    }
                }
            } else {
                items(state.emergencyContacts, key = { it.id }) { contact ->
                    EmergencyContactRow(
                        contact = contact,
                        isDeleting = state.isDeletingContactId == contact.id,
                        onDeleteClick = { onDeleteContact(contact.id) }
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                }
            }

            // Important Safety Guidance
            item {
                Spacer(modifier = Modifier.height(spacing.lg))
                SafetyGuidanceCard()
                Spacer(modifier = Modifier.height(spacing.xl))
            }
        }
    }

    // Confirmation Dialog
    if (state.showConfirmationDialog) {
        IshaaraDialog(
            onDismissRequest = onDismissConfirmation,
            title = "Confirm Safety Alert",
            message = "Are you sure you want to trigger a safety alert? This will immediately register an active emergency record with the Ishaara operations team for this ride.",
            confirmLabel = if (state.isSubmitting) "Sending..." else "Send Safety Alert",
            dismissLabel = "Cancel",
            onConfirm = onConfirmSos,
            isDanger = true
        )
    }

    // Cancel Dialog
    if (state.showCancelDialog) {
        IshaaraDialog(
            onDismissRequest = onDismissCancelDialog,
            title = "Cancel Safety Alert",
            message = "Are you sure you want to cancel the active safety alert? The operations team will be notified of the cancellation.",
            confirmLabel = if (state.isCancelling) "Cancelling..." else "Confirm Cancellation",
            dismissLabel = "Keep Alert",
            onConfirm = onConfirmCancel,
            isDanger = false
        )
    }

    // Add Emergency Contact Dialog
    if (state.showAddContactDialog) {
        AddEmergencyContactDialog(
            isAdding = state.isAddingContact,
            errorMessage = state.contactErrorMessage,
            onDismiss = onDismissAddContactDialog,
            onConfirm = onAddContact
        )
    }
}

@Composable
private fun TriggerSosCard(
    isSubmitting: Boolean,
    onTriggerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, IshaaraPalette.Crimson100, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IshaaraPalette.Crimson100),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IshaaraPalette.Crimson900
                    )
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                Column {
                    Text(
                        text = "Emergency Assistance",
                        style = typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground
                    )
                    Text(
                        text = "Use only if you are in immediate danger or distress.",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = "Triggering SOS attaches the ride's current location snapshot and notifies Ishaara transit operations for response coordination.",
                style = typography.bodySmall,
                color = colors.foregroundSubtle
            )

            Spacer(modifier = Modifier.height(spacing.md))

            IshaaraButton(
                text = if (isSubmitting) "Sending Safety Alert..." else "Send Safety Alert (SOS)",
                onClick = onTriggerClick,
                enabled = !isSubmitting,
                variant = IshaaraButtonVariant.Danger,
                size = IshaaraButtonSize.Large,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Send safety emergency alert for this ride" }
            )
        }
    }
}

@Composable
private fun ActiveAlertCard(
    event: EmergencyEvent,
    isCancelling: Boolean,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, IshaaraPalette.Crimson500, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(IshaaraPalette.Crimson500)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(
                        text = "ACTIVE SAFETY ALERT",
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IshaaraPalette.Crimson900
                    )
                }

                val badgeVariant = when (event.status) {
                    EmergencyStatus.ACTIVE -> IshaaraBadgeVariant.Danger
                    EmergencyStatus.ACKNOWLEDGED -> IshaaraBadgeVariant.Warning
                    EmergencyStatus.RESOLVED -> IshaaraBadgeVariant.Success
                    else -> IshaaraBadgeVariant.Neutral
                }

                IshaaraBadge(
                    text = event.status.name,
                    variant = badgeVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Incident Reference: ${event.eventId}",
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.foreground
            )

            Text(
                text = "Triggered: ${event.triggeredAt}",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            if (event.locationSnapshot.hasCoordinates) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "Location snapshot attached (${event.locationSnapshot.provider})",
                    style = typography.labelSmall,
                    color = colors.foregroundSubtle
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            IshaaraButton(
                text = if (isCancelling) "Cancelling Alert..." else "Cancel Safety Alert",
                onClick = onCancelClick,
                enabled = !isCancelling,
                variant = IshaaraButtonVariant.Outlined,
                size = IshaaraButtonSize.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmergencyContactRow(
    contact: EmergencyContact,
    isDeleting: Boolean = false,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.foreground
                )
                Text(
                    text = maskPhoneNumber(contact.phoneNumber),
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                IshaaraBadge(
                    text = contact.relationship.name,
                    variant = IshaaraBadgeVariant.Neutral
                )

                IshaaraButton(
                    text = if (isDeleting) "..." else "✕",
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    loading = isDeleting,
                    size = IshaaraButtonSize.Small,
                    variant = IshaaraButtonVariant.Text
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEmergencyContactDialog(
    isAdding: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, com.ishara.app.domain.model.EmergencyContactRelationship) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember {
        mutableStateOf(com.ishara.app.domain.model.EmergencyContactRelationship.PARENT)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(IshaaraTheme.shapes.sm)
                .background(IshaaraTheme.colors.surfaceElevated)
                .border(IshaaraTheme.borders.thin, IshaaraTheme.colors.border, IshaaraTheme.shapes.sm)
                .padding(IshaaraTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(IshaaraTheme.spacing.sm)
        ) {
            Text(
                text = "Add Emergency Contact",
                style = IshaaraTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IshaaraTheme.colors.foreground
            )
            Spacer(modifier = Modifier.height(IshaaraTheme.spacing.xs))

            IshaaraTextField(
                value = name,
                onValueChange = { name = it },
                label = "Contact Name",
                placeholder = "e.g. Jane Doe",
                modifier = Modifier.fillMaxWidth()
            )

            IshaaraTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                placeholder = "e.g. +919876543210",
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Relationship",
                style = IshaaraTheme.typography.labelSmall,
                color = IshaaraTheme.colors.foregroundMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    com.ishara.app.domain.model.EmergencyContactRelationship.PARENT,
                    com.ishara.app.domain.model.EmergencyContactRelationship.SPOUSE,
                    com.ishara.app.domain.model.EmergencyContactRelationship.SIBLING,
                    com.ishara.app.domain.model.EmergencyContactRelationship.FRIEND,
                    com.ishara.app.domain.model.EmergencyContactRelationship.OTHER
                ).forEach { rel ->
                    val isSelected = relationship == rel
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) IshaaraTheme.colors.primarySubtle else IshaaraTheme.colors.surface)
                            .border(
                                1.dp,
                                if (isSelected) IshaaraTheme.colors.primary else IshaaraTheme.colors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { relationship = rel }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rel.name,
                            style = IshaaraTheme.typography.labelSmall,
                            color = if (isSelected) IshaaraTheme.colors.primary else IshaaraTheme.colors.foreground
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = IshaaraTheme.typography.bodySmall,
                    color = IshaaraTheme.colors.danger
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(IshaaraTheme.spacing.sm)
            ) {
                IshaaraButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = IshaaraButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                    enabled = !isAdding
                )
                IshaaraButton(
                    text = if (isAdding) "Saving..." else "Save Contact",
                    onClick = { onConfirm(name, phone, relationship) },
                    variant = IshaaraButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && phone.isNotBlank() && !isAdding,
                    loading = isAdding
                )
            }
        }
    }
}

@Composable
private fun SafetyGuidanceCard(modifier: Modifier = Modifier) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = "Safety Principles",
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "• Alerts notify Ishaara transit operators of vehicle and ride incidents.\n• Stay in well-lit areas near designated bus stops whenever possible.\n• In life-threatening emergencies, also contact official local emergency services directly.",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )
        }
    }
}

/**
 * Masks phone numbers for privacy in transit UI (e.g. "+91 98765 43210" -> "+91 ••••• ••210").
 */
private fun maskPhoneNumber(phone: String): String {
    if (phone.length <= 4) return phone
    val visibleSuffix = phone.takeLast(3)
    val prefix = if (phone.startsWith("+")) phone.take(3) else ""
    return "$prefix ••••• ••$visibleSuffix"
}
