package com.ishara.app.ui.screen.passenger.discover

import com.ishara.app.domain.model.Trip

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val trips: List<Trip> = emptyList(),
    val error: String? = null
)
