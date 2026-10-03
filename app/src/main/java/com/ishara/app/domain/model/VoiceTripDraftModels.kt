package com.ishara.app.domain.model

import com.ishara.app.core.location.LocationCoordinates

/**
 * Domain lifecycle status of a Voice Trip Draft.
 */
enum class VoiceTripDraftStatus {
    CREATED,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    UNKNOWN
}

/**
 * Verified physical endpoint of an interpreted voice trip draft.
 */
data class VoiceDraftEndpoint(
    val query: String,
    val displayName: String,
    val formattedAddress: String,
    val coordinates: LocationCoordinates
)

/**
 * Pure domain representation of a verified Voice Trip Draft.
 * Independent of transport protocols, JSON parsers, and Android SDK classes.
 */
data class VoiceTripDraft(
    val id: String,
    val originalTranscript: String,
    val normalizedTranscript: String,
    val intent: String,
    val origin: VoiceDraftEndpoint,
    val destination: VoiceDraftEndpoint,
    val status: VoiceTripDraftStatus,
    val expiresAt: String
) {
    /**
     * Converts this verified voice trip draft directly into a [DiscoveryQuery]
     * for seamless handoff to Phase 06 Trip Discovery.
     */
    fun toDiscoveryQuery(): DiscoveryQuery {
        return DiscoveryQuery(
            originLatitude = origin.coordinates.latitude,
            originLongitude = origin.coordinates.longitude,
            originName = origin.displayName,
            originAddress = origin.formattedAddress,
            destinationLatitude = destination.coordinates.latitude,
            destinationLongitude = destination.coordinates.longitude,
            destinationName = destination.displayName,
            destinationAddress = destination.formattedAddress,
            maxPickupDistanceMeters = DiscoveryQuery.DEFAULT_PICKUP_RADIUS_METERS,
            maxDestinationDeviationMeters = DiscoveryQuery.DEFAULT_DESTINATION_RADIUS_METERS
        )
    }

    /**
     * Converts the destination into a [StudentDestination] for home screen persistence.
     */
    fun toStudentDestination(): StudentDestination {
        return StudentDestination(
            name = destination.displayName,
            formattedAddress = destination.formattedAddress,
            coordinates = destination.coordinates
        )
    }
}
