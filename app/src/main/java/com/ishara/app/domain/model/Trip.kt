package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate

enum class TripStatus {
    CREATED, ACTIVE, COMPLETED, CANCELLED
}

data class TripLocation(
    val address: String,
    val coordinate: GeoJsonCoordinate
)

data class Trip(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val origin: TripLocation,
    val destination: TripLocation,
    val waypoints: List<TripLocation> = emptyList(),
    val totalSeats: Int,
    val availableSeats: Int,
    val baseFarePaise: Int,
    val status: TripStatus,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
