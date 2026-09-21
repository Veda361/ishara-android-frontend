package com.ishara.app.core.location

/**
 * GeoJSON Point coordinate representation strictly following RFC 7946:
 * Backend coordinate format is `[longitude, latitude]`.
 *
 * This wrapper guards against the coordinate inversion hazard between:
 * - Android / Google Maps: (latitude, longitude)
 * - Ishaara Backend: [longitude, latitude]
 */
data class GeoJsonCoordinate(
    val longitude: Double,
    val latitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees. Received: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees. Received: $longitude" }
    }

    /**
     * Converts to standard GeoJSON 2-element array [longitude, latitude].
     */
    fun toArray(): DoubleArray = doubleArrayOf(longitude, latitude)

    companion object {
        /**
         * Creates a GeoJsonCoordinate from a 2-element DoubleArray [longitude, latitude].
         */
        fun fromArray(array: DoubleArray): GeoJsonCoordinate {
            require(array.size >= 2) { "GeoJSON coordinate array must have at least 2 elements [lng, lat]" }
            return GeoJsonCoordinate(
                longitude = array[0],
                latitude = array[1]
            )
        }

        /**
         * Creates a GeoJsonCoordinate from standard Android (latitude, longitude).
         */
        fun fromLatLng(latitude: Double, longitude: Double): GeoJsonCoordinate {
            return GeoJsonCoordinate(
                longitude = longitude,
                latitude = latitude
            )
        }
    }
}
