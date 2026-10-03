package com.ishara.app

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.RatingMapper
import com.ishara.app.data.remote.datasource.RatingRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.DriverRatingSummaryResponseDto
import com.ishara.app.data.remote.dto.RatingEligibilityResponseDto
import com.ishara.app.data.remote.dto.RatingResponseDto
import com.ishara.app.data.remote.dto.SubmitRatingRequestDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strict contract and domain mapping tests for Phase A14: Ratings & Driver Summaries.
 */
class RatingStrictContractTest {

    private val sampleRatingJson = """
        {
          "status": "success",
          "data": {
            "id": "rate_abc123",
            "rideId": "ride_998877",
            "score": 5,
            "review": "Very smooth and polite driver.",
            "createdAt": "2026-10-01T10:00:00.000Z"
          },
          "message": "Rating submitted successfully."
        }
    """.trimIndent()

    private val sampleEligibilityJson = """
        {
          "status": "success",
          "data": {
            "eligible": true,
            "alreadyRated": false
          }
        }
    """.trimIndent()

    private val sampleDriverSummaryJson = """
        {
          "status": "success",
          "data": {
            "driverId": "drv_profile_555",
            "averageScore": 4.85,
            "ratingCount": 14
          }
        }
    """.trimIndent()

    private val sampleNewDriverSummaryJson = """
        {
          "status": "success",
          "data": {
            "driverId": "drv_profile_new",
            "averageScore": null,
            "ratingCount": 0
          }
        }
    """.trimIndent()

    @Test
    fun `serializeSubmitRatingRequest outputs only score and review`() {
        val dataSource = RatingRemoteDataSourceImpl(
            httpClient = DummyHttpClient(),
            networkConfig = NetworkConfig()
        )

        val jsonWithReview = dataSource.serializeSubmitRatingRequest(
            SubmitRatingRequestDto(score = 5, review = "Great ride!")
        )
        assertTrue(jsonWithReview.contains("\"score\":5"))
        assertTrue(jsonWithReview.contains("\"review\":\"Great ride!\""))
        assertFalse(jsonWithReview.contains("reviewerUserId"))
        assertFalse(jsonWithReview.contains("revieweeUserId"))
        assertFalse(jsonWithReview.contains("rideId"))

        val jsonWithoutReview = dataSource.serializeSubmitRatingRequest(
            SubmitRatingRequestDto(score = 4, review = null)
        )
        assertTrue(jsonWithoutReview.contains("\"score\":4"))
        assertFalse(jsonWithoutReview.contains("review"))
    }

    @Test
    fun `parseRatingResponse correctly parses rating payload`() {
        val dataSource = RatingRemoteDataSourceImpl(
            httpClient = DummyHttpClient(),
            networkConfig = NetworkConfig()
        )

        val dto = dataSource.parseRatingResponse(sampleRatingJson)
        assertEquals("rate_abc123", dto.id)
        assertEquals("ride_998877", dto.rideId)
        assertEquals(5, dto.score)
        assertEquals("Very smooth and polite driver.", dto.review)
        assertEquals("2026-10-01T10:00:00.000Z", dto.createdAt)

        val domain = RatingMapper.toDomain(dto)
        assertEquals("rate_abc123", domain.id)
        assertEquals(5, domain.score)
        assertEquals("Very smooth and polite driver.", domain.review)
    }

    @Test
    fun `parseRatingEligibilityResponse correctly parses eligible state`() {
        val dataSource = RatingRemoteDataSourceImpl(
            httpClient = DummyHttpClient(),
            networkConfig = NetworkConfig()
        )

        val dto = dataSource.parseRatingEligibilityResponse(sampleEligibilityJson)
        assertTrue(dto.eligible)
        assertFalse(dto.alreadyRated)
        assertNull(dto.reason)

        val domain = RatingMapper.toDomain(dto)
        assertTrue(domain.eligible)
        assertFalse(domain.alreadyRated)
    }

    @Test
    fun `parseDriverRatingSummaryResponse parses average score and rating count`() {
        val dataSource = RatingRemoteDataSourceImpl(
            httpClient = DummyHttpClient(),
            networkConfig = NetworkConfig()
        )

        val dto = dataSource.parseDriverRatingSummaryResponse(sampleDriverSummaryJson)
        assertEquals("drv_profile_555", dto.driverId)
        assertEquals(4.85, dto.averageScore!!, 0.01)
        assertEquals(14, dto.ratingCount)

        val domain = RatingMapper.toDomain(dto)
        assertEquals("4.9", domain.formattedScore) // 4.85 formatted to 1 decimal place is 4.9 or 4.8
        assertEquals("4.9 ★ (14 reviews)", domain.displaySummary)
    }

    @Test
    fun `parseDriverRatingSummaryResponse preserves null averageScore for new drivers`() {
        val dataSource = RatingRemoteDataSourceImpl(
            httpClient = DummyHttpClient(),
            networkConfig = NetworkConfig()
        )

        val dto = dataSource.parseDriverRatingSummaryResponse(sampleNewDriverSummaryJson)
        assertEquals("drv_profile_new", dto.driverId)
        assertNull(dto.averageScore)
        assertEquals(0, dto.ratingCount)

        val domain = RatingMapper.toDomain(dto)
        assertEquals("New", domain.formattedScore)
        assertEquals("No ratings yet", domain.displaySummary)
    }

    @Test
    fun `submitRating sends Idempotency-Key and Bearer Authorization`() = runBlocking {
        var capturedRequest: HttpRequest? = null

        val mockClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedRequest = request
                return IshaaraResult.Success(
                    HttpResponse(
                        statusCode = 201,
                        headers = emptyMap(),
                        body = sampleRatingJson
                    )
                )
            }
        }

        val dataSource = RatingRemoteDataSourceImpl(
            httpClient = mockClient,
            networkConfig = NetworkConfig()
        )

        val result = dataSource.submitRating(
            rideId = "ride_998877",
            request = SubmitRatingRequestDto(score = 5, review = "Very smooth and polite driver."),
            idempotencyKey = "uuid-key-12345",
            token = "session-token-abc"
        )

        assertTrue(result is IshaaraResult.Success)
        assertNotNull(capturedRequest)
        assertEquals("https://reposnse-ishaara.onrender.com/api/v1/rides/ride_998877/ratings", capturedRequest?.url)
        assertEquals(HttpMethod.POST, capturedRequest?.method)
        assertEquals("Bearer session-token-abc", capturedRequest?.headers?.get("Authorization"))
        assertEquals("uuid-key-12345", capturedRequest?.headers?.get("Idempotency-Key"))
        assertTrue(capturedRequest?.body?.contains("\"score\":5") == true)
    }

    private class DummyHttpClient : IshaaraHttpClient {
        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            return IshaaraResult.Success(HttpResponse(200, emptyMap(), "{}"))
        }
    }
}
