package com.ishara.app.feature.driver.agency.ui

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
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.feature.driver.agency.DriverAgencyMembershipViewModel

/**
 * Driver Agency Membership & Fleet Affiliation Screen.
 * Authoritative UI reflecting backend agency membership states.
 */
@Composable
fun DriverAgencyMembershipScreen(
    viewModel: DriverAgencyMembershipViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        text = "Agency Affiliation",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground
                    )
                    Text(
                        text = "Fleet & Transport Association",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            IshaaraButton(
                text = if (uiState.isRefreshing) "Refreshing..." else "Refresh",
                onClick = { viewModel.refresh() },
                variant = IshaaraButtonVariant.Text,
                enabled = !uiState.isRefreshing
            )
        }

        IshaaraDivider()

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // User Feedback Message Banner
            if (uiState.userFeedbackMessage != null) {
                item {
                    IshaaraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        containerColor = colors.success.copy(alpha = 0.12f),
                        borderColor = colors.success.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = uiState.userFeedbackMessage ?: "",
                            style = typography.bodySmall,
                            color = colors.success
                        )
                    }
                }
            }

            // Error Message Banner
            if (uiState.errorMessage != null) {
                item {
                    IshaaraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        containerColor = colors.danger.copy(alpha = 0.12f),
                        borderColor = colors.danger.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = typography.bodySmall,
                            color = colors.danger
                        )
                    }
                }
            }

            // Current Membership State Card
            item {
                MembershipStatusSection(
                    state = uiState.membershipState,
                    isCancelling = uiState.isCancellingRequest,
                    onCancelRequest = { viewModel.cancelPendingMembership() }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Agency Discovery & Request Section (shown when no membership or rejected)
            val canApply = uiState.membershipState is DriverMembershipState.NoMembership ||
                    uiState.membershipState is DriverMembershipState.Rejected

            if (canApply) {
                item {
                    Text(
                        text = "Find Your Transport Fleet",
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.foreground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search and select a licensed agency to submit your affiliation request.",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    IshaaraTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = "Search agency by name or city...",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Selected Agency Application Form
                if (uiState.selectedAgency != null) {
                    item {
                        SelectedAgencyForm(
                            agency = uiState.selectedAgency!!,
                            notes = uiState.applicationNotes,
                            onNotesChanged = { viewModel.onNotesChanged(it) },
                            isSubmitting = uiState.isSubmittingRequest,
                            onSubmit = { viewModel.submitMembershipRequest() },
                            onDeselect = { viewModel.onSelectAgency(null) }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Available Agencies List
                if (uiState.isLoadingAgencies) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading active agencies...",
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                    }
                } else if (uiState.availableAgencies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active transport agencies found matching your search.",
                                style = typography.bodySmall,
                                color = colors.foregroundMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Available Agencies (${uiState.availableAgencies.size})",
                            style = typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.foregroundMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(uiState.availableAgencies) { agency ->
                        AgencyListItem(
                            agency = agency,
                            isSelected = uiState.selectedAgency?.id == agency.id,
                            onSelect = { viewModel.onSelectAgency(agency) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MembershipStatusSection(
    state: DriverMembershipState,
    isCancelling: Boolean,
    onCancelRequest: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    when (state) {
        is DriverMembershipState.Loading -> {
            IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Checking agency affiliation status...",
                    style = typography.bodyMedium,
                    color = colors.foregroundMuted
                )
            }
        }

        is DriverMembershipState.Approved -> {
            val mem = state.membership
            IshaaraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.success.copy(alpha = 0.08f),
                borderColor = colors.success.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE FLEET AFFILIATION",
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mem.agencyName,
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                        if (!mem.agencyCity.isNullOrBlank() || !mem.agencyState.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(mem.agencyCity, mem.agencyState).joinToString(", "),
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                        if (!mem.agencyContactEmail.isNullOrBlank()) {
                            Text(
                                text = "Contact: ${mem.agencyContactEmail}",
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                    }
                    IshaaraBadge(
                        text = "ACTIVE",
                        variant = IshaaraBadgeVariant.Success
                    )
                }
            }
        }

        is DriverMembershipState.Pending -> {
            val mem = state.membership
            IshaaraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.warning.copy(alpha = 0.08f),
                borderColor = colors.warning.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "APPLICATION SUBMITTED",
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.warning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mem.agencyName,
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pending agency approval",
                            style = typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.warning
                        )
                        if (!mem.agencyCity.isNullOrBlank() || !mem.agencyState.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(mem.agencyCity, mem.agencyState).joinToString(", "),
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                        if (!mem.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: \"${mem.notes}\"",
                                style = typography.bodySmall,
                                color = colors.foregroundMuted
                            )
                        }
                    }
                    IshaaraBadge(
                        text = "PENDING",
                        variant = IshaaraBadgeVariant.Warning
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                IshaaraButton(
                    text = if (isCancelling) "Cancelling Request..." else "Cancel Request",
                    onClick = onCancelRequest,
                    variant = IshaaraButtonVariant.Danger,
                    enabled = !isCancelling,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is DriverMembershipState.Rejected -> {
            val mem = state.membership
            IshaaraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.danger.copy(alpha = 0.08f),
                borderColor = colors.danger.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MEMBERSHIP DECLINED",
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.danger
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mem.agencyName,
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                        if (!mem.rejectionReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${mem.rejectionReason}",
                                style = typography.bodySmall,
                                color = colors.danger
                            )
                        }
                    }
                    IshaaraBadge(
                        text = "REJECTED",
                        variant = IshaaraBadgeVariant.Danger
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You may re-apply to this agency or select another transport fleet below.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }

        is DriverMembershipState.NoMembership -> {
            IshaaraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.surface,
                borderColor = colors.border
            ) {
                Text(
                    text = "NO AGENCY AFFILIATION",
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foregroundMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Independent / Unassigned Driver",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You are currently not affiliated with a transport agency. To operate under an agency fleet, browse and request membership below.",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }
        }

        is DriverMembershipState.Error -> {
            IshaaraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.danger.copy(alpha = 0.08f),
                borderColor = colors.danger.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "Could not resolve membership status.",
                    style = typography.bodyMedium,
                    color = colors.danger
                )
            }
        }
    }
}

@Composable
private fun SelectedAgencyForm(
    agency: Agency,
    notes: String,
    onNotesChanged: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onDeselect: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.primary.copy(alpha = 0.04f),
        borderColor = colors.primary.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected Agency",
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = agency.name,
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
                if (!agency.city.isNullOrBlank() || !agency.state.isNullOrBlank()) {
                    Text(
                        text = listOfNotNull(agency.city, agency.state).joinToString(", "),
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }
            IshaaraButton(
                text = "Change",
                onClick = onDeselect,
                variant = IshaaraButtonVariant.Text
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        IshaaraTextField(
            value = notes,
            onValueChange = onNotesChanged,
            placeholder = "Optional notes (e.g. shifts preferred, routes)...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        IshaaraButton(
            text = if (isSubmitting) "Submitting Application..." else "Submit Membership Request",
            onClick = onSubmit,
            variant = IshaaraButtonVariant.Primary,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AgencyListItem(
    agency: Agency,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        containerColor = if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface,
        borderColor = if (isSelected) colors.primary else colors.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = agency.name,
                    style = typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground
                )
                if (!agency.city.isNullOrBlank() || !agency.state.isNullOrBlank()) {
                    Text(
                        text = listOfNotNull(agency.city, agency.state).joinToString(", "),
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
                if (!agency.contactEmail.isNullOrBlank()) {
                    Text(
                        text = agency.contactEmail,
                        style = typography.labelSmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            IshaaraButton(
                text = if (isSelected) "Selected" else "Select",
                onClick = onSelect,
                variant = if (isSelected) IshaaraButtonVariant.Primary else IshaaraButtonVariant.Outlined
            )
        }
    }
}
