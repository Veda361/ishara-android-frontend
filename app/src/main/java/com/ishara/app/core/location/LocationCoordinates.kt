package com.ishara.app.core.location

/**
 * Domain-level representation of device/vehicle geographical position and movement telemetry.
 */
data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val headingDegrees: Float? = null,
    val speedMps: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    /**
     * Converts to backend-compliant GeoJSON Point coordinate [longitude, latitude].
     */
    fun toGeoJson(): GeoJsonCoordinate = GeoJsonCoordinate.fromLatLng(latitude, longitude)

    /**
     * Converts to GeoJSON DoubleArray [longitude, latitude].
     */
    fun toGeoJsonArray(): DoubleArray = doubleArrayOf(longitude, latitude)

    companion object {
        fun fromGeoJson(geoJson: GeoJsonCoordinate): LocationCoordinates = LocationCoordinates(
            latitude = geoJson.latitude,
            longitude = geoJson.longitude
        )
    }
}
