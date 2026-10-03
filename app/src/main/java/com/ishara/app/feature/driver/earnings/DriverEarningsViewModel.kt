package com.ishara.app.feature.driver.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.usecase.GetDriverEarningsUseCase
import com.ishara.app.domain.usecase.RefreshDriverEarningsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class DriverEarningsViewModel(
    private val getDriverEarningsUseCase: GetDriverEarningsUseCase,
    private val refreshDriverEarningsUseCase: RefreshDriverEarningsUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverEarningsUiState())
    val uiState: StateFlow<DriverEarningsUiState> = _uiState.asStateFlow()

    private var currentFetchJob: Job? = null
    private val requestCounter = AtomicLong(0)

    init {
        loadEarnings(period = EarningsPeriodType.TODAY, isRefresh = false)
    }

    fun selectPeriod(period: EarningsPeriodType) {
        if (_uiState.value.selectedPeriod == period && period != EarningsPeriodType.CUSTOM) {
            return
        }
        currentFetchJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                customDateValidationError = null,
                currentPage = 1,
                hasMore = false,
                userFacingError = null
            )
        }

        if (period == EarningsPeriodType.CUSTOM) {
            val from = _uiState.value.customFromDate
            val to = _uiState.value.customToDate
            if (from.isNotBlank() && to.isNotBlank()) {
                applyCustomDateRange(from, to)
            }
        } else {
            loadEarnings(period = period, isRefresh = false)
        }
    }

    fun applyCustomDateRange(fromIso: String, toIso: String) {
        val trimmedFrom = fromIso.trim()
        val trimmedTo = toIso.trim()

        if (trimmedFrom.isBlank() || trimmedTo.isBlank()) {
            _uiState.update {
                it.copy(
                    customDateValidationError = "Both start and end dates are required for custom range."
                )
            }
            return
        }

        if (trimmedFrom > trimmedTo) {
            _uiState.update {
                it.copy(
                    customFromDate = trimmedFrom,
                    customToDate = trimmedTo,
                    customDateValidationError = "Start date must be before or equal to end date."
                )
            }
            return
        }

        currentFetchJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPeriod = EarningsPeriodType.CUSTOM,
                customFromDate = trimmedFrom,
                customToDate = trimmedTo,
                customDateValidationError = null,
                currentPage = 1,
                hasMore = false,
                userFacingError = null
            )
        }

        loadEarnings(
            period = EarningsPeriodType.CUSTOM,
            from = trimmedFrom,
            to = trimmedTo,
            isRefresh = false
        )
    }

    fun refresh() {
        if (_uiState.value.isRefreshing || _uiState.value.isLoading) return
        val current = _uiState.value
        val from = if (current.selectedPeriod == EarningsPeriodType.CUSTOM) current.customFromDate else null
        val to = if (current.selectedPeriod == EarningsPeriodType.CUSTOM) current.customToDate else null

        loadEarnings(
            period = current.selectedPeriod,
            from = from,
            to = to,
            isRefresh = true
        )
    }

    fun retry() {
        val current = _uiState.value
        val from = if (current.selectedPeriod == EarningsPeriodType.CUSTOM) current.customFromDate else null
        val to = if (current.selectedPeriod == EarningsPeriodType.CUSTOM) current.customToDate else null

        loadEarnings(
            period = current.selectedPeriod,
            from = from,
            to = to,
            isRefresh = false
        )
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) {
            return
        }

        val nextPage = current.currentPage + 1
        val from = if (current.selectedPeriod == EarningsPeriodType.CUSTOM) current.customFromDate else null
        val to = if (current.selectedPeriod == EarningsPeriodType.CUSTOM) current.customToDate else null
        val requestId = requestCounter.get()

        _uiState.update { it.copy(isLoadingMore = true, userFacingError = null) }

        viewModelScope.launch(dispatchers.main) {
            val result = getDriverEarningsUseCase(
                period = current.selectedPeriod,
                from = from,
                to = to,
                page = nextPage,
                limit = 20
            )

            if (requestCounter.get() != requestId) return@launch

            when (result) {
                is IshaaraResult.Success -> {
                    val newEarnings = result.data
                    val existingRideIds = current.rideItems.map { it.rideId }.toSet()
                    val deduplicatedNewItems = newEarnings.items.filter { it.rideId !in existingRideIds }
                    val mergedRides = current.rideItems + deduplicatedNewItems

                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            earnings = newEarnings,
                            rideItems = mergedRides,
                            currentPage = nextPage,
                            hasMore = newEarnings.pagination.hasMore,
                            isOffline = false,
                            isSessionExpired = false,
                            isUnauthorized = false,
                            userFacingError = null
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    val error = result.error
                    val isExpired = error is IshaaraError.Authentication
                    val isUnauthorized = error is IshaaraError.Forbidden
                    val errorMessage = mapErrorMessage(error)
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            isSessionExpired = isExpired,
                            isUnauthorized = isUnauthorized,
                            userFacingError = errorMessage
                        )
                    }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(userFacingError = null) }
    }

    /**
     * Clears all earnings state, active coroutines, and transient data upon logout or account switch.
     */
    fun clearState() {
        currentFetchJob?.cancel()
        requestCounter.incrementAndGet()
        _uiState.value = DriverEarningsUiState()
    }

    private fun loadEarnings(
        period: EarningsPeriodType,
        from: String? = null,
        to: String? = null,
        isRefresh: Boolean = false
    ) {
        currentFetchJob?.cancel()
        val requestId = requestCounter.incrementAndGet()

        currentFetchJob = viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    userFacingError = null
                )
            }

            val result = if (isRefresh) {
                refreshDriverEarningsUseCase(
                    period = period,
                    from = from,
                    to = to
                )
            } else {
                getDriverEarningsUseCase(
                    period = period,
                    from = from,
                    to = to,
                    page = 1,
                    limit = 20
                )
            }

            if (requestCounter.get() != requestId) return@launch

            when (result) {
                is IshaaraResult.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            earnings = data,
                            rideItems = data.items,
                            currentPage = 1,
                            hasMore = data.pagination.hasMore,
                            isOffline = false,
                            isSessionExpired = false,
                            isUnauthorized = false,
                            userFacingError = null,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    val error = result.error
                    val isOffline = error is IshaaraError.Network
                    val isExpired = error is IshaaraError.Authentication
                    val isUnauthorized = error is IshaaraError.Forbidden
                    val userMessage = mapErrorMessage(error)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isOffline = isOffline,
                            isSessionExpired = isExpired,
                            isUnauthorized = isUnauthorized,
                            userFacingError = userMessage
                        )
                    }
                }
            }
        }
    }

    private fun mapErrorMessage(error: IshaaraError): String {
        return when (error) {
            is IshaaraError.Network -> "Unable to connect to network. Please check your internet connection."
            is IshaaraError.Authentication -> "Session expired. Please sign in again."
            is IshaaraError.Forbidden -> "Access denied: Driver permissions required."
            is IshaaraError.NotFound -> "No earnings information found."
            is IshaaraError.Validation -> error.message.ifBlank { "Invalid date range parameters." }
            is IshaaraError.Server -> "Backend service temporarily unavailable. Please retry shortly."
            else -> error.message.ifBlank { "An unexpected error occurred while fetching earnings." }
        }
    }
}
