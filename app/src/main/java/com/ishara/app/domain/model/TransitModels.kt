package com.ishara.app.domain.model

import com.ishara.app.core.location.LocationCoordinates

/**
 * Categorization of transit vehicles operating on the mobility network.
 */
enum class TransitVehicleType {
    BUS,
    AUTO,
    CAB,
    OTHER;

    companion object {
        fun fromBackend(value: String?): TransitVehicleType {
            return when (value?.uppercase()) {
                "BUS" -> BUS
                "AUTO" -> AUTO
                "CAB" -> CAB
                else -> OTHER
            }
        }
    }
}

/**
 * Operating status of a transit route or active trip service.
 */
enum class TransitOperatingStatus {
    ACTIVE,
    SCHEDULED,
    COMPLETED,
    INACTIVE;

    companion object {
        fun fromBackend(value: String?): TransitOperatingStatus {
            return when (value?.uppercase()) {
                "ACTIVE" -> ACTIVE
                "CREATED" -> SCHEDULED
                "COMPLETED" -> COMPLETED
                else -> INACTIVE
            }
        }
    }
}

/**
 * Stop sequence role along a transit corridor.
 */
enum class TransitStopType {
    ORIGIN,
    DESTINATION,
    INTERMEDIATE
}

/**
 * Physical boarding / alighting transit stop.
 */
data class TransitStop(
    val id: String,
    val name: String,
    val formattedAddress: String,
    val coordinates: LocationCoordinates,
    val sequence: Int,
    val stopType: TransitStopType
)

/**
 * High-level transit route or active corridor service.
 */
data class TransitRoute(
    val id: String,
    val name: String,
    val originStop: TransitStop,
    val destinationStop: TransitStop,
    val stops: List<TransitStop>,
    val geometry: List<LocationCoordinates>,
    val distanceMeters: Double?,
    val durationSeconds: Long?,
    val vehicleType: TransitVehicleType,
    val vehiclePlate: String,
    val driverName: String,
    val operatingStatus: TransitOperatingStatus,
    val startedAtEpochMillis: Long?,
    val cachedAtEpochMillis: Long
)

/**
 * Operating schedule or timetable note for a transit service.
 */
data class TransitSchedule(
    val routeId: String,
    val status: TransitOperatingStatus,
    val startedAtEpochMillis: Long?,
    val operatingNote: String
)

/**
 * Freshness status of cached transit resources.
 */
enum class TransitCacheFreshness {
    FRESH,
    STALE,
    UNAVAILABLE
}

/**
 * Metadata capturing the state and age of local transit cache.
 */
data class TransitCacheMetadata(
    val lastRefreshedAtMillis: Long,
    val itemCount: Int,
    val freshness: TransitCacheFreshness
)

/**
 * Centralized cache policy governing transit information validity and staleness.
 */
data class TransitCachePolicy(
    val freshnessDurationMillis: Long = 10 * 60 * 1000L,       // 10 minutes: fresh
    val staleDurationMillis: Long = 24 * 60 * 60 * 1000L,        // 24 hours: stale but readable
    val maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L            // 7 days: expired
) {
    /**
     * Evaluates freshness classification against current time.
     */
    fun evaluateFreshness(
        lastRefreshedAtMillis: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): TransitCacheFreshness {
        if (lastRefreshedAtMillis <= 0L) return TransitCacheFreshness.UNAVAILABLE
        val age = currentTimeMillis - lastRefreshedAtMillis
        if (age < 0) return TransitCacheFreshness.FRESH
        return when {
            age <= freshnessDurationMillis -> TransitCacheFreshness.FRESH
            age <= staleDurationMillis -> TransitCacheFreshness.STALE
            else -> TransitCacheFreshness.UNAVAILABLE
        }
    }
}
