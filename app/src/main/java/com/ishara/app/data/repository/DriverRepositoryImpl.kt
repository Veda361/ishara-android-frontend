package com.ishara.app.data.repository

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.remote.datasource.DriverRemoteDataSource
import com.ishara.app.data.remote.dto.DriverLocationUpdateDto
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.repository.DriverRepository

class DriverRepositoryImpl(
    private val remoteDataSource: DriverRemoteDataSource,
    private val localDataSource: SessionLocalDataSource
) : DriverRepository {

    override suspend fun getDriverProfile(): IshaaraResult<DriverProfile> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.getDriverProfile(session.token).map { dto ->
            DriverProfile(
                id = dto.id,
                userId = dto.userId,
                licenseNumberMasked = dto.licenseNumber,
                yearsOfExperience = dto.yearsOfExperience,
                isOnline = dto.isOnline,
                ratingAverage = dto.ratingAverage,
                totalRatingsCount = dto.totalRatingsCount
            )
        }
    }

    override suspend fun setOnline(): IshaaraResult<Unit> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication())
        return remoteDataSource.setOnline(session.token)
    }

    override suspend fun setOffline(): IshaaraResult<Unit> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication())
        return remoteDataSource.setOffline(session.token)
    }

    override suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        val dto = DriverLocationUpdateDto(
            coordinates = location.toGeoJsonArray(),
            heading = location.headingDegrees,
            speed = location.speedMps,
            accuracy = location.accuracyMeters
        )
        return remoteDataSource.updateLocation(dto, session.token)
    }
}
