package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AssignedVehicleState
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleType
import com.ishara.app.domain.repository.DriverReadinessRepository
import com.ishara.app.domain.repository.DriverVehicleRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Retrieves the currently assigned vehicle for the authenticated driver.
 */
class GetAssignedVehicleUseCase(
    private val repository: DriverVehicleRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): IshaaraResult<AssignedVehicleState> {
        return repository.getAssignedVehicle(forceRefresh)
    }
}

/**
 * Observes real-time updates to the driver's assigned vehicle state.
 */
class ObserveAssignedVehicleUseCase(
    private val repository: DriverVehicleRepository
) {
    operator fun invoke(): StateFlow<AssignedVehicleState?> {
        return repository.observeAssignedVehicle()
    }
}

/**
 * Refreshes assigned vehicle and synchronization with A06 operational readiness.
 */
class RefreshAssignedVehicleUseCase(
    private val vehicleRepository: DriverVehicleRepository,
    private val readinessRepository: DriverReadinessRepository? = null
) {
    suspend operator fun invoke(): IshaaraResult<AssignedVehicleState> {
        val result = vehicleRepository.refreshAssignedVehicle()
        if (result is IshaaraResult.Success) {
            readinessRepository?.refreshDriverReadiness()
        }
        return result
    }
}

/**
 * Lists all vehicles registered under the driver's individual profile.
 */
class ListMyVehiclesUseCase(
    private val repository: DriverVehicleRepository
) {
    suspend operator fun invoke(): IshaaraResult<List<Vehicle>> {
        return repository.listMyVehicles()
    }
}

/**
 * Retrieves details for a specific vehicle owned by the driver.
 */
class GetVehicleDetailsUseCase(
    private val repository: DriverVehicleRepository
) {
    suspend operator fun invoke(vehicleId: String): IshaaraResult<Vehicle> {
        return repository.getVehicleDetails(vehicleId)
    }
}

/**
 * Registers an individual vehicle under the authenticated driver's profile.
 */
class RegisterVehicleUseCase(
    private val repository: DriverVehicleRepository
) {
    suspend operator fun invoke(
        registrationNumber: String,
        vehicleType: VehicleType,
        make: String,
        model: String,
        capacity: Int? = null
    ): IshaaraResult<Vehicle> {
        return repository.registerVehicle(
            registrationNumber = registrationNumber,
            vehicleType = vehicleType,
            make = make,
            model = model,
            capacity = capacity
        )
    }
}

/**
 * Assigns the authenticated driver to an owned or authorized vehicle,
 * and triggers operational readiness refresh.
 */
class AssignSelfToVehicleUseCase(
    private val vehicleRepository: DriverVehicleRepository,
    private val readinessRepository: DriverReadinessRepository? = null
) {
    suspend operator fun invoke(
        vehicleId: String,
        driverProfileId: String
    ): IshaaraResult<DriverVehicleAssignment> {
        val result = vehicleRepository.assignSelfToVehicle(vehicleId, driverProfileId)
        if (result is IshaaraResult.Success) {
            readinessRepository?.refreshDriverReadiness()
        }
        return result
    }
}

/**
 * Unassigns the driver from the active vehicle, and updates readiness accordingly.
 */
class UnassignVehicleUseCase(
    private val vehicleRepository: DriverVehicleRepository,
    private val readinessRepository: DriverReadinessRepository? = null
) {
    suspend operator fun invoke(
        vehicleId: String,
        reason: String? = null
    ): IshaaraResult<DriverVehicleAssignment?> {
        val result = vehicleRepository.unassignVehicle(vehicleId, reason)
        if (result is IshaaraResult.Success) {
            readinessRepository?.refreshDriverReadiness()
        }
        return result
    }
}

/**
 * Retrieves assignment history for a vehicle.
 */
class GetVehicleAssignmentHistoryUseCase(
    private val repository: DriverVehicleRepository
) {
    suspend operator fun invoke(vehicleId: String): IshaaraResult<List<DriverVehicleAssignment>> {
        return repository.getAssignmentHistory(vehicleId)
    }
}

/**
 * Clears vehicle state on logout or account switch.
 */
class ClearVehicleStateUseCase(
    private val repository: DriverVehicleRepository
) {
    operator fun invoke() {
        repository.clearVehicleState()
    }
}
