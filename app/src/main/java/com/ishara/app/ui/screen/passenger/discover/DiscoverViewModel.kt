package com.ishara.app.ui.screen.passenger.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.domain.usecase.DiscoverTripsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val discoverTripsUseCase: DiscoverTripsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    fun onEvent(event: DiscoverEvent) {
        when (event) {
            is DiscoverEvent.SearchTrips -> {
                searchTrips(event)
            }
            DiscoverEvent.Refresh -> {
                // Logic to refresh last search if any
            }
        }
    }

    private fun searchTrips(event: DiscoverEvent.SearchTrips) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            discoverTripsUseCase(
                pickup = event.origin,
                destination = event.destination,
                passengerCount = event.passengerCount
            ).onSuccess { trips ->
                _uiState.update { it.copy(isLoading = false, trips = trips) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
