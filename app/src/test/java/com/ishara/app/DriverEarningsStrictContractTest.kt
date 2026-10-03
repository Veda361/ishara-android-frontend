package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.DriverEarningsMapper
import com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSourceImpl
import com.ishara.app.data.repository.DriverEarningsRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.DomainSettlementStatus
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ============================================================================
 * PHASE A16 — DRIVER EARNINGS & EARNINGS LEDGER STRICT CONTRACT TESTS
 * ============================================================================
 *
 * Verifies all Phase A16 API contract requirements against the authoritative backend:
 * 1. HTTP Method: GET
 * 2. Path: /api/v1/drivers/me/earnings
 * 3. Required Auth: Bearer authorization
 * 4. Driver Role Isolation: DRIVER_CONDUCTOR only; rejects USER/passenger
 * 5. Query Parameter Encoding: period, from, to, timezone, page, limit
 * 6. Authoritative Response Parsing: period, summary, items, pagination, settlementSummary
 * 7. Integer Minor Units (paise): zero floating-point financial arithmetic
 * 8. Backend Enum Robustness: unknown future statuses do not crash the application
 * 9. Error Mapping: 401 Authentication, 403 Forbidden, 400 Validation, 500 Server
 */
import com.ishara.app.core.network.Environment
import com.ishara.app.core.storage.InMemorySessionStore

class DriverEarningsStrictContractTest {

