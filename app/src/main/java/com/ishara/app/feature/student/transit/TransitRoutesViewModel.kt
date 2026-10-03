package com.ishara.app.feature.student.transit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.repository.TransitResource
import com.ishara.app.domain.usecase.GetTransitRoutesUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransitRoutesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val routes: List<TransitRoute> = emptyList(),
    val freshness: TransitCacheFreshness = TransitCacheFreshness.UNAVAILABLE,
    val isOffline: Boolean = false,
    val lastRefreshedAtMillis: Long? = null,
    val errorMessage: String? = null
)

class TransitRoutesViewModel(
    private val getTransitRoutesUseCase: GetTransitRoutesUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitRoutesUiState(isLoading = true))
    val uiState: StateFlow<TransitRoutesUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    init {
        loadRoutes(forceRefresh = false)
    }

    fun loadRoutes(forceRefresh: Boolean = false) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch(dispatchers.main) {
            getTransitRoutesUseCase(forceRefresh = forceRefresh).collect { resource ->
                when (resource) {
                    is TransitResource.Loading -> {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = current.routes.isEmpty() && resource.cachedData.isNullOrEmpty(),
                                isRefreshing = current.routes.isNotEmpty() || !resource.cachedData.isNullOrEmpty(),
                                routes = resource.cachedData ?: current.routes,
                                isOffline = resource.isOffline,
                                errorMessage = null
                            )
                        }
                    }
                    is TransitResource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                routes = resource.data,
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
                                routes = resource.cachedData ?: current.routes,
                                isOffline = resource.isOffline,
                                errorMessage = if (current.routes.isEmpty() && resource.cachedData.isNullOrEmpty()) {
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
        loadRoutes(forceRefresh = true)
    }

    fun onRouteSelected(routeId: String) {
        navigationManager.navigate(IshaaraDestination.StudentRouteDetails.createRoute(routeId))
    }

    fun onNavigateBack() {
        navigationManager.navigateUp()
    }
}
