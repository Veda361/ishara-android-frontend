package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.DriverEarningsPaginationDto
import com.ishara.app.data.remote.dto.DriverEarningsPeriodDto
import com.ishara.app.data.remote.dto.DriverEarningsResponseDto
import com.ishara.app.data.remote.dto.DriverEarningsSummaryDto
import com.ishara.app.data.remote.dto.DriverRideEarningsItemDto
import com.ishara.app.data.remote.dto.DriverSettlementSummaryDto
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.DomainSettlementStatus
import com.ishara.app.domain.model.DriverEarnings
import com.ishara.app.domain.model.DriverEarningsPagination
import com.ishara.app.domain.model.DriverEarningsPeriod
import com.ishara.app.domain.model.DriverEarningsSummary
import com.ishara.app.domain.model.DriverRideEarningsItem
import com.ishara.app.domain.model.DriverSettlementSummary
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.Money

object DriverEarningsMapper {

    fun toDomain(dto: DriverEarningsResponseDto): DriverEarnings {
        return DriverEarnings(
            period = toDomain(dto.period),
            summary = toDomain(dto.summary),
            items = dto.items.map { toDomain(it) },
            pagination = toDomain(dto.pagination)
        )
    }

    fun toDomain(dto: DriverEarningsPeriodDto): DriverEarningsPeriod {
        return DriverEarningsPeriod(
            period = EarningsPeriodType.fromBackend(dto.period),
            from = dto.from,
            to = dto.to,
            timezone = dto.timezone.ifBlank { "Asia/Kolkata" }
        )
    }

    fun toDomain(dto: DriverEarningsSummaryDto): DriverEarningsSummary {
        val currency = dto.currency.ifBlank { "INR" }
        return DriverEarningsSummary(
            grossEarnings = Money(amountMinor = maxOf(0L, dto.grossEarningsMinor), currency = currency),
            platformDeductions = Money(amountMinor = maxOf(0L, dto.platformDeductionsMinor), currency = currency),
            netEarnings = Money(amountMinor = maxOf(0L, dto.netEarningsMinor), currency = currency),
            refundDeductions = Money(amountMinor = maxOf(0L, dto.refundDeductionsMinor), currency = currency),
            completedRidesCount = maxOf(0, dto.completedRidesCount),
            settlementSummary = toDomain(dto.settlementSummary, currency),
            currency = currency
        )
    }

    fun toDomain(dto: DriverSettlementSummaryDto, currency: String = "INR"): DriverSettlementSummary {
        return DriverSettlementSummary(
            settledAmount = Money(amountMinor = maxOf(0L, dto.settledAmountMinor), currency = currency),
            pendingSettlementAmount = Money(amountMinor = maxOf(0L, dto.pendingSettlementAmountMinor), currency = currency),
            unreadySettlementAmount = Money(amountMinor = maxOf(0L, dto.unreadySettlementAmountMinor), currency = currency),
            failedSettlementAmount = Money(amountMinor = maxOf(0L, dto.failedSettlementAmountMinor), currency = currency)
        )
    }

    fun toDomain(dto: DriverRideEarningsItemDto): DriverRideEarningsItem {
        val currency = dto.currency.ifBlank { "INR" }
        return DriverRideEarningsItem(
            rideId = dto.rideId,
            tripId = dto.tripId,
            completedAt = dto.completedAt,
            pickupAddress = dto.pickupAddress,
            destinationAddress = dto.destinationAddress,
            grossAmount = Money(amountMinor = maxOf(0L, dto.grossAmountMinor), currency = currency),
            platformFee = Money(amountMinor = maxOf(0L, dto.platformFeeMinor), currency = currency),
            netAmount = Money(amountMinor = maxOf(0L, dto.netAmountMinor), currency = currency),
            currency = currency,
            paymentStatus = DomainPaymentStatus.fromBackend(dto.paymentStatus),
            settlementStatus = DomainSettlementStatus.fromBackend(dto.settlementStatus)
        )
    }

    fun toDomain(dto: DriverEarningsPaginationDto): DriverEarningsPagination {
        return DriverEarningsPagination(
            total = maxOf(0, dto.total),
            page = maxOf(1, dto.page),
            limit = maxOf(1, dto.limit),
            hasMore = dto.hasMore
        )
    }
}
