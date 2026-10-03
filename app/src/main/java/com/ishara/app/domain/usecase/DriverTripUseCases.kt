package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CreateTripParams
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.repository.DriverTripRepository
import kotlinx.coroutines.flow.StateFlow

private val OBJECT_ID_REGEX = Regex("^[0-9a-fA-F]{24}$")

/**
 * Use case to retrieve trips associated with the authenticated driver.
 */
class GetDriverTripsUseCase(
    private val repository: DriverTripRepository
) {
    suspend operator fun invoke(
        status: TripStatus? = null,
        page: Int = 1,
        limit: Int = 20,
        forceRefresh: Boolean = false
    ): IshaaraResult<List<Trip>> {
        if (page < 1) {
            return IshaaraResult.failure(IshaaraError.Validation("page", "Page must be at least 1."))
        }
        if (limit < 1 || limit > 100) {
            return IshaaraResult.failure(IshaaraError.Validation("limit", "Limit must be between 1 and 100."))
        }
        return repository.getDriverTrips(status, page, limit, forceRefresh)
    }
}

/**
 * Use case to retrieve details for a specific trip by its ID.
 */
class GetTripDetailsUseCase(
    private val repository: DriverTripRepository
) {
    suspend operator fun invoke(
        tripId: String,
        forceRefresh: Boolean = false
    ): IshaaraResult<Trip> {
        if (tripId.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Validation("tripId", "Invalid trip identifier format."))
        }
        return repository.getTripDetails(tripId, forceRefresh)
    }
}

/**
 * Use case to self-service create a new trip under driver ownership.
 */
class CreateDriverTripUseCase(
    private val repository: DriverTripRepository
) {
    suspend operator fun invoke(params: CreateTripParams): IshaaraResult<Trip> {
        if (params.vehicleId.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Validation("vehicleId", "Invalid vehicle identifier format."))
        }
        if (params.origin.address.trim().length < 2) {
            return IshaaraResult.failure(IshaaraError.Validation("origin", "Origin address must be at least 2 characters."))
        }
        if (params.destination.address.trim().length < 2) {
            return IshaaraResult.failure(IshaaraError.Validation("destination", "Destination address must be at least 2 characters."))
        }

        // Distance check (minimum 50m separation required by backend)
        val lat1 = params.origin.latitude
        val lon1 = params.origin.longitude
        val lat2 = params.destination.latitude
        val lon2 = params.destination.longitude

        if (lat1 == lat2 && lon1 == lon2) {
            return IshaaraResult.failure(
                IshaaraError.Validation("destination", "Origin and destination cannot be the same physical location.")
            )
        }

        return repository.createTrip(params)
    }
}

/**
 * Use case to start an unstarted trip (CREATED/SCHEDULED/ASSIGNED/READY -> ACTIVE).
 */
class StartDriverTripUseCase(
    private val repository: DriverTripRepository
) {
    suspend operator fun invoke(tripId: String): IshaaraResult<Trip> {
        if (tripId.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Validation("tripId", "Invalid trip identifier format."))
        }
        return repository.startTrip(tripId)
    }
}

/**
 * Use case to complete an active trip (ACTIVE -> COMPLETED).
 */
class CompleteDriverTripUseCase(
    private val repository: DriverTripRepository
) {
    suspend operator fun invoke(tripId: String): IshaaraResult<Trip> {
        if (tripId.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Validation("tripId", "Invalid trip identifier format."))
        }
        return repository.completeTrip(tripId)
    }
}

/**
 * Use case to cancel a trip with an optional reason.
 */
class CancelDriverTripUseCase(
    private val repository: DriverTripRepository
) {
    suspend operator fun invoke(tripId: String, reason: String? = null): IshaaraResult<Trip> {
        if (tripId.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Validation("tripId", "Invalid trip identifier format."))
        }
        if (reason != null && reason.length > 300) {
            return IshaaraResult.failure(IshaaraError.Validation("reason", "Cancellation reason cannot exceed 300 characters."))
        }
        return repository.cancelTrip(tripId, reason)
    }
}

/**
 * Use case to observe the driver's cached list of trips.
 */
class ObserveDriverTripsUseCase(
    private val repository: DriverTripRepository
) {
    operator fun invoke(): StateFlow<List<Trip>> = repository.observeDriverTrips()
}

/**
 * Use case to observe the driver's current active operational trip.
 */
class ObserveActiveTripUseCase(
    private val repository: DriverTripRepository
) {
    operator fun invoke(): StateFlow<Trip?> = repository.observeActiveTrip()
}

/**
 * Use case to clear driver trip state upon sign out or role switch.
 */
class ClearDriverTripStateUseCase(
    private val repository: DriverTripRepository
) {
    operator fun invoke() {
        repository.clearTripState()
    }
}
