package com.ishara.app.feature.student.riderequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestLocationWaypoint
import com.ishara.app.domain.usecase.CreateRideRequestUseCase
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Production ViewModel managing the Ride Request Review and Submission flow.
 *
 * Implements:
 * 1. Double-tap and duplicate submission prevention.
 * 2. Persistent Idempotency-Key per review instance.
 * 3. Accurate state transitions (ReviewReady -> Submitting -> Submitted | Error).
 * 4. Factual PENDING state representation upon successful submission.
 */
class RideRequestViewModel(
    val trip: DiscoveredTrip,
    val pickupAddress: String,
    val destinationAddress: String,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    private val createRideRequestUseCase: CreateRideRequestUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = externalScope ?: viewModelScope

    // Single stable idempotency key generated once per review session
    val idempotencyKey: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(
        RideRequestUiState(
            trip = trip,
            pickupAddress = pickupAddress,
            destinationAddress = destinationAddress,
            stage = RideRequestStage.ReviewReady,
            isSubmitting = false
        )
    )
    val uiState: StateFlow<RideRequestUiState> = _uiState.asStateFlow()

    private var submitJob: Job? = null

    /**
     * Confirms and submits the ride request to the backend.
     * Guarded against duplicate / concurrent calls.
     */
    fun submitRideRequest() {
        if (_uiState.value.isSubmitting || _uiState.value.stage is RideRequestStage.Submitted || submitJob?.isActive == true) {
            return
        }

        val input = RideRequestInput(
            tripId = trip.tripId,
            pickup = RideRequestLocationWaypoint(
                formattedAddress = pickupAddress,
                latitude = pickupLatitude,
                longitude = pickupLongitude,
                name = trip.originName
            ),
            destination = RideRequestLocationWaypoint(
                formattedAddress = destinationAddress,
                latitude = destinationLatitude,
                longitude = destinationLongitude,
                name = trip.destinationName
            )
        )

        _uiState.update {
            it.copy(
                stage = RideRequestStage.Submitting,
                isSubmitting = true,
                errorMessage = null
            )
        }

        submitJob?.cancel()
        submitJob = scope.launch(dispatchers.io) {
            val result = createRideRequestUseCase.execute(input, idempotencyKey)
            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            stage = RideRequestStage.Submitted(result.data),
                            isSubmitting = false,
                            errorMessage = null
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            stage = RideRequestStage.Error(result.error),
                            isSubmitting = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Retries submission reusing the same stable idempotency key.
     */
    fun retry() {
        submitRideRequest()
    }

    /**
     * Navigates back to Student Home after viewing the submitted request.
     */
    fun onDoneClicked() {
        navigationManager.navigate(IshaaraDestination.StudentHome.route)
    }

    /**
     * Navigates to the live Ride Request Status tracking screen.
     */
    fun onViewStatusClicked(requestId: String) {
        navigationManager.navigate(IshaaraDestination.StudentRideRequestStatus.createRoute(requestId))
    }

    /**
     * Navigates back to Discovery to re-evaluate options.
     */
    fun onBackClicked() {
        navigationManager.navigateUp()
    }
}
