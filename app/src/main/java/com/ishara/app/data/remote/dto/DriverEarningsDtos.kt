package com.ishara.app.data.remote.dto

/**
 * Data Transfer Objects for Driver Earnings and Financial Subsystem.
 * Models exact backend responses without inventing fields.
 */

data class DriverEarningsPeriodDto(
    val period: String = "today",
    val from: String = "",
    val to: String = "",
    val timezone: String = "Asia/Kolkata"
)

data class DriverSettlementSummaryDto(
    val settledAmountMinor: Long = 0L,
    val pendingSettlementAmountMinor: Long = 0L,
    val unreadySettlementAmountMinor: Long = 0L,
    val failedSettlementAmountMinor: Long = 0L
)

data class DriverEarningsSummaryDto(
    val grossEarningsMinor: Long = 0L,
    val platformDeductionsMinor: Long = 0L,
    val netEarningsMinor: Long = 0L,
    val refundDeductionsMinor: Long = 0L,
    val completedRidesCount: Int = 0,
    val settlementSummary: DriverSettlementSummaryDto = DriverSettlementSummaryDto(),
    val currency: String = "INR"
)

data class DriverRideEarningsItemDto(
    val rideId: String,
    val tripId: String = "",
    val completedAt: String? = null,
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val grossAmountMinor: Long = 0L,
    val platformFeeMinor: Long = 0L,
    val netAmountMinor: Long = 0L,
    val currency: String = "INR",
    val paymentStatus: String = "PENDING",
    val settlementStatus: String = "UNSETTLED"
)

data class DriverEarningsPaginationDto(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val hasMore: Boolean = false
)

data class DriverEarningsResponseDto(
    val period: DriverEarningsPeriodDto = DriverEarningsPeriodDto(),
    val summary: DriverEarningsSummaryDto = DriverEarningsSummaryDto(),
    val items: List<DriverRideEarningsItemDto> = emptyList(),
    val pagination: DriverEarningsPaginationDto = DriverEarningsPaginationDto()
)
