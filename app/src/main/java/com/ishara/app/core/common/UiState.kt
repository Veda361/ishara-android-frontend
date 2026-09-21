package com.ishara.app.core.common

import com.ishara.app.core.result.IshaaraError

/**
 * Common state representation for Screen UI and ViewModels.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Empty(val message: String = "No data available") : UiState<Nothing>
    data class Error(val error: IshaaraError) : UiState<Nothing>

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
