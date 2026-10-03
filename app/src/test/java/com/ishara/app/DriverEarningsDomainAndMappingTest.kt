package com.ishara.app

import com.ishara.app.data.mapper.DriverEarningsMapper
import com.ishara.app.data.remote.dto.DriverEarningsPaginationDto
import com.ishara.app.data.remote.dto.DriverEarningsPeriodDto
import com.ishara.app.data.remote.dto.DriverEarningsResponseDto
import com.ishara.app.data.remote.dto.DriverEarningsSummaryDto
import com.ishara.app.data.remote.dto.DriverRideEarningsItemDto
import com.ishara.app.data.remote.dto.DriverSettlementSummaryDto
import com.ishara.app.domain.model.DomainPaymentStatus
import com.ishara.app.domain.model.DomainSettlementStatus
import com.ishara.app.domain.model.EarningsPeriodType
import com.ishara.app.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain and Mapper Unit Tests for Phase 16 Driver Earnings & Payout Analytics.
 */
class DriverEarningsDomainAndMappingTest {

    // =========================================================================
    // 1. Money Exact Representation Tests
    // =========================================================================

    @Test
    fun `Money correctly calculates major amount and formats Indian currency`() {
        val moneyZero = Money(amountMinor = 0L, currency = "INR")
        assertEquals(0.0, moneyZero.amountMajor, 0.0001)
        assertEquals("₹0.00", moneyZero.formatDisplay())

        val moneyPaise = Money(amountMinor = 2500L, currency = "INR")
        assertEquals(25.0, moneyPaise.amountMajor, 0.0001)
        assertEquals("₹25.00", moneyPaise.formatDisplay())

        val moneyThousand = Money(amountMinor = 125000L, currency = "INR")
        assertEquals(1250.0, moneyThousand.amountMajor, 0.0001)
        assertEquals("₹1250.00", moneyThousand.formatDisplay())

        val moneyLarge = Money(amountMinor = 5000000L, currency = "INR")
        assertEquals(50000.0, moneyLarge.amountMajor, 0.0001)
        assertEquals("₹50000.00", moneyLarge.formatDisplay())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Money rejects negative minor amount`() {
        Money(amountMinor = -1L, currency = "INR")
    }

    // =========================================================================
    // 2. Period Filter Type Mapping Tests
    // =========================================================================

    @Test
    fun `EarningsPeriodType maps backend strings correctly`() {
        assertEquals(EarningsPeriodType.TODAY, EarningsPeriodType.fromBackend("today"))
        assertEquals(EarningsPeriodType.TODAY, EarningsPeriodType.fromBackend("TODAY"))
        assertEquals(EarningsPeriodType.WEEK, EarningsPeriodType.fromBackend("week"))
        assertEquals(EarningsPeriodType.WEEK, EarningsPeriodType.fromBackend("WEEK"))
        assertEquals(EarningsPeriodType.MONTH, EarningsPeriodType.fromBackend("month"))
        assertEquals(EarningsPeriodType.MONTH, EarningsPeriodType.fromBackend("MONTH"))
        assertEquals(EarningsPeriodType.CUSTOM, EarningsPeriodType.fromBackend("custom"))
        assertEquals(EarningsPeriodType.CUSTOM, EarningsPeriodType.fromBackend("CUSTOM"))
        // Fallback to TODAY for null or unrecognized
        assertEquals(EarningsPeriodType.TODAY, EarningsPeriodType.fromBackend(null))
        assertEquals(EarningsPeriodType.TODAY, EarningsPeriodType.fromBackend("year"))
    }

    // =========================================================================
    // 3. Domain Settlement Status Mapping Tests
    // =========================================================================

    @Test
    fun `DomainSettlementStatus maps backend states with forward compatibility`() {
        assertEquals(DomainSettlementStatus.PROCESSED, DomainSettlementStatus.fromBackend("PROCESSED"))
        assertTrue(DomainSettlementStatus.PROCESSED.isSettled)
        assertFalse(DomainSettlementStatus.PROCESSED.isPending)

        assertEquals(DomainSettlementStatus.PENDING, DomainSettlementStatus.fromBackend("PENDING"))
        assertTrue(DomainSettlementStatus.PENDING.isPending)
        assertFalse(DomainSettlementStatus.PENDING.isSettled)

        assertEquals(DomainSettlementStatus.NOT_READY, DomainSettlementStatus.fromBackend("NOT_READY"))
        assertTrue(DomainSettlementStatus.NOT_READY.isPending)

        assertEquals(DomainSettlementStatus.PROCESSING, DomainSettlementStatus.fromBackend("PROCESSING"))
        assertTrue(DomainSettlementStatus.PROCESSING.isPending)

        assertEquals(DomainSettlementStatus.RECONCILING, DomainSettlementStatus.fromBackend("RECONCILING"))
        assertTrue(DomainSettlementStatus.RECONCILING.isPending)

        assertEquals(DomainSettlementStatus.FAILED, DomainSettlementStatus.fromBackend("FAILED"))
        assertTrue(DomainSettlementStatus.FAILED.isFailed)

        assertEquals(DomainSettlementStatus.UNSETTLED, DomainSettlementStatus.fromBackend("UNSETTLED"))
        assertFalse(DomainSettlementStatus.UNSETTLED.isSettled)

        // Unknown forward status does not crash
        assertEquals(DomainSettlementStatus.UNKNOWN, DomainSettlementStatus.fromBackend("FUTURE_SETTLEMENT_STATE"))
        assertEquals(DomainSettlementStatus.UNKNOWN, DomainSettlementStatus.fromBackend(null))
    }

    // =========================================================================
    // 4. Driver Earnings Mapper Tests
    // =========================================================================

    @Test
    fun `DriverEarningsMapper maps complete DTO to domain model accurately`() {
        val dto = DriverEarningsResponseDto(
            period = DriverEarningsPeriodDto(
                period = "today",
                from = "2026-09-28T00:00:00.000+05:30",
                to = "2026-09-28T23:59:59.999+05:30",
                timezone = "Asia/Kolkata"
            ),
            summary = DriverEarningsSummaryDto(
                grossEarningsMinor = 80000L,
                platformDeductionsMinor = 8000L,
                netEarningsMinor = 72000L,
                refundDeductionsMinor = 0L,
                completedRidesCount = 2,
                settlementSummary = DriverSettlementSummaryDto(
                    settledAmountMinor = 45000L,
                    pendingSettlementAmountMinor = 27000L,
                    unreadySettlementAmountMinor = 0L,
                    failedSettlementAmountMinor = 0L
                ),
                currency = "INR"
            ),
            items = listOf(
                DriverRideEarningsItemDto(
                    rideId = "ride_1",
                    tripId = "trip_1",
                    completedAt = "2026-09-28T08:00:00.000Z",
                    pickupAddress = "Sector 62, Noida",
                    destinationAddress = "Botanical Garden, Noida",
                    grossAmountMinor = 50000L,
                    platformFeeMinor = 5000L,
                    netAmountMinor = 45000L,
                    currency = "INR",
                    paymentStatus = "CAPTURED",
                    settlementStatus = "PROCESSED"
                ),
                DriverRideEarningsItemDto(
                    rideId = "ride_2",
                    tripId = "trip_1",
                    completedAt = "2026-09-28T09:30:00.000Z",
                    pickupAddress = "Mayur Vihar, Delhi",
                    destinationAddress = "Noida City Center",
                    grossAmountMinor = 30000L,
                    platformFeeMinor = 3000L,
                    netAmountMinor = 27000L,
                    currency = "INR",
                    paymentStatus = "CAPTURED",
                    settlementStatus = "PENDING"
                )
            ),
            pagination = DriverEarningsPaginationDto(
                total = 2,
                page = 1,
                limit = 20,
                hasMore = false
            )
        )

        val domain = DriverEarningsMapper.toDomain(dto)

        // Verify Period
        assertEquals(EarningsPeriodType.TODAY, domain.period.period)
        assertEquals("2026-09-28T00:00:00.000+05:30", domain.period.from)
        assertEquals("2026-09-28T23:59:59.999+05:30", domain.period.to)
        assertEquals("Asia/Kolkata", domain.period.timezone)

        // Verify Summary
        assertEquals(80000L, domain.summary.grossEarnings.amountMinor)
        assertEquals("₹800.00", domain.summary.grossEarnings.formatDisplay())
        assertEquals(8000L, domain.summary.platformDeductions.amountMinor)
        assertEquals("₹80.00", domain.summary.platformDeductions.formatDisplay())
        assertEquals(72000L, domain.summary.netEarnings.amountMinor)
        assertEquals("₹720.00", domain.summary.netEarnings.formatDisplay())
        assertEquals(0L, domain.summary.refundDeductions.amountMinor)
        assertEquals(2, domain.summary.completedRidesCount)

        // Verify Settlement Summary
        assertEquals(45000L, domain.summary.settlementSummary.settledAmount.amountMinor)
        assertEquals("₹450.00", domain.summary.settlementSummary.settledAmount.formatDisplay())
        assertEquals(27000L, domain.summary.settlementSummary.pendingSettlementAmount.amountMinor)
        assertEquals("₹270.00", domain.summary.settlementSummary.pendingSettlementAmount.formatDisplay())

        // Verify Items
        assertEquals(2, domain.items.size)
        val item1 = domain.items[0]
        assertEquals("ride_1", item1.rideId)
        assertEquals("trip_1", item1.tripId)
        assertEquals("Sector 62, Noida", item1.pickupAddress)
        assertEquals(50000L, item1.grossAmount.amountMinor)
        assertEquals(5000L, item1.platformFee.amountMinor)
        assertEquals(45000L, item1.netAmount.amountMinor)
        assertEquals(DomainPaymentStatus.CAPTURED, item1.paymentStatus)
        assertEquals(DomainSettlementStatus.PROCESSED, item1.settlementStatus)

        val item2 = domain.items[1]
        assertEquals("ride_2", item2.rideId)
        assertEquals(DomainPaymentStatus.CAPTURED, item2.paymentStatus)
        assertEquals(DomainSettlementStatus.PENDING, item2.settlementStatus)

        // Verify Pagination
        assertEquals(2, domain.pagination.total)
        assertEquals(1, domain.pagination.page)
        assertEquals(20, domain.pagination.limit)
        assertFalse(domain.pagination.hasMore)
    }

    @Test
    fun `DriverEarningsMapper maps zero-state cleanly without null pointer exceptions`() {
        val zeroDto = DriverEarningsResponseDto()
        val domain = DriverEarningsMapper.toDomain(zeroDto)

        assertEquals(0L, domain.summary.grossEarnings.amountMinor)
        assertEquals("₹0.00", domain.summary.grossEarnings.formatDisplay())
        assertEquals(0L, domain.summary.netEarnings.amountMinor)
        assertEquals("₹0.00", domain.summary.netEarnings.formatDisplay())
        assertEquals(0, domain.summary.completedRidesCount)
        assertTrue(domain.items.isEmpty())
        assertEquals(0, domain.pagination.total)
        assertFalse(domain.pagination.hasMore)
    }
}
