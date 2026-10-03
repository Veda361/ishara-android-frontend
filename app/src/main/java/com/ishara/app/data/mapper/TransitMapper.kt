package com.ishara.app.data.mapper

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.data.remote.dto.TransitTripDto
import com.ishara.app.data.remote.dto.TransitTripLocationDto
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.model.TransitSchedule
import com.ishara.app.domain.model.TransitStop
import com.ishara.app.domain.model.TransitStopType
import com.ishara.app.domain.model.TransitVehicleType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TransitMapper {

    fun toDomain(dto: TransitTripDto, cachedAtMillis: Long = System.currentTimeMillis()): TransitRoute {
        val originStop = toTransitStop(
            dto = dto.origin,
            fallbackId = "${dto.id}_origin",
            sequence = 0,
            type = TransitStopType.ORIGIN
        )
        val destinationStop = toTransitStop(
            dto = dto.destination,
            fallbackId = "${dto.id}_dest",
            sequence = 1,
            type = TransitStopType.DESTINATION
        )

        val geometry = dto.route?.geometryCoordinates?.mapNotNull { point ->
            if (point.size >= 2) {
                // GeoJSON format is [longitude, latitude]
                LocationCoordinates(
                    latitude = point[1],
                    longitude = point[0]
                )
            } else null
        } ?: emptyList()

        val routeDisplayName = "${originStop.name} ↔ ${destinationStop.name}"
        val startedAtEpoch = parseIsoTimestamp(dto.startedAt)

        return TransitRoute(
            id = dto.id,
            name = routeDisplayName,
            originStop = originStop,
            destinationStop = destinationStop,
            stops = listOf(originStop, destinationStop),
            geometry = geometry,
            distanceMeters = dto.route?.distanceMeters,
            durationSeconds = dto.route?.durationSeconds,
            vehicleType = TransitVehicleType.fromBackend(dto.vehicle.vehicleType),
            vehiclePlate = dto.vehicle.registrationNumber,
            driverName = dto.driver.name,
            operatingStatus = TransitOperatingStatus.fromBackend(dto.status),
            startedAtEpochMillis = startedAtEpoch,
            cachedAtEpochMillis = cachedAtMillis
        )
    }

    fun toTransitStop(
        dto: TransitTripLocationDto,
        fallbackId: String,
        sequence: Int,
        type: TransitStopType
    ): TransitStop {
        val stopId = dto.googlePlaceId
            ?: dto.serpApiDataId
            ?: fallbackId

        val name = when {
            !dto.name.isNullOrBlank() -> dto.name.trim()
            dto.formattedAddress.isNotBlank() -> dto.formattedAddress.substringBefore(",").trim()
            else -> "Transit Stop"
        }

        val lat = if (dto.coordinates.size >= 2) dto.coordinates[1] else 0.0
        val lng = if (dto.coordinates.size >= 2) dto.coordinates[0] else 0.0

        return TransitStop(
            id = stopId,
            name = name,
            formattedAddress = dto.formattedAddress,
            coordinates = LocationCoordinates(latitude = lat, longitude = lng),
            sequence = sequence,
            stopType = type
        )
    }

    fun toSchedule(route: TransitRoute): TransitSchedule {
        val note = if (route.operatingStatus == TransitOperatingStatus.ACTIVE) {
            "Active transit service operating now; static timetables not exposed by backend"
        } else {
            "Transit service currently inactive"
        }

        return TransitSchedule(
            routeId = route.id,
            status = route.operatingStatus,
            startedAtEpochMillis = route.startedAtEpochMillis,
            operatingNote = note
        )
    }

    private fun parseIsoTimestamp(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(iso)?.time ?: run {
                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fallbackFormat.parse(iso)?.time
            }
        } catch (_: Exception) {
            null
        }
    }
}
