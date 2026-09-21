package com.ishara.app.data.remote.dto

data class TripLocationDto(
    val address: String,
    val coordinates: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TripLocationDto
        return address == other.address && coordinates.contentEquals(other.coordinates)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + coordinates.contentHashCode()
        return result
    }
}

data class DiscoverTripsRequestDto(
    val pickup: TripLocationDto,
    val destination: TripLocationDto,
    val passengerCount: Int = 1
)

data class TripDto(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val origin: TripLocationDto,
    val destination: TripLocationDto,
    val waypoints: List<TripLocationDto> = emptyList(),
    val totalSeats: Int,
    val availableSeats: Int,
    val baseFarePaise: Int,
    val status: String,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
