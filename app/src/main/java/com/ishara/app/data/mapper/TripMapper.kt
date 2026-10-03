package com.ishara.app.data.mapper

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.data.remote.dto.CleanTripResponseDto
import com.ishara.app.data.remote.dto.CreateTripRequestDto
import com.ishara.app.data.remote.dto.LocationInputDto
import com.ishara.app.data.remote.dto.TripDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.data.remote.dto.TripRouteDto
import com.ishara.app.data.remote.dto.TripRouteGeometryDto
import com.ishara.app.domain.model.CreateTripParams
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripActorRole
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.model.TripRoute
import com.ishara.app.domain.model.TripRouteGeometry
import com.ishara.app.domain.model.TripStatus

object TripMapper {

    fun toDomain(dto: TripLocationDto): TripLocation {
        return TripLocation(
            address = dto.address,
            coordinate = if (dto.coordinates.size >= 2) {
                // Backend coordinates format is [longitude, latitude] GeoJSON
                GeoJsonCoordinate(latitude = dto.coordinates[1], longitude = dto.coordinates[0])
            } else {
                GeoJsonCoordinate(latitude = 0.0, longitude = 0.0)
            },
            name = dto.name,
            googlePlaceId = dto.googlePlaceId,
            serpApiDataId = dto.serpApiDataId
        )
    }

    fun toDto(location: TripLocation): TripLocationDto {
        return TripLocationDto(
            address = location.address,
            // [longitude, latitude]
            coordinates = doubleArrayOf(location.longitude, location.latitude),
            name = location.name,
            googlePlaceId = location.googlePlaceId,
            serpApiDataId = location.serpApiDataId
        )
    }

    fun toLocationInputDto(location: TripLocation): LocationInputDto {
        return LocationInputDto(
            name = location.name,
            formattedAddress = location.address,
            latitude = location.latitude,
            longitude = location.longitude,
            googlePlaceId = location.googlePlaceId,
            serpApiDataId = location.serpApiDataId
        )
    }

    fun toDomain(routeDto: TripRouteDto?): TripRoute? {
        if (routeDto == null) return null
        return TripRoute(
            geometry = routeDto.geometry?.let { geom ->
                TripRouteGeometry(
                    type = geom.type,
                    coordinates = geom.coordinates.map {
                        Pair(it.getOrElse(0) { 0.0 }, it.getOrElse(1) { 0.0 })
                    }
                )
            },
            distanceMeters = routeDto.distanceMeters,
            durationSeconds = routeDto.durationSeconds,
            provider = routeDto.provider
        )
    }

    fun toDto(route: TripRoute?): TripRouteDto? {
        if (route == null) return null
        return TripRouteDto(
            geometry = route.geometry?.let { geom ->
                TripRouteGeometryDto(
                    type = geom.type,
                    coordinates = geom.coordinates.map { doubleArrayOf(it.first, it.second) }
                )
            },
            distanceMeters = route.distanceMeters,
            durationSeconds = route.durationSeconds,
            provider = route.provider
        )
    }

    fun toDomainStatus(status: String): TripStatus {
        return when (status.trim().uppercase()) {
            "SCHEDULED" -> TripStatus.SCHEDULED
            "ASSIGNED" -> TripStatus.ASSIGNED
            "READY" -> TripStatus.READY
            "ACTIVE" -> TripStatus.ACTIVE
            "COMPLETED" -> TripStatus.COMPLETED
            "CANCELLED" -> TripStatus.CANCELLED
            else -> TripStatus.CREATED
        }
    }

    fun toDomainRole(role: String?): TripActorRole? {
        return when (role?.trim()?.uppercase()) {
            "DRIVER" -> TripActorRole.DRIVER
            "AGENCY_OWNER" -> TripActorRole.AGENCY_OWNER
            "ADMIN" -> TripActorRole.ADMIN
            else -> null
        }
    }

    /**
     * Authoritative conversion from backend CleanTripResponseDto to domain Trip.
     */
    fun toDomain(dto: CleanTripResponseDto): Trip {
        return Trip(
            id = dto.id,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            agencyId = dto.agencyId,
            operatorId = dto.operatorId,
            origin = toDomain(dto.origin),
            destination = toDomain(dto.destination),
            route = toDomain(dto.route),
            status = toDomainStatus(dto.status),
            scheduledDepartureAt = dto.scheduledDepartureAt,
            startedAt = dto.startedAt,
            completedAt = dto.completedAt,
            cancelledAt = dto.cancelledAt,
            cancellationReason = dto.cancellationReason,
            cancelledBy = dto.cancelledBy,
            cancelledByRole = toDomainRole(dto.cancelledByRole),
            createdBy = dto.createdBy,
            createdByRole = toDomainRole(dto.createdByRole),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toCreateRequestDto(params: CreateTripParams): CreateTripRequestDto {
        return CreateTripRequestDto(
            vehicleId = params.vehicleId,
            origin = toLocationInputDto(params.origin),
            destination = toLocationInputDto(params.destination),
            route = toDto(params.route),
            scheduledDepartureAt = params.scheduledDepartureAt
        )
    }

    // Legacy conversion for student discovery compatibility
    fun toDomain(dto: TripDto): Trip {
        return Trip(
            id = dto.id,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            origin = toDomain(dto.origin),
            destination = toDomain(dto.destination),
            waypoints = dto.waypoints.map { toDomain(it) },
            totalSeats = dto.totalSeats,
            availableSeats = dto.availableSeats,
            baseFarePaise = dto.baseFarePaise,
            status = toDomainStatus(dto.status),
            startedAt = dto.startedAt?.toString(),
            completedAt = dto.completedAt?.toString()
        )
    }
}
