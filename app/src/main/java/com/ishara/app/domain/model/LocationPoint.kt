package com.ishara.app.domain.model

import com.ishara.app.core.location.GeoJsonCoordinate

/**
 * Domain model representing a verified geographic point for origin/destination selection.
 * Strictly maps to backend coordinate pairs [longitude, latitude] and location metadata.
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val formattedAddress: String? = null
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
    }

    val coordinate: GeoJsonCoordinate
        get() = GeoJsonCoordinate(latitude = latitude, longitude = longitude)

    val displayName: String
        get() = name?.ifBlank { null } ?: formattedAddress?.ifBlank { null } ?: String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude)
}
