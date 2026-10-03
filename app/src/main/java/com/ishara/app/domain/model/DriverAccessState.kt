package com.ishara.app.domain.model

/**
 * Authoritative domain access control state model separating application access
 * from operational capability access.
 *
 * Architectural Rule:
 * - App Access: Authenticated driver with a profile is always granted app access (including
 *   when verificationStatus == UNDER_REVIEW / PENDING or REJECTED).
 * - Operational Access: Only granted when platform verification is VERIFIED, account is not suspended,
 *   and agency membership requirements are fulfilled.
 */
data class DriverAccessState(
    val appAccessible: Boolean = true,
    val platformVerified: Boolean = false,
    val verificationStatus: DriverVerificationStatus = DriverVerificationStatus.PENDING,
    val membershipActive: Boolean = false,
    val membershipStatus: AgencyMembershipStatus? = null,
    val agencyName: String? = null,
    val vehicleAssigned: Boolean = false,
    val operationalAccess: Boolean = false,
    val operationalLockReason: String? = null
) {
    companion object {
        fun resolve(
            verificationStatus: DriverVerificationStatus,
            isSuspended: Boolean = false,
            operatingType: String = "",
            membershipState: DriverMembershipState? = null,
            vehicle: DriverAssignedVehicle? = null
        ): DriverAccessState {
            val isVerified = verificationStatus == DriverVerificationStatus.VERIFIED && !isSuspended
            val isAgency = operatingType.equals("AGENCY", ignoreCase = true)

            val isMembershipActive = when {
                !isAgency -> true // Individual drivers do not require agency affiliation
                membershipState is DriverMembershipState.Approved -> true
                else -> false
            }

            val approvedAgency = (membershipState as? DriverMembershipState.Approved)?.membership
            val pendingAgency = (membershipState as? DriverMembershipState.Pending)?.membership

            val hasVehicle = vehicle != null

            // Operational access requires platform verification AND valid membership
            val canOperate = isVerified && isMembershipActive

            val lockReason = when {
                isSuspended -> "Account administratively suspended. Contact platform support."
                verificationStatus == DriverVerificationStatus.REJECTED -> "Platform verification was not approved. Please review feedback and resubmit."
                !isVerified -> "Operational features are locked until platform verification is complete."
                !isMembershipActive -> "Active fleet affiliation is required to receive vehicle and route assignments."
                else -> null
            }

            return DriverAccessState(
                appAccessible = true,
                platformVerified = isVerified,
                verificationStatus = verificationStatus,
                membershipActive = isMembershipActive,
                membershipStatus = approvedAgency?.status ?: pendingAgency?.status,
                agencyName = approvedAgency?.agencyName ?: pendingAgency?.agencyName,
                vehicleAssigned = hasVehicle,
                operationalAccess = canOperate,
                operationalLockReason = lockReason
            )
        }
    }
}
