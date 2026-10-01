package com.ishara.app.ui.screen.passenger.discover

import com.ishara.app.core.location.GeoJsonCoordinate

sealed class DiscoverEvent {
    data class SearchTrips(
        val origin: GeoJsonCoordinate,
        val destination: GeoJsonCoordinate,
        val passengerCount: Int = 1
    ) : DiscoverEvent()
    data object Refresh : DiscoverEvent()
}
