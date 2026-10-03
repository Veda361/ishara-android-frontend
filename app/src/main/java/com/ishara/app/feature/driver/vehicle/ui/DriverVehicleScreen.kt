package com.ishara.app.feature.driver.vehicle.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishara.app.core.designsystem.component.IshaaraBadge
import com.ishara.app.core.designsystem.component.IshaaraBadgeVariant
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.component.IshaaraCard
import com.ishara.app.core.designsystem.component.IshaaraDivider
import com.ishara.app.core.designsystem.component.IshaaraTextField
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleAssignmentStatus
import com.ishara.app.domain.model.VehicleType
import com.ishara.app.feature.driver.vehicle.DriverVehicleViewModel

/**
 * Production driver-facing Vehicle & Assignment screen (Phase A07).
 */
@Composable
fun DriverVehicleScreen(
    viewModel: DriverVehicleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

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
                    Text(
                        text = "←",
                        style = typography.titleMedium,
                        color = colors.foreground
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Vehicle Assignment",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.foreground
                    )
                    Text(
                        text = "Fleet & Operational Vehicle",
                        style = typography.bodySmall,
                        color = colors.foregroundMuted
                    )
                }
            }

            // Refresh Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable { viewModel.refresh() },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary
                    )
                } else {
                    Text(
                        text = "↻",
                        style = typography.titleMedium,
                        color = colors.foreground
                    )
                }
            }
        }

        IshaaraDivider()

        // Error Banner
        uiState.errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(colors.danger.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, colors.danger.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = typography.bodySmall,
                        color = colors.danger,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✕",
                        style = typography.labelMedium,
                        color = colors.danger,
                        modifier = Modifier.clickable { viewModel.clearError() }
                    )
                }
            }
        }

        // Success Banner
        uiState.successMessage?.let { success ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .background(colors.success.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, colors.success.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = success,
                        style = typography.bodySmall,
                        color = colors.success,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✕",
                        style = typography.labelMedium,
                        color = colors.success,
                        modifier = Modifier.clickable { viewModel.clearSuccess() }
                    )
                }
            }
        }

        // Main Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    CircularProgressIndicator(color = colors.primary)
                    Text(
                        text = "Loading vehicle assignment...",
                        style = typography.bodyMedium,
                        color = colors.foregroundMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Section 1: Current Assigned Vehicle
                item {
                    val assigned = uiState.assignedVehicle
                    val assignment = uiState.activeAssignment

                    if (assigned != null) {
                        ActiveAssignedVehicleCard(
                            vehicle = assigned,
                            assignment = assignment,
                            onUnassign = {
                                viewModel.unassignVehicle(assigned.id, "Driver requested unassignment")
                            },
                            isSubmitting = uiState.isSubmitting
                        )
                    } else {
                        NoAssignedVehicleCard(
                            onRegisterClick = { viewModel.openRegisterDialog() }
                        )
                    }
                }

                // Section 2: Individual Vehicle Management
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REGISTERED INDIVIDUAL VEHICLES",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                        Text(
                            text = "+ Register Vehicle",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary,
                            modifier = Modifier.clickable { viewModel.openRegisterDialog() }
                        )
                    }
                }

                if (uiState.ownedVehicles.isEmpty()) {
                    item {
                        IshaaraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Individual Vehicles Registered",
                                    style = typography.titleSmall,
                                    color = colors.foreground
                                )
                                Text(
                                    text = "If you own an individual transport vehicle (auto, cab, car), you can register it here and assign it for operations.",
                                    style = typography.bodySmall,
                                    color = colors.foregroundMuted
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.ownedVehicles) { vehicle ->
                        val isCurrentlyAssigned = uiState.assignedVehicle?.id == vehicle.id
                        OwnedVehicleItemCard(
                            vehicle = vehicle,
                            isCurrentlyAssigned = isCurrentlyAssigned,
                            onAssign = {
                                uiState.activeAssignment?.driverId?.let { driverId ->
                                    viewModel.assignSelfToVehicle(vehicle.id, driverId)
                                }
                            },
                            isSubmitting = uiState.isSubmitting
                        )
                    }
                }

                // Section 3: Assignment History
                if (uiState.assignmentHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "ASSIGNMENT AUDIT HISTORY",
                            style = typography.labelSmall,
                            color = colors.foregroundMuted
                        )
                    }

                    items(uiState.assignmentHistory) { historyItem ->
                        AssignmentHistoryCard(assignment = historyItem)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Register Vehicle Dialog
    if (uiState.showRegisterDialog) {
        RegisterVehicleDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.closeRegisterDialog() },
            onSubmit = { reg, type, make, model, cap ->
                viewModel.registerVehicle(reg, type, make, model, cap)
            }
        )
    }
}

