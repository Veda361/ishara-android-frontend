package com.ishara.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PointDto(
    val type: String = "Point",
    val coordinates: List<Double>
)

@Serializable
data class TripLocationDto(
    val name: String,
    val coordinates: List<Double>,
    val id: String? = null,
    val address: String? = null
)

@Serializable
data class DiscoverTripsRequestDto(
    val origin: TripLocationDto,
    val destination: TripLocationDto
)

@Serializable
data class DiscoveryMatchDto(
    val tripId: String,
    val pickupDistanceMeters: Int,
    val dropoffDistanceMeters: Int,
    val estimatedPickupTime: String,
    val availableSeats: Int,
    val fareEstimateMinor: Int
)

@Serializable
data class DiscoveryResponseDto(
    val discoverySessionId: String,
    val matches: List<DiscoveryMatchDto>
)

@Serializable
data class CreateTripRequestDto(
    val origin: TripLocationDto,
    val destination: TripLocationDto,
    val scheduledStartTime: String,
    val vehicleId: String
)

@Serializable
data class TripDto(
    val id: String,
    val origin: TripLocationDto,
    val destination: TripLocationDto,
    val waypoints: List<TripLocationDto> = emptyList(),
    val scheduledStartTime: String,
    val vehicleId: String,
    val status: String,
    val driverId: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val totalSeats: Int = 0,
    val availableSeats: Int = 0,
    val baseFarePaise: Int = 0
)
