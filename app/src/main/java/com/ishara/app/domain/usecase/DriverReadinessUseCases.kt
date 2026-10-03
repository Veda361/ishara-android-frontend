package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverOperationalReadiness
import com.ishara.app.domain.repository.DriverReadinessRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase to evaluate authoritative driver operational readiness.
 */
class GetDriverReadinessUseCase(
    private val repository: DriverReadinessRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): IshaaraResult<DriverOperationalReadiness> {
        return repository.getDriverReadiness(forceRefresh)
    }
}

/**
 * UseCase to observe cached driver operational readiness reactively.
 */
class ObserveDriverReadinessUseCase(
    private val repository: DriverReadinessRepository
) {
    operator fun invoke(): StateFlow<DriverOperationalReadiness?> {
        return repository.observeDriverReadiness()
    }
}

/**
 * UseCase to explicitly force-refresh authoritative readiness from backend.
 */
class RefreshDriverReadinessUseCase(
    private val repository: DriverReadinessRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverOperationalReadiness> {
        return repository.refreshDriverReadiness()
    }
}

/**
 * UseCase to flush transient/cached readiness state on logout or account switch.
 */
class ClearDriverReadinessStateUseCase(
    private val repository: DriverReadinessRepository
) {
    operator fun invoke() {
        repository.clearReadinessState()
    }
}
