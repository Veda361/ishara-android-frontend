package com.ishara.app.data.repository

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.remote.datasource.DriverRemoteDataSource
import com.ishara.app.data.remote.dto.*
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.repository.DriverRepository

class DriverRepositoryImpl(
    private val remoteDataSource: DriverRemoteDataSource,
    private val sessionStore: SessionStore
) : DriverRepository {

    override suspend fun getDriverProfile(): IshaaraResult<DriverProfile> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.getDriverProfile(token).map { response ->
            val dto = response.data ?: throw IllegalStateException("Empty driver profile")
            dto.toDomain()
        }
    }

    override suspend fun setOnline(): IshaaraResult<Unit> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.setOnline(token).map { }
    }

    override suspend fun setOffline(): IshaaraResult<Unit> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.setOffline(token).map { }
    }

    override suspend fun updateLocation(
        location: LocationCoordinates
    ): IshaaraResult<Unit> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        val request = UpdateDriverLocationRequestDto(
            latitude = location.latitude,
            longitude = location.longitude,
            heading = location.headingDegrees?.toDouble(),
            speed = location.speedMps?.toDouble()
        )

        return remoteDataSource.updateLocation(token, request).map { }
    }

    private fun DriverProfileDto.toDomain(): DriverProfile = DriverProfile(
        id = id,
        userId = userId,
        licenseNumberMasked = licenseNumber,
        yearsOfExperience = yearsOfExperience,
        isOnline = status == "ONLINE",
        ratingAverage = 0.0, // Should be fetched from rating summary if needed
        totalRatingsCount = 0,
        emergencyContact = emergencyContact?.let {
            EmergencyContact(
                name = it.name,
                phoneNumber = it.phoneNumber,
                relationship = it.relationship
            )
        }
    )
}
