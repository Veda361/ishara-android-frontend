package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverRatingSummary
import com.ishara.app.domain.model.Rating
import com.ishara.app.domain.model.RatingEligibility
import com.ishara.app.domain.repository.RatingRepository
import com.ishara.app.domain.usecase.CheckRatingEligibilityUseCase
import com.ishara.app.domain.usecase.SubmitRatingUseCase
import com.ishara.app.feature.rating.RatingUiState
import com.ishara.app.feature.rating.RatingViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingViewModelTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private val testDispatchers = TestDispatcherProvider()

    @Test
    fun `initial state becomes Form when ride is eligible`() = runBlocking {
        val fakeRepo = FakeRatingRepository(
            eligibility = RatingEligibility(eligible = true, alreadyRated = false)
        )
        val viewModel = createViewModel(fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state is RatingUiState.Form)
        val form = state as RatingUiState.Form
        assertEquals(5, form.score)
        assertEquals("", form.review)
        assertFalse(form.isSubmitting)
    }

    @Test
    fun `initial state becomes Ineligible when already rated`() = runBlocking {
        val fakeRepo = FakeRatingRepository(
            eligibility = RatingEligibility(eligible = false, alreadyRated = true)
        )
        val viewModel = createViewModel(fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state is RatingUiState.Ineligible)
        val inelig = state as RatingUiState.Ineligible
        assertTrue(inelig.alreadyRated)
        assertEquals("You have already submitted a rating for this trip.", inelig.reason)
    }

    @Test
    fun `initial state becomes Ineligible when ride not completed`() = runBlocking {
        val fakeRepo = FakeRatingRepository(
            eligibility = RatingEligibility(eligible = false, alreadyRated = false, reason = "RIDE_NOT_COMPLETED")
        )
        val viewModel = createViewModel(fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state is RatingUiState.Ineligible)
        val inelig = state as RatingUiState.Ineligible
        assertFalse(inelig.alreadyRated)
        assertEquals("This ride has not been completed yet.", inelig.reason)
    }

    @Test
    fun `onScoreChanged clamps values between 1 and 5`() = runBlocking {
        val fakeRepo = FakeRatingRepository()
        val viewModel = createViewModel(fakeRepo)

        viewModel.onScoreChanged(4)
        assertEquals(4, (viewModel.uiState.value as RatingUiState.Form).score)

        viewModel.onScoreChanged(10)
        assertEquals(5, (viewModel.uiState.value as RatingUiState.Form).score)

        viewModel.onScoreChanged(-1)
        assertEquals(1, (viewModel.uiState.value as RatingUiState.Form).score)
    }

    @Test
    fun `onReviewChanged caps at 500 characters and sets validation error`() = runBlocking {
        val fakeRepo = FakeRatingRepository()
        val viewModel = createViewModel(fakeRepo)

        val over500 = "x".repeat(505)
        viewModel.onReviewChanged(over500)

        val form = viewModel.uiState.value as RatingUiState.Form
        assertEquals(500, form.review.length)
        assertEquals("Review cannot exceed 500 characters", form.validationError)
    }

    @Test
    fun `submitRating transitions to Success state upon completion`() = runBlocking {
        val fakeRepo = FakeRatingRepository()
        val viewModel = createViewModel(fakeRepo)

        viewModel.onScoreChanged(5)
        viewModel.onReviewChanged("Excellent ride experience!")
        viewModel.submitRating()

        val state = viewModel.uiState.value
        assertTrue(state is RatingUiState.Success)
        val success = state as RatingUiState.Success
        assertEquals("rate_test_123", success.rating.id)
        assertEquals(5, success.rating.score)
    }

    @Test
    fun `submitRating handles 409 duplicate submission error gracefully`() = runBlocking {
        val fakeRepo = FakeRatingRepository(
            submitResult = IshaaraResult.Failure(
                IshaaraError.Server(409, "RATING_ALREADY_SUBMITTED")
            )
        )
        val viewModel = createViewModel(fakeRepo)

        viewModel.submitRating()

        val state = viewModel.uiState.value
        assertTrue(state is RatingUiState.Form)
        val form = state as RatingUiState.Form
        assertFalse(form.isSubmitting)
        assertEquals("A rating has already been submitted for this ride.", form.serverError)
    }

    private fun createViewModel(repo: RatingRepository): RatingViewModel {
        return RatingViewModel(
            rideId = "ride_test_999",
            checkRatingEligibilityUseCase = CheckRatingEligibilityUseCase(repo),
            submitRatingUseCase = SubmitRatingUseCase(repo),
            dispatchers = testDispatchers
        )
    }

    private class FakeRatingRepository(
        var eligibility: RatingEligibility = RatingEligibility(eligible = true, alreadyRated = false),
        var submitResult: IshaaraResult<Rating>? = null
    ) : RatingRepository {

        override suspend fun checkRatingEligibility(rideId: String): IshaaraResult<RatingEligibility> {
            return IshaaraResult.Success(eligibility)
        }

        override suspend fun submitRating(
            rideId: String,
            score: Int,
            review: String?,
            idempotencyKey: String?
        ): IshaaraResult<Rating> {
            return submitResult ?: IshaaraResult.Success(
                Rating(
                    id = "rate_test_123",
                    rideId = rideId,
                    score = score,
                    review = review,
                    createdAt = "2026-10-01T12:00:00.000Z"
                )
            )
        }

        override suspend fun getRatingsByRide(rideId: String): IshaaraResult<List<Rating>> {
            return IshaaraResult.Success(emptyList())
        }

        override suspend fun getMyRatingSummary(): IshaaraResult<DriverRatingSummary> {
            return IshaaraResult.Success(
                DriverRatingSummary(driverId = "drv_test", averageScore = 4.9, ratingCount = 20)
            )
        }
    }
}
