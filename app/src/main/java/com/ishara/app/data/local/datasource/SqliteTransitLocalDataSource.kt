package com.ishara.app.data.local.datasource

import android.content.ContentValues
import android.database.Cursor
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.data.local.db.TransitDatabaseHelper
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitCacheMetadata
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.model.TransitStop
import com.ishara.app.domain.model.TransitStopType
import com.ishara.app.domain.model.TransitVehicleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production SQLite implementation of TransitLocalDataSource.
 * Guarantees transactional atomicity, strict stop sequence ordering, and thread safety.
 */
class SqliteTransitLocalDataSource(
    private val dbHelper: TransitDatabaseHelper
) : TransitLocalDataSource {

    private val mutex = Mutex()
    private val routesFlow = MutableStateFlow<List<TransitRoute>>(emptyList())
    private var isInitialized = false

    override fun observeRoutes(): Flow<List<TransitRoute>> {
        if (!isInitialized) {
            routesFlow.value = queryAllRoutesInternal()
            isInitialized = true
        }
        return routesFlow.asStateFlow()
    }

    override suspend fun getRoutes(): List<TransitRoute> = mutex.withLock {
        val routes = queryAllRoutesInternal()
        routesFlow.value = routes
        routes
    }

    override suspend fun getRouteById(routeId: String): TransitRoute? = mutex.withLock {
        queryRouteByIdInternal(routeId)
    }

    override suspend fun saveRoutes(routes: List<TransitRoute>, refreshedAtMillis: Long) = mutex.withLock {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Clear existing cached transit routes & stops atomically
            db.delete(TransitDatabaseHelper.TABLE_ROUTES, null, null)
            db.delete(TransitDatabaseHelper.TABLE_STOPS, null, null)

            for (route in routes) {
                val routeValues = ContentValues().apply {
                    put(TransitDatabaseHelper.COL_ROUTE_ID, route.id)
                    put(TransitDatabaseHelper.COL_ROUTE_NAME, route.name)
                    put(TransitDatabaseHelper.COL_ROUTE_DISTANCE, route.distanceMeters)
                    put(TransitDatabaseHelper.COL_ROUTE_DURATION, route.durationSeconds)
                    put(TransitDatabaseHelper.COL_ROUTE_VEHICLE_TYPE, route.vehicleType.name)
                    put(TransitDatabaseHelper.COL_ROUTE_VEHICLE_PLATE, route.vehiclePlate)
                    put(TransitDatabaseHelper.COL_ROUTE_DRIVER_NAME, route.driverName)
                    put(TransitDatabaseHelper.COL_ROUTE_OPERATING_STATUS, route.operatingStatus.name)
                    put(TransitDatabaseHelper.COL_ROUTE_STARTED_AT, route.startedAtEpochMillis)
                    put(TransitDatabaseHelper.COL_ROUTE_CACHED_AT, refreshedAtMillis)
                    put(TransitDatabaseHelper.COL_ROUTE_GEOMETRY, serializeGeometry(route.geometry))
                }
                db.insert(TransitDatabaseHelper.TABLE_ROUTES, null, routeValues)

                // Insert ordered stops
                for (stop in route.stops) {
                    val stopValues = ContentValues().apply {
                        put(TransitDatabaseHelper.COL_STOP_ID, stop.id)
                        put(TransitDatabaseHelper.COL_STOP_ROUTE_ID, route.id)
                        put(TransitDatabaseHelper.COL_STOP_NAME, stop.name)
                        put(TransitDatabaseHelper.COL_STOP_ADDRESS, stop.formattedAddress)
                        put(TransitDatabaseHelper.COL_STOP_LAT, stop.coordinates.latitude)
                        put(TransitDatabaseHelper.COL_STOP_LNG, stop.coordinates.longitude)
                        put(TransitDatabaseHelper.COL_STOP_SEQUENCE, stop.sequence)
                        put(TransitDatabaseHelper.COL_STOP_TYPE, stop.stopType.name)
                    }
                    db.insert(TransitDatabaseHelper.TABLE_STOPS, null, stopValues)
                }
            }

            // Update cache metadata
            val metaValues = ContentValues().apply {
                put(TransitDatabaseHelper.COL_META_KEY, TransitDatabaseHelper.DEFAULT_META_KEY)
                put(TransitDatabaseHelper.COL_META_LAST_REFRESHED, refreshedAtMillis)
                put(TransitDatabaseHelper.COL_META_ITEM_COUNT, routes.size)
                put(TransitDatabaseHelper.COL_META_STATUS, "VALID")
            }
            db.insertWithOnConflict(
                TransitDatabaseHelper.TABLE_METADATA,
                null,
                metaValues,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        routesFlow.value = routes
        isInitialized = true
    }

    override suspend fun getCacheMetadata(): TransitCacheMetadata? = mutex.withLock {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TransitDatabaseHelper.TABLE_METADATA,
            null,
            "${TransitDatabaseHelper.COL_META_KEY} = ?",
            arrayOf(TransitDatabaseHelper.DEFAULT_META_KEY),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val lastRefreshed = it.getLong(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_META_LAST_REFRESHED))
                val count = it.getInt(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_META_ITEM_COUNT))
                TransitCacheMetadata(
                    lastRefreshedAtMillis = lastRefreshed,
                    itemCount = count,
                    freshness = TransitCacheFreshness.FRESH
                )
            } else null
        }
    }

    override suspend fun clear() = mutex.withLock {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TransitDatabaseHelper.TABLE_ROUTES, null, null)
            db.delete(TransitDatabaseHelper.TABLE_STOPS, null, null)
            db.delete(TransitDatabaseHelper.TABLE_METADATA, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        routesFlow.value = emptyList()
    }

    private fun queryAllRoutesInternal(): List<TransitRoute> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TransitDatabaseHelper.TABLE_ROUTES,
            null,
            null,
            null,
            null,
            null,
            "${TransitDatabaseHelper.COL_ROUTE_CACHED_AT} DESC"
        )
        val list = mutableListOf<TransitRoute>()
        cursor.use {
            while (it.moveToNext()) {
                val route = mapCursorToRoute(it, db)
                list.add(route)
            }
        }
        return list
    }

    private fun queryRouteByIdInternal(routeId: String): TransitRoute? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TransitDatabaseHelper.TABLE_ROUTES,
            null,
            "${TransitDatabaseHelper.COL_ROUTE_ID} = ?",
            arrayOf(routeId),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                mapCursorToRoute(it, db)
            } else null
        }
    }

    private fun mapCursorToRoute(cursor: Cursor, db: android.database.sqlite.SQLiteDatabase): TransitRoute {
        val routeId = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_NAME))
        val dist = if (cursor.isNull(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_DISTANCE))) {
            null
        } else {
            cursor.getDouble(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_DISTANCE))
        }
        val dur = if (cursor.isNull(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_DURATION))) {
            null
        } else {
            cursor.getLong(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_DURATION))
        }
        val vTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_VEHICLE_TYPE))
        val vPlate = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_VEHICLE_PLATE))
        val driver = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_DRIVER_NAME))
        val opStatusStr = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_OPERATING_STATUS))
        val startedAt = if (cursor.isNull(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_STARTED_AT))) {
            null
        } else {
            cursor.getLong(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_STARTED_AT))
        }
        val cachedAt = cursor.getLong(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_CACHED_AT))
        val geomStr = cursor.getString(cursor.getColumnIndexOrThrow(TransitDatabaseHelper.COL_ROUTE_GEOMETRY))

        // Load ordered stops
        val stops = queryStopsForRoute(db, routeId)
        val originStop = stops.find { it.stopType == TransitStopType.ORIGIN }
            ?: stops.firstOrNull()
            ?: TransitStop(
                id = "${routeId}_origin",
                name = "Origin Stop",
                formattedAddress = "Origin Address",
                coordinates = LocationCoordinates(0.0, 0.0),
                sequence = 0,
                stopType = TransitStopType.ORIGIN
            )

        val destStop = stops.find { it.stopType == TransitStopType.DESTINATION }
            ?: stops.lastOrNull()
            ?: TransitStop(
                id = "${routeId}_dest",
                name = "Destination Stop",
                formattedAddress = "Destination Address",
                coordinates = LocationCoordinates(0.0, 0.0),
                sequence = 1,
                stopType = TransitStopType.DESTINATION
            )

        return TransitRoute(
            id = routeId,
            name = name,
            originStop = originStop,
            destinationStop = destStop,
            stops = stops,
            geometry = deserializeGeometry(geomStr),
            distanceMeters = dist,
            durationSeconds = dur,
            vehicleType = try { TransitVehicleType.valueOf(vTypeStr) } catch (_: Exception) { TransitVehicleType.OTHER },
            vehiclePlate = vPlate,
            driverName = driver,
            operatingStatus = try { TransitOperatingStatus.valueOf(opStatusStr) } catch (_: Exception) { TransitOperatingStatus.INACTIVE },
            startedAtEpochMillis = startedAt,
            cachedAtEpochMillis = cachedAt
        )
    }

    private fun queryStopsForRoute(db: android.database.sqlite.SQLiteDatabase, routeId: String): List<TransitStop> {
        val cursor = db.query(
            TransitDatabaseHelper.TABLE_STOPS,
            null,
            "${TransitDatabaseHelper.COL_STOP_ROUTE_ID} = ?",
            arrayOf(routeId),
            null,
            null,
            "${TransitDatabaseHelper.COL_STOP_SEQUENCE} ASC"
        )
        val stops = mutableListOf<TransitStop>()
        cursor.use {
            while (it.moveToNext()) {
                val stopId = it.getString(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_ID))
                val name = it.getString(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_NAME))
                val address = it.getString(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_ADDRESS))
                val lat = it.getDouble(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_LAT))
                val lng = it.getDouble(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_LNG))
                val seq = it.getInt(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_SEQUENCE))
                val typeStr = it.getString(it.getColumnIndexOrThrow(TransitDatabaseHelper.COL_STOP_TYPE))
                val type = try { TransitStopType.valueOf(typeStr) } catch (_: Exception) { TransitStopType.INTERMEDIATE }

                stops.add(
                    TransitStop(
                        id = stopId,
                        name = name,
                        formattedAddress = address,
                        coordinates = LocationCoordinates(lat, lng),
                        sequence = seq,
                        stopType = type
                    )
                )
            }
        }
        return stops
    }

    private fun serializeGeometry(coords: List<LocationCoordinates>): String {
        return coords.joinToString(";") { "${it.latitude},${it.longitude}" }
    }

    private fun deserializeGeometry(serialized: String?): List<LocationCoordinates> {
        if (serialized.isNullOrBlank()) return emptyList()
        return serialized.split(";").mapNotNull { part ->
            val latLng = part.split(",")
            if (latLng.size >= 2) {
                val lat = latLng[0].toDoubleOrNull()
                val lng = latLng[1].toDoubleOrNull()
                if (lat != null && lng != null) LocationCoordinates(lat, lng) else null
            } else null
        }
    }
}
