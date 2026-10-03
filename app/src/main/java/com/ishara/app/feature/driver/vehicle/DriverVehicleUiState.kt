package com.ishara.app.feature.driver.vehicle

import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleAssignmentStatus

/**
 * UI State for the Driver Vehicle & Assignment screen (Phase A07).
 */
data class DriverVehicleUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val assignedVehicle: Vehicle? = null,
    val activeAssignment: DriverVehicleAssignment? = null,
    val ownedVehicles: List<Vehicle> = emptyList(),
    val assignmentHistory: List<DriverVehicleAssignment> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showRegisterDialog: Boolean = false,
    val isOffline: Boolean = false
) {
    val hasActiveAssignment: Boolean
        get() = activeAssignment?.status == VehicleAssignmentStatus.ACTIVE && assignedVehicle != null && assignedVehicle.isActive

    val isAssigned: Boolean
        get() = assignedVehicle != null
}
