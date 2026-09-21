package com.ishara.app.domain.model

enum class RideRequestStatus {
    PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED
}

enum class RideStatus {
    REQUESTED, CONFIRMED, DRIVER_ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
}

data class RideRequest(
    val id: String,
    val tripId: String,
    val passengerId: String,
    val pickup: TripLocation,
    val dropoff: TripLocation,
    val seatsRequested: Int,
    val status: RideRequestStatus,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class Ride(
    val id: String,
    val tripId: String,
    val passengerId: String,
    val driverId: String,
    val vehicleId: String,
    val pickup: TripLocation,
    val dropoff: TripLocation,
    val seats: Int,
    val farePaise: Int,
    val status: RideStatus,
    val createdAtMillis: Long = System.currentTimeMillis()
)
