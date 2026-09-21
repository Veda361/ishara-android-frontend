package com.ishara.app

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.location.LocationCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Validates GeoJSON [longitude, latitude] serialization to prevent coordinate inversion bugs.
 */
class GeoJsonCoordinateTest {

    @Test
    fun geoJson_storesLongitudeFirst_andLatitudeSecond() {
        val lat = 18.5204 // Pune, India
        val lng = 73.8567

        val geoJson = GeoJsonCoordinate.fromLatLng(latitude = lat, longitude = lng)

        // GeoJSON array must be [lng, lat]
        val array = geoJson.toArray()
        assertEquals(lng, array[0], 0.0001)
        assertEquals(lat, array[1], 0.0001)
    }

    @Test
    fun geoJson_fromArray_mapsCorrectlyToCoordinates() {
        val lng = 72.8777
        val lat = 19.0760 // Mumbai
        val array = doubleArrayOf(lng, lat)

        val geoJson = GeoJsonCoordinate.fromArray(array)
        assertEquals(lng, geoJson.longitude, 0.0001)
        assertEquals(lat, geoJson.latitude, 0.0001)

        val location = LocationCoordinates.fromGeoJson(geoJson)
        assertEquals(lat, location.latitude, 0.0001)
        assertEquals(lng, location.longitude, 0.0001)
    }

    @Test
    fun geoJson_rejectsInvalidLatitude() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoJsonCoordinate(longitude = 50.0, latitude = 95.0)
        }
    }

    @Test
    fun geoJson_rejectsInvalidLongitude() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoJsonCoordinate(longitude = 190.0, latitude = 20.0)
        }
    }
}
