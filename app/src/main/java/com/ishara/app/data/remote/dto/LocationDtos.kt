package com.ishara.app.data.remote.dto

/**
 * DTO matching backend ResolvedLocation from GET /api/v1/locations/search.
 */
data class ResolvedLocationDto(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String,
    val displayName: String? = null,
    val provider: String? = null,
    val googlePlaceId: String? = null,
    val serpApiDataId: String? = null,
    val serpApiDataCid: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null
)

/**
 * Standard API envelope response for location search.
 */
data class LocationSearchResponseDto(
    val success: Boolean,
    val data: List<ResolvedLocationDto> = emptyList(),
    val message: String? = null
)
