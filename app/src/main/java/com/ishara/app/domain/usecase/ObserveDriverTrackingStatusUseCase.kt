package com.ishara.app.domain.usecase

import com.ishara.app.feature.driver.tracking.DriverTrackingCoordinator
import com.ishara.app.feature.driver.tracking.DriverTrackingStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case to observe the live operational status of the driver telemetry engine.
 */
class ObserveDriverTrackingStatusUseCase(
    private val trackingCoordinator: DriverTrackingCoordinator
) {
    operator fun invoke(): StateFlow<DriverTrackingStatus> {
        return trackingCoordinator.status
    }
}
