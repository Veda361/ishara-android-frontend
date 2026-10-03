package com.ishara.app.data.remote.dto

/**
 * Public discovery DTO representing an Agency.
 */
data class CleanPublicAgencyResponseDto(
    val id: String,
    val name: String,
    val businessName: String? = null,
    val city: String? = null,
    val state: String? = null,
    val contactPhoneMasked: String? = null,
    val contactEmail: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)

/**
 * DTO representing driver membership affiliation details.
 */
data class CleanDriverMembershipResponseDto(
    val id: String,
    val agencyId: String,
    val agencyName: String,
    val agencyCity: String? = null,
    val agencyState: String? = null,
    val agencyContactEmail: String? = null,
    val status: String,
    val requestedAt: String? = null,
    val respondedAt: String? = null,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Request payload for creating a driver membership affiliation.
 */
data class RequestAgencyMembershipRequestDto(
    val agencyId: String,
    val notes: String? = null
)

/**
 * Response payload for membership cancellation.
 */
data class CancelMembershipResponseDto(
    val message: String,
    val cancelledMembershipId: String
)
