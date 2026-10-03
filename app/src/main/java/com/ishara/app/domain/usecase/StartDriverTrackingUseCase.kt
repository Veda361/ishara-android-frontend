package com.ishara.app.domain.usecase

import com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator

/**
 * Use case to initiate continuous GPS telemetry tracking for an active trip.
 */
class StartDriverTrackingUseCase(
    private val trackingCoordinator: DriverTrackingCoordinator
) {
    operator fun invoke(tripId: String) {
        trackingCoordinator.startTracking(tripId)
    }
}
