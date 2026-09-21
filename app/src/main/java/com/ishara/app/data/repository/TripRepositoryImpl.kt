package com.ishara.app.data.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.TripMapper
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.dto.DiscoverTripsRequestDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.repository.TripRepository

class TripRepositoryImpl(
    private val remoteDataSource: TripRemoteDataSource,
    private val localDataSource: SessionLocalDataSource
) : TripRepository {

    override suspend fun discoverTrips(
        pickup: GeoJsonCoordinate,
        destination: GeoJsonCoordinate,
        passengerCount: Int
    ): IshaaraResult<List<Trip>> {
        val session = localDataSource.getSession()
        val request = DiscoverTripsRequestDto(
            pickup = TripLocationDto(address = "Pickup", coordinates = pickup.toArray()),
            destination = TripLocationDto(address = "Destination", coordinates = destination.toArray()),
            passengerCount = passengerCount
        )

        return remoteDataSource.discoverTrips(request, session?.token).map { dtos ->
            dtos.map { TripMapper.toDomain(it) }
        }
    }

    override suspend fun getTripById(tripId: String): IshaaraResult<Trip> {
        val session = localDataSource.getSession()
        return remoteDataSource.getTripById(tripId, session?.token).map { TripMapper.toDomain(it) }
    }

    override suspend fun startTrip(tripId: String): IshaaraResult<Trip> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Authentication())
        return remoteDataSource.startTrip(tripId, session.token).map { TripMapper.toDomain(it) }
    }

    override suspend fun completeTrip(tripId: String): IshaaraResult<Trip> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Authentication())
        return remoteDataSource.completeTrip(tripId, session.token).map { TripMapper.toDomain(it) }
    }
}
