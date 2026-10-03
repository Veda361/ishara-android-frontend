package com.ishara.app

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.utils.TransitFormatters
import com.ishara.app.data.local.datasource.InMemoryTransitLocalDataSource
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitCachePolicy
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.model.TransitStop
import com.ishara.app.domain.model.TransitStopType
import com.ishara.app.domain.model.TransitVehicleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitCachePolicyAndLocalSourceTest {

    private val sampleStop1 = TransitStop(
        id = "stop_1",
        name = "Station Stop",
        formattedAddress = "Station Road",
        coordinates = LocationCoordinates(25.44, 78.58),
        sequence = 0,
        stopType = TransitStopType.ORIGIN
    )

    private val sampleStop2 = TransitStop(
        id = "stop_2",
        name = "Campus Stop",
        formattedAddress = "Campus Gate",
        coordinates = LocationCoordinates(25.46, 78.60),
        sequence = 1,
        stopType = TransitStopType.DESTINATION
    )

    private val sampleRoute = TransitRoute(
        id = "route_101",
        name = "Station ↔ Campus",
        originStop = sampleStop1,
        destinationStop = sampleStop2,
        stops = listOf(sampleStop1, sampleStop2),
        geometry = listOf(LocationCoordinates(25.44, 78.58), LocationCoordinates(25.46, 78.60)),
        distanceMeters = 3500.0,
        durationSeconds = 600L,
        vehicleType = TransitVehicleType.BUS,
        vehiclePlate = "UP93AT9999",
        driverName = "Suresh",
        operatingStatus = TransitOperatingStatus.ACTIVE,
        startedAtEpochMillis = 1000L,
        cachedAtEpochMillis = 2000L
    )

    @Test
    fun testInMemoryTransitLocalDataSourceOperations() = runBlocking {
        val localDataSource = InMemoryTransitLocalDataSource()

        // 1. Initial state empty
        val initial = localDataSource.getRoutes()
        assertTrue(initial.isEmpty())
        assertNull(localDataSource.getCacheMetadata())

        // 2. Save routes
        localDataSource.saveRoutes(listOf(sampleRoute), refreshedAtMillis = 5000L)

        // 3. Query all
        val savedList = localDataSource.getRoutes()
        assertEquals(1, savedList.size)
        assertEquals("route_101", savedList[0].id)

        // 4. Query by ID
        val single = localDataSource.getRouteById("route_101")
        assertNotNull(single)
        assertEquals("Station ↔ Campus", single?.name)

        val missing = localDataSource.getRouteById("unknown_id")
        assertNull(missing)

        // 5. Metadata
        val meta = localDataSource.getCacheMetadata()
        assertNotNull(meta)
        assertEquals(5000L, meta?.lastRefreshedAtMillis)
        assertEquals(1, meta?.itemCount)

        // 6. Observe Flow
        val flowValue = localDataSource.observeRoutes().first()
        assertEquals(1, flowValue.size)

        // 7. Clear
        localDataSource.clear()
        assertTrue(localDataSource.getRoutes().isEmpty())
        assertNull(localDataSource.getCacheMetadata())
    }

    @Test
    fun testTransitCachePolicyFreshnessEvaluation() {
        val policy = TransitCachePolicy(
            freshnessDurationMillis = 10 * 60 * 1000L, // 10 min
            staleDurationMillis = 24 * 60 * 60 * 1000L  // 24 hours
        )
        val now = 1_000_000_000L

        // 1. Unavailable when timestamp <= 0
        assertEquals(TransitCacheFreshness.UNAVAILABLE, policy.evaluateFreshness(0L, now))

        // 2. Fresh when age <= 10 min (e.g. 5 minutes ago)
        val fiveMinAgo = now - (5 * 60 * 1000L)
        assertEquals(TransitCacheFreshness.FRESH, policy.evaluateFreshness(fiveMinAgo, now))

        // 3. Stale when age > 10 min and <= 24 hours (e.g. 2 hours ago)
        val twoHoursAgo = now - (2 * 60 * 60 * 1000L)
        assertEquals(TransitCacheFreshness.STALE, policy.evaluateFreshness(twoHoursAgo, now))

        // 4. Unavailable when age > 24 hours (e.g. 25 hours ago)
        val twentyFiveHoursAgo = now - (25 * 60 * 60 * 1000L)
        assertEquals(TransitCacheFreshness.UNAVAILABLE, policy.evaluateFreshness(twentyFiveHoursAgo, now))
    }

    @Test
    fun testTransitFormatters() {
        val now = 100_000_000L

        // Relative time
        assertEquals("Updated just now", TransitFormatters.formatRelativeTime(now - 30_000L, now))
        assertEquals("Updated 15 min ago", TransitFormatters.formatRelativeTime(now - (15 * 60_000L), now))
        assertEquals("Updated 2 hours ago", TransitFormatters.formatRelativeTime(now - (2 * 3600_000L), now))
        assertEquals("Updated yesterday", TransitFormatters.formatRelativeTime(now - (24 * 3600_000L), now))
        assertEquals("Saved offline", TransitFormatters.formatRelativeTime(0L, now))

        // Distance formatting
        assertEquals("450 m", TransitFormatters.formatDistance(450.0))
        assertEquals("4.5 km", TransitFormatters.formatDistance(4500.0))
        assertNull(TransitFormatters.formatDistance(null))
        assertNull(TransitFormatters.formatDistance(0.0))

        // Duration formatting
        assertEquals("15 min", TransitFormatters.formatDuration(900L))
        assertEquals("1h", TransitFormatters.formatDuration(3600L))
        assertEquals("1h 15m", TransitFormatters.formatDuration(4500L))
        assertNull(TransitFormatters.formatDuration(null))
    }
}
