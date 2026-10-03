package com.ishara.app.feature.driver.earnings

import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.DriverRideEarningsItem
import com.ishara.app.domain.model.EarningsPeriodType

/**
 * Immutable UI state model for Driver Earnings & Historical Analytics.
 */
data class DriverEarningsUiState(
    val selectedPeriod: EarningsPeriodType = EarningsPeriodType.TODAY,
    val customFromDate: String = "",
    val customToDate: String = "",
    val customDateValidationError: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val earnings: DriverEarnings? = null,
    val rideItems: List<DriverRideEarningsItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val isOffline: Boolean = false,
    val isSessionExpired: Boolean = false,
    val isUnauthorized: Boolean = false,
    val userFacingError: String? = null,
    val lastUpdatedTimestamp: Long? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && earnings != null && earnings.summary.completedRidesCount == 0 && rideItems.isEmpty()

    val isContentAvailable: Boolean
        get() = earnings != null
}