@Composable
private fun ActiveAssignedVehicleCard(
    vehicle: Vehicle,
    assignment: DriverVehicleAssignment?,
    onUnassign: () -> Unit,
    isSubmitting: Boolean
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE ASSIGNED VEHICLE",
                    style = typography.labelSmall,
                    color = colors.foregroundMuted
                )
                if (assignment?.status == VehicleAssignmentStatus.ACTIVE) {
                    IshaaraBadge(text = "ASSIGNED", variant = IshaaraBadgeVariant.Success)
                } else {
                    IshaaraBadge(text = "UNASSIGNED", variant = IshaaraBadgeVariant.Neutral)
                }
            }

            Text(
                text = vehicle.registrationNumber,
                style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.foreground
            )

            val details = listOfNotNull(vehicle.make, vehicle.model, vehicle.vehicleType.name).joinToString(" • ")
            Text(
                text = details,
                style = typography.bodyMedium,
                color = colors.foregroundMuted
            )

            IshaaraDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Ownership", style = typography.labelSmall, color = colors.foregroundMuted)
                    Text(text = vehicle.ownershipType.name, style = typography.bodySmall, color = colors.foreground)
                }
                Column {
                    Text(text = "Capacity", style = typography.labelSmall, color = colors.foregroundMuted)
                    Text(text = vehicle.formattedCapacity, style = typography.bodySmall, color = colors.foreground)
                }
                Column {
                    Text(text = "Platform Verified", style = typography.labelSmall, color = colors.foregroundMuted)
                    Text(
                        text = if (vehicle.isVerified) "Verified" else "Pending Review",
                        style = typography.bodySmall,
                        color = if (vehicle.isVerified) colors.success else colors.warning
                    )
                }
            }

            if (assignment != null) {
                IshaaraDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Assigned At", style = typography.labelSmall, color = colors.foregroundMuted)
                        Text(text = assignment.assignedAt.take(10), style = typography.bodySmall, color = colors.foreground)
                    }
                    Column {
                        Text(text = "Assigned By", style = typography.labelSmall, color = colors.foregroundMuted)
                        Text(text = assignment.assignedByRole.name, style = typography.bodySmall, color = colors.foreground)
                    }
                }
            }

            // Only individual vehicles can be directly unassigned by driver
            if (vehicle.ownershipType.name == "INDIVIDUAL") {
                Spacer(modifier = Modifier.height(4.dp))
                IshaaraButton(
                    text = "Unassign Vehicle",
                    onClick = onUnassign,
                    variant = IshaaraButtonVariant.Outlined,
                    loading = isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NoAssignedVehicleCard(
    onRegisterClick: () -> Unit
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "VEHICLE ASSIGNMENT",
                style = typography.labelSmall,
                color = colors.foregroundMuted
            )

            Text(
                text = "No vehicle currently assigned",
                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.foreground
            )

            Text(
                text = "Your driver account does not have an active vehicle assignment. If you belong to a transport agency, your fleet manager will assign a vehicle to your account. If you operate individually, you can register and assign your vehicle.",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            IshaaraButton(
                text = "Register Individual Vehicle",
                onClick = onRegisterClick,
                variant = IshaaraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OwnedVehicleItemCard(
    vehicle: Vehicle,
    isCurrentlyAssigned: Boolean,
    onAssign: () -> Unit,
    isSubmitting: Boolean
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.registrationNumber,
                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.foreground
                )
                if (isCurrentlyAssigned) {
                    IshaaraBadge(text = "CURRENTLY ACTIVE", variant = IshaaraBadgeVariant.Success)
                } else {
                    IshaaraBadge(text = vehicle.vehicleType.name, variant = IshaaraBadgeVariant.Neutral)
                }
            }

            Text(
                text = "${vehicle.make} ${vehicle.model} • ${vehicle.formattedCapacity}",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            if (!isCurrentlyAssigned) {
                IshaaraButton(
                    text = "Assign to This Vehicle",
                    onClick = onAssign,
                    variant = IshaaraButtonVariant.Outlined,
                    loading = isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AssignmentHistoryCard(
    assignment: DriverVehicleAssignment
) {
    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography

    IshaaraCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status: ${assignment.status.name}",
                    style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (assignment.status == VehicleAssignmentStatus.ACTIVE) colors.success else colors.foregroundMuted
                )
                Text(
                    text = "Role: ${assignment.assignedByRole.name}",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            Text(
                text = "Assigned: ${assignment.assignedAt}",
                style = typography.bodySmall,
                color = colors.foregroundMuted
            )

            assignment.unassignedAt?.let { unassigned ->
                Text(
                    text = "Ended: $unassigned",
                    style = typography.bodySmall,
                    color = colors.foregroundMuted
                )
            }

            assignment.reason?.let { reason ->
                Text(
                    text = "Reason: $reason",
                    style = typography.bodySmall,
                    color = colors.foregroundSubtle
                )
            }
        }
    }
}

@Composable
private fun RegisterVehicleDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (registrationNumber: String, vehicleType: VehicleType, make: String, model: String, capacity: Int?) -> Unit
) {
    var regNumber by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var capacityStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(VehicleType.AUTO) }

    val colors = IshaaraTheme.colors
    val typography = IshaaraTheme.typography
    val spacing = IshaaraTheme.spacing

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text(
                    text = "Register Individual Vehicle",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.foreground
                )

                IshaaraTextField(
                    value = regNumber,
                    onValueChange = { regNumber = it.uppercase() },
                    label = "Registration Number (e.g. UP32AB1234)",
                    placeholder = "UP32AB1234",
                    modifier = Modifier.fillMaxWidth()
                )

                IshaaraTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = "Make / Brand (e.g. Bajaj, Tata, Maruti)",
                    placeholder = "Bajaj",
                    modifier = Modifier.fillMaxWidth()
                )

                IshaaraTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Model (e.g. RE Compact, Starbus)",
                    placeholder = "RE Compact",
                    modifier = Modifier.fillMaxWidth()
                )

                IshaaraTextField(
                    value = capacityStr,
                    onValueChange = { capacityStr = it },
                    label = "Seating Capacity (Optional)",
                    placeholder = "3",
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Vehicle Type: ${selectedType.name}",
                    style = typography.labelMedium,
                    color = colors.foregroundMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(VehicleType.AUTO, VehicleType.CAB, VehicleType.E_RICKSHAW, VehicleType.BUS).forEach { type ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedType == type) colors.primary.copy(alpha = 0.2f) else colors.surface)
                                .border(
                                    1.dp,
                                    if (selectedType == type) colors.primary else colors.border,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.name.take(4),
                                style = typography.labelSmall,
                                color = if (selectedType == type) colors.primary else colors.foreground
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IshaaraButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = IshaaraButtonVariant.Outlined,
                        modifier = Modifier.weight(1f)
                    )
                    IshaaraButton(
                        text = "Register",
                        onClick = {
                            val cap = capacityStr.toIntOrNull()
                            if (regNumber.isNotBlank() && make.isNotBlank() && model.isNotBlank()) {
                                onSubmit(regNumber, selectedType, make, model, cap)
                            }
                        },
                        variant = IshaaraButtonVariant.Primary,
                        loading = isSubmitting,
                        enabled = regNumber.length >= 4 && make.isNotBlank() && model.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
