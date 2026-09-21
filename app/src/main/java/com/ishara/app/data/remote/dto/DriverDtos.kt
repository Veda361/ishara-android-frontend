package com.ishara.app.data.remote.dto

data class DriverLocationUpdateDto(
    val coordinates: DoubleArray,
    val heading: Float? = null,
    val speed: Float? = null,
    val accuracy: Float? = null,
    val recordedAt: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DriverLocationUpdateDto
        return coordinates.contentEquals(other.coordinates)
    }

    override fun hashCode(): Int {
        return coordinates.contentHashCode()
    }
}

data class DriverProfileDto(
    val id: String,
    val userId: String,
    val licenseNumber: String,
    val yearsOfExperience: Int,
    val isOnline: Boolean,
    val ratingAverage: Double,
    val totalRatingsCount: Int
)
