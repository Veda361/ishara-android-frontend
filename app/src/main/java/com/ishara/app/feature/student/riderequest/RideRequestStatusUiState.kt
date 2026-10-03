package com.ishara.app.feature.student.riderequest

import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus

/**
 * UDF single source of truth for the Ride Request status tracking screen.
 * Tracks the authoritative lifecycle status (PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED).
 */
data class RideRequestStatusUiState(
    val requestId: String,
    val trip: DiscoveredTrip? = null,
    val request: RideRequestResult? = null,
    val status: RideRequestStatus = RideRequestStatus.PENDING,
    val isLoading: Boolean = false,
    val isCancelling: Boolean = false,
    val errorMessage: String? = null,
    val cancelSuccessMessage: String? = null,
    val associatedRideId: String? = null
)
