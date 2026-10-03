package com.ishara.app.feature.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.usecase.CheckRatingEligibilityUseCase
import com.ishara.app.domain.usecase.SubmitRatingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Production ViewModel managing the Post-Ride Rating lifecycle.
 * Enforces client-side validation, server eligibility checks, idempotency, and clean failure states.
 */
class RatingViewModel(
    val rideId: String,
    private val checkRatingEligibilityUseCase: CheckRatingEligibilityUseCase,
    private val submitRatingUseCase: SubmitRatingUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val tag = "RatingViewModel"
    private var cachedIdempotencyKey: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow<RatingUiState>(RatingUiState.Loading)
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()

    init {
        checkEligibility()
    }

    fun checkEligibility() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = RatingUiState.Loading

            when (val result = checkRatingEligibilityUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    val eligibility = result.data
                    if (eligibility.alreadyRated) {
                        _uiState.value = RatingUiState.Ineligible(
                            reason = "You have already submitted a rating for this trip.",
                            alreadyRated = true
                        )
                    } else if (!eligibility.eligible) {
                        val reasonText = when (eligibility.reason) {
                            "RIDE_NOT_COMPLETED" -> "This ride has not been completed yet."
                            "NOT_A_PARTICIPANT" -> "Only participants of this ride can submit a rating."
                            else -> eligibility.reason ?: "Ride is currently not eligible for rating."
                        }
                        _uiState.value = RatingUiState.Ineligible(
                            reason = reasonText,
                            alreadyRated = false
                        )
                    } else {
                        _uiState.value = RatingUiState.Form(rideId = rideId)
                    }
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Failed to check rating eligibility: ${result.error}")
                    // Gracefully allow form entry; backend will validate definitively on submit
                    _uiState.value = RatingUiState.Form(rideId = rideId)
                }
            }
        }
    }

    fun onScoreChanged(newScore: Int) {
        val bounded = newScore.coerceIn(1, 5)
        updateForm { it.copy(score = bounded, validationError = null) }
    }

    fun onReviewChanged(text: String) {
        if (text.length > 500) {
            updateForm {
                it.copy(
                    review = text.take(500),
                    validationError = "Review cannot exceed 500 characters"
                )
            }
        } else {
            updateForm {
                it.copy(
                    review = text,
                    validationError = null
                )
            }
        }
    }

    fun submitRating() {
        val currentForm = (_uiState.value as? RatingUiState.Form) ?: return
        if (currentForm.isSubmitting) return

        if (currentForm.score !in 1..5) {
            updateForm { it.copy(validationError = "Please select a rating between 1 and 5 stars") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            updateForm { it.copy(isSubmitting = true, serverError = null, validationError = null) }

            val result = submitRatingUseCase(
                rideId = rideId,
                score = currentForm.score,
                review = currentForm.review.trim().ifBlank { null },
                idempotencyKey = cachedIdempotencyKey
            )

            when (result) {
                is IshaaraResult.Success -> {
                    IshaaraLogger.i(tag, "Rating submitted successfully: id=${result.data.id}")
                    _uiState.value = RatingUiState.Success(result.data)
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.e(tag, "Failed to submit rating: ${result.error}")
                    val userMessage = mapErrorToUserMessage(result.error)
                    updateForm {
                        it.copy(
                            isSubmitting = false,
                            serverError = userMessage
                        )
                    }
                }
            }
        }
    }

    private fun mapErrorToUserMessage(error: IshaaraError): String {
        return when (error) {
            is IshaaraError.Network -> "Network connection unavailable. Please check your connection and try again."
            is IshaaraError.Server -> {
                when {
                    error.code == 409 || error.message.contains("RATING_ALREADY_SUBMITTED") ->
                        "A rating has already been submitted for this ride."
                    error.code == 403 || error.message.contains("RIDE_NOT_COMPLETED") ->
                        "This ride cannot be rated because it has not been marked completed."
                    error.code == 429 || error.message.contains("RATING_RATE_LIMITED") ->
                        "Too many rating submissions. Please wait a moment before trying again."
                    else -> error.message.ifBlank { "Failed to submit rating. Please try again." }
                }
            }
            is IshaaraError.Validation -> error.message
            else -> "An unexpected error occurred while submitting your review."
        }
    }

    private inline fun updateForm(transform: (RatingUiState.Form) -> RatingUiState.Form) {
        _uiState.update { current ->
            if (current is RatingUiState.Form) transform(current) else current
        }
    }
}
