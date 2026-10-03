package com.ishara.app.domain.model

import com.ishara.app.core.result.IshaaraError

/**
 * Operational lifecycle states for an Agency entity.
 */
enum class AgencyStatus {
    ACTIVE,
    INACTIVE
}

/**
 * Authoritative driver-agency membership status states.
 * Strictly aligned with backend AgencyMembershipStatus enum.
 */
enum class AgencyMembershipStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * Public discovery representation of an Agency.
 * Excludes internal administrative credentials, revenue, or tax identifiers.
 */
data class Agency(
    val id: String,
    val name: String,
    val businessName: String? = null,
    val city: String? = null,
    val state: String? = null,
    val contactPhoneMasked: String? = null,
    val contactEmail: String? = null,
    val status: AgencyStatus = AgencyStatus.ACTIVE,
    val createdAt: String? = null
)

/**
 * Sanitized view of a driver's agency affiliation record.
 * Aligns with backend CleanDriverMembershipResponse.
 */
data class DriverAgencyMembership(
    val id: String,
    val agencyId: String,
    val agencyName: String,
    val agencyCity: String? = null,
    val agencyState: String? = null,
    val agencyContactEmail: String? = null,
    val status: AgencyMembershipStatus,
    val requestedAt: String? = null,
    val respondedAt: String? = null,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Domain-level state machine representing the driver's relationship to an Agency.
 */
sealed class DriverMembershipState {
    /** Loading or initial check in progress */
    data object Loading : DriverMembershipState()

    /** Driver is not affiliated with any agency and has no pending request */
    data object NoMembership : DriverMembershipState()

    /** Driver has submitted a membership request that is pending agency owner review */
    data class Pending(val membership: DriverAgencyMembership) : DriverMembershipState()

    /** Driver is an approved, active member of the agency */
    data class Approved(val membership: DriverAgencyMembership) : DriverMembershipState()

    /** Driver's latest membership request was rejected by the agency */
    data class Rejected(val membership: DriverAgencyMembership) : DriverMembershipState()

    /** An error occurred while resolving membership state */
    data class Error(val error: IshaaraError) : DriverMembershipState()
}
