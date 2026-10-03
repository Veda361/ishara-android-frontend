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

    /**
     * Serializes timestamp to ISO-8601 UTC string (YYYY-MM-DDTHH:mm:ss.SSSZ).
     */
    fun toIso8601Utc(): String = formatIso8601Utc(timestampMillis)

    /**
     * Validates that coordinates fall within physical bounds and are not NaN/Infinite/Null Island.
     */
    fun isValid(allowNullIsland: Boolean = false): Boolean {
        if (latitude.isNaN() || latitude.isInfinite() || longitude.isNaN() || longitude.isInfinite()) {
            return false
        }
        if (latitude < -90.0 || latitude > 90.0) return false
        if (longitude < -180.0 || longitude > 180.0) return false
        if (!allowNullIsland && latitude == 0.0 && longitude == 0.0) return false
        return true
    }

    companion object {
        fun fromGeoJson(geoJson: GeoJsonCoordinate): LocationCoordinates = LocationCoordinates(
            latitude = geoJson.latitude,
            longitude = geoJson.longitude
        )

        fun formatIso8601Utc(millis: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return sdf.format(java.util.Date(millis))
        }
    }
}
