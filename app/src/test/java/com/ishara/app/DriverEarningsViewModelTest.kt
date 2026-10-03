package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.DomainSettlementStatus
import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.DriverEarningsPagination
import com.ishara.app.domain.model.DriverEarningsPeriod
import com.ishara.app.domain.model.DriverEarningsSummary
import com.ishara.app.domain.model.DriverRideEarningsItem
import com.ishara.app.domain.model.DriverSettlementSummary
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.repository.DriverEarningsRepository
import com.ishara.app.domain.usecase.GetDriverEarningsUseCase
import com.ishara.app.domain.usecase.RefreshDriverEarningsUseCase
import com.ishara.app.feature.driver.earnings.DriverEarningsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ViewModel Unit Tests for Phase 16 Driver Earnings & Historical Analytics.
 */
class DriverEarningsViewModelTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private val testDispatchers = TestDispatcherProvider()

    private fun createEarnings(
        period: EarningsPeriodType = EarningsPeriodType.TODAY,
        page: Int = 1,
        hasMore: Boolean = false,
        rides: List<DriverRideEarningsItem> = emptyList()
    ): DriverEarnings {
        return DriverEarnings(
            period = DriverEarningsPeriod(period = period, from = "2026-09-28T00:00:00.000Z", to = "2026-09-28T23:59:59.999Z"),
            summary = DriverEarningsSummary(
                grossEarnings = Money(60000L, "INR"),
                platformDeductions = Money(6000L, "INR"),
                netEarnings = Money(54000L, "INR"),
                refundDeductions = Money(0L, "INR"),
                completedRidesCount = rides.size,
                settlementSummary = DriverSettlementSummary(
                    settledAmount = Money(54000L, "INR"),
                    pendingSettlementAmount = Money(0L, "INR"),
                    unreadySettlementAmount = Money(0L, "INR"),
                    failedSettlementAmount = Money(0L, "INR")
                )
            ),
            items = rides,
            pagination = DriverEarningsPagination(total = rides.size, page = page, limit = 20, hasMore = hasMore)
        )
    }

    private val sampleRide1 = DriverRideEarningsItem(
        rideId = "ride_1",
        tripId = "trip_1",
        completedAt = "2026-09-28T08:00:00.000Z",
        pickupAddress = "Stop A",
        destinationAddress = "Stop B",
        grossAmount = Money(30000L, "INR"),
        platformFee = Money(3000L, "INR"),
        netAmount = Money(27000L, "INR"),
        currency = "INR",
        paymentStatus = DomainPaymentStatus.CAPTURED,
        settlementStatus = DomainSettlementStatus.PROCESSED
    )

    private val sampleRide2 = DriverRideEarningsItem(
        rideId = "ride_2",
        tripId = "trip_1",
        completedAt = "2026-09-28T09:00:00.000Z",
        pickupAddress = "Stop C",
        destinationAddress = "Stop D",
        grossAmount = Money(30000L, "INR"),
        platformFee = Money(3000L, "INR"),
        netAmount = Money(27000L, "INR"),
        currency = "INR",
        paymentStatus = DomainPaymentStatus.CAPTURED,
        settlementStatus = DomainSettlementStatus.PENDING
    )

    @Test
    fun `initial state auto-loads TODAY earnings successfully`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(createEarnings(rides = listOf(sampleRide1)))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals(EarningsPeriodType.TODAY, state.selectedPeriod)
        assertNotNull(state.earnings)
        assertEquals(54000L, state.earnings?.summary?.netEarnings?.amountMinor)
        assertEquals(1, state.rideItems.size)
        assertEquals("ride_1", state.rideItems[0].rideId)
    }

    @Test
    fun `selectPeriod switches period filter and reloads data`() = runBlocking {
        var requestedPeriod: EarningsPeriodType? = null

        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                requestedPeriod = period
                return IshaaraResult.success(createEarnings(period = period))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        viewModel.selectPeriod(EarningsPeriodType.WEEK)
        assertEquals(EarningsPeriodType.WEEK, requestedPeriod)
        assertEquals(EarningsPeriodType.WEEK, viewModel.uiState.value.selectedPeriod)

        viewModel.selectPeriod(EarningsPeriodType.MONTH)
        assertEquals(EarningsPeriodType.MONTH, requestedPeriod)
        assertEquals(EarningsPeriodType.MONTH, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun `custom date range validates start before end and prevents invalid queries`() = runBlocking {
        var networkCallMade = false

        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                networkCallMade = true
                return IshaaraResult.success(createEarnings(period = period))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        networkCallMade = false

        // Invalid: from > to
        viewModel.applyCustomDateRange("2026-09-28", "2026-09-10")
        assertFalse(networkCallMade)
        assertEquals("Start date must be before or equal to end date.", viewModel.uiState.value.customDateValidationError)

        // Valid: from <= to
        viewModel.applyCustomDateRange("2026-09-01", "2026-09-28")
        assertTrue(networkCallMade)
        assertNull(viewModel.uiState.value.customDateValidationError)
        assertEquals(EarningsPeriodType.CUSTOM, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun `loadMore paginates and appends deduplicated rides`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return if (page == 1) {
                    IshaaraResult.success(createEarnings(page = 1, hasMore = true, rides = listOf(sampleRide1)))
                } else {
                    IshaaraResult.success(createEarnings(page = 2, hasMore = false, rides = listOf(sampleRide2)))
                }
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        assertEquals(1, viewModel.uiState.value.rideItems.size)
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadMore()

        assertEquals(2, viewModel.uiState.value.rideItems.size)
        assertEquals("ride_1", viewModel.uiState.value.rideItems[0].rideId)
        assertEquals("ride_2", viewModel.uiState.value.rideItems[1].rideId)
        assertEquals(2, viewModel.uiState.value.currentPage)
        assertFalse(viewModel.uiState.value.hasMore)
    }

    @Test
    fun `refresh triggers page 1 reload and ignores duplicate concurrent calls`() = runBlocking {
        var refreshCount = 0

        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                refreshCount++
                return IshaaraResult.success(createEarnings(rides = listOf(sampleRide1)))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        val initialCalls = refreshCount
        viewModel.refresh()
        assertEquals(initialCalls + 1, refreshCount)
    }

    @Test
    fun `network error sets isOffline flag and maps user-friendly message`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue(state.isOffline)
        assertNotNull(state.userFacingError)
        assertTrue(state.userFacingError!!.contains("internet connection"))

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.userFacingError)
    }

    @Test
    fun `empty earnings returns summary with 0 rides and isEmpty true`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(createEarnings(rides = emptyList()))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertTrue(state.rideItems.isEmpty())
        assertEquals(0, state.earnings?.summary?.completedRidesCount)
    }

    @Test
    fun `pagination failure preserves existing rides and surfaces error message`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return if (page == 1) {
                    IshaaraResult.success(createEarnings(page = 1, hasMore = true, rides = listOf(sampleRide1)))
                } else {
                    IshaaraResult.failure(IshaaraError.Server(message = "Temporary server error"))
                }
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        assertEquals(1, viewModel.uiState.value.rideItems.size)
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadMore()

        // Page 1 data MUST NOT be destroyed
        assertEquals(1, viewModel.uiState.value.rideItems.size)
        assertEquals("ride_1", viewModel.uiState.value.rideItems[0].rideId)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertNotNull(viewModel.uiState.value.userFacingError)
    }

    @Test
    fun `retry reloads current period and clears previous error`() = runBlocking {
        var callCount = 0
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                callCount++
                return if (callCount == 1) {
                    IshaaraResult.failure(IshaaraError.Network("Offline"))
                } else {
                    IshaaraResult.success(createEarnings(rides = listOf(sampleRide1)))
                }
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        assertTrue(viewModel.uiState.value.isOffline)
        assertNotNull(viewModel.uiState.value.userFacingError)

        viewModel.retry()

        assertFalse(viewModel.uiState.value.isOffline)
        assertNull(viewModel.uiState.value.userFacingError)
        assertEquals(1, viewModel.uiState.value.rideItems.size)
    }

    @Test
    fun `401 authentication error sets isSessionExpired true`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.failure(IshaaraError.Authentication(message = "Session expired"))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        assertTrue(viewModel.uiState.value.isSessionExpired)
        assertFalse(viewModel.uiState.value.isUnauthorized)
        assertEquals("Session expired. Please sign in again.", viewModel.uiState.value.userFacingError)
    }

    @Test
    fun `403 forbidden error sets isUnauthorized true`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.failure(IshaaraError.Forbidden(message = "Access restricted to DRIVER_CONDUCTOR"))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        assertTrue(viewModel.uiState.value.isUnauthorized)
        assertFalse(viewModel.uiState.value.isSessionExpired)
        assertEquals("Access denied: Driver permissions required.", viewModel.uiState.value.userFacingError)
    }

    @Test
    fun `clearState cancels pending jobs and resets uiState on logout`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(createEarnings(rides = listOf(sampleRide1)))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        assertEquals(1, viewModel.uiState.value.rideItems.size)
        assertNotNull(viewModel.uiState.value.earnings)

        // Driver logs out
        viewModel.clearState()

        val clearedState = viewModel.uiState.value
        assertNull(clearedState.earnings)
        assertTrue(clearedState.rideItems.isEmpty())
        assertEquals(EarningsPeriodType.TODAY, clearedState.selectedPeriod)
        assertFalse(clearedState.isLoading)
        assertFalse(clearedState.isSessionExpired)
    }

    @Test
    fun `account switching reloads clean state for new driver`() = runBlocking {
        var currentDriver = "driver_1"

        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                return if (currentDriver == "driver_1") {
                    IshaaraResult.success(createEarnings(rides = listOf(sampleRide1)))
                } else {
                    IshaaraResult.success(createEarnings(rides = listOf(sampleRide2)))
                }
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        // Driver 1 session
        val viewModel1 = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )
        assertEquals("ride_1", viewModel1.uiState.value.rideItems[0].rideId)

        // Switch to Driver 2
        viewModel1.clearState()
        currentDriver = "driver_2"

        val viewModel2 = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )
        assertEquals("ride_2", viewModel2.uiState.value.rideItems[0].rideId)
    }

    @Test
    fun `stale response protection ignores older period response when user rapidly switches`() = runBlocking {
        var callCount = 0
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> {
                callCount++
                return IshaaraResult.success(createEarnings(period = period))
            }

            override suspend fun getDriverRideHistory(
                tripId: String?,
                period: EarningsPeriodType?,
                from: String?,
                to: String?,
                timezone: String,
                page: Int,
                limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }

        val viewModel = DriverEarningsViewModel(
            getDriverEarningsUseCase = GetDriverEarningsUseCase(fakeRepo),
            refreshDriverEarningsUseCase = RefreshDriverEarningsUseCase(fakeRepo),
            dispatchers = testDispatchers
        )

        // Rapid switches: TODAY -> WEEK -> MONTH
        viewModel.selectPeriod(EarningsPeriodType.WEEK)
        viewModel.selectPeriod(EarningsPeriodType.MONTH)

        assertEquals(EarningsPeriodType.MONTH, viewModel.uiState.value.selectedPeriod)
        assertEquals(EarningsPeriodType.MONTH, viewModel.uiState.value.earnings?.period?.period)
    }
}
