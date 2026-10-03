package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.VehicleMapper
import com.ishara.app.data.remote.datasource.VehicleRemoteDataSource
import com.ishara.app.data.remote.dto.AssignVehicleRequestDto
import com.ishara.app.data.remote.dto.CreateVehicleRequestDto
import com.ishara.app.data.remote.dto.UnassignVehicleRequestDto
import com.ishara.app.domain.model.AssignedVehicleState
import com.ishara.app.domain.model.DriverVehicleAssignment
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.model.Vehicle
import com.ishara.app.domain.model.VehicleType
import com.ishara.app.domain.repository.DriverVehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of [DriverVehicleRepository].
 *
 * Enforces:
 * 1. Authoritative backend source of truth for all vehicle and assignment data.
 * 2. Role boundary check: Only DRIVER_CONDUCTOR sessions may access driver vehicle resources.
 * 3. Transient caching: In-memory cache is used strictly for UI responsiveness, never as local permanent authorization.
 * 4. Data isolation & account switching: Clear state immediately on logout/account switch.
 * 5. Concurrency safety: Mutex protection prevents duplicate parallel network executions.
 */
class DriverVehicleRepositoryImpl(
    private val remoteDataSource: VehicleRemoteDataSource,
    private val localDataSource: SessionLocalDataSource
) : DriverVehicleRepository {

    private val mutex = Mutex()
    private val _assignedVehicleFlow = MutableStateFlow<AssignedVehicleState?>(null)

    override fun observeAssignedVehicle(): StateFlow<AssignedVehicleState?> = _assignedVehicleFlow.asStateFlow()

    override suspend fun getAssignedVehicle(forceRefresh: Boolean): IshaaraResult<AssignedVehicleState> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle assignment operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        if (!forceRefresh) {
            val cached = _assignedVehicleFlow.value
            if (cached != null) {
                return IshaaraResult.success(cached)
            }
        }

        return fetchAssignedVehicleFromBackend(session.token)
    }

    override suspend fun refreshAssignedVehicle(): IshaaraResult<AssignedVehicleState> {
        return getAssignedVehicle(forceRefresh = true)
    }

    private suspend fun fetchAssignedVehicleFromBackend(token: String): IshaaraResult<AssignedVehicleState> {
        val result = remoteDataSource.getMyAssignedVehicle(token)
        return when (result) {
            is IshaaraResult.Success -> {
                val domainState = VehicleMapper.toDomain(result.data)
                _assignedVehicleFlow.value = domainState
                IshaaraLogger.d("DriverVehicleRepo", "Refreshed assigned vehicle: hasActive=${domainState.hasActiveAssignment}")
                IshaaraResult.success(domainState)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e("DriverVehicleRepo", "Failed to fetch assigned vehicle: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun listMyVehicles(): IshaaraResult<List<Vehicle>> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        return when (val result = remoteDataSource.listMyVehicles(session.token)) {
            is IshaaraResult.Success -> {
                val list = result.data.map { VehicleMapper.toDomain(it) }
                IshaaraResult.success(list)
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun getVehicleDetails(vehicleId: String): IshaaraResult<Vehicle> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        return when (val result = remoteDataSource.getVehicleById(vehicleId, session.token)) {
            is IshaaraResult.Success -> IshaaraResult.success(VehicleMapper.toDomain(result.data))
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun registerVehicle(
        registrationNumber: String,
        vehicleType: VehicleType,
        make: String,
        model: String,
        capacity: Int?
    ): IshaaraResult<Vehicle> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle registration is restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        val request = CreateVehicleRequestDto(
            registrationNumber = registrationNumber.trim(),
            vehicleType = vehicleType.name,
            make = make.trim(),
            model = model.trim(),
            capacity = capacity
        )

        return when (val result = remoteDataSource.createVehicle(request, session.token)) {
            is IshaaraResult.Success -> IshaaraResult.success(VehicleMapper.toDomain(result.data))
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun assignSelfToVehicle(
        vehicleId: String,
        driverProfileId: String
    ): IshaaraResult<DriverVehicleAssignment> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle assignment is restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        val request = AssignVehicleRequestDto(driverId = driverProfileId)
        return when (val result = remoteDataSource.assignVehicle(vehicleId, request, session.token)) {
            is IshaaraResult.Success -> {
                val assignment = VehicleMapper.toDomain(result.data)
                // Invalidate/refresh assigned vehicle
                fetchAssignedVehicleFromBackend(session.token)
                IshaaraResult.success(assignment)
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun unassignVehicle(
        vehicleId: String,
        reason: String?
    ): IshaaraResult<DriverVehicleAssignment?> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle unassignment is restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        val request = UnassignVehicleRequestDto(reason = reason?.trim())
        return when (val result = remoteDataSource.unassignVehicle(vehicleId, request, session.token)) {
            is IshaaraResult.Success -> {
                val assignment = result.data?.let { VehicleMapper.toDomain(it) }
                // Clear active assigned vehicle
                _assignedVehicleFlow.value = AssignedVehicleState.EMPTY
                IshaaraResult.success(assignment)
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun getAssignmentHistory(vehicleId: String): IshaaraResult<List<DriverVehicleAssignment>> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Vehicle operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        return when (val result = remoteDataSource.getVehicleAssignmentHistory(vehicleId, session.token)) {
            is IshaaraResult.Success -> {
                val list = result.data.map { VehicleMapper.toDomain(it) }
                IshaaraResult.success(list)
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override fun clearVehicleState() {
        _assignedVehicleFlow.value = null
        IshaaraLogger.d("DriverVehicleRepo", "Vehicle and assignment state cleared.")
    }
}
