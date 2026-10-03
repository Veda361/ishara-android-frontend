package com.ishara.app.feature.driver.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.VehicleType
import com.ishara.app.domain.usecase.AssignSelfToVehicleUseCase
import com.ishara.app.domain.usecase.GetAssignedVehicleUseCase
import com.ishara.app.domain.usecase.GetVehicleAssignmentHistoryUseCase
import com.ishara.app.domain.usecase.ListMyVehiclesUseCase
import com.ishara.app.domain.usecase.ObserveAssignedVehicleUseCase
import com.ishara.app.domain.usecase.RefreshAssignedVehicleUseCase
import com.ishara.app.domain.usecase.RegisterVehicleUseCase
import com.ishara.app.domain.usecase.UnassignVehicleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Driver Vehicle and Assignment operations (Phase A07).
 */
class DriverVehicleViewModel(
    private val getAssignedVehicleUseCase: GetAssignedVehicleUseCase,
    private val refreshAssignedVehicleUseCase: RefreshAssignedVehicleUseCase,
    private val observeAssignedVehicleUseCase: ObserveAssignedVehicleUseCase? = null,
    private val listMyVehiclesUseCase: ListMyVehiclesUseCase? = null,
    private val registerVehicleUseCase: RegisterVehicleUseCase? = null,
    private val assignSelfToVehicleUseCase: AssignSelfToVehicleUseCase? = null,
    private val unassignVehicleUseCase: UnassignVehicleUseCase? = null,
    private val getVehicleAssignmentHistoryUseCase: GetVehicleAssignmentHistoryUseCase? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(DriverVehicleUiState())
    val uiState: StateFlow<DriverVehicleUiState> = _uiState.asStateFlow()

    init {
        loadAssignedVehicle(forceRefresh = false)
        observeAssignedVehicle()
        loadOwnedVehicles()
    }

    private fun observeAssignedVehicle() {
        observeAssignedVehicleUseCase?.let { useCase ->
            scope.launch(dispatchers.main) {
                useCase().collect { assignedState ->
                    if (assignedState != null) {
                        _uiState.update {
                            it.copy(
                                assignedVehicle = assignedState.vehicle,
                                activeAssignment = assignedState.assignment
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadAssignedVehicle(forceRefresh: Boolean = false) {
        scope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = !forceRefresh, isRefreshing = forceRefresh, errorMessage = null) }

            val result = if (forceRefresh) {
                refreshAssignedVehicleUseCase()
            } else {
                getAssignedVehicleUseCase(forceRefresh = false)
            }

            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            assignedVehicle = result.data.vehicle,
                            activeAssignment = result.data.assignment,
                            isOffline = false
                        )
                    }
                    result.data.vehicle?.id?.let { loadAssignmentHistory(it) }
                }
                is IshaaraResult.Failure -> {
                    val isOffline = result.error is IshaaraError.Network
                    val message = mapErrorMessage(result.error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = message,
                            isOffline = isOffline
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadAssignedVehicle(forceRefresh = true)
        loadOwnedVehicles()
    }

    fun loadOwnedVehicles() {
        listMyVehiclesUseCase?.let { useCase ->
            scope.launch(dispatchers.main) {
                when (val result = useCase()) {
                    is IshaaraResult.Success -> {
                        _uiState.update { it.copy(ownedVehicles = result.data) }
                    }
                    is IshaaraResult.Failure -> {
                        IshaaraLogger.w("DriverVehicleVM", "Failed to load owned vehicles: ${result.error.message}")
                    }
                }
            }
        }
    }

    fun registerVehicle(
        registrationNumber: String,
        vehicleType: VehicleType,
        make: String,
        model: String,
        capacity: Int?
    ) {
        val useCase = registerVehicleUseCase ?: return
        scope.launch(dispatchers.main) {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            when (val result = useCase(registrationNumber, vehicleType, make, model, capacity)) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showRegisterDialog = false,
                            successMessage = "Vehicle ${result.data.registrationNumber} registered successfully."
                        )
                    }
                    loadOwnedVehicles()
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    fun assignSelfToVehicle(vehicleId: String, driverProfileId: String) {
        val useCase = assignSelfToVehicleUseCase ?: return
        scope.launch(dispatchers.main) {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            when (val result = useCase(vehicleId, driverProfileId)) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            activeAssignment = result.data,
                            assignedVehicle = result.data.vehicle,
                            successMessage = "Vehicle assigned successfully."
                        )
                    }
                    loadAssignedVehicle(forceRefresh = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    fun unassignVehicle(vehicleId: String, reason: String? = null) {
        val useCase = unassignVehicleUseCase ?: return
        scope.launch(dispatchers.main) {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            when (val result = useCase(vehicleId, reason)) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            assignedVehicle = null,
                            activeAssignment = null,
                            successMessage = "Vehicle unassigned successfully."
                        )
                    }
                    loadAssignedVehicle(forceRefresh = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    fun loadAssignmentHistory(vehicleId: String) {
        val useCase = getVehicleAssignmentHistoryUseCase ?: return
        scope.launch(dispatchers.main) {
            when (val result = useCase(vehicleId)) {
                is IshaaraResult.Success -> {
                    _uiState.update { it.copy(assignmentHistory = result.data) }
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w("DriverVehicleVM", "Failed to load assignment history: ${result.error.message}")
                }
            }
        }
    }

    fun openRegisterDialog() {
        _uiState.update { it.copy(showRegisterDialog = true, errorMessage = null) }
    }

    fun closeRegisterDialog() {
        _uiState.update { it.copy(showRegisterDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun mapErrorMessage(error: IshaaraError): String {
        return when (error) {
            is IshaaraError.Conflict -> error.message.ifBlank { "Vehicle assignment or registration conflict occurred." }
            is IshaaraError.Forbidden -> error.message.ifBlank { "Access denied. Action not permitted." }
            is IshaaraError.NotFound -> error.message.ifBlank { "Vehicle or assignment record not found." }
            is IshaaraError.Validation -> error.message.ifBlank { "Invalid vehicle information provided." }
            is IshaaraError.RateLimited -> "Rate limit exceeded. Please wait a moment and try again."
            is IshaaraError.Server -> "Server error occurred. Please try again later."
            is IshaaraError.Network -> "Network connection unavailable. Please check your connection."
            is IshaaraError.Timeout -> "Request timed out. Please try again."
            is IshaaraError.Authentication -> "Authentication session expired. Please sign in again."
            else -> error.message.ifBlank { "An unexpected error occurred." }
        }
    }
}
