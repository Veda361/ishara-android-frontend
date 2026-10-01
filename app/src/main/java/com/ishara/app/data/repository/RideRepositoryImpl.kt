package com.ishara.app.data.repository

import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.remote.datasource.RideRemoteDataSource
import com.ishara.app.data.remote.dto.*
import com.ishara.app.domain.model.*
import com.ishara.app.domain.repository.RideRepository

class RideRepositoryImpl(
    private val remoteDataSource: RideRemoteDataSource,
    private val sessionStore: SessionStore
) : RideRepository {

    override suspend fun createRideRequest(
        tripId: String,
        pickupAddress: String,
        dropoffAddress: String,
        seatsRequested: Int
    ): IshaaraResult<RideRequest> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        val request = CreateRideRequestDto(
            tripId = tripId,
            discoverySessionId = "manual_request", // Backend expects a session ID
            pickup = TripLocationDto(name = pickupAddress, coordinates = listOf(0.0, 0.0)),
            destination = TripLocationDto(name = dropoffAddress, coordinates = listOf(0.0, 0.0)),
            seatsRequested = seatsRequested
        )

        return remoteDataSource.createRideRequest(token, request).map { response ->
            response.data?.toDomain() ?: throw IllegalStateException("Empty ride request data")
        }
    }

    override suspend fun getActiveRide(rideId: String): IshaaraResult<Ride> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.getRide(token, rideId).map { response ->
            response.data?.toDomain() ?: throw IllegalStateException("Empty ride data")
        }
    }

    override suspend fun cancelRide(rideId: String, reason: String?): IshaaraResult<Unit> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.cancelRide(token, rideId, reason ?: "User cancelled").map { }
    }

    private fun RideRequestDto.toDomain(): RideRequest = RideRequest(
        id = id,
        tripId = tripId,
        passengerId = passengerId,
        pickup = TripLocation(pickup.name, GeoJsonCoordinate.fromArray(pickup.coordinates.toDoubleArray())),
        dropoff = TripLocation(destination.name, GeoJsonCoordinate.fromArray(destination.coordinates.toDoubleArray())),
        seatsRequested = seatsRequested,
        status = try {
            RideRequestStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            RideRequestStatus.PENDING
        },
        createdAtMillis = 0 // Parse timestamp if needed
    )

    private fun RideDto.toDomain(): Ride = Ride(
        id = id,
        tripId = tripId,
        passengerId = passengerId,
        driverId = driverId,
        vehicleId = vehicleId,
        pickup = TripLocation(pickup.name, GeoJsonCoordinate.fromArray(pickup.coordinates.toDoubleArray())),
        dropoff = TripLocation(destination.name, GeoJsonCoordinate.fromArray(destination.coordinates.toDoubleArray())),
        seats = seatsBooked,
        farePaise = fareSnapshot?.totalMinor ?: 0,
        status = try {
            RideStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            RideStatus.REQUESTED
        },
        createdAtMillis = 0
    )
}
