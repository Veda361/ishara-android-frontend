package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.remote.datasource.RatingRemoteDataSource
import com.ishara.app.data.remote.dto.DriverRatingSummaryResponseDto
import com.ishara.app.data.remote.dto.RatingEligibilityResponseDto
import com.ishara.app.data.remote.dto.RatingResponseDto
import com.ishara.app.data.remote.dto.SubmitRatingRequestDto
import com.ishara.app.data.repository.RatingRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingRepositoryTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private val testDispatchers = TestDispatcherProvider()

    private val fakeSessionStore = InMemorySessionStore(
        AuthSession(
            token = "valid_test_token_123",
            userId = "user_456",
            role = UserRole.USER
        )
    )

    @Test
    fun `submitRating rejects score below 1 with validation error`() = runBlocking {
        val repo = RatingRepositoryImpl(
            remoteDataSource = FakeRatingRemoteDataSource(),
            sessionStore = fakeSessionStore,
            dispatchers = testDispatchers
        )

        val result = repo.submitRating(rideId = "ride_123", score = 0)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("Score must be an integer between 1 and 5", error.message)
    }

    @Test
    fun `submitRating rejects score above 5 with validation error`() = runBlocking {
        val repo = RatingRepositoryImpl(
            remoteDataSource = FakeRatingRemoteDataSource(),
            sessionStore = fakeSessionStore,
            dispatchers = testDispatchers
        )

        val result = repo.submitRating(rideId = "ride_123", score = 6)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("Score must be an integer between 1 and 5", error.message)
    }

    @Test
    fun `submitRating rejects review exceeding 500 characters`() = runBlocking {
        val repo = RatingRepositoryImpl(
            remoteDataSource = FakeRatingRemoteDataSource(),
            sessionStore = fakeSessionStore,
            dispatchers = testDispatchers
        )

        val longReview = "a".repeat(501)
        val result = repo.submitRating(rideId = "ride_123", score = 5, review = longReview)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("Review must not exceed 500 characters", error.message)
    }

    @Test
    fun `submitRating successfully invokes remote and maps domain`() = runBlocking {
        val fakeDataSource = FakeRatingRemoteDataSource()
        val repo = RatingRepositoryImpl(
            remoteDataSource = fakeDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = testDispatchers
        )

        val result = repo.submitRating(
            rideId = "ride_123",
            score = 5,
            review = "Punctual and courteous"
        )

        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertEquals("rate_fake_1", data.id)
        assertEquals("ride_123", data.rideId)
        assertEquals(5, data.score)
        assertEquals("Punctual and courteous", data.review)
        assertEquals("valid_test_token_123", fakeDataSource.lastToken)
    }

    @Test
    fun `checkRatingEligibility maps remote response correctly`() = runBlocking {
        val repo = RatingRepositoryImpl(
            remoteDataSource = FakeRatingRemoteDataSource(),
            sessionStore = fakeSessionStore,
            dispatchers = testDispatchers
        )

        val result = repo.checkRatingEligibility("ride_123")
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertTrue(data.eligible)
        assertEquals(false, data.alreadyRated)
    }

    @Test
    fun `getMyRatingSummary maps driver rating summary correctly`() = runBlocking {
        val repo = RatingRepositoryImpl(
            remoteDataSource = FakeRatingRemoteDataSource(),
            sessionStore = fakeSessionStore,
            dispatchers = testDispatchers
        )

        val result = repo.getMyRatingSummary()
        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertEquals("drv_profile_99", data.driverId)
        assertEquals(4.75, data.averageScore!!, 0.01)
        assertEquals(8, data.ratingCount)
    }

    private class FakeRatingRemoteDataSource : RatingRemoteDataSource {
        var lastToken: String? = null

        override suspend fun checkRatingEligibility(
            rideId: String,
            token: String?
        ): IshaaraResult<RatingEligibilityResponseDto> {
            lastToken = token
            return IshaaraResult.Success(
                RatingEligibilityResponseDto(eligible = true, alreadyRated = false)
            )
        }

        override suspend fun submitRating(
            rideId: String,
            request: SubmitRatingRequestDto,
            idempotencyKey: String?,
            token: String?
        ): IshaaraResult<RatingResponseDto> {
            lastToken = token
            return IshaaraResult.Success(
                RatingResponseDto(
                    id = "rate_fake_1",
                    rideId = rideId,
                    score = request.score,
                    review = request.review,
                    createdAt = "2026-10-01T12:00:00.000Z"
                )
            )
        }

        override suspend fun getRatingsByRide(
            rideId: String,
            token: String?
        ): IshaaraResult<List<RatingResponseDto>> {
            lastToken = token
            return IshaaraResult.Success(emptyList())
        }

        override suspend fun getMyRatingSummary(
            token: String?
        ): IshaaraResult<DriverRatingSummaryResponseDto> {
            lastToken = token
            return IshaaraResult.Success(
                DriverRatingSummaryResponseDto(
                    driverId = "drv_profile_99",
                    averageScore = 4.75,
                    ratingCount = 8
                )
            )
        }
    }
}
