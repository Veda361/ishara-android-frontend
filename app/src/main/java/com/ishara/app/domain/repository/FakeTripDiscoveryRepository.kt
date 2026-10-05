package com.ishara.app.domain.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverLocation
import com.ishara.app.domain.model.RoutePolyline
import com.ishara.app.domain.model.TripMatch
import com.ishara.app.domain.repository.TripDiscoveryRepository

class FakeTripDiscoveryRepository : TripDiscoveryRepository {

    override suspend fun discoverTrips(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ): IshaaraResult<List<TripMatch>> {

        return IshaaraResult.success(

            listOf(

                TripMatch(

                    tripId = "trip_001",

                    pickupDistanceMeters = 150,

                    dropoffDistanceMeters = 220,

                    estimatedPickupTime = "4 min",

                    availableSeats = 2,

                    fareEstimateMinor = 2000

                )

            )

        )

    }

    override suspend fun getTripRoute(
        tripId: String
    ): IshaaraResult<RoutePolyline> {

        return IshaaraResult.success(

            RoutePolyline(emptyList())

        )

    }

    override suspend fun getDriverLocation(
        rideId: String
    ): IshaaraResult<DriverLocation> {

        return IshaaraResult.success(

            DriverLocation(

                coordinate = GeoJsonCoordinate(
                    longitude = 73.8567,
                    latitude = 18.5204
                ),

                heading = 90.0,

                speed = 12.0,

                accuracy = 5.0,

                recordedAt = ""

            )

        )

    }

}