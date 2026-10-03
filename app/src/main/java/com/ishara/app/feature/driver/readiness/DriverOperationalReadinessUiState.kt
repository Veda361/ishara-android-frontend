package com.ishara.app.feature.driver.readiness

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.DriverOperationalReadiness
import com.ishara.app.domain.model.DriverReadinessBlocker

/**
 * Explicit stages for the Driver Operational Readiness screen.
 */
sealed interface DriverReadinessUiStage {
    /** Initial or full-screen loading state evaluating platform readiness */
    object Loading : DriverReadinessUiStage

    /** Driver meets all platform prerequisites and is authorized to enter operations */
    data class Ready(val readiness: DriverOperationalReadiness) : DriverReadinessUiStage

    /** Driver has pending platform requirements (verification, agency, profile) */
    data class NotReady(
        val readiness: DriverOperationalReadiness,
        val blockers: List<DriverReadinessBlocker>
    ) : DriverReadinessUiStage

    /** Driver account is administratively suspended from platform operations */
    data class Suspended(
        val readiness: DriverOperationalReadiness,
        val reason: String? = null
    ) : DriverReadinessUiStage

    /** Network or communication error with backend */
    data class Error(val error: IshaaraError, val canRetry: Boolean = true) : DriverReadinessUiStage

    /** Device is offline or network is unreachable */
    data class Offline(val error: IshaaraError? = null) : DriverReadinessUiStage
}

/**
 * Complete immutable UI state for Driver Operational Readiness screen following UDF.
 */
data class DriverOperationalReadinessUiState(
    val stage: DriverReadinessUiStage = DriverReadinessUiStage.Loading,
    val isRefreshing: Boolean = false,
    val userFacingMessage: String? = null
)
