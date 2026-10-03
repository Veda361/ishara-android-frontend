package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.DiscoveredTripDriver
import com.ishara.app.domain.model.DiscoveredTripVehicle
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.model.TripCompatibility
import com.ishara.app.domain.repository.RideRepository
import com.ishara.app.domain.usecase.CreateRideRequestUseCase
import com.ishara.app.feature.student.riderequest.RideRequestStage
import com.ishara.app.feature.student.riderequest.RideRequestViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideRequestViewModelTest {

    private val testDispatcher = Dispatchers.Unconfined
    private val testScope = CoroutineScope(testDispatcher + Job())

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val sampleTrip = DiscoveredTrip(
        tripId = "651a2b3c4d5e6f7a8b9c0d1e",
        driver = DiscoveredTripDriver("drv_1", "Verified Operator"),
        vehicle = DiscoveredTripVehicle("veh_1", "UP93AT1234", "BUS", "Tata", "Starbus"),
        originName = "Gate A",
        originAddress = "Kenyatta University Gate A",
        originCoordinate = GeoJsonCoordinate(latitude = -1.1818, longitude = 36.9275),
        destinationName = "Safari Park",
        destinationAddress = "Safari Park Hotel",
        destinationCoordinate = GeoJsonCoordinate(latitude = -1.2185, longitude = 36.8856),
        distanceMeters = 4500.0,
        durationSeconds = 900.0,
        pickupDistanceMeters = 50.0,
        destinationDistanceMeters = 80.0,
        estimatedDetourMeters = 130.0,
        compatibility = TripCompatibility.HIGH,
        matchScore = 0.95
    )

    private val sampleResult = RideRequestResult(
        id = "req_123",
        tripId = "651a2b3c4d5e6f7a8b9c0d1e",
        driverId = "drv_1",
        userId = "usr_1",
        pickupAddress = "Kenyatta University Gate A",
        destinationAddress = "Safari Park Hotel",
        status = RideRequestStatus.PENDING,
        requestedAt = "2026-09-23T13:00:00Z",
        expiresAt = "2026-09-23T13:02:00Z"
    )

    private class FakeRideRepository(
        var resultToReturn: IshaaraResult<RideRequestResult>
    ) : RideRepository {
        var callCount = 0
        var lastIdempotencyKey: String? = null

        override suspend fun submitRideRequest(
            input: RideRequestInput,
            idempotencyKey: String?
        ): IshaaraResult<RideRequestResult> {
            callCount++
            lastIdempotencyKey = idempotencyKey
            return resultToReturn
        }

        override suspend fun createRideRequest(tripId: String, pickupAddress: String, dropoffAddress: String, seatsRequested: Int) = throw UnsupportedOperationException()
        override suspend fun getActiveRide(rideId: String) = throw UnsupportedOperationException()
        override suspend fun getActivePassengerRide() = IshaaraResult.success(null)
        override suspend fun cancelRide(rideId: String, reason: String?) = IshaaraResult.success(Unit)
    }

    @Test
    fun testInitialState_isReviewReadyWithStableIdempotencyKey() = runBlocking {
        val fakeRepo = FakeRideRepository(IshaaraResult.success(sampleResult))
        val useCase = CreateRideRequestUseCase(fakeRepo)
        val viewModel = RideRequestViewModel(
            trip = sampleTrip,
            pickupAddress = sampleTrip.originAddress,
            destinationAddress = sampleTrip.destinationAddress,
            pickupLatitude = sampleTrip.originCoordinate.latitude,
            pickupLongitude = sampleTrip.originCoordinate.longitude,
            destinationLatitude = sampleTrip.destinationCoordinate.latitude,
            destinationLongitude = sampleTrip.destinationCoordinate.longitude,
            createRideRequestUseCase = useCase,
            navigationManager = NavigationManager(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        val state = viewModel.uiState.value
        assertTrue(state.stage is RideRequestStage.ReviewReady)
        assertFalse(state.isSubmitting)
        assertNotNull(viewModel.idempotencyKey)
        assertTrue(viewModel.idempotencyKey.isNotBlank())
    }

    @Test
    fun testSubmitRideRequest_transitionsToSubmittedWithPendingStatus() = runBlocking {
        val fakeRepo = FakeRideRepository(IshaaraResult.success(sampleResult))
        val useCase = CreateRideRequestUseCase(fakeRepo)
        val viewModel = RideRequestViewModel(
            trip = sampleTrip,
            pickupAddress = sampleTrip.originAddress,
            destinationAddress = sampleTrip.destinationAddress,
            pickupLatitude = sampleTrip.originCoordinate.latitude,
            pickupLongitude = sampleTrip.originCoordinate.longitude,
            destinationLatitude = sampleTrip.destinationCoordinate.latitude,
            destinationLongitude = sampleTrip.destinationCoordinate.longitude,
            createRideRequestUseCase = useCase,
            navigationManager = NavigationManager(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        viewModel.submitRideRequest()

        val state = viewModel.uiState.value
        assertTrue(state.stage is RideRequestStage.Submitted)
        assertFalse(state.isSubmitting)
        val submitted = (state.stage as RideRequestStage.Submitted).result
        assertEquals("req_123", submitted.id)
        assertEquals(RideRequestStatus.PENDING, submitted.status)
        assertEquals(1, fakeRepo.callCount)
        assertEquals(viewModel.idempotencyKey, fakeRepo.lastIdempotencyKey)
    }

    @Test
    fun testDuplicateSubmission_isPreventedWhileRequestInProgress() = runBlocking {
        val fakeRepo = FakeRideRepository(IshaaraResult.success(sampleResult))
        val useCase = CreateRideRequestUseCase(fakeRepo)
        val viewModel = RideRequestViewModel(
            trip = sampleTrip,
            pickupAddress = sampleTrip.originAddress,
            destinationAddress = sampleTrip.destinationAddress,
            pickupLatitude = sampleTrip.originCoordinate.latitude,
            pickupLongitude = sampleTrip.originCoordinate.longitude,
            destinationLatitude = sampleTrip.destinationCoordinate.latitude,
            destinationLongitude = sampleTrip.destinationCoordinate.longitude,
            createRideRequestUseCase = useCase,
            navigationManager = NavigationManager(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        // First click
        viewModel.submitRideRequest()
        // Immediate second click (before coroutine completes)
        viewModel.submitRideRequest()

        // Call count must strictly be 1
        assertEquals(1, fakeRepo.callCount)
    }

    @Test
    fun testSubmissionFailure_transitionsToErrorAndRetryPreservesIdempotencyKey() = runBlocking {
        val fakeRepo = FakeRideRepository(
            IshaaraResult.failure(IshaaraError.Conflict(message = "You already have an active pending ride request for this trip."))
        )
        val useCase = CreateRideRequestUseCase(fakeRepo)
        val viewModel = RideRequestViewModel(
            trip = sampleTrip,
            pickupAddress = sampleTrip.originAddress,
            destinationAddress = sampleTrip.destinationAddress,
            pickupLatitude = sampleTrip.originCoordinate.latitude,
            pickupLongitude = sampleTrip.originCoordinate.longitude,
            destinationLatitude = sampleTrip.destinationCoordinate.latitude,
            destinationLongitude = sampleTrip.destinationCoordinate.longitude,
            createRideRequestUseCase = useCase,
            navigationManager = NavigationManager(),
            dispatchers = testDispatchers,
            externalScope = testScope
        )

        val originalKey = viewModel.idempotencyKey

        viewModel.submitRideRequest()

        val state = viewModel.uiState.value
        assertTrue(state.stage is RideRequestStage.Error)
        assertFalse(state.isSubmitting)
        assertTrue(state.errorMessage?.contains("already have an active pending") == true)

        // Now change repo to succeed and retry
        fakeRepo.resultToReturn = IshaaraResult.success(sampleResult)
        viewModel.retry()

        val retriedState = viewModel.uiState.value
        assertTrue(retriedState.stage is RideRequestStage.Submitted)
        assertEquals(2, fakeRepo.callCount)
        // Must reuse the exact same idempotency key
        assertEquals(originalKey, fakeRepo.lastIdempotencyKey)
    }
}
