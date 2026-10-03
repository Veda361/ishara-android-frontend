package com.ishara.app.feature.student.transit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.data.mapper.TransitMapper
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.model.TransitSchedule
import com.ishara.app.domain.repository.TransitResource
import com.ishara.app.domain.usecase.GetRouteDetailsUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RouteDetailsUiState(
    val routeId: String,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val route: TransitRoute? = null,
    val schedule: TransitSchedule? = null,
    val freshness: TransitCacheFreshness = TransitCacheFreshness.UNAVAILABLE,
    val isOffline: Boolean = false,
    val lastRefreshedAtMillis: Long? = null,
    val errorMessage: String? = null
)

class RouteDetailsViewModel(
    val routeId: String,
    private val getRouteDetailsUseCase: GetRouteDetailsUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteDetailsUiState(routeId = routeId, isLoading = true))
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    init {
        loadDetails(forceRefresh = false)
    }

    fun loadDetails(forceRefresh: Boolean = false) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch(dispatchers.main) {
            getRouteDetailsUseCase(routeId = routeId, forceRefresh = forceRefresh).collect { resource ->
                when (resource) {
                    is TransitResource.Loading -> {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = current.route == null && resource.cachedData == null,
                                isRefreshing = current.route != null || resource.cachedData != null,
                                route = resource.cachedData ?: current.route,
                                schedule = (resource.cachedData ?: current.route)?.let { TransitMapper.toSchedule(it) },
                                isOffline = resource.isOffline,
                                errorMessage = null
                            )
                        }
                    }
                    is TransitResource.Success -> {
                        val route = resource.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                route = route,
                                schedule = TransitMapper.toSchedule(route),
                                freshness = resource.freshness,
                                isOffline = resource.isOffline,
                                lastRefreshedAtMillis = resource.lastRefreshedAt,
                                errorMessage = null
                            )
                        }
                    }
                    is TransitResource.Error -> {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                route = resource.cachedData ?: current.route,
                                schedule = (resource.cachedData ?: current.route)?.let { TransitMapper.toSchedule(it) },
                                isOffline = resource.isOffline,
                                errorMessage = if (current.route == null && resource.cachedData == null) {
                                    resource.message
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        loadDetails(forceRefresh = true)
    }

    fun onNavigateBack() {
        navigationManager.navigateUp()
    }
}
