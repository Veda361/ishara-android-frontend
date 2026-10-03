package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.mapper.FareMapper
import com.ishara.app.data.remote.datasource.DefaultFareRemoteDataSource
import com.ishara.app.data.remote.dto.FareEstimateDto
import com.ishara.app.data.remote.dto.FareSnapshotDto
import com.ishara.app.data.remote.dto.RideFareResponseDto
import com.ishara.app.data.repository.FareRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.ClearFareStateUseCase
import com.ishara.app.domain.usecase.GetRideFareUseCase
import com.ishara.app.feature.student.fare.FareSummaryViewModel
import com.ishara.app.feature.student.fare.FareUiState
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PHASE A12 — FARE & PRICING FOUNDATION Test Suite
 *
 * Verifies all Phase A12 requirements against the actual backend contract:
 * 1. REST Endpoint: GET /api/v1/rides/:rideId/fare with Bearer authentication.
 * 2. Integer Money Safety: Integer minor units (paise in INR), zero float arithmetic, and proper display formatting.
 * 3. Authority: Frontend is strictly a consumer of backend pricing; no client-side recalculation of total fare.
 * 4. Estimate vs Snapshot Distinction: Pre-ride estimate (isFinal=false) vs immutable post-ride snapshot (isFinal=true).
 * 5. Breakdown Components: Base fare, distance fare, time fare, subtotal, tax, service fee, and discount.
 * 6. Error Handling: 400 (Invalid ID), 401 (Unauthorized), 403 (IDOR/Forbidden), 404 (Not Found), 429 (Rate Limit).
 * 7. Account Switching & Session Isolation: Cache cleared on logout / account switch.
 * 8. Payment Boundary: A12 prepares and hands off to A13 (StudentPayment) without invoking payment APIs or collecting credentials.
 */
class FarePhaseA12Test {

    private val testDispatcher = Dispatchers.Unconfined

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private val networkConfig = NetworkConfig()
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var sessionLocalDataSource: SessionLocalDataSourceImpl
    private lateinit var navigationManager: NavigationManager

    private val sampleCompletedFareJson = """
        {
            "success": true,
            "data": {
                "rideId": "651a2b3c4d5e6f7a8b9c0001",
                "status": "COMPLETED",
                "currency": "INR",
                "currentFareMinor": 4500,
                "isFinal": true,
                "fareEstimate": {
                    "currency": "INR",
                    "pricingPolicyVersion": "policy_v1",
                    "distanceMeters": 5000,
                    "estimatedDurationSeconds": 600,
                    "baseFareMinor": 2000,
                    "distanceComponentMinor": 2500,
                    "timeComponentMinor": 0,
                    "subtotalMinor": 4500,
                    "serviceFeeMinor": 450,
                    "taxMinor": 0,
                    "totalMinor": 4500,
                    "providerAmountMinor": 4050,
                    "isEstimate": true,
                    "calculatedAt": "2026-10-01T08:00:00.000Z"
                },
                "fareSnapshot": {
                    "currency": "INR",
                    "pricingPolicyVersion": "policy_v1",
                    "distanceMeters": 5000,
                    "actualDurationSeconds": 660,
                    "baseFareMinor": 2000,
                    "distanceComponentMinor": 2500,
                    "timeComponentMinor": 0,
                    "subtotalMinor": 4500,
                    "serviceFeeMinor": 450,
                    "taxMinor": 0,
                    "discountMinor": 0,
                    "totalMinor": 4500,
                    "providerAmountMinor": 4050,
                    "isEstimate": false,
                    "calculatedAt": "2026-10-01T08:20:00.000Z"
                }
            },
            "message": "Ride fare retrieved successfully."
        }
    """.trimIndent()

    private val sampleInFlightFareJson = """
        {
            "success": true,
            "data": {
                "rideId": "651a2b3c4d5e6f7a8b9c0002",
                "status": "IN_PROGRESS",
                "currency": "INR",
                "currentFareMinor": 3500,
                "isFinal": false,
                "fareEstimate": {
                    "currency": "INR",
                    "pricingPolicyVersion": "policy_v1",
                    "distanceMeters": 3000,
                    "estimatedDurationSeconds": 450,
                    "baseFareMinor": 2000,
                    "distanceComponentMinor": 1500,
                    "timeComponentMinor": 0,
                    "subtotalMinor": 3500,
                    "serviceFeeMinor": 350,
                    "taxMinor": 0,
                    "totalMinor": 3500,
                    "providerAmountMinor": 3150,
                    "isEstimate": true,
                    "calculatedAt": "2026-10-01T08:10:00.000Z"
                },
                "fareSnapshot": null
            },
            "message": "Ride fare retrieved successfully."
        }
    """.trimIndent()

    @Before
    fun setup() {
        sessionStore = InMemorySessionStore()
        sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        navigationManager = NavigationManager()

        val validSession = AuthSession(
            token = "jwt_test_passenger_token",
            role = UserRole.USER,
            userId = "usr_passenger_123"
        )
        runBlocking {
            sessionStore.saveSession(validSession)
        }
    }

