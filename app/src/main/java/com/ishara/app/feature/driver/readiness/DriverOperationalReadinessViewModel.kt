package com.ishara.app.feature.driver.readiness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.DriverReadinessMapper
import com.ishara.app.domain.model.DriverOperationalReadiness
import com.ishara.app.domain.model.DriverReadinessStatus
import com.ishara.app.domain.usecase.GetDriverReadinessUseCase
import com.ishara.app.domain.usecase.ObserveDriverReadinessUseCase
import com.ishara.app.domain.usecase.RefreshDriverReadinessUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Driver Operational Readiness screen experience.
 *
 * Implements:
 * 1. Strict UDF through [DriverOperationalReadinessUiState].
 * 2. Stage mapping: Loading, Ready, NotReady, Suspended, Error, Offline.
 * 3. Authoritative backend result consumption (no client-side formula).
 * 4. Blocker identification and mapping.
 * 5. Explicit refresh and error retry capabilities.
 */
class DriverOperationalReadinessViewModel(
    private val getDriverReadinessUseCase: GetDriverReadinessUseCase,
    private val refreshDriverReadinessUseCase: RefreshDriverReadinessUseCase,
    private val observeDriverReadinessUseCase: ObserveDriverReadinessUseCase? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(DriverOperationalReadinessUiState())
    val uiState: StateFlow<DriverOperationalReadinessUiState> = _uiState.asStateFlow()

    init {
        loadReadiness(forceRefresh = false)
        observeCachedReadiness()
    }

    private fun observeCachedReadiness() {
        observeDriverReadinessUseCase?.let { useCase ->
            scope.launch(dispatchers.main) {
                useCase().collect { readiness ->
                    if (readiness != null && _uiState.value.stage !is DriverReadinessUiStage.Loading) {
                        _uiState.update { it.copy(stage = resolveStage(readiness)) }
                    }
                }
            }
        }
    }

    fun loadReadiness(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _uiState.update { it.copy(stage = DriverReadinessUiStage.Loading) }
        } else {
            _uiState.update { it.copy(isRefreshing = true) }
        }

        scope.launch(dispatchers.io) {
            val result = if (forceRefresh) {
                refreshDriverReadinessUseCase()
            } else {
                getDriverReadinessUseCase(forceRefresh = false)
            }

            when (result) {
                is IshaaraResult.Success -> {
                    val stage = resolveStage(result.data)
                    _uiState.update {
                        it.copy(
                            stage = stage,
                            isRefreshing = false,
                            userFacingMessage = null
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    val stage = resolveErrorStage(result.error)
                    _uiState.update {
                        it.copy(
                            stage = stage,
                            isRefreshing = false
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadReadiness(forceRefresh = true)
    }

    fun retry() {
        loadReadiness(forceRefresh = true)
    }

    private fun resolveStage(readiness: DriverOperationalReadiness): DriverReadinessUiStage {
        return when {
            readiness.isSuspended -> {
                DriverReadinessUiStage.Suspended(
                    readiness = readiness,
                    reason = if (readiness.reasons.contains(com.ishara.app.domain.model.DriverReadinessReason.DRIVER_SUSPENDED)) {
                        "Account operational privileges have been administratively suspended."
                    } else null
                )
            }
            readiness.status == DriverReadinessStatus.READY && readiness.authorized -> {
                DriverReadinessUiStage.Ready(readiness)
            }
            else -> {
                val blockers = readiness.reasons.map { DriverReadinessMapper.toBlocker(it) }
                DriverReadinessUiStage.NotReady(
                    readiness = readiness,
                    blockers = blockers
                )
            }
        }
    }

    private fun resolveErrorStage(error: IshaaraError): DriverReadinessUiStage {
        return when (error) {
            is IshaaraError.Network -> DriverReadinessUiStage.Offline(error)
            is IshaaraError.Forbidden -> DriverReadinessUiStage.Error(error, canRetry = false)
            else -> DriverReadinessUiStage.Error(error, canRetry = true)
        }
    }

    companion object {
        private const val TAG = "DriverReadinessVM"
    }
}
