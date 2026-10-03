package com.ishara.app.feature.driver.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.usecase.CancelDriverTripUseCase
import com.ishara.app.domain.usecase.CompleteDriverTripUseCase
import com.ishara.app.domain.usecase.GetTripDetailsUseCase
import com.ishara.app.domain.usecase.GetVehicleDetailsUseCase
import com.ishara.app.domain.usecase.StartDriverTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing the Driver Trip Detail screen and authoritative lifecycle actions.
 */
class DriverTripDetailViewModel(
    private val tripId: String,
    private val getTripDetailsUseCase: GetTripDetailsUseCase,
    private val startDriverTripUseCase: StartDriverTripUseCase,
    private val completeDriverTripUseCase: CompleteDriverTripUseCase,
    private val cancelDriverTripUseCase: CancelDriverTripUseCase,
    private val getVehicleDetailsUseCase: GetVehicleDetailsUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    init {
        loadTripDetails(forceRefresh = false)
    }

    fun refresh() {
        loadTripDetails(forceRefresh = true)
    }

    private fun loadTripDetails(forceRefresh: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            if (!forceRefresh) {
                _uiState.update { it.copy(contentState = TripDetailContentState.Loading) }
            }

            when (val result = getTripDetailsUseCase(tripId, forceRefresh = forceRefresh)) {
                is IshaaraResult.Success -> {
                    val trip = result.data
                    _uiState.update {
                        it.copy(
                            contentState = TripDetailContentState.Success(trip = trip)
                        )
                    }
                    resolveVehicle(trip.vehicleId)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            contentState = mapErrorToContentState(result.error)
                        )
                    }
                }
            }
        }
    }

    private fun resolveVehicle(vehicleId: String) {
        viewModelScope.launch(dispatchers.io) {
            when (val res = getVehicleDetailsUseCase(vehicleId)) {
                is IshaaraResult.Success -> {
                    val currentSuccess = _uiState.value.contentState as? TripDetailContentState.Success
                    if (currentSuccess != null) {
                        _uiState.update {
                            it.copy(
                                contentState = currentSuccess.copy(assignedVehicle = res.data)
                            )
                        }
                    }
                }
                is IshaaraResult.Failure -> {
                    // Informational only, non-fatal for trip display
                }
            }
        }
    }

    fun startTrip() {
        if (_uiState.value.isActionLoading) return
        _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null, actionSuccessMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = startDriverTripUseCase(tripId)) {
                is IshaaraResult.Success -> {
                    val updatedTrip = result.data
                    val currentVehicle = (_uiState.value.contentState as? TripDetailContentState.Success)?.assignedVehicle
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            contentState = TripDetailContentState.Success(trip = updatedTrip, assignedVehicle = currentVehicle),
                            actionSuccessMessage = "Trip started successfully."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun completeTrip() {
        if (_uiState.value.isActionLoading) return
        _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null, actionSuccessMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = completeDriverTripUseCase(tripId)) {
                is IshaaraResult.Success -> {
                    val updatedTrip = result.data
                    val currentVehicle = (_uiState.value.contentState as? TripDetailContentState.Success)?.assignedVehicle
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            contentState = TripDetailContentState.Success(trip = updatedTrip, assignedVehicle = currentVehicle),
                            actionSuccessMessage = "Trip completed successfully."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun openCancelDialog() {
        _uiState.update { it.copy(isCancelDialogOpen = true, actionErrorMessage = null) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(isCancelDialogOpen = false) }
    }

    fun confirmCancelTrip(reason: String?) {
        _uiState.update { it.copy(isCancelDialogOpen = false, isActionLoading = true, actionErrorMessage = null, actionSuccessMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            when (val result = cancelDriverTripUseCase(tripId, reason)) {
                is IshaaraResult.Success -> {
                    val updatedTrip = result.data
                    val currentVehicle = (_uiState.value.contentState as? TripDetailContentState.Success)?.assignedVehicle
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            contentState = TripDetailContentState.Success(trip = updatedTrip, assignedVehicle = currentVehicle),
                            actionSuccessMessage = "Trip cancelled."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionErrorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearActionMessages() {
        _uiState.update { it.copy(actionErrorMessage = null, actionSuccessMessage = null) }
    }

    private fun mapErrorToContentState(error: IshaaraError): TripDetailContentState.Error {
        return when (error) {
            is IshaaraError.NotFound -> TripDetailContentState.Error(
                message = "Trip not found or does not belong to your account.",
                isNotFound = true,
                canRetry = false
            )
            is IshaaraError.Forbidden -> TripDetailContentState.Error(
                message = error.message,
                isForbidden = true,
                canRetry = false
            )
            is IshaaraError.Network -> TripDetailContentState.Error(
                message = "Network unavailable. Please check your connection."
            )
            is IshaaraError.Timeout -> TripDetailContentState.Error(
                message = "Connection timed out. Tap retry to reload trip details."
            )
            else -> TripDetailContentState.Error(
                message = error.message
            )
        }
    }
}