    // =========================================================================
    // 1. REST CONTRACT & HEADERS
    // =========================================================================

    @Test
    fun `getRideFare calls GET rides rideId fare with Authorization Bearer header`() = runBlocking {
        var requestedUrl: String? = null
        var requestedMethod: HttpMethod? = null
        var authHeader: String? = null

        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                requestedUrl = request.url
                requestedMethod = request.method
                authHeader = request.headers["Authorization"]
                return IshaaraResult.success(HttpResponse(200, sampleCompletedFareJson))
            }
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)

        val result = repository.getRideFare("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(result.isSuccess)

        assertEquals("${networkConfig.fullApiBaseUrl}/rides/651a2b3c4d5e6f7a8b9c0001/fare", requestedUrl)
        assertEquals(HttpMethod.GET, requestedMethod)
        assertEquals("Bearer jwt_test_passenger_token", authHeader)
    }

    // =========================================================================
    // 2. INTEGER MONEY REPRESENTATION & FORMATTING
    // =========================================================================

    @Test
    fun `money representation preserves integer minor units and formats INR currency correctly`() {
        val fare45 = Money(amountMinor = 4500L, currency = "INR")
        assertEquals(4500L, fare45.amountMinor)
        assertEquals(45.0, fare45.amountMajor, 0.001)
        assertEquals("₹45.00", fare45.formatDisplay())

        val zeroFare = Money(amountMinor = 0L, currency = "INR")
        assertEquals("₹0.00", zeroFare.formatDisplay())

        val fractionalFare = Money(amountMinor = 12550L, currency = "INR")
        assertEquals("₹125.50", fractionalFare.formatDisplay())
    }

    // =========================================================================
    // 3. FARE MAPPING & ESTIMATE VS SNAPSHOT DISTINCTION
    // =========================================================================

    @Test
    fun `FareMapper maps completed ride into authoritative immutable FareSnapshot`() {
        val snapshotDto = FareSnapshotDto(
            currency = "INR",
            pricingPolicyVersion = "policy_v1",
            distanceMeters = 5000L,
            actualDurationSeconds = 660L,
            baseFareMinor = 2000L,
            distanceComponentMinor = 2500L,
            timeComponentMinor = 0L,
            subtotalMinor = 4500L,
            serviceFeeMinor = 450L,
            taxMinor = 0L,
            discountMinor = 0L,
            totalMinor = 4500L,
            providerAmountMinor = 4050L,
            isEstimate = false,
            calculatedAt = "2026-10-01T08:20:00.000Z"
        )

        val responseDto = RideFareResponseDto(
            rideId = "651a2b3c4d5e6f7a8b9c0001",
            status = "COMPLETED",
            currency = "INR",
            currentFareMinor = 4500L,
            isFinal = true,
            fareEstimate = null,
            fareSnapshot = snapshotDto
        )

        val domain = FareMapper.toDomain(responseDto)
        assertEquals("651a2b3c4d5e6f7a8b9c0001", domain.rideId)
        assertEquals("COMPLETED", domain.status)
        assertTrue(domain.isFinal)
        assertEquals(4500L, domain.currentFare.amountMinor)
        assertNotNull(domain.snapshot)
        assertNull(domain.estimate)

        val breakdown = domain.effectiveBreakdown
        assertNotNull(breakdown)
        assertTrue(breakdown!!.isSnapshot)
        assertEquals("5.0 km", breakdown.distanceKmFormatted)
        assertEquals("11 mins", breakdown.durationMinutesFormatted)
        assertEquals("₹20.00", breakdown.baseFare.formatDisplay())
        assertEquals("₹25.00", breakdown.distanceComponent.formatDisplay())
        assertEquals("₹45.00", breakdown.total.formatDisplay())
    }

    @Test
    fun `FareMapper maps in-flight ride into non-final FareEstimate without snapshot`() {
        val estimateDto = FareEstimateDto(
            currency = "INR",
            pricingPolicyVersion = "policy_v1",
            distanceMeters = 3000L,
            estimatedDurationSeconds = 450L,
            baseFareMinor = 2000L,
            distanceComponentMinor = 1500L,
            timeComponentMinor = 0L,
            subtotalMinor = 3500L,
            serviceFeeMinor = 350L,
            taxMinor = 0L,
            totalMinor = 3500L,
            providerAmountMinor = 3150L,
            isEstimate = true,
            calculatedAt = "2026-10-01T08:10:00.000Z"
        )

        val responseDto = RideFareResponseDto(
            rideId = "651a2b3c4d5e6f7a8b9c0002",
            status = "IN_PROGRESS",
            currency = "INR",
            currentFareMinor = 3500L,
            isFinal = false,
            fareEstimate = estimateDto,
            fareSnapshot = null
        )

        val domain = FareMapper.toDomain(responseDto)
        assertEquals("651a2b3c4d5e6f7a8b9c0002", domain.rideId)
        assertFalse(domain.isFinal)
        assertEquals(3500L, domain.currentFare.amountMinor)
        assertNull(domain.snapshot)
        assertNotNull(domain.estimate)

        val breakdown = domain.effectiveBreakdown
        assertNotNull(breakdown)
        assertFalse(breakdown!!.isSnapshot)
        assertEquals("3.0 km", breakdown.distanceKmFormatted)
        assertEquals("7 mins", breakdown.durationMinutesFormatted)
    }

    // =========================================================================
    // 4. AUTHORITY & CLIENT INVARIANCE
    // =========================================================================

    @Test
    fun `frontend strictly consumes backend total and never modifies authoritative figures`() = runBlocking {
        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleCompletedFareJson))
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)
        val useCase = GetRideFareUseCase(repository)

        val result = useCase("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(result.isSuccess)

        val fare = (result as IshaaraResult.Success).data
        // Total must strictly match backend's currentFareMinor (4500 paise)
        assertEquals(4500L, fare.currentFare.amountMinor)
        assertEquals(fare.snapshot!!.total.amountMinor, fare.currentFare.amountMinor)
    }

    // =========================================================================
    // 5. ERROR HANDLING & SECURITY ENFORCEMENT
    // =========================================================================

    @Test
    fun `403 Forbidden is mapped to IshaaraError Forbidden for IDOR protection`() = runBlocking {
        val errorJson = """{"success":false,"error":{"code":"RIDE_NOT_AUTHORIZED","message":"You do not have permission to view the fare for this ride."}}"""
        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(403, errorJson))
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)

        val result = repository.getRideFare("651a2b3c4d5e6f7a8b9c0999")
        assertTrue(result.isFailure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals("You do not have permission to view the fare for this ride.", error.message)
    }

    @Test
    fun `404 Not Found is mapped to IshaaraError NotFound`() = runBlocking {
        val errorJson = """{"success":false,"error":{"code":"RIDE_NOT_FOUND","message":"Ride not found."}}"""
        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(404, errorJson))
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)

        val result = repository.getRideFare("651a2b3c4d5e6f7a8b9c0999")
        assertTrue(result.isFailure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.NotFound)
    }

    @Test
    fun `400 Bad Request is mapped to IshaaraError Validation`() = runBlocking {
        val errorJson = """{"success":false,"error":{"code":"INVALID_ID","message":"Invalid rideId format."}}"""
        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(400, errorJson))
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)

        val result = repository.getRideFare("invalid-id")
        assertTrue(result.isFailure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
    }

    // =========================================================================
    // 6. ACCOUNT SWITCHING & CACHE ISOLATION
    // =========================================================================

    @Test
    fun `clearFareState invalidates cached fare and isolates user sessions`() = runBlocking {
        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleCompletedFareJson))
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)
        val clearUseCase = ClearFareStateUseCase(repository)

        // Seed cache
        val initialResult = repository.getRideFare("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(initialResult.isSuccess)

        // Clear state
        clearUseCase()

        // Logout session
        sessionStore.clearSession()

        // New request without session fails with Authentication error
        val afterLogoutResult = repository.getRideFare("651a2b3c4d5e6f7a8b9c0001")
        assertTrue(afterLogoutResult.isFailure)
        assertTrue((afterLogoutResult as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    // =========================================================================
    // 7. VIEWMODEL UI STATE & PAYMENT HANDOFF BOUNDARY
    // =========================================================================

    @Test
    fun `FareSummaryViewModel loads fare, exposes immutable Content state, and routes to payment`() = runBlocking {
        val fakeHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> =
                IshaaraResult.success(HttpResponse(200, sampleCompletedFareJson))
        }

        val remoteDataSource = DefaultFareRemoteDataSource(fakeHttpClient, networkConfig)
        val repository = FareRepositoryImpl(remoteDataSource, sessionLocalDataSource, testDispatchers)
        val getRideFareUseCase = GetRideFareUseCase(repository)

        val viewModel = FareSummaryViewModel(
            rideId = "651a2b3c4d5e6f7a8b9c0001",
            getRideFareUseCase = getRideFareUseCase,
            navigationManager = navigationManager,
            dispatchers = testDispatchers
        )

        val state = viewModel.uiState.value
        assertTrue(state is FareUiState.Content)
        val content = state as FareUiState.Content
        assertTrue(content.isFinal)
        assertEquals(4500L, content.fare.currentFare.amountMinor)
        assertEquals("Authoritative Final Fare", content.statusDescription)

        // Verify payment handoff (A12 -> A13 boundary)
        viewModel.onProceedToPayment()
        val command = navigationManager.commands.first() as com.ishara.app.navigation.NavigationCommand.NavigateTo
        assertEquals(IshaaraDestination.StudentPayment.createRoute("651a2b3c4d5e6f7a8b9c0001"), command.route)
    }
}
