package com.ishara.app.data.remote.dto

/**
 * Strict request DTO for creating a voice trip draft via device-recognized transcript.
 * Matches backend `DeviceTranscriptInputSchema.strict()` exactly.
 */
data class CreateVoiceTripDraftRequestDto(
    val inputMode: String = "DEVICE_TRANSCRIPT",
    val transcript: String,
    val languageHint: String? = null
)

/**
 * Endpoint container for origin or destination queries and their resolved physical coordinates.
 */
data class VoiceTripDraftEndpointDto(
    val query: String,
    val resolved: ResolvedLocationDto
)

/**
 * Public response DTO matching backend `VoiceTripDraftResponse`.
 */
data class VoiceTripDraftResponseDto(
    val id: String,
    val driverId: String? = null,
    val inputMode: String,
    val originalTranscript: String,
    val normalizedTranscript: String,
    val intent: String,
    val origin: VoiceTripDraftEndpointDto,
    val destination: VoiceTripDraftEndpointDto,
    val status: String,
    val tripId: String? = null,
    val expiresAt: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Standard API envelope DTO for voice trip draft responses.
 */
data class VoiceTripDraftEnvelopeDto(
    val statusCode: Int? = null,
    val success: Boolean,
    val data: VoiceTripDraftResponseDto? = null,
    val message: String? = null
)
