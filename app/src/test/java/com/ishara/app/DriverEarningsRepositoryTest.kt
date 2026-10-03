package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.remote.datasource.DriverEarningsRemoteDataSource
import com.ishara.app.data.remote.dto.DriverEarningsPaginationDto
import com.ishara.app.data.remote.dto.DriverEarningsPeriodDto
import com.ishara.app.data.remote.dto.DriverEarningsResponseDto
import com.ishara.app.data.remote.dto.DriverEarningsSummaryDto
import com.ishara.app.data.remote.dto.DriverRideEarningsItemDto
import com.ishara.app.data.remote.dto.DriverSettlementSummaryDto
import com.ishara.app.data.repository.DriverEarningsRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.DomainSettlementStatus
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Repository Unit Tests for Phase 16 Driver Earnings & Historical Analytics.
 */
class DriverEarningsRepositoryTest {

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val driverSession = AuthSession(
        token = "jwt_driver_auth_token",
        userId = "driver_user_id_123",
        role = UserRole.DRIVER_CONDUCTOR
    )

    private val sampleResponseDto = DriverEarningsResponseDto(
        period = DriverEarningsPeriodDto(
            period = "today",
            from = "2026-09-28T00:00:00.000Z",
            to = "2026-09-28T23:59:59.999Z",
            timezone = "Asia/Kolkata"
        ),
        summary = DriverEarningsSummaryDto(
            grossEarningsMinor = 50000L,
            platformDeductionsMinor = 5000L,
            netEarningsMinor = 45000L,
            refundDeductionsMinor = 0L,
            completedRidesCount = 1,
            settlementSummary = DriverSettlementSummaryDto(
                settledAmountMinor = 45000L,
                pendingSettlementAmountMinor = 0L
            ),
            currency = "INR"
        ),
        items = listOf(
            DriverRideEarningsItemDto(
                rideId = "ride_1",
                tripId = "trip_1",
                pickupAddress = "Pickup Point",
                destinationAddress = "Drop Point",
                grossAmountMinor = 50000L,
                platformFeeMinor = 5000L,
                netAmountMinor = 45000L,
                currency = "INR",
                paymentStatus = "CAPTURED",
                settlementStatus = "PROCESSED"
            )
        ),
        pagination = DriverEarningsPaginationDto(total = 1, page = 1, limit = 20, hasMore = false)
    )

    @Test
    fun `getDriverEarnings succeeds and maps authoritative financial figures`() = runBlocking {
        val sessionStore = InMemorySessionStore(driverSession)
        val fakeDataSource = object : DriverEarningsRemoteDataSource {
            override suspend fun getDriverEarnings(
                period: String,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> {
                assertEquals("today", period)
                assertEquals("jwt_driver_auth_token", token)
                return IshaaraResult.success(sampleResponseDto)
            }

            override suspend fun getDriverRidesWithFinancials(
                status: String?,
                tripId: String?,
                period: String?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val repository = DriverEarningsRepositoryImpl(fakeDataSource, sessionStore, testDispatchers)
        val result = repository.getDriverEarnings(period = EarningsPeriodType.TODAY)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals(45000L, data?.summary?.netEarnings?.amountMinor)
        assertEquals("₹450.00", data?.summary?.netEarnings?.formatDisplay())
        assertEquals(50000L, data?.summary?.grossEarnings?.amountMinor)
        assertEquals(5000L, data?.summary?.platformDeductions?.amountMinor)
        assertEquals(1, data?.items?.size)
        assertEquals(DomainPaymentStatus.CAPTURED, data?.items?.get(0)?.paymentStatus)
        assertEquals(DomainSettlementStatus.PROCESSED, data?.items?.get(0)?.settlementStatus)
    }

    @Test
    fun `getDriverEarnings fails with Authentication error when session is null`() = runBlocking {
        val emptySessionStore = InMemorySessionStore(null)
        val fakeDataSource = object : DriverEarningsRemoteDataSource {
            override suspend fun getDriverEarnings(
                period: String,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> = IshaaraResult.success(sampleResponseDto)

            override suspend fun getDriverRidesWithFinancials(
                status: String?,
                tripId: String?,
                period: String?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val repository = DriverEarningsRepositoryImpl(fakeDataSource, emptySessionStore, testDispatchers)
        val result = repository.getDriverEarnings(period = EarningsPeriodType.TODAY)

        assertFalse(result.isSuccess)
        val error = result.errorOrNull()
        assertTrue(error is IshaaraError.Authentication)
    }

    @Test
    fun `getDriverEarnings propagates network failure from remote data source`() = runBlocking {
        val sessionStore = InMemorySessionStore(driverSession)
        val fakeDataSource = object : DriverEarningsRemoteDataSource {
            override suspend fun getDriverEarnings(
                period: String,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> {
                return IshaaraResult.failure(IshaaraError.Network("Socket timeout"))
            }

            override suspend fun getDriverRidesWithFinancials(
                status: String?,
                tripId: String?,
                period: String?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val repository = DriverEarningsRepositoryImpl(fakeDataSource, sessionStore, testDispatchers)
        val result = repository.getDriverEarnings(period = EarningsPeriodType.TODAY)

        assertFalse(result.isSuccess)
        assertTrue(result.errorOrNull() is IshaaraError.Network)
    }

    @Test
    fun `getDriverRideHistory calls data source with status=COMPLETED and withFinancials=true`() = runBlocking {
        val sessionStore = InMemorySessionStore(driverSession)
        var capturedStatus: String? = null

        val fakeDataSource = object : DriverEarningsRemoteDataSource {
            override suspend fun getDriverEarnings(
                period: String,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> = IshaaraResult.failure(IshaaraError.Unknown())

            override suspend fun getDriverRidesWithFinancials(
                status: String?,
                tripId: String?,
                period: String?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int,
                token: String
            ): IshaaraResult<DriverEarningsResponseDto> {
                capturedStatus = status
                return IshaaraResult.success(sampleResponseDto)
            }
        }

        val repository = DriverEarningsRepositoryImpl(fakeDataSource, sessionStore, testDispatchers)
        val result = repository.getDriverRideHistory(tripId = "trip_1", period = EarningsPeriodType.TODAY)

        assertTrue(result.isSuccess)
        assertEquals("COMPLETED", capturedStatus)
    }
}
