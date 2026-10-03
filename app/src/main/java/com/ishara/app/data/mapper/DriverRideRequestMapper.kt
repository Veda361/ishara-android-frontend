package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.DriverRideRequestListResponse
import com.ishara.app.data.remote.dto.DriverRideResponseDto
import com.ishara.app.data.remote.dto.LocationDto
import com.ishara.app.data.remote.dto.RideRequestItemDto
import com.ishara.app.domain.model.DriverPassengerRide
import com.ishara.app.domain.model.DriverRideRequest
import com.ishara.app.domain.model.DriverRideRequestPage
import com.ishara.app.domain.model.DriverRideRequestStatus
import com.ishara.app.domain.model.DriverRideStatus

object DriverRideRequestMapper {

    fun mapPage(dto: DriverRideRequestListResponse): DriverRideRequestPage {
        return DriverRideRequestPage(
            items = dto.items.map { mapItem(it) },
            total = dto.total,
            page = dto.page,
            limit = dto.limit,
            hasMore = dto.hasMore
        )
    }

    fun mapItem(dto: RideRequestItemDto): DriverRideRequest {
        val (pickupLat, pickupLng) = parseCoordinates(dto.pickup)
        val (destLat, destLng) = parseCoordinates(dto.destination)

        return DriverRideRequest(
            id = dto.id,
            tripId = dto.tripId,
            driverId = dto.driverId,
            passengerId = dto.userId,
            pickupAddress = dto.pickup.formattedAddress ?: "Pickup location",
            pickupCoordinates = Pair(pickupLat, pickupLng),
            destinationAddress = dto.destination.formattedAddress ?: "Destination location",
            destinationCoordinates = Pair(destLat, destLng),
            status = DriverRideRequestStatus.fromBackend(dto.status),
            requestedAt = dto.requestedAt,
            expiresAt = dto.expiresAt,
            respondedAt = dto.respondedAt,
            rejectionReason = dto.rejectionReason,
            cancellationReason = dto.cancellationReason
        )
    }

    fun mapRide(dto: DriverRideResponseDto): DriverPassengerRide {
        val (pickupLat, pickupLng) = parseCoordinates(dto.pickup)
        val (destLat, destLng) = parseCoordinates(dto.destination)

        return DriverPassengerRide(
            id = dto.id,
            rideRequestId = dto.rideRequestId,
            tripId = dto.tripId,
            driverId = dto.driverId,
            passengerId = dto.userId,
            pickupAddress = dto.pickup.formattedAddress ?: "Pickup location",
            pickupCoordinates = Pair(pickupLat, pickupLng),
            destinationAddress = dto.destination.formattedAddress ?: "Destination location",
            destinationCoordinates = Pair(destLat, destLng),
            status = DriverRideStatus.fromBackend(dto.status),
            pickupTime = dto.pickupTime,
            startTime = dto.startTime,
            completionTime = dto.completionTime,
            cancellationReason = dto.cancellationReason
        )
    }

    /**
     * GeoJSON standard RFC 7946 coordinates format: [longitude, latitude].
     * Map into domain (latitude, longitude) pair safely.
     */
    private fun parseCoordinates(location: LocationDto): Pair<Double, Double> {
        val coords = location.coordinates
        return if (coords.size >= 2) {
            val longitude = coords[0]
            val latitude = coords[1]
            Pair(latitude, longitude)
        } else {
            Pair(0.0, 0.0)
        }
    }
}
