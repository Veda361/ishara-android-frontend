package com.ishara.app.data.mapper

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.data.remote.dto.DiscoveryCoordinateDto
import com.ishara.app.data.remote.dto.DiscoveryItemDto
import com.ishara.app.data.remote.dto.DiscoveryOptionsDto
import com.ishara.app.data.remote.dto.DiscoveryResponseDto
import com.ishara.app.data.remote.dto.DiscoverySearchRequestDto
import com.ishara.app.data.remote.dto.PublicTripResponseDto
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.DiscoveredTripDriver
import com.ishara.app.domain.model.DiscoveredTripFare
import com.ishara.app.domain.model.DiscoveredTripVehicle
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.DiscoveryResult
import com.ishara.app.domain.model.PassengerTripDetails
import com.ishara.app.domain.model.TripCompatibility

/**
 * Mapper bridging the strict backend Discovery DTOs and domain models.
 *
 * CRITICAL COORDINATE RULES:
 * 1. Request DTO: Uses explicit `latitude` and `longitude` numbers as required by backend `CoordinateSchema`.
 * 2. Response DTO: Origin and Destination coordinates use GeoJSON Point arrays where:
 *    index 0 = longitude
 *    index 1 = latitude
 */
object DiscoveryMapper {

    fun toRequestDto(query: DiscoveryQuery): DiscoverySearchRequestDto {
        val optionsDto = if (query.maxPickupDistanceMeters != null ||
            query.maxDestinationDeviationMeters != null ||
            query.maxResults != null ||
            query.cursor != null
        ) {
            DiscoveryOptionsDto(
                maxPickupDistanceMeters = query.maxPickupDistanceMeters,
                maxDestinationDeviationMeters = query.maxDestinationDeviationMeters,
                maxResults = query.maxResults,
                cursor = query.cursor
            )
        } else {
            null
        }

        return DiscoverySearchRequestDto(
            origin = DiscoveryCoordinateDto(
                latitude = query.originLatitude,
                longitude = query.originLongitude,
                name = query.originName,
                formattedAddress = query.originAddress
            ),
            destination = DiscoveryCoordinateDto(
                latitude = query.destinationLatitude,
                longitude = query.destinationLongitude,
                name = query.destinationName,
                formattedAddress = query.destinationAddress
            ),
            options = optionsDto
        )
    }

    fun toDomain(dto: DiscoveryResponseDto): DiscoveryResult {
        return DiscoveryResult(
            discoverySessionId = dto.discoverySessionId,
            items = dto.items.map { toDomain(it) },
            limit = dto.pagination.limit,
            hasMore = dto.pagination.hasMore,
            nextCursor = dto.pagination.nextCursor
        )
    }

    fun toDomain(dto: DiscoveryItemDto): DiscoveredTrip {
        // Backend GeoJSON array: [longitude, latitude]
        val originCoords = dto.origin.coordinates.coordinates
        val originLong = if (originCoords.isNotEmpty()) originCoords[0] else 0.0
        val originLat = if (originCoords.size > 1) originCoords[1] else 0.0

        val destCoords = dto.destination.coordinates.coordinates
        val destLong = if (destCoords.isNotEmpty()) destCoords[0] else 0.0
        val destLat = if (destCoords.size > 1) destCoords[1] else 0.0

        val compatibility = when (dto.match.compatibility.uppercase()) {
            "HIGH" -> TripCompatibility.HIGH
            "MEDIUM" -> TripCompatibility.MEDIUM
            "LOW" -> TripCompatibility.LOW
            else -> TripCompatibility.UNKNOWN
        }

        return DiscoveredTrip(
            tripId = dto.tripId,
            driver = DiscoveredTripDriver(
                id = dto.driver.id,
                name = dto.driver.name.ifBlank { "Verified Driver" },
                imageUrl = dto.driver.image
            ),
            vehicle = DiscoveredTripVehicle(
                id = dto.vehicle.id,
                registrationNumber = dto.vehicle.registrationNumber,
                vehicleType = dto.vehicle.vehicleType,
                make = dto.vehicle.make,
                model = dto.vehicle.model
            ),
            originName = dto.origin.name,
            originAddress = dto.origin.formattedAddress,
            originCoordinate = GeoJsonCoordinate(latitude = originLat, longitude = originLong),
            destinationName = dto.destination.name,
            destinationAddress = dto.destination.formattedAddress,
            destinationCoordinate = GeoJsonCoordinate(latitude = destLat, longitude = destLong),
            distanceMeters = dto.routeSummary.distanceMeters,
            durationSeconds = dto.routeSummary.durationSeconds,
            pickupDistanceMeters = dto.match.pickupDistanceMeters,
            destinationDistanceMeters = dto.match.destinationDistanceMeters,
            estimatedDetourMeters = dto.match.estimatedDetourMeters,
            compatibility = compatibility,
            matchScore = dto.match.score,
            estimatedFare = dto.estimatedFare?.let {
                DiscoveredTripFare(
                    amountMinor = it.amountMinor,
                    currency = it.currency,
                    formatted = it.formatted
                )
            }
        )
    }

    fun toDomain(dto: PublicTripResponseDto): PassengerTripDetails {
        val originCoords = dto.origin.coordinates.coordinates
        val originLong = if (originCoords.isNotEmpty()) originCoords[0] else 0.0
        val originLat = if (originCoords.size > 1) originCoords[1] else 0.0

        val destCoords = dto.destination.coordinates.coordinates
        val destLong = if (destCoords.isNotEmpty()) destCoords[0] else 0.0
        val destLat = if (destCoords.size > 1) destCoords[1] else 0.0

        val routeCoords = dto.route?.geometry?.coordinates?.map { coord ->
            val lng = if (coord.isNotEmpty()) coord[0] else 0.0
            val lat = if (coord.size > 1) coord[1] else 0.0
            GeoJsonCoordinate(latitude = lat, longitude = lng)
        } ?: emptyList()

        return PassengerTripDetails(
            tripId = dto.id,
            status = TripMapper.toDomainStatus(dto.status),
            originName = dto.origin.name,
            originAddress = dto.origin.formattedAddress,
            originCoordinate = GeoJsonCoordinate(latitude = originLat, longitude = originLong),
            destinationName = dto.destination.name,
            destinationAddress = dto.destination.formattedAddress,
            destinationCoordinate = GeoJsonCoordinate(latitude = destLat, longitude = destLong),
            distanceMeters = dto.route?.distanceMeters,
            durationSeconds = dto.route?.durationSeconds,
            routeCoordinates = routeCoords,
            scheduledDepartureAt = dto.scheduledDepartureAt,
            startedAt = dto.startedAt,
            createdAt = dto.createdAt,
            driver = DiscoveredTripDriver(
                id = dto.driver.id,
                name = dto.driver.name?.ifBlank { "Verified Driver" } ?: "Verified Driver",
                imageUrl = dto.driver.image
            ),
            vehicle = DiscoveredTripVehicle(
                id = dto.vehicle.id,
                registrationNumber = dto.vehicle.registrationNumber,
                vehicleType = dto.vehicle.vehicleType,
                make = dto.vehicle.make,
                model = dto.vehicle.model
            )
        )
    }
}

