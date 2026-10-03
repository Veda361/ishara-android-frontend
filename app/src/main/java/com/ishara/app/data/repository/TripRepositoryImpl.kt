package com.ishara.app.data.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.DiscoveryMapper
import com.ishara.app.data.mapper.TripMapper
import com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSource
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.dto.DiscoverTripsRequestDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.DiscoveryResult
import com.ishara.app.domain.model.PassengerTripDetails
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.repository.TripRepository

class TripRepositoryImpl(
    private val remoteDataSource: TripRemoteDataSource,
    private val localDataSource: SessionLocalDataSource,
    private val discoveryRemoteDataSource: DiscoveryRemoteDataSource? = null
) : TripRepository {

    override suspend fun discoverTrips(query: DiscoveryQuery): IshaaraResult<DiscoveryResult> {
        val session = localDataSource.getSession()
        val requestDto = DiscoveryMapper.toRequestDto(query)
        val dataSource = discoveryRemoteDataSource
            ?: return IshaaraResult.failure(
                com.ishara.app.core.result.IshaaraError.Unknown(
                    cause = IllegalStateException("DiscoveryRemoteDataSource not configured")
                )
            )

        return dataSource.discoverTrips(requestDto, session?.token).map { responseDto ->
            DiscoveryMapper.toDomain(responseDto)
        }
    }

    override suspend fun getPassengerTripDetails(tripId: String): IshaaraResult<PassengerTripDetails> {
        val session = localDataSource.getSession()
        val dataSource = discoveryRemoteDataSource
            ?: return IshaaraResult.failure(
                com.ishara.app.core.result.IshaaraError.Unknown(
                    cause = IllegalStateException("DiscoveryRemoteDataSource not configured")
                )
            )

        return dataSource.getPassengerTripDetails(tripId, session?.token).map { responseDto ->
            DiscoveryMapper.toDomain(responseDto)
        }
    }


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
