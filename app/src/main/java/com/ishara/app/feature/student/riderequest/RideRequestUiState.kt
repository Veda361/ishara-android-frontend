package com.ishara.app.feature.student.riderequest

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.RideRequestResult

/**
 * Lifecycle stage for the Ride Request review and submission flow.
 */
sealed interface RideRequestStage {
    object ReviewReady : RideRequestStage
    object Submitting : RideRequestStage
    data class Submitted(val result: RideRequestResult) : RideRequestStage
    data class Error(val error: IshaaraError) : RideRequestStage
}

/**
 * UDF single source of truth for the Ride Request review screen.
 */
data class RideRequestUiState(
    val trip: DiscoveredTrip,
    val pickupAddress: String,
    val destinationAddress: String,
    val stage: RideRequestStage = RideRequestStage.ReviewReady,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)
