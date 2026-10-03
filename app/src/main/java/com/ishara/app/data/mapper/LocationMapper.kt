package com.ishara.app.data.mapper

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.data.remote.dto.ResolvedLocationDto
import com.ishara.app.domain.model.SearchResultLocation

/**
 * Maps location API transfer objects to pure domain models.
 * Centralizes coordinate mapping and guards against raw vendor identifiers leaking into the UI.
 */
object LocationMapper {

    fun toDomain(dto: ResolvedLocationDto): SearchResultLocation {
        val placeName = when {
            !dto.displayName.isNullOrBlank() -> dto.displayName
            dto.formattedAddress.isNotBlank() -> dto.formattedAddress.substringBefore(',').trim()
            else -> "Location"
        }

        val id = dto.googlePlaceId
            ?: dto.serpApiDataId
            ?: "${dto.latitude}_${dto.longitude}"

        return SearchResultLocation(
            id = id,
            name = placeName,
            formattedAddress = dto.formattedAddress,
            city = dto.city,
            state = dto.state,
            country = dto.country,
            coordinates = LocationCoordinates(
                latitude = dto.latitude,
                longitude = dto.longitude
            )
        )
    }

    fun toDomainList(dtos: List<ResolvedLocationDto>): List<SearchResultLocation> {
        return dtos.map { toDomain(it) }
    }
}
