package com.ishara.app.data.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.TripMapper
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.dto.DiscoverTripsRequestDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripLocation
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.repository.TripRepository

class TripRepositoryImpl(
    private val remoteDataSource: TripRemoteDataSource,
    private val sessionStore: SessionStore
) : TripRepository {

    override suspend fun discoverTrips(
        pickup: GeoJsonCoordinate,
        destination: GeoJsonCoordinate,
        passengerCount: Int
    ): IshaaraResult<List<Trip>> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        val request = DiscoverTripsRequestDto(
            origin = TripLocationDto(
                name = "Pickup",
                coordinates = pickup.toArray().toList()
            ),
            destination = TripLocationDto(
                name = "Destination",
                coordinates = destination.toArray().toList()
            )
        )

        return remoteDataSource.discoverTrips(request, token).map { response ->
            response.data?.matches?.map { match ->
                Trip(
                    id = match.tripId,
                    driverId = "",
                    vehicleId = "",
                    origin = TripLocation("Pickup", pickup),
                    destination = TripLocation("Destination", destination),
                    totalSeats = 0,
                    availableSeats = match.availableSeats,
                    baseFarePaise = match.fareEstimateMinor,
                    status = TripStatus.ACTIVE
                )
            } ?: emptyList()
        }
    }

    override suspend fun getTripById(tripId: String): IshaaraResult<Trip> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.getTripById(tripId, token).map { response ->
            val dto = response.data ?: throw IllegalStateException("Empty trip data")
            TripMapper.toDomain(dto)
        }
    }

    override suspend fun startTrip(tripId: String): IshaaraResult<Trip> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.startTrip(tripId, token).flatMap {
            getTripById(tripId)
        }
    }

    override suspend fun completeTrip(tripId: String): IshaaraResult<Trip> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.completeTrip(tripId, token).flatMap {
            getTripById(tripId)
        }
    }
}
