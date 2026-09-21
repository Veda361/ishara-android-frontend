package com.ishara.app.data.mapper

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.data.remote.dto.TripDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.model.TripStatus

object TripMapper {
    fun toDomain(dto: TripLocationDto): TripLocation {
        return TripLocation(
            address = dto.address,
            coordinate = GeoJsonCoordinate.fromArray(dto.coordinates)
        )
    }

    fun toDto(location: TripLocation): TripLocationDto {
        return TripLocationDto(
            address = location.address,
            coordinates = location.coordinate.toArray()
        )
    }

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
            status = try {
                TripStatus.valueOf(dto.status.uppercase())
            } catch (_: Exception) {
                TripStatus.CREATED
            },
            startedAt = dto.startedAt,
            completedAt = dto.completedAt
        )
    }
}
