package com.ishara.app.domain.model

import java.util.Locale

/**
 * Period filter types supported by backend GET /api/v1/drivers/me/earnings.
 */
enum class EarningsPeriodType(val queryParam: String) {
    TODAY("today"),
    WEEK("week"),
    MONTH("month"),
    CUSTOM("custom");

    companion object {
        fun fromBackend(value: String?): EarningsPeriodType {
            return when (value?.lowercase(Locale.ROOT)) {
                "today" -> TODAY
                "week" -> WEEK
                "month" -> MONTH
                "custom" -> CUSTOM
                else -> TODAY
            }
        }
    }
}

/**
 * Authoritative settlement lifecycle statuses verified from backend constants.
 */
enum class DomainSettlementStatus {
    NOT_READY,
    PENDING,
    PROCESSING,
    PROCESSED,
    RECONCILING,
    FAILED,
    UNSETTLED,
    UNKNOWN;

    val isSettled: Boolean get() = this == PROCESSED
    val isPending: Boolean get() = this == PENDING || this == PROCESSING || this == NOT_READY || this == RECONCILING
    val isFailed: Boolean get() = this == FAILED

    companion object {
        fun fromBackend(value: String?): DomainSettlementStatus {
            return when (value?.uppercase(Locale.ROOT)) {
                "NOT_READY" -> NOT_READY
                "PENDING" -> PENDING
                "PROCESSING" -> PROCESSING
                "PROCESSED" -> PROCESSED
                "RECONCILING" -> RECONCILING
                "FAILED" -> FAILED
                "UNSETTLED" -> UNSETTLED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Verified period bounds returned by backend.
 */
data class DriverEarningsPeriod(
    val period: EarningsPeriodType,
    val from: String,
    val to: String,
    val timezone: String = "Asia/Kolkata"
)

/**
 * Authoritative settlement amounts grouped by status from SettlementModel aggregation.
 */
data class DriverSettlementSummary(
    val settledAmount: Money,
    val pendingSettlementAmount: Money,
    val unreadySettlementAmount: Money,
    val failedSettlementAmount: Money
)

/**
 * Authoritative earnings summary aggregated by backend PaymentModel & SettlementModel.
 */
data class DriverEarningsSummary(
    val grossEarnings: Money,
    val platformDeductions: Money,
    val netEarnings: Money,
    val refundDeductions: Money,
    val completedRidesCount: Int,
    val settlementSummary: DriverSettlementSummary,
    val currency: String = "INR"
)

/**
 * Authoritative completed ride earnings item enriched via anti-N+1 lookup.
 */
data class DriverRideEarningsItem(
    val rideId: String,
    val tripId: String,
    val completedAt: String?,
    val pickupAddress: String,
    val destinationAddress: String,
    val grossAmount: Money,
    val platformFee: Money,
    val netAmount: Money,
    val currency: String,
    val paymentStatus: DomainPaymentStatus,
    val settlementStatus: DomainSettlementStatus
)

/**
 * Bounded pagination details returned by backend.
 */
data class DriverEarningsPagination(
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Complete authoritative driver earnings domain model.
 */
data class DriverEarnings(
    val period: DriverEarningsPeriod,
    val summary: DriverEarningsSummary,
    val items: List<DriverRideEarningsItem>,
    val pagination: DriverEarningsPagination
)
