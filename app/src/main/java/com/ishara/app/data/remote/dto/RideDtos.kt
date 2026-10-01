package com.ishara.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RideRequestDto(
    val id: String,
    val tripId: String,
    val passengerId: String,
    val pickup: TripLocationDto,
    val destination: TripLocationDto,
    val seatsRequested: Int,
    val status: String,
    val createdAt: String
)

@Serializable
data class CreateRideRequestDto(
    val tripId: String,
    val discoverySessionId: String,
    val pickup: TripLocationDto,
    val destination: TripLocationDto,
    val seatsRequested: Int
)

@Serializable
data class RideDto(
    val id: String,
    val tripId: String,
    val passengerId: String,
    val driverId: String,
    val vehicleId: String,
    val status: String,
    val pickup: TripLocationDto,
    val destination: TripLocationDto,
    val seatsBooked: Int,
    val fareSnapshot: FareSnapshotDto? = null,
    val createdAt: String,
    val completedAt: String? = null
)

@Serializable
data class FareSnapshotDto(
    val baseFareMinor: Int,
    val distanceFareMinor: Int,
    val serviceFeeMinor: Int,
    val totalMinor: Int,
    val currency: String,
    val snapshotAt: String
)

@Serializable
data class FareBreakdownDto(
    val rideId: String,
    val status: String,
    val currency: String,
    val currentFareMinor: Int,
    val isFinal: Boolean,
    val fareEstimate: FareEstimateDto? = null,
    val fareSnapshot: FareSnapshotDto? = null
)

@Serializable
data class FareEstimateDto(
    val baseFareMinor: Int,
    val distanceFareMinor: Int,
    val totalMinor: Int,
    val currency: String
)

@Serializable
data class RideTrackingDto(
    val rideId: String,
    val status: String,
    val currentPosition: PointDto? = null,
    val remainingDistanceMeters: Int? = null,
    val remainingSeconds: Int? = null,
    val lastUpdated: String? = null
)

@Serializable
data class PaymentOrderRequestDto(
    val idempotencyKey: String? = null
)

@Serializable
data class PaymentRecordDto(
    val id: String,
    val rideId: String,
    val userId: String,
    val driverId: String,
    val grossAmountMinor: Int,
    val platformFeeMinor: Int,
    val providerAmountMinor: Int,
    val currency: String,
    val status: String,
    val provider: String,
    val providerOrderId: String,
    val expiresAt: String
)

@Serializable
data class PaymentVerificationRequestDto(
    val providerOrderId: String,
    val providerPaymentId: String,
    val signature: String
)
