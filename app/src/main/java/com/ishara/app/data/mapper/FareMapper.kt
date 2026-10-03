package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.FareEstimateDto
import com.ishara.app.data.remote.dto.FareSnapshotDto
import com.ishara.app.data.remote.dto.RideFareResponseDto
import com.ishara.app.domain.model.FareEstimate
import com.ishara.app.domain.model.FareSnapshot
import com.ishara.app.domain.model.Money
import com.ishara.app.domain.model.RideFare

/**
 * Mapper transforming backend wire DTOs into domain fare entities.
 *
 * Guarantees:
 * 1. Strictly preserves backend's authoritative totals.
 * 2. Enforces non-negative money invariants.
 * 3. Never calculates or overrides financial figures on the client side.
 */
object FareMapper {

    fun toDomain(dto: RideFareResponseDto): RideFare {
        val currency = dto.currency.ifBlank { "INR" }
        val authoritativeCurrentFare = Money(
            amountMinor = dto.currentFareMinor.coerceAtLeast(0L),
            currency = currency
        )

        val estimate = dto.fareEstimate?.let { mapEstimate(it, currency) }
        val snapshot = dto.fareSnapshot?.let { mapSnapshot(it, currency) }

        return RideFare(
            rideId = dto.rideId,
            status = dto.status,
            currentFare = authoritativeCurrentFare,
            isFinal = dto.isFinal,
            estimate = estimate,
            snapshot = snapshot
        )
    }

    private fun mapEstimate(dto: FareEstimateDto, currency: String): FareEstimate {
        val curr = dto.currency.ifBlank { currency }
        return FareEstimate(
            currency = curr,
            pricingPolicyVersion = dto.pricingPolicyVersion,
            distanceMeters = dto.distanceMeters.coerceAtLeast(0L),
            estimatedDurationSeconds = dto.estimatedDurationSeconds?.coerceAtLeast(0L),
            baseFare = Money(dto.baseFareMinor.coerceAtLeast(0L), curr),
            distanceComponent = Money(dto.distanceComponentMinor.coerceAtLeast(0L), curr),
            timeComponent = Money(dto.timeComponentMinor.coerceAtLeast(0L), curr),
            subtotal = Money(dto.subtotalMinor.coerceAtLeast(0L), curr),
            serviceFee = Money(dto.serviceFeeMinor.coerceAtLeast(0L), curr),
            tax = Money(dto.taxMinor.coerceAtLeast(0L), curr),
            total = Money(dto.totalMinor.coerceAtLeast(0L), curr),
            calculatedAt = dto.calculatedAt
        )
    }

    private fun mapSnapshot(dto: FareSnapshotDto, currency: String): FareSnapshot {
        val curr = dto.currency.ifBlank { currency }
        return FareSnapshot(
            currency = curr,
            pricingPolicyVersion = dto.pricingPolicyVersion,
            distanceMeters = dto.distanceMeters.coerceAtLeast(0L),
            actualDurationSeconds = dto.actualDurationSeconds?.coerceAtLeast(0L),
            baseFare = Money(dto.baseFareMinor.coerceAtLeast(0L), curr),
            distanceComponent = Money(dto.distanceComponentMinor.coerceAtLeast(0L), curr),
            timeComponent = Money(dto.timeComponentMinor.coerceAtLeast(0L), curr),
            subtotal = Money(dto.subtotalMinor.coerceAtLeast(0L), curr),
            serviceFee = Money(dto.serviceFeeMinor.coerceAtLeast(0L), curr),
            tax = Money(dto.taxMinor.coerceAtLeast(0L), curr),
            discount = Money(dto.discountMinor.coerceAtLeast(0L), curr),
            total = Money(dto.totalMinor.coerceAtLeast(0L), curr),
            calculatedAt = dto.calculatedAt
        )
    }
}
