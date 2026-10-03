package com.ishara.app.domain.usecase

import com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator

/**
 * Use case to halt GPS telemetry tracking when a trip completes, cancels, or driver goes offline.
 */
class StopDriverTrackingUseCase(
    private val trackingCoordinator: DriverTrackingCoordinator
) {
    operator fun invoke() {
        trackingCoordinator.stopTracking()
    }
}
