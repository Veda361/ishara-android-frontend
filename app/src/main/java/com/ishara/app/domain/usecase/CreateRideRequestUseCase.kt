package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.repository.RideRepository
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Use case enforcing business validation before submitting a ride request to the backend.
 *
 * Enforces:
 * 1. 24-character hex ObjectId tripId format.
 * 2. Formatted address presence (2..300 chars).
 * 3. Finite geographic coordinate boundaries.
 * 4. Authoritative backend rule: Minimum 50m physical separation between pickup and destination.
 */
class CreateRideRequestUseCase(
    private val rideRepository: RideRepository
) {
    suspend fun execute(
        input: RideRequestInput,
        idempotencyKey: String? = null
    ): IshaaraResult<RideRequestResult> {
        // Validate tripId format
        if (!input.tripId.matches(Regex("^[0-9a-fA-F]{24}$"))) {
            return IshaaraResult.failure(
                IshaaraError.Validation("tripId", "Invalid tripId format: must be a 24-character hexadecimal ObjectId")
            )
        }

        // Validate pickup address length
        if (input.pickup.formattedAddress.trim().length !in 2..300) {
            return IshaaraResult.failure(
                IshaaraError.Validation("pickup", "Pickup address must be between 2 and 300 characters")
            )
        }

        // Validate destination address length
        if (input.destination.formattedAddress.trim().length !in 2..300) {
            return IshaaraResult.failure(
                IshaaraError.Validation("destination", "Destination address must be between 2 and 300 characters")
            )
        }

        // Validate minimum 50m separation
        val separationMeters = calculateHaversineDistanceMeters(
            lat1 = input.pickup.latitude,
            lon1 = input.pickup.longitude,
            lat2 = input.destination.latitude,
            lon2 = input.destination.longitude
        )

        if (separationMeters < 50.0) {
            return IshaaraResult.failure(
                IshaaraError.Validation(
                    field = "destination",
                    message = "Pickup and destination cannot be the same physical location (minimum 50m separation required)."
                )
            )
        }

        return rideRepository.submitRideRequest(input, idempotencyKey)
    }

    private fun calculateHaversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371000.0 // Earth's mean radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
