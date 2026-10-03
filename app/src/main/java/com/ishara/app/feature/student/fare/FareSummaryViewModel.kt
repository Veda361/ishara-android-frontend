package com.ishara.app.feature.student.fare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.usecase.GetRideFareUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FareSummaryViewModel(
    val rideId: String,
    private val getRideFareUseCase: GetRideFareUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val tag = "FareSummaryViewModel"

    private val _uiState = MutableStateFlow<FareUiState>(FareUiState.Loading)
    val uiState: StateFlow<FareUiState> = _uiState.asStateFlow()

    init {
        loadFare()
    }

    fun loadFare() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = FareUiState.Loading

            when (val result = getRideFareUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    _uiState.value = FareUiState.Content(
                        fare = result.data,
                        isRefreshing = false
                    )
                }
                is IshaaraResult.Failure -> {
                    handleError(result.error)
                }
            }
        }
    }

    fun refresh() {
        val current = _uiState.value as? FareUiState.Content ?: return
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = current.copy(isRefreshing = true)

            when (val result = getRideFareUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    _uiState.value = FareUiState.Content(
                        fare = result.data,
                        isRefreshing = false
                    )
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Failed to refresh fare: ${result.error.message}")
                    _uiState.value = current.copy(
                        isRefreshing = false,
                        bannerNotice = "Could not refresh fare. Showing last known state."
                    )
                }
            }
        }
    }

    fun onProceedToPayment() {
        IshaaraLogger.i(tag, "Proceeding to Phase A13 payment for ride: $rideId")
        navigationManager.navigate(IshaaraDestination.StudentPayment.createRoute(rideId))
    }

    fun onNavigateBack() {
        navigationManager.navigateUp()
    }

    private fun handleError(error: IshaaraError) {
        val isAuth = error is IshaaraError.Authentication ||
                error is IshaaraError.Forbidden ||
                (error is IshaaraError.Server && error.code in listOf(401, 403))

        val message = when {
            isAuth -> "You do not have permission to view the fare for this ride."
            error is IshaaraError.NotFound -> "Ride or fare record not found."
            error is IshaaraError.Network -> "Network unavailable. Please check your internet connection."
            else -> error.message.ifBlank { "Failed to load fare information." }
        }

        _uiState.value = FareUiState.Error(
            message = message,
            isUnauthorized = isAuth,
            canRetry = !isAuth
        )
    }
}
