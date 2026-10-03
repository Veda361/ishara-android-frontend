package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.location.LocationCoordinates

/**
 * Domain representation of a resolved geographic location from search or geocoding.
 * Never leaks raw backend DTOs, database identifiers, or external vendor metadata into UI.
 */
data class SearchResultLocation(
    val id: String,
    val name: String,
    val formattedAddress: String,
    val city: String?,
    val state: String?,
    val country: String?,
    val coordinates: LocationCoordinates
) {
    /**
     * Converts domain coordinates into RFC 7946 GeoJSON [longitude, latitude] format.
     */
    fun toGeoJson(): GeoJsonCoordinate = coordinates.toGeoJson()
}

/**
 * Domain representation of a selected student trip destination.
 */
data class StudentDestination(
    val name: String,
    val formattedAddress: String,
    val coordinates: LocationCoordinates,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Human-readable representation of device current location.
 * Guards against raw latitude/longitude exposure in user-facing UI.
 */
data class CurrentLocationDisplay(
    val title: String,
    val subtitle: String,
    val coordinates: LocationCoordinates
)

/**
 * Explicit domain permission states for location hardware access.
 * Handles the complete lifecycle of runtime permissions and hardware availability.
 */
enum class LocationPermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    SERVICE_DISABLED,
    REQUEST_IN_PROGRESS,
    UNAVAILABLE,
    STALE
}
