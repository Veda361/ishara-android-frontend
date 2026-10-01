package com.ishara.app.data.mapper

import com.ishara.app.core.common.DateUtils
import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.data.remote.dto.TripDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.model.TripStatus

/**
 * Maps between Trip Remote DTOs and Domain Models.
 * Ensures consistent handling of coordinates and timestamps.
 */
object TripMapper {
    fun toDomain(dto: TripLocationDto): TripLocation {
        return TripLocation(
            address = dto.address ?: dto.name,
            coordinate = GeoJsonCoordinate.fromArray(dto.coordinates.toDoubleArray())
        )
    }

    fun toDto(location: TripLocation): TripLocationDto {
        return TripLocationDto(
            name = location.address,
            address = location.address,
            coordinates = location.coordinate.toArray().toList()
        )
    }

    fun toDomain(dto: TripDto): Trip {
        return Trip(
            id = dto.id,
            driverId = dto.driverId ?: "",
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
            startedAt = DateUtils.parseIso8601(dto.startedAt),
            completedAt = DateUtils.parseIso8601(dto.completedAt)
        )
    }
}
