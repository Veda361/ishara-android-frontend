package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.DriverIdentityDto
import com.ishara.app.data.remote.dto.DriverOperationalContextResponseDto
import com.ishara.app.data.remote.dto.DriverProfileResponseDto
import com.ishara.app.data.remote.dto.DriverTripDto
import com.ishara.app.data.remote.dto.DriverTripLocationDto
import com.ishara.app.data.remote.dto.DriverVehicleDto
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverAssignedVehicle
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverTodayStats
import com.ishara.app.domain.model.DriverTripLocation
import com.ishara.app.domain.model.DriverTripStatus
import com.ishara.app.domain.model.DriverVerificationStatus

/**
 * Maps between Driver DTOs and pure domain models.
 * Strictly respects RFC 7946 GeoJSON [longitude, latitude] parsing.
 */
object DriverMapper {

    fun toDomain(dto: DriverOperationalContextResponseDto): DriverOperationalContext {
        return DriverOperationalContext(
            driver = toDomain(dto.driver),
            vehicle = dto.vehicle?.let { toDomain(it) },
            activeTrip = dto.activeTrip?.let { toDomain(it) },
            activeRidesCount = dto.activeRidesCount,
            todayStats = DriverTodayStats(
                completedRidesCount = dto.todayStats.completedRidesCount,
                isOnline = dto.todayStats.isOnline,
                currentDate = dto.todayStats.currentDate,
                timezone = dto.todayStats.timezone
            )
        )
    }

    fun toDomain(dto: DriverIdentityDto): DriverIdentity {
        return DriverIdentity(
            id = dto.id,
            userId = dto.userId,
            verificationStatus = mapVerificationStatus(dto.verificationStatus),
            status = mapDriverStatus(dto.status),
            licenseNumberMasked = dto.licenseNumberMasked,
            licenseVerifiedAt = dto.licenseVerifiedAt
        )
    }

    fun toDomain(dto: DriverProfileResponseDto): DriverIdentity {
        return DriverIdentity(
            id = dto.id,
            userId = dto.userId,
            verificationStatus = mapVerificationStatus(dto.verificationStatus),
            status = mapDriverStatus(dto.status),
            licenseNumberMasked = dto.licenseNumberMasked,
            licenseVerifiedAt = dto.licenseVerifiedAt
        )
    }

    fun toDomain(dto: com.ishara.app.data.remote.dto.CleanDriverProfileResponseDto): com.ishara.app.domain.model.DriverProfile {
        return com.ishara.app.domain.model.DriverProfile(
            id = dto.id,
            userId = dto.userId,
            verificationStatus = mapVerificationStatus(dto.verificationStatus),
            status = mapDriverStatus(dto.status),
            licenseNumberMasked = dto.licenseNumberMasked,
            licenseVerifiedAt = dto.licenseVerifiedAt,
            submittedAt = dto.submittedAt,
            reviewedAt = dto.reviewedAt,
            reviewedBy = dto.reviewedBy,
            rejectionReason = dto.rejectionReason,
            yearsOfExperience = dto.yearsOfExperience,
            emergencyContact = dto.emergencyContact?.let { toDomain(it) },
            operatingType = dto.operatingType.orEmpty(),
            isSuspended = dto.isSuspended,
            suspensionReason = dto.suspensionReason,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toDomain(dto: com.ishara.app.data.remote.dto.CleanDriverVerificationResponseDto): com.ishara.app.domain.model.DriverVerificationDetails {
        return com.ishara.app.domain.model.DriverVerificationDetails(
            driverId = dto.driverId,
            userId = dto.userId,
            verificationStatus = mapVerificationStatus(dto.verificationStatus),
            submittedAt = dto.submittedAt,
            reviewedAt = dto.reviewedAt,
            reviewedBy = dto.reviewedBy,
            rejectionReason = dto.rejectionReason,
            licenseNumberMasked = dto.licenseNumberMasked,
            operatingType = dto.operatingType.orEmpty(),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun toDomain(dto: com.ishara.app.data.remote.dto.CleanEmergencyContactDto): com.ishara.app.domain.model.DriverEmergencyContact {
        return com.ishara.app.domain.model.DriverEmergencyContact(
            name = dto.name,
            phoneNumber = dto.phoneNumberMasked ?: "",
            relationship = dto.relationship
        )
    }


    fun toDomain(dto: DriverVehicleDto): DriverAssignedVehicle {
        return DriverAssignedVehicle(
            id = dto.id,
            registrationNumber = dto.registrationNumber,
            vehicleType = dto.vehicleType,
            make = dto.make,
            model = dto.model,
            isActive = dto.isActive,
            isVerified = dto.isVerified
        )
    }

    fun toDomain(dto: DriverTripDto): DriverActiveTrip {
        return DriverActiveTrip(
            id = dto.id,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            origin = toDomain(dto.origin),
            destination = toDomain(dto.destination),
            distanceMeters = dto.route?.distanceMeters,
            durationSeconds = dto.route?.durationSeconds,
            status = mapTripStatus(dto.status),
            startedAt = dto.startedAt,
            completedAt = dto.completedAt,
            cancelledAt = dto.cancelledAt
        )
    }

    fun toDomain(dto: DriverTripLocationDto): DriverTripLocation {
        val coords = dto.coordinates.coordinates
        // Strictly RFC 7946: index 0 is Longitude, index 1 is Latitude
        val lon = if (coords.isNotEmpty()) coords[0] else 0.0
        val lat = if (coords.size > 1) coords[1] else 0.0
        return DriverTripLocation(
            name = dto.name,
            formattedAddress = dto.formattedAddress,
            latitude = lat,
            longitude = lon
        )
    }

    fun mapDriverStatus(status: String): DriverProfileStatus {
        return when (status.uppercase()) {
            "ONLINE" -> DriverProfileStatus.ONLINE
            "ON_RIDE" -> DriverProfileStatus.ON_RIDE
            else -> DriverProfileStatus.OFFLINE
        }
    }

    fun mapVerificationStatus(status: String): DriverVerificationStatus {
        return when (status.uppercase()) {
            "VERIFIED" -> DriverVerificationStatus.VERIFIED
            "REJECTED" -> DriverVerificationStatus.REJECTED
            else -> DriverVerificationStatus.PENDING
        }
    }

    fun mapTripStatus(status: String): DriverTripStatus {
        return when (status.uppercase()) {
            "SCHEDULED" -> DriverTripStatus.SCHEDULED
            "ASSIGNED" -> DriverTripStatus.ASSIGNED
            "READY" -> DriverTripStatus.READY
            "ACTIVE" -> DriverTripStatus.ACTIVE
            "COMPLETED" -> DriverTripStatus.COMPLETED
            "CANCELLED" -> DriverTripStatus.CANCELLED
            else -> DriverTripStatus.CREATED
        }
    }
}
