package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AssignedVehicleState
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleType
import kotlinx.coroutines.flow.StateFlow

/**
 * Authoritative repository contract for Vehicle Registration and Driver-Vehicle Assignment (Phase A07).
 */
interface DriverVehicleRepository {

    /**
     * Observes the currently assigned vehicle state flow.
     */
    fun observeAssignedVehicle(): StateFlow<AssignedVehicleState?>

    /**
     * Retrieves the driver's currently assigned vehicle.
     * @param forceRefresh When true, bypasses transient memory cache and queries backend.
     */
    suspend fun getAssignedVehicle(forceRefresh: Boolean = false): IshaaraResult<AssignedVehicleState>

    /**
     * Explicitly refreshes current assigned vehicle from the authoritative backend.
     */
    suspend fun refreshAssignedVehicle(): IshaaraResult<AssignedVehicleState>

    /**
     * Lists vehicles owned exclusively by the authenticated driver.
     */
    suspend fun listMyVehicles(): IshaaraResult<List<Vehicle>>

    /**
     * Retrieves details of a specific vehicle owned by the authenticated driver.
     */
    suspend fun getVehicleDetails(vehicleId: String): IshaaraResult<Vehicle>

    /**
     * Registers an individual vehicle under the authenticated driver's profile.
     */
    suspend fun registerVehicle(
        registrationNumber: String,
        vehicleType: VehicleType,
        make: String,
        model: String,
        capacity: Int?
    ): IshaaraResult<Vehicle>

    /**
     * Assigns driver to an owned or authorized vehicle.
     */
    suspend fun assignSelfToVehicle(
        vehicleId: String,
        driverProfileId: String
    ): IshaaraResult<DriverVehicleAssignment>

    /**
     * Unassigns driver from the specified vehicle.
     */
    suspend fun unassignVehicle(
        vehicleId: String,
        reason: String? = null
    ): IshaaraResult<DriverVehicleAssignment?>

    /**
     * Retrieves historical assignment audit log for a vehicle.
     */
    suspend fun getAssignmentHistory(vehicleId: String): IshaaraResult<List<DriverVehicleAssignment>>

    /**
     * Flushes all cached vehicle and assignment state upon logout or account switch.
     */
    fun clearVehicleState()
}
