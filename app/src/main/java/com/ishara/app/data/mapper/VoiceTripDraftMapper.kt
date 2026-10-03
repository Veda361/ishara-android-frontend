package com.ishara.app.data.mapper

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.data.remote.dto.VoiceTripDraftEndpointDto
import com.ishara.app.data.remote.dto.VoiceTripDraftResponseDto
import com.ishara.app.domain.model.VoiceDraftEndpoint
import com.ishara.app.domain.model.VoiceTripDraft
import com.ishara.app.domain.model.VoiceTripDraftStatus

/**
 * Maps Voice Trip Draft DTOs to pure domain entities.
 */
object VoiceTripDraftMapper {

    fun toDomain(dto: VoiceTripDraftResponseDto): VoiceTripDraft {
        return VoiceTripDraft(
            id = dto.id,
            originalTranscript = dto.originalTranscript,
            normalizedTranscript = dto.normalizedTranscript,
            intent = dto.intent,
            origin = toEndpointDomain(dto.origin),
            destination = toEndpointDomain(dto.destination),
            status = mapStatus(dto.status),
            expiresAt = dto.expiresAt
        )
    }

    private fun toEndpointDomain(dto: VoiceTripDraftEndpointDto): VoiceDraftEndpoint {
        val resolved = dto.resolved
        val displayName = when {
            !resolved.displayName.isNullOrBlank() -> resolved.displayName
            resolved.formattedAddress.isNotBlank() -> resolved.formattedAddress.substringBefore(',').trim()
            dto.query.isNotBlank() -> dto.query
            else -> "Location"
        }

        return VoiceDraftEndpoint(
            query = dto.query,
            displayName = displayName,
            formattedAddress = resolved.formattedAddress,
            coordinates = LocationCoordinates(
                latitude = resolved.latitude,
                longitude = resolved.longitude
            )
        )
    }

    private fun mapStatus(rawStatus: String): VoiceTripDraftStatus {
        return when (rawStatus.uppercase()) {
            "CREATED" -> VoiceTripDraftStatus.CREATED
            "CONFIRMED" -> VoiceTripDraftStatus.CONFIRMED
            "CANCELLED" -> VoiceTripDraftStatus.CANCELLED
            "EXPIRED" -> VoiceTripDraftStatus.EXPIRED
            else -> VoiceTripDraftStatus.UNKNOWN
        }
    }
}