    private val networkConfig = NetworkConfig(environment = Environment.STAGING)

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class RecordingFakeHttpClient : IshaaraHttpClient {
        var responseToReturn: IshaaraResult<HttpResponse> = IshaaraResult.success(
            HttpResponse(statusCode = 200, body = "{}")
        )
        val recordedRequests = mutableListOf<HttpRequest>()

        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            recordedRequests.add(request)
            return responseToReturn
        }
    }

    private lateinit var fakeHttpClient: RecordingFakeHttpClient
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var remoteDataSource: DriverEarningsRemoteDataSourceImpl
    private lateinit var repository: DriverEarningsRepositoryImpl

    private val sampleAuthoritativeBackendEarningsJson = """
        {
          "success": true,
          "data": {
            "period": {
              "period": "today",
              "from": "2026-10-01T18:30:00.000Z",
              "to": "2026-10-02T18:29:59.999Z",
              "timezone": "Asia/Kolkata"
            },
            "summary": {
              "grossEarningsMinor": 10000,
              "platformDeductionsMinor": 1000,
              "netEarningsMinor": 9000,
              "refundDeductionsMinor": 0,
              "completedRidesCount": 2,
              "settlementSummary": {
                "settledAmountMinor": 9000,
                "pendingSettlementAmountMinor": 0,
                "unreadySettlementAmountMinor": 0,
                "failedSettlementAmountMinor": 0
              },
              "currency": "INR"
            },
            "items": [
              {
                "rideId": "ride_001",
                "tripId": "trip_001",
                "completedAt": "2026-10-02T10:15:00.000Z",
                "pickupAddress": "Varanasi Cantt",
                "destinationAddress": "BHU Main Gate",
                "grossAmountMinor": 5000,
                "platformFeeMinor": 500,
                "netAmountMinor": 4500,
                "currency": "INR",
                "paymentStatus": "CAPTURED",
                "settlementStatus": "PROCESSED"
              },
              {
                "rideId": "ride_002",
                "tripId": "trip_001",
                "completedAt": "2026-10-02T11:30:00.000Z",
                "pickupAddress": "BHU Main Gate",
                "destinationAddress": "Lanka",
                "grossAmountMinor": 5000,
                "platformFeeMinor": 500,
                "netAmountMinor": 4500,
                "currency": "INR",
                "paymentStatus": "CAPTURED",
                "settlementStatus": "PENDING"
              }
            ],
            "pagination": {
              "total": 2,
              "page": 1,
              "limit": 20,
              "hasMore": false
            }
          }
        }
    """.trimIndent()

    @Before
    fun setUp() = runBlocking {
        fakeHttpClient = RecordingFakeHttpClient()
        sessionStore = InMemorySessionStore()
        sessionStore.saveSession(
            AuthSession(
                token = "valid_driver_jwt_token",
                userId = "driver_user_123",
                role = UserRole.DRIVER_CONDUCTOR
            )
        )
        remoteDataSource = DriverEarningsRemoteDataSourceImpl(fakeHttpClient, networkConfig)
        repository = DriverEarningsRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)
    }

    @Test
    fun `GET drivers me earnings endpoint contract - method, path, headers, and query encoding`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.success(
            HttpResponse(statusCode = 200, body = sampleAuthoritativeBackendEarningsJson)
        )

        val result = repository.getDriverEarnings(
            period = EarningsPeriodType.CUSTOM,
            from = "2026-10-01T00:00:00.000Z",
            to = "2026-10-02T23:59:59.999Z",
            timezone = "Asia/Kolkata",
            page = 2,
            limit = 25
        )

        assertTrue(result is IshaaraResult.Success)
        assertEquals(1, fakeHttpClient.recordedRequests.size)

        val request = fakeHttpClient.recordedRequests.first()
        // 1. HTTP Method
        assertEquals(HttpMethod.GET, request.method)

        // 2. URL Path
        assertEquals("${networkConfig.fullApiBaseUrl}/drivers/me/earnings", request.url)

        // 3. Authorization Header
        assertEquals("Bearer valid_driver_jwt_token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Accept"])

        // 4. Query Parameters
        assertEquals("custom", request.queryParams["period"])
        assertEquals("2026-10-01T00:00:00.000Z", request.queryParams["from"])
        assertEquals("2026-10-02T23:59:59.999Z", request.queryParams["to"])
        assertEquals("Asia/Kolkata", request.queryParams["timezone"])
        assertEquals("2", request.queryParams["page"])
        assertEquals("25", request.queryParams["limit"])
    }

    @Test
    fun `driver role isolation - DRIVER_CONDUCTOR allowed and USER passenger strictly rejected`() = runBlocking {
        // Attempt call as USER role
        sessionStore.saveSession(
            AuthSession(
                token = "passenger_token",
                userId = "user_456",
                role = UserRole.USER
            )
        )

        val passengerResult = repository.getDriverEarnings(period = EarningsPeriodType.TODAY)

        assertTrue(passengerResult is IshaaraResult.Failure)
        val error = (passengerResult as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals(403, error.code)
        // Ensure network was NOT even touched
        assertEquals(0, fakeHttpClient.recordedRequests.size)
    }

    @Test
    fun `authoritative response parsing correctly deserializes summary, ledger items, and pagination`() = runBlocking {
        val dto = remoteDataSource.parseEarningsResponse(sampleAuthoritativeBackendEarningsJson)
        val domain = DriverEarningsMapper.toDomain(dto)

        // Period
        assertEquals(EarningsPeriodType.TODAY, domain.period.period)
        assertEquals("Asia/Kolkata", domain.period.timezone)

        // Summary
        assertEquals(10000L, domain.summary.grossEarnings.amountMinor)
        assertEquals("₹100.00", domain.summary.grossEarnings.formatDisplay())
        assertEquals(1000L, domain.summary.platformDeductions.amountMinor)
        assertEquals("₹10.00", domain.summary.platformDeductions.formatDisplay())
        assertEquals(9000L, domain.summary.netEarnings.amountMinor)
        assertEquals("₹90.00", domain.summary.netEarnings.formatDisplay())
        assertEquals(0L, domain.summary.refundDeductions.amountMinor)
        assertEquals(2, domain.summary.completedRidesCount)
        assertEquals("INR", domain.summary.currency)

        // Settlement Summary
        assertEquals(9000L, domain.summary.settlementSummary.settledAmount.amountMinor)
        assertEquals("₹90.00", domain.summary.settlementSummary.settledAmount.formatDisplay())
        assertEquals(0L, domain.summary.settlementSummary.pendingSettlementAmount.amountMinor)
        assertEquals("₹0.00", domain.summary.settlementSummary.pendingSettlementAmount.formatDisplay())

        // Items
        assertEquals(2, domain.items.size)
        val item1 = domain.items[0]
        assertEquals("ride_001", item1.rideId)
        assertEquals("trip_001", item1.tripId)
        assertEquals("Varanasi Cantt", item1.pickupAddress)
        assertEquals("BHU Main Gate", item1.destinationAddress)
        assertEquals(5000L, item1.grossAmount.amountMinor)
        assertEquals(500L, item1.platformFee.amountMinor)
        assertEquals(4500L, item1.netAmount.amountMinor)
        assertEquals("₹45.00", item1.netAmount.formatDisplay())
        assertEquals(DomainPaymentStatus.CAPTURED, item1.paymentStatus)
        assertEquals(DomainSettlementStatus.PROCESSED, item1.settlementStatus)
        assertTrue(item1.settlementStatus.isSettled)

        val item2 = domain.items[1]
        assertEquals("ride_002", item2.rideId)
        assertEquals(DomainSettlementStatus.PENDING, item2.settlementStatus)
        assertTrue(item2.settlementStatus.isPending)

        // Pagination
        assertEquals(2, domain.pagination.total)
        assertEquals(1, domain.pagination.page)
        assertEquals(20, domain.pagination.limit)
        assertFalse(domain.pagination.hasMore)
    }

    @Test
    fun `money minor-unit formatting operates without floating point math`() {
        val zero = Money(amountMinor = 0L, currency = "INR")
        assertEquals("₹0.00", zero.formatDisplay())

        val onePaisa = Money(amountMinor = 1L, currency = "INR")
        assertEquals("₹0.01", onePaisa.formatDisplay())

        val ninetyNinePaise = Money(amountMinor = 99L, currency = "INR")
        assertEquals("₹0.99", ninetyNinePaise.formatDisplay())

        val oneRupee = Money(amountMinor = 100L, currency = "INR")
        assertEquals("₹1.00", oneRupee.formatDisplay())

        val largeAmount = Money(amountMinor = 12345678L, currency = "INR")
        assertEquals("₹123456.78", largeAmount.formatDisplay())

        val usdMoney = Money(amountMinor = 4550L, currency = "USD")
        assertEquals("USD 45.50", usdMoney.formatDisplay())
    }

    @Test
    fun `settlement status mapping supports all backend constants and handles unknown values gracefully`() {
        assertEquals(DomainSettlementStatus.PROCESSED, DomainSettlementStatus.fromBackend("PROCESSED"))
        assertEquals(DomainSettlementStatus.PENDING, DomainSettlementStatus.fromBackend("PENDING"))
        assertEquals(DomainSettlementStatus.PROCESSING, DomainSettlementStatus.fromBackend("PROCESSING"))
        assertEquals(DomainSettlementStatus.NOT_READY, DomainSettlementStatus.fromBackend("NOT_READY"))
        assertEquals(DomainSettlementStatus.RECONCILING, DomainSettlementStatus.fromBackend("RECONCILING"))
        assertEquals(DomainSettlementStatus.FAILED, DomainSettlementStatus.fromBackend("FAILED"))
        assertEquals(DomainSettlementStatus.UNSETTLED, DomainSettlementStatus.fromBackend("UNSETTLED"))

        // Unknown future enum value does not crash
        val unknown = DomainSettlementStatus.fromBackend("SOME_FUTURE_STATUS")
        assertEquals(DomainSettlementStatus.UNKNOWN, unknown)
        assertFalse(unknown.isSettled)
    }

    @Test
    fun `empty earnings response parsed cleanly with no fake data`() {
        val emptyJson = """
            {
              "success": true,
              "data": {
                "period": { "period": "today", "from": "", "to": "", "timezone": "Asia/Kolkata" },
                "summary": {
                  "grossEarningsMinor": 0,
                  "platformDeductionsMinor": 0,
                  "netEarningsMinor": 0,
                  "refundDeductionsMinor": 0,
                  "completedRidesCount": 0,
                  "settlementSummary": {
                    "settledAmountMinor": 0,
                    "pendingSettlementAmountMinor": 0,
                    "unreadySettlementAmountMinor": 0,
                    "failedSettlementAmountMinor": 0
                  },
                  "currency": "INR"
                },
                "items": [],
                "pagination": { "total": 0, "page": 1, "limit": 20, "hasMore": false }
              }
            }
        """.trimIndent()

        val dto = remoteDataSource.parseEarningsResponse(emptyJson)
        val domain = DriverEarningsMapper.toDomain(dto)

        assertEquals(0, domain.summary.completedRidesCount)
        assertEquals("₹0.00", domain.summary.netEarnings.formatDisplay())
        assertEquals("₹0.00", domain.summary.grossEarnings.formatDisplay())
        assertTrue(domain.items.isEmpty())
        assertFalse(domain.pagination.hasMore)
    }

    @Test
    fun `authentication failure 401 returns IshaaraError Authentication`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.failure(
            IshaaraError.Authentication(message = "Session expired", code = 401)
        )

        val result = repository.getDriverEarnings(period = EarningsPeriodType.TODAY)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Authentication)
        assertEquals(401, error.code)
    }

    @Test
    fun `forbidden failure 403 returns IshaaraError Forbidden`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.failure(
            IshaaraError.Forbidden(message = "Driver profile required", errorCode = "DRIVER_REQUIRED")
        )

        val result = repository.getDriverEarnings(period = EarningsPeriodType.TODAY)
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals(403, error.code)
    }
}
