package com.ishara.app.feature.student.search

import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination

/**
 * Presentation state for location search.
 */
data class LocationSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchResultLocation> = emptyList(),
    val recentDestinations: List<StudentDestination> = emptyList(),
    val errorMessage: String? = null,
    val isEmptyResult: Boolean = false
)
