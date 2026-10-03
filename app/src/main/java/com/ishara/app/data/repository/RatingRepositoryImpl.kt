package com.ishara.app.data.repository

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.RatingMapper
import com.ishara.app.data.remote.datasource.RatingRemoteDataSource
import com.ishara.app.data.remote.dto.SubmitRatingRequestDto
import com.ishara.app.domain.model.DriverRatingSummary
import com.ishara.app.domain.model.Rating
import com.ishara.app.domain.model.RatingEligibility
import com.ishara.app.domain.repository.RatingRepository
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production implementation of [RatingRepository].
 * Handles session token injection, background dispatching, and DTO-to-Domain mapping.
 */
class RatingRepositoryImpl(
    private val remoteDataSource: RatingRemoteDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : RatingRepository {

    override suspend fun checkRatingEligibility(
        rideId: String
    ): IshaaraResult<RatingEligibility> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.checkRatingEligibility(rideId, token).map { dto ->
            RatingMapper.toDomain(dto)
        }
    }

    override suspend fun submitRating(
        rideId: String,
        score: Int,
        review: String?,
        idempotencyKey: String?
    ): IshaaraResult<Rating> = withContext(dispatchers.io) {
        // Pre-validation in repository
        if (score < 1 || score > 5) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Validation(field = "score", message = "Score must be an integer between 1 and 5")
            )
        }
        if (review != null && review.length > 500) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Validation(field = "review", message = "Review must not exceed 500 characters")
            )
        }

        val session = sessionStore.getSession()
        val token = session?.token
        val key = idempotencyKey ?: UUID.randomUUID().toString()

        val request = SubmitRatingRequestDto(
            score = score,
            review = review?.trim()?.ifBlank { null }
        )

        remoteDataSource.submitRating(rideId, request, key, token).map { dto ->
            RatingMapper.toDomain(dto)
        }
    }

    override suspend fun getRatingsByRide(
        rideId: String
    ): IshaaraResult<List<Rating>> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.getRatingsByRide(rideId, token).map { dtos ->
            dtos.map { RatingMapper.toDomain(it) }
        }
    }

    override suspend fun getMyRatingSummary(): IshaaraResult<DriverRatingSummary> =
        withContext(dispatchers.io) {
            val session = sessionStore.getSession()
            val token = session?.token

            remoteDataSource.getMyRatingSummary(token).map { dto ->
                RatingMapper.toDomain(dto)
            }
        }
}
