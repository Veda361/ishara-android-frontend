package com.ishara.app.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite Database Helper for offline transit routes, stops, and cache metadata.
 * Uses strict transactional schema management and safe non-destructive migration.
 */
class TransitDatabaseHelper(
    context: Context,
    databaseName: String = DATABASE_NAME,
    version: Int = DATABASE_VERSION
) : SQLiteOpenHelper(context, databaseName, null, version) {

    companion object {
        const val DATABASE_NAME = "ishaara_transit.db"
        const val DATABASE_VERSION = 1

        // Tables
        const val TABLE_ROUTES = "cached_routes"
        const val TABLE_STOPS = "cached_stops"
        const val TABLE_METADATA = "transit_cache_metadata"

        // Routes Columns
        const val COL_ROUTE_ID = "route_id"
        const val COL_ROUTE_NAME = "name"
        const val COL_ROUTE_DISTANCE = "distance_meters"
        const val COL_ROUTE_DURATION = "duration_seconds"
        const val COL_ROUTE_VEHICLE_TYPE = "vehicle_type"
        const val COL_ROUTE_VEHICLE_PLATE = "vehicle_plate"
        const val COL_ROUTE_DRIVER_NAME = "driver_name"
        const val COL_ROUTE_OPERATING_STATUS = "operating_status"
        const val COL_ROUTE_STARTED_AT = "started_at_epoch"
        const val COL_ROUTE_CACHED_AT = "cached_at_epoch"
        const val COL_ROUTE_GEOMETRY = "geometry_geojson"

        // Stops Columns
        const val COL_STOP_ID = "stop_id"
        const val COL_STOP_ROUTE_ID = "route_id"
        const val COL_STOP_NAME = "name"
        const val COL_STOP_ADDRESS = "formatted_address"
        const val COL_STOP_LAT = "latitude"
        const val COL_STOP_LNG = "longitude"
        const val COL_STOP_SEQUENCE = "sequence"
        const val COL_STOP_TYPE = "stop_type"

        // Metadata Columns
        const val COL_META_KEY = "cache_key"
        const val COL_META_LAST_REFRESHED = "last_refreshed_at"
        const val COL_META_ITEM_COUNT = "item_count"
        const val COL_META_STATUS = "status"

        const val DEFAULT_META_KEY = "active_transit_routes"

        // SQL statements
        val SQL_CREATE_TABLE_ROUTES = """
            CREATE TABLE IF NOT EXISTS $TABLE_ROUTES (
                $COL_ROUTE_ID TEXT PRIMARY KEY,
                $COL_ROUTE_NAME TEXT NOT NULL,
                $COL_ROUTE_DISTANCE REAL,
                $COL_ROUTE_DURATION INTEGER,
                $COL_ROUTE_VEHICLE_TYPE TEXT NOT NULL,
                $COL_ROUTE_VEHICLE_PLATE TEXT NOT NULL,
                $COL_ROUTE_DRIVER_NAME TEXT NOT NULL,
                $COL_ROUTE_OPERATING_STATUS TEXT NOT NULL,
                $COL_ROUTE_STARTED_AT INTEGER,
                $COL_ROUTE_CACHED_AT INTEGER NOT NULL,
                $COL_ROUTE_GEOMETRY TEXT
            );
        """.trimIndent()

        val SQL_CREATE_TABLE_STOPS = """
            CREATE TABLE IF NOT EXISTS $TABLE_STOPS (
                $COL_STOP_ID TEXT NOT NULL,
                $COL_STOP_ROUTE_ID TEXT NOT NULL,
                $COL_STOP_NAME TEXT NOT NULL,
                $COL_STOP_ADDRESS TEXT NOT NULL,
                $COL_STOP_LAT REAL NOT NULL,
                $COL_STOP_LNG REAL NOT NULL,
                $COL_STOP_SEQUENCE INTEGER NOT NULL,
                $COL_STOP_TYPE TEXT NOT NULL,
                PRIMARY KEY ($COL_STOP_ROUTE_ID, $COL_STOP_SEQUENCE)
            );
        """.trimIndent()

        val SQL_CREATE_TABLE_METADATA = """
            CREATE TABLE IF NOT EXISTS $TABLE_METADATA (
                $COL_META_KEY TEXT PRIMARY KEY,
                $COL_META_LAST_REFRESHED INTEGER NOT NULL,
                $COL_META_ITEM_COUNT INTEGER NOT NULL,
                $COL_META_STATUS TEXT NOT NULL
            );
        """.trimIndent()

        val SQL_CREATE_INDEX_STOPS = """
            CREATE INDEX IF NOT EXISTS idx_cached_stops_route_id ON $TABLE_STOPS($COL_STOP_ROUTE_ID);
        """.trimIndent()

        val SQL_CREATE_INDEX_ROUTES_CACHED_AT = """
            CREATE INDEX IF NOT EXISTS idx_cached_routes_cached_at ON $TABLE_ROUTES($COL_ROUTE_CACHED_AT);
        """.trimIndent()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE_ROUTES)
        db.execSQL(SQL_CREATE_TABLE_STOPS)
        db.execSQL(SQL_CREATE_TABLE_METADATA)
        db.execSQL(SQL_CREATE_INDEX_STOPS)
        db.execSQL(SQL_CREATE_INDEX_ROUTES_CACHED_AT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        applyMigrations(db, oldVersion, newVersion)
    }

    /**
     * Non-destructive migration runner preserving existing cached data across schema upgrades.
     */
    fun applyMigrations(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var current = oldVersion
        if (current == 1 && newVersion >= 2) {
            // Future migration example: Add extra column without dropping tables
            // db.execSQL("ALTER TABLE $TABLE_ROUTES ADD COLUMN is_favorite INTEGER DEFAULT 0;")
            current = 2
        }
    }
}
