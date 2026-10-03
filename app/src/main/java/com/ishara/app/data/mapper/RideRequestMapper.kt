package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.CreateRideRequestDto
import com.ishara.app.data.remote.dto.LocationWaypointDto
import com.ishara.app.data.remote.dto.RideRequestResponseDto
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestLocationWaypoint
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus

/**
 * Mapper for converting between RideRequest domain models and strict DTOs.
 */
object RideRequestMapper {

    fun toDto(domain: RideRequestInput): CreateRideRequestDto {
        return CreateRideRequestDto(
            tripId = domain.tripId,
            pickup = toDto(domain.pickup),
            destination = toDto(domain.destination),
            discoverySessionId = domain.discoverySessionId
        )
    }

    fun toDto(domain: RideRequestLocationWaypoint): LocationWaypointDto {
        return LocationWaypointDto(
            formattedAddress = domain.formattedAddress,
            latitude = domain.latitude,
            longitude = domain.longitude,
            name = domain.name,
            googlePlaceId = domain.googlePlaceId,
            serpApiDataId = domain.serpApiDataId
        )
    }

    fun toDomain(dto: RideRequestResponseDto, associatedRideId: String? = null): RideRequestResult {
        val status = when (dto.status.uppercase()) {
            "PENDING" -> RideRequestStatus.PENDING
            "ACCEPTED" -> RideRequestStatus.ACCEPTED
            "REJECTED" -> RideRequestStatus.REJECTED
            "CANCELLED" -> RideRequestStatus.CANCELLED
            "EXPIRED" -> RideRequestStatus.EXPIRED
            else -> RideRequestStatus.PENDING
        }

        // Coordinates in GeoJSON RFC 7946: [longitude, latitude]
        val pickupCoords = if (dto.pickup.coordinates.coordinates.size >= 2) {
            com.ishara.app.core.location.GeoJsonCoordinate(
                latitude = dto.pickup.coordinates.coordinates[1],
                longitude = dto.pickup.coordinates.coordinates[0]
            )
        } else null

        val destCoords = if (dto.destination.coordinates.coordinates.size >= 2) {
            com.ishara.app.core.location.GeoJsonCoordinate(
                latitude = dto.destination.coordinates.coordinates[1],
                longitude = dto.destination.coordinates.coordinates[0]
            )
        } else null

        return RideRequestResult(
            id = dto.id,
            tripId = dto.tripId,
            driverId = dto.driverId,
            userId = dto.userId,
            pickupAddress = dto.pickup.formattedAddress,
            destinationAddress = dto.destination.formattedAddress,
            status = status,
            requestedAt = dto.requestedAt,
            expiresAt = dto.expiresAt,
            respondedAt = dto.respondedAt,
            discoverySessionId = dto.discoverySessionId,
            rejectionReason = dto.rejectionReason,
            cancellationReason = dto.cancellationReason,
            pickupCoordinates = pickupCoords,
            destinationCoordinates = destCoords,
            associatedRideId = associatedRideId
        )
    }
}
