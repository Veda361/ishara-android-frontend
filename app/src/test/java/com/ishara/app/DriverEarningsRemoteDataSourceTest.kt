package com.ishara.app

import com.ishara.app.core.network.Environment
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSourceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Remote Data Source Tests for Phase 16 Driver Earnings & Historical Analytics.
 */
class DriverEarningsRemoteDataSourceTest {

    private val networkConfig = NetworkConfig(environment = Environment.STAGING)

    @Test
    fun `parseEarningsResponse correctly parses complete backend earnings response`() {
        val json = """
            {
                "success": true,
                "data": {
                    "period": {
                        "period": "week",
                        "from": "2026-09-21T00:00:00.000+05:30",
                        "to": "2026-09-28T23:59:59.999+05:30",
                        "timezone": "Asia/Kolkata"
                    },
                    "summary": {
                        "grossEarningsMinor": 150000,
                        "platformDeductionsMinor": 15000,
                        "netEarningsMinor": 135000,
                        "refundDeductionsMinor": 2000,
                        "completedRidesCount": 5,
                        "settlementSummary": {
                            "settledAmountMinor": 100000,
                            "pendingSettlementAmountMinor": 35000,
                            "unreadySettlementAmountMinor": 0,
                            "failedSettlementAmountMinor": 0
                        },
                        "currency": "INR"
                    },
                    "items": [
                        {
                            "rideId": "ride_101",
                            "tripId": "trip_201",
                            "completedAt": "2026-09-25T11:00:00.000Z",
                            "pickupAddress": "Gate 1, Campus",
                            "destinationAddress": "Metro Station",
                            "grossAmountMinor": 30000,
                            "platformFeeMinor": 3000,
                            "netAmountMinor": 27000,
                            "currency": "INR",
                            "paymentStatus": "CAPTURED",
                            "settlementStatus": "PROCESSED"
                        }
                    ],
                    "pagination": {
                        "total": 5,
                        "page": 1,
                        "limit": 20,
                        "hasMore": false
                    }
                },
                "timestamp": "2026-09-28T14:00:00.000Z"
            }
        """.trimIndent()

        val dataSource = DriverEarningsRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                    IshaaraResult.failure(IshaaraError.Unknown())
            },
            networkConfig = networkConfig
        )

        val dto = dataSource.parseEarningsResponse(json)

        assertEquals("week", dto.period.period)
        assertEquals("2026-09-21T00:00:00.000+05:30", dto.period.from)
        assertEquals(150000L, dto.summary.grossEarningsMinor)
        assertEquals(15000L, dto.summary.platformDeductionsMinor)
        assertEquals(135000L, dto.summary.netEarningsMinor)
        assertEquals(2000L, dto.summary.refundDeductionsMinor)
        assertEquals(5, dto.summary.completedRidesCount)
        assertEquals(100000L, dto.summary.settlementSummary.settledAmountMinor)
        assertEquals(35000L, dto.summary.settlementSummary.pendingSettlementAmountMinor)
        assertEquals("INR", dto.summary.currency)

        assertEquals(1, dto.items.size)
        val item = dto.items[0]
        assertEquals("ride_101", item.rideId)
        assertEquals("trip_201", item.tripId)
        assertEquals("Gate 1, Campus", item.pickupAddress)
        assertEquals("Metro Station", item.destinationAddress)
        assertEquals(30000L, item.grossAmountMinor)
        assertEquals(3000L, item.platformFeeMinor)
        assertEquals(27000L, item.netAmountMinor)
        assertEquals("CAPTURED", item.paymentStatus)
        assertEquals("PROCESSED", item.settlementStatus)

        assertEquals(5, dto.pagination.total)
        assertEquals(1, dto.pagination.page)
        assertEquals(20, dto.pagination.limit)
        assertFalse(dto.pagination.hasMore)
    }

    @Test
    fun `getDriverEarnings constructs correct query params and Bearer header`() = runBlocking {
        var capturedRequest: HttpRequest? = null

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedRequest = request
                val emptyJson = """{"success":true,"data":{"summary":{"grossEarningsMinor":0},"items":[]}}"""
                return IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = emptyJson))
            }
        }

        val dataSource = DriverEarningsRemoteDataSourceImpl(fakeClient, networkConfig)

        val result = dataSource.getDriverEarnings(
            period = "today",
            from = null,
            to = null,
            timezone = "Asia/Kolkata",
            page = 1,
            limit = 20,
            token = "driver_test_jwt"
        )

        assertTrue(result.isSuccess)
        assertNotNull(capturedRequest)
        assertEquals("${networkConfig.fullApiBaseUrl}/drivers/me/earnings", capturedRequest?.url)
        assertEquals(HttpMethod.GET, capturedRequest?.method)
        assertEquals("Bearer driver_test_jwt", capturedRequest?.headers?.get("Authorization"))
        assertEquals("today", capturedRequest?.queryParams?.get("period"))
        assertEquals("Asia/Kolkata", capturedRequest?.queryParams?.get("timezone"))
        assertEquals("1", capturedRequest?.queryParams?.get("page"))
        assertEquals("20", capturedRequest?.queryParams?.get("limit"))
    }

    @Test
    fun `getDriverRidesWithFinancials requests withFinancials=true and parses items`() = runBlocking {
        var capturedRequest: HttpRequest? = null

        val ridesJson = """
            {
                "success": true,
                "data": {
                    "items": [
                        {
                            "id": "ride_999",
                            "tripId": "trip_888",
                            "completedAt": "2026-09-28T10:00:00.000Z",
                            "pickup": {
                                "formattedAddress": "Bus Stand"
                            },
                            "destination": {
                                "formattedAddress": "Tech Park"
                            },
                            "financialStatus": {
                                "grossAmountMinor": 40000,
                                "platformFeeMinor": 4000,
                                "netAmountMinor": 36000,
                                "currency": "INR",
                                "paymentStatus": "CAPTURED",
                                "settlementStatus": "PENDING"
                            }
                        }
                    ],
                    "total": 1,
                    "page": 1,
                    "limit": 20,
                    "hasMore": false
                }
            }
        """.trimIndent()

        val fakeClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedRequest = request
                return IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = ridesJson))
            }
        }

        val dataSource = DriverEarningsRemoteDataSourceImpl(fakeClient, networkConfig)

        val result = dataSource.getDriverRidesWithFinancials(
            status = "COMPLETED",
            tripId = null,
            period = "today",
            from = null,
            to = null,
            timezone = "Asia/Kolkata",
            page = 1,
            limit = 20,
            token = "driver_token_abc"
        )

        assertTrue(result.isSuccess)
        assertNotNull(capturedRequest)
        assertEquals("${networkConfig.fullApiBaseUrl}/drivers/me/rides", capturedRequest?.url)
        assertEquals("true", capturedRequest?.queryParams?.get("withFinancials"))
        assertEquals("COMPLETED", capturedRequest?.queryParams?.get("status"))

        val dto = result.getOrNull()
        assertNotNull(dto)
        assertEquals(1, dto?.items?.size)
        val item = dto?.items?.get(0)
        assertEquals("ride_999", item?.rideId)
        assertEquals("Bus Stand", item?.pickupAddress)
        assertEquals("Tech Park", item?.destinationAddress)
        assertEquals(40000L, item?.grossAmountMinor)
        assertEquals(4000L, item?.platformFeeMinor)
        assertEquals(36000L, item?.netAmountMinor)
        assertEquals("CAPTURED", item?.paymentStatus)
        assertEquals("PENDING", item?.settlementStatus)
    }
}
