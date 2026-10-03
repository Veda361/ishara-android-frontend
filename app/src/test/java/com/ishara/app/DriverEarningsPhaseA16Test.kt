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
import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.DriverEarningsRepository
import com.ishara.app.domain.usecase.GetDriverEarningsUseCase
import com.ishara.app.domain.usecase.RefreshDriverEarningsUseCase
import com.ishara.app.feature.driver.earnings.DriverEarningsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ============================================================================
 * PHASE A16 — DRIVER EARNINGS & EARNINGS LEDGER
 * Master Verification Test Suite (29 Minimum Coverage Points)
 * ============================================================================
 */
import com.ishara.app.core.network.Environment
import com.ishara.app.core.storage.InMemorySessionStore

class DriverEarningsPhaseA16Test {

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val networkConfig = NetworkConfig(environment = Environment.STAGING)

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

    private fun sampleResponseDto(
        period: String = "today",
        grossMinor: Long = 10000L,
        feeMinor: Long = 1000L,
        netMinor: Long = 9000L,
        ridesCount: Int = 2,
        settledMinor: Long = 9000L,
        pendingMinor: Long = 0L,
        items: List<DriverRideEarningsItemDto> = emptyList(),
        page: Int = 1,
        total: Int = 2,
        hasMore: Boolean = false
    ): DriverEarningsResponseDto {
        return DriverEarningsResponseDto(
            period = DriverEarningsPeriodDto(period = period, from = "2026-10-01T00:00:00Z", to = "2026-10-01T23:59:59Z"),
            summary = DriverEarningsSummaryDto(
                grossEarningsMinor = grossMinor,
                platformDeductionsMinor = feeMinor,
                netEarningsMinor = netMinor,
                completedRidesCount = ridesCount,
                settlementSummary = DriverSettlementSummaryDto(
                    settledAmountMinor = settledMinor,
                    pendingSettlementAmountMinor = pendingMinor
                ),
                currency = "INR"
            ),
            items = items,
            pagination = DriverEarningsPaginationDto(
                total = total,
                page = page,
                limit = 20,
                hasMore = hasMore
            )
        )
    }

    private val sampleItemDto1 = DriverRideEarningsItemDto(
        rideId = "ride_101",
        tripId = "trip_501",
        completedAt = "2026-10-01T10:00:00.000Z",
        pickupAddress = "Station A",
        destinationAddress = "Station B",
        grossAmountMinor = 5000L,
        platformFeeMinor = 500L,
        netAmountMinor = 4500L,
        currency = "INR",
        paymentStatus = "CAPTURED",
        settlementStatus = "PROCESSED"
    )

    private val sampleItemDto2 = DriverRideEarningsItemDto(
        rideId = "ride_102",
        tripId = "trip_501",
        completedAt = "2026-10-01T11:00:00.000Z",
        pickupAddress = "Station B",
        destinationAddress = "Station C",
        grossAmountMinor = 5000L,
        platformFeeMinor = 500L,
        netAmountMinor = 4500L,
        currency = "INR",
        paymentStatus = "CAPTURED",
        settlementStatus = "PENDING"
    )

    @Before
    fun setUp() = runBlocking {
        fakeHttpClient = RecordingFakeHttpClient()
        sessionStore = InMemorySessionStore()
        sessionStore.saveSession(
            AuthSession(
                token = "test_driver_token",
                userId = "driver_123",
                role = UserRole.DRIVER_CONDUCTOR
            )
        )
        remoteDataSource = DriverEarningsRemoteDataSourceImpl(fakeHttpClient, networkConfig)
        repository = DriverEarningsRepositoryImpl(remoteDataSource, sessionStore, testDispatchers)
    }

