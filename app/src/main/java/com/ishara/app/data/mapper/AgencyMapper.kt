package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.CleanDriverMembershipResponseDto
import com.ishara.app.data.remote.dto.CleanPublicAgencyResponseDto
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.AgencyMembershipStatus
import com.ishara.app.domain.model.AgencyStatus
import com.ishara.app.domain.model.DriverAgencyMembership

/**
 * Mapper for converting Agency and Membership DTOs to domain models.
 */
object AgencyMapper {

    fun toDomain(dto: CleanPublicAgencyResponseDto): Agency {
        val mappedStatus = when (dto.status?.uppercase()) {
            "INACTIVE" -> AgencyStatus.INACTIVE
            else -> AgencyStatus.ACTIVE
        }

        return Agency(
            id = dto.id,
            name = dto.name,
            businessName = dto.businessName,
            city = dto.city,
            state = dto.state,
            contactPhoneMasked = dto.contactPhoneMasked,
            contactEmail = dto.contactEmail,
            status = mappedStatus,
            createdAt = dto.createdAt
        )
    }

    fun toDomain(dto: CleanDriverMembershipResponseDto): DriverAgencyMembership {
        val mappedStatus = when (dto.status.uppercase()) {
            "APPROVED" -> AgencyMembershipStatus.APPROVED
            "REJECTED" -> AgencyMembershipStatus.REJECTED
            else -> AgencyMembershipStatus.PENDING
        }

        return DriverAgencyMembership(
            id = dto.id,
            agencyId = dto.agencyId,
            agencyName = dto.agencyName,
            agencyCity = dto.agencyCity,
            agencyState = dto.agencyState,
            agencyContactEmail = dto.agencyContactEmail,
            status = mappedStatus,
            requestedAt = dto.requestedAt,
            respondedAt = dto.respondedAt,
            rejectionReason = dto.rejectionReason,
            notes = dto.notes,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
}
