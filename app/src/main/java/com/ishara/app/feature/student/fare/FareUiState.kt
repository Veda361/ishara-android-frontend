package com.ishara.app.feature.student.fare

import com.ishara.app.domain.model.RideFare

/**
 * Immutable UI state hierarchy for the Fare Summary & Review screen.
 */
sealed interface FareUiState {

    data object Loading : FareUiState

    data class Content(
        val fare: RideFare,
        val isRefreshing: Boolean = false,
        val bannerNotice: String? = null
    ) : FareUiState {
        val isFinal: Boolean get() = fare.isFinal
        val isCompleted: Boolean get() = fare.status == "COMPLETED"

        val statusDescription: String
            get() = if (fare.isFinal) {
                "Authoritative Final Fare"
            } else {
                "Estimated Fare (Subject to final distance & time)"
            }
    }

    data class Error(
        val message: String,
        val isUnauthorized: Boolean = false,
        val canRetry: Boolean = true
    ) : FareUiState
}