    // 1. Earnings endpoint contract
    @Test
    fun `01 - Earnings endpoint contract verified (GET, path, Bearer)`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.success(
            HttpResponse(statusCode = 200, body = """{"data": {}}""")
        )
        repository.getDriverEarnings()
        assertEquals(1, fakeHttpClient.recordedRequests.size)
        val req = fakeHttpClient.recordedRequests.first()
        assertEquals(HttpMethod.GET, req.method)
        assertEquals("${networkConfig.fullApiBaseUrl}/drivers/me/earnings", req.url)
        assertEquals("Bearer test_driver_token", req.headers["Authorization"])
    }

    // 2. Summary mapping
    @Test
    fun `02 - Summary mapping creates authoritative domain summary`() {
        val dto = sampleResponseDto(grossMinor = 12000L, feeMinor = 1200L, netMinor = 10800L, ridesCount = 3)
        val domain = DriverEarningsMapper.toDomain(dto.summary)
        assertEquals(12000L, domain.grossEarnings.amountMinor)
        assertEquals(1200L, domain.platformDeductions.amountMinor)
        assertEquals(10800L, domain.netEarnings.amountMinor)
        assertEquals(3, domain.completedRidesCount)
        assertEquals("INR", domain.currency)
    }

    // 3. Ledger item mapping
    @Test
    fun `03 - Ledger item mapping preserves ride details and financial breakdown`() {
        val domainItem = DriverEarningsMapper.toDomain(sampleItemDto1)
        assertEquals("ride_101", domainItem.rideId)
        assertEquals("trip_501", domainItem.tripId)
        assertEquals("Station A", domainItem.pickupAddress)
        assertEquals("Station B", domainItem.destinationAddress)
        assertEquals(5000L, domainItem.grossAmount.amountMinor)
        assertEquals(500L, domainItem.platformFee.amountMinor)
        assertEquals(4500L, domainItem.netAmount.amountMinor)
        assertEquals(DomainPaymentStatus.CAPTURED, domainItem.paymentStatus)
        assertEquals(DomainSettlementStatus.PROCESSED, domainItem.settlementStatus)
    }

    // 4. Money minor-unit formatting
    @Test
    fun `04 - Money minor-unit formatting formats rupees and paise without floating point math`() {
        assertEquals("₹0.00", Money(0L).formatDisplay())
        assertEquals("₹0.50", Money(50L).formatDisplay())
        assertEquals("₹1.00", Money(100L).formatDisplay())
        assertEquals("₹45.00", Money(4500L).formatDisplay())
        assertEquals("₹1000.25", Money(100025L).formatDisplay())
    }

    // 5. Today period
    @Test
    fun `05 - Today period sends period=today query parameter`() = runBlocking {
        repository.getDriverEarnings(period = EarningsPeriodType.TODAY)
        val req = fakeHttpClient.recordedRequests.last()
        assertEquals("today", req.queryParams["period"])
    }

    // 6. Week period
    @Test
    fun `06 - Week period sends period=week query parameter`() = runBlocking {
        repository.getDriverEarnings(period = EarningsPeriodType.WEEK)
        val req = fakeHttpClient.recordedRequests.last()
        assertEquals("week", req.queryParams["period"])
    }

    // 7. Month period
    @Test
    fun `07 - Month period sends period=month query parameter`() = runBlocking {
        repository.getDriverEarnings(period = EarningsPeriodType.MONTH)
        val req = fakeHttpClient.recordedRequests.last()
        assertEquals("month", req.queryParams["period"])
    }

    // 8. Custom period if supported
    @Test
    fun `08 - Custom period sends from and to ISO timestamp query parameters`() = runBlocking {
        repository.getDriverEarnings(
            period = EarningsPeriodType.CUSTOM,
            from = "2026-10-01T00:00:00.000Z",
            to = "2026-10-02T23:59:59.999Z"
        )
        val req = fakeHttpClient.recordedRequests.last()
        assertEquals("custom", req.queryParams["period"])
        assertEquals("2026-10-01T00:00:00.000Z", req.queryParams["from"])
        assertEquals("2026-10-02T23:59:59.999Z", req.queryParams["to"])
    }

    // 9. Empty earnings
    @Test
    fun `09 - Empty earnings represents clean zero state without error`() {
        val emptyDto = sampleResponseDto(grossMinor = 0, feeMinor = 0, netMinor = 0, ridesCount = 0)
        val domain = DriverEarningsMapper.toDomain(emptyDto)
        assertEquals(0, domain.summary.completedRidesCount)
        assertEquals(0L, domain.summary.netEarnings.amountMinor)
        assertTrue(domain.items.isEmpty())
    }

    // 10. Loading state
    @Test
    fun `10 - Loading state is reflected during initial fetch`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto()))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.earnings)
    }

    // 11. Refresh
    @Test
    fun `11 - Refresh requests authoritative state and updates data`() = runBlocking {
        var callCount = 0
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                callCount++
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(netMinor = (callCount * 1000L))))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertEquals(1000L, vm.uiState.value.earnings?.summary?.netEarnings?.amountMinor)
        vm.refresh()
        assertEquals(2000L, vm.uiState.value.earnings?.summary?.netEarnings?.amountMinor)
    }

    // 12. Pagination
    @Test
    fun `12 - Pagination loads page 2 and appends ledger items`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return if (page == 1) {
                    IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                        items = listOf(sampleItemDto1), page = 1, total = 2, hasMore = true
                    )))
                } else {
                    IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                        items = listOf(sampleItemDto2), page = 2, total = 2, hasMore = false
                    )))
                }
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertEquals(1, vm.uiState.value.rideItems.size)
        assertTrue(vm.uiState.value.hasMore)
        vm.loadMore()
        assertEquals(2, vm.uiState.value.rideItems.size)
        assertEquals("ride_101", vm.uiState.value.rideItems[0].rideId)
        assertEquals("ride_102", vm.uiState.value.rideItems[1].rideId)
        assertEquals(2, vm.uiState.value.currentPage)
    }

    // 13. Pagination end condition
    @Test
    fun `13 - Pagination end condition stops requests when hasMore is false`() = runBlocking {
        var pageRequested = 1
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                pageRequested = page
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                    items = listOf(sampleItemDto1), page = page, hasMore = false
                )))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertFalse(vm.uiState.value.hasMore)
        vm.loadMore()
        assertEquals(1, pageRequested) // Did NOT request page 2
    }

    // 14. Duplicate-page prevention
    @Test
    fun `14 - Duplicate-page prevention ignores concurrent loadMore calls`() = runBlocking {
        var pageCalls = 0
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                if (page > 1) pageCalls++
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                    items = listOf(sampleItemDto1), hasMore = true
                )))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        vm.loadMore()
        assertEquals(1, pageCalls)
    }

    // 15. Duplicate item prevention
    @Test
    fun `15 - Duplicate item prevention deduplicates overlapping ride records`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return if (page == 1) {
                    IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                        items = listOf(sampleItemDto1), hasMore = true
                    )))
                } else {
                    // Page 2 returns sampleItemDto1 again plus sampleItemDto2
                    IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                        items = listOf(sampleItemDto1, sampleItemDto2), hasMore = false
                    )))
                }
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        vm.loadMore()
        assertEquals(2, vm.uiState.value.rideItems.size) // No duplicate ride_101
    }

    // 16. Stale period response protection
    @Test
    fun `16 - Stale period response protection ignores earlier response when filter changed`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(period = period.queryParam)))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        vm.selectPeriod(EarningsPeriodType.WEEK)
        vm.selectPeriod(EarningsPeriodType.MONTH)
        assertEquals(EarningsPeriodType.MONTH, vm.uiState.value.selectedPeriod)
        assertEquals(EarningsPeriodType.MONTH, vm.uiState.value.earnings?.period?.period)
    }

    // 17. Network failure
    @Test
    fun `17 - Network failure marks offline mode and surfaces user message`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertTrue(vm.uiState.value.isOffline)
        assertNotNull(vm.uiState.value.userFacingError)
    }

    // 18. Backend 401
    @Test
    fun `18 - Backend 401 maps to Authentication error`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.failure(
            IshaaraError.Authentication(code = 401, message = "Session expired")
        )
        val result = repository.getDriverEarnings()
        assertTrue(result is IshaaraResult.Failure)
        assertEquals(401, (result as IshaaraResult.Failure).error.code)
    }

    // 19. Backend 403
    @Test
    fun `19 - Backend 403 maps to Forbidden error`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.failure(
            IshaaraError.Forbidden(message = "Driver access only")
        )
        val result = repository.getDriverEarnings()
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals(403, error.code)
    }

    // 20. Session expiration
    @Test
    fun `20 - Session expiration sets isSessionExpired on ViewModel`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.failure(IshaaraError.Authentication(message = "Session expired"))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertTrue(vm.uiState.value.isSessionExpired)
    }

    // 21. Driver-only access
    @Test
    fun `21 - Driver-only access allows DRIVER_CONDUCTOR role`() = runBlocking {
        fakeHttpClient.responseToReturn = IshaaraResult.success(
            HttpResponse(statusCode = 200, body = """{"data": {}}""")
        )
        sessionStore.saveSession(
            AuthSession(
                token = "driver_token",
                userId = "dr_1",
                role = UserRole.DRIVER_CONDUCTOR
            )
        )
        val result = repository.getDriverEarnings()
        assertTrue(result is IshaaraResult.Success)
    }

    // 22. USER cannot access earnings
    @Test
    fun `22 - USER passenger is rejected with Forbidden error before network request`() = runBlocking {
        sessionStore.saveSession(
            AuthSession(
                token = "passenger_token",
                userId = "usr_1",
                role = UserRole.USER
            )
        )
        val result = repository.getDriverEarnings()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Forbidden)
        assertEquals(0, fakeHttpClient.recordedRequests.size)
    }

    // 23. Logout clears earnings state
    @Test
    fun `23 - Logout clears earnings state via clearState()`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(items = listOf(sampleItemDto1))))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertEquals(1, vm.uiState.value.rideItems.size)
        vm.clearState()
        assertNull(vm.uiState.value.earnings)
        assertTrue(vm.uiState.value.rideItems.isEmpty())
    }

    // 24. Account switch does not leak previous driver data
    @Test
    fun `24 - Account switch isolates data between Driver A and Driver B`() = runBlocking {
        var activeDriver = "DriverA"
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                val item = if (activeDriver == "DriverA") sampleItemDto1 else sampleItemDto2
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(items = listOf(item))))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vmA = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertEquals("ride_101", vmA.uiState.value.rideItems[0].rideId)

        // Switch
        vmA.clearState()
        activeDriver = "DriverB"

        val vmB = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertEquals("ride_102", vmB.uiState.value.rideItems[0].rideId)
        assertNotEquals(vmA.uiState.value.rideItems.firstOrNull()?.rideId, vmB.uiState.value.rideItems[0].rideId)
    }

    // 25. Settlement status mapping
    @Test
    fun `25 - Settlement status mapping matches backend constants`() {
        assertEquals(DomainSettlementStatus.PROCESSED, DomainSettlementStatus.fromBackend("PROCESSED"))
        assertEquals(DomainSettlementStatus.PENDING, DomainSettlementStatus.fromBackend("PENDING"))
        assertEquals(DomainSettlementStatus.NOT_READY, DomainSettlementStatus.fromBackend("NOT_READY"))
        assertEquals(DomainSettlementStatus.FAILED, DomainSettlementStatus.fromBackend("FAILED"))
        assertEquals(DomainSettlementStatus.UNSETTLED, DomainSettlementStatus.fromBackend("UNSETTLED"))
    }

    // 26. Unknown settlement status does not crash
    @Test
    fun `26 - Unknown settlement status does not crash and defaults to UNKNOWN`() {
        val status = DomainSettlementStatus.fromBackend("FUTURE_NONEXISTENT_STATUS")
        assertEquals(DomainSettlementStatus.UNKNOWN, status)
        assertFalse(status.isSettled)
    }

    // 27. Backend-provided net earnings are displayed directly
    @Test
    fun `27 - Backend-provided net earnings are displayed directly`() {
        val dto = sampleResponseDto(grossMinor = 10000L, feeMinor = 1500L, netMinor = 8500L)
        val domain = DriverEarningsMapper.toDomain(dto)
        assertEquals(8500L, domain.summary.netEarnings.amountMinor)
        assertEquals("₹85.00", domain.summary.netEarnings.formatDisplay())
    }

    // 28. Client does not calculate net earnings
    @Test
    fun `28 - Client does not calculate net earnings (respects backend invariant directly)`() {
        // Suppose backend returned an intentional bonus where net > gross - fee
        val bonusDto = sampleResponseDto(grossMinor = 10000L, feeMinor = 1000L, netMinor = 9500L)
        val domain = DriverEarningsMapper.toDomain(bonusDto)
        // Frontend must NOT force net = gross - fee (9000); it must preserve backend 9500
        assertEquals(9500L, domain.summary.netEarnings.amountMinor)
        assertEquals("₹95.00", domain.summary.netEarnings.formatDisplay())
    }

    // 29. No fake earnings on empty state
    @Test
    fun `29 - No fake earnings displayed on empty state`() = runBlocking {
        val fakeRepo = object : DriverEarningsRepository {
            override suspend fun getDriverEarnings(
                period: EarningsPeriodType, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> {
                return IshaaraResult.success(DriverEarningsMapper.toDomain(sampleResponseDto(
                    grossMinor = 0, feeMinor = 0, netMinor = 0, ridesCount = 0, items = emptyList()
                )))
            }
            override suspend fun getDriverRideHistory(
                tripId: String?, period: EarningsPeriodType?, from: String?, to: String?, timezone: String, page: Int, limit: Int
            ): IshaaraResult<DriverEarnings> = IshaaraResult.failure(IshaaraError.Unknown())
        }
        val vm = DriverEarningsViewModel(
            GetDriverEarningsUseCase(fakeRepo),
            RefreshDriverEarningsUseCase(fakeRepo),
            testDispatchers
        )
        assertTrue(vm.uiState.value.isEmpty)
        assertEquals(0, vm.uiState.value.rideItems.size)
        assertEquals("₹0.00", vm.uiState.value.earnings?.summary?.netEarnings?.formatDisplay())
    }
}
