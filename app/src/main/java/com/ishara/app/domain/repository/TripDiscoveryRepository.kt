package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RoutePolyline
import com.ishara.app.domain.model.TripMatch
import com.ishara.app.domain.model.DriverLocation

interface TripDiscoveryRepository {

    suspend fun discoverTrips(

        originLat: Double,

        originLng: Double,

        destinationLat: Double,

        destinationLng: Double

    ): IshaaraResult<List<TripMatch>>

    suspend fun getTripRoute(

        tripId: String

    ): IshaaraResult<RoutePolyline>

    suspend fun getDriverLocation(

        rideId: String

    ): IshaaraResult<DriverLocation>

}