package com.ishara.app.feature.student.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase
import com.ishara.app.domain.usecase.SearchLocationsUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing responsive, debounced destination search.
 * Features:
 * - 350ms search debounce
 * - Out-of-order race condition cancellation via flatMapLatest
 * - Instant clear and retry handling
 * - Clean domain model mapping (zero raw API leak into UI)
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LocationSearchViewModel(
    private val searchLocationsUseCase: SearchLocationsUseCase,
    private val manageRecentDestinationsUseCase: ManageRecentDestinationsUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null,
    private val debounceMillis: Long = 350L
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(LocationSearchUiState())
    val uiState: StateFlow<LocationSearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        observeRecentDestinations()
        setupDebouncedSearch()
    }

    private fun observeRecentDestinations() {
        scope.launch(dispatchers.io) {
            manageRecentDestinationsUseCase.getRecentDestinations().collect { recents ->
                _uiState.update { it.copy(recentDestinations = recents) }
            }
        }
    }

    private fun setupDebouncedSearch() {
        scope.launch(dispatchers.io) {
            _queryFlow
                .debounce(debounceMillis)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    flow {
                        val trimmed = query.trim()
                        if (trimmed.length < 2) {
                            emit(SearchResultInternal.Idle)
                        } else {
                            emit(SearchResultInternal.Loading)
                            val result = searchLocationsUseCase(trimmed)
                            emit(SearchResultInternal.Completed(result))
                        }
                    }
                }
                .collect { searchResult ->
                    when (searchResult) {
                        is SearchResultInternal.Idle -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    results = emptyList(),
                                    errorMessage = null,
                                    isEmptyResult = false
                                )
                            }
                        }
                        is SearchResultInternal.Loading -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = true,
                                    errorMessage = null,
                                    isEmptyResult = false
                                )
                            }
                        }
                        is SearchResultInternal.Completed -> {
                            when (val result = searchResult.result) {
                                is IshaaraResult.Success -> {
                                    val locations = result.data
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            results = locations,
                                            isEmptyResult = locations.isEmpty(),
                                            errorMessage = null
                                        )
                                    }
                                }
                                is IshaaraResult.Failure -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            results = emptyList(),
                                            isEmptyResult = false,
                                            errorMessage = "Couldn't load nearby locations. Please check your connection."
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onClearQuery() {
        onQueryChange("")
    }

    fun onRetry() {
        val currentQuery = _uiState.value.query
        _queryFlow.value = ""
        _queryFlow.value = currentQuery
    }

    fun onLocationSelected(location: SearchResultLocation, onSelected: (StudentDestination) -> Unit) {
        val destination = StudentDestination(
            name = location.name,
            formattedAddress = location.formattedAddress,
            coordinates = location.coordinates
        )
        scope.launch(dispatchers.io) {
            manageRecentDestinationsUseCase.saveDestination(destination)
        }
        onSelected(destination)
        navigationManager.navigateUp()
    }

    fun onRecentSelected(destination: StudentDestination, onSelected: (StudentDestination) -> Unit) {
        onSelected(destination)
        navigationManager.navigateUp()
    }

    fun onBackClick() {
        navigationManager.navigateUp()
    }

    private sealed interface SearchResultInternal {
        object Idle : SearchResultInternal
        object Loading : SearchResultInternal
        data class Completed(val result: IshaaraResult<List<SearchResultLocation>>) : SearchResultInternal
    }
}
