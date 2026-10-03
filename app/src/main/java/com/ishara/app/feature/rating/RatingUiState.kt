package com.ishara.app.feature.rating

import com.ishara.app.domain.model.Rating

/**
 * Immutable UI State for the Post-Ride Rating flow.
 */
sealed interface RatingUiState {

    /** Checking rating eligibility */
    object Loading : RatingUiState

    /** Not eligible to rate (e.g. ride not completed, already rated) */
    data class Ineligible(
        val reason: String,
        val alreadyRated: Boolean
    ) : RatingUiState

    /** Form ready for user input */
    data class Form(
        val rideId: String,
        val score: Int = 5,
        val review: String = "",
        val isSubmitting: Boolean = false,
        val validationError: String? = null,
        val serverError: String? = null
    ) : RatingUiState {
        val reviewCharacterCount: Int
            get() = review.length

        val isReviewLengthValid: Boolean
            get() = review.length <= 500

        val canSubmit: Boolean
            get() = !isSubmitting && score in 1..5 && isReviewLengthValid
    }

    /** Rating successfully submitted */
    data class Success(
        val rating: Rating
    ) : RatingUiState
}
