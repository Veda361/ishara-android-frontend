package com.ishara.app.domain.model

data class TripMatch(

    val tripId: String,

    val pickupDistanceMeters: Int,

    val dropoffDistanceMeters: Int,

    val estimatedPickupTime: String,

    val availableSeats: Int,

    val fareEstimateMinor: Int

)