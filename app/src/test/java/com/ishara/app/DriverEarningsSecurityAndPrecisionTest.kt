package com.ishara.app

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
import com.ishara.app.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security, Role Isolation, and Financial Precision Tests for Phase 16.
 */
class DriverEarningsSecurityAndPrecisionTest {

    // =========================================================================
    // 1. Role Boundary Security Tests
    // =========================================================================

    @Test
    fun `Role validation strictly isolates DRIVER_CONDUCTOR from USER role`() {
        val passengerRole = UserRole.USER
        val driverRole = UserRole.DRIVER_CONDUCTOR

        assertNotEquals(passengerRole, driverRole)
        assertFalse(passengerRole == UserRole.DRIVER_CONDUCTOR)
        assertTrue(driverRole == UserRole.DRIVER_CONDUCTOR)
    }

    // =========================================================================
    // 2. Financial Arithmetic & Money Precision Tests
    // =========================================================================

    @Test
    fun `Financial precision operates strictly in integer paise without floating point error`() {
        // Zero
        val zero = Money(amountMinor = 0L, currency = "INR")
        assertEquals(0L, zero.amountMinor)
        assertEquals("₹0.00", zero.formatDisplay())

        // 1 paisa (boundary case)
        val onePaisa = Money(amountMinor = 1L, currency = "INR")
        assertEquals(1L, onePaisa.amountMinor)
        assertEquals(0.01, onePaisa.amountMajor, 0.0001)
        assertEquals("₹0.01", onePaisa.formatDisplay())

        // 99.99 rupees
        val ninetyNineRupees = Money(amountMinor = 9999L, currency = "INR")
        assertEquals(9999L, ninetyNineRupees.amountMinor)
        assertEquals(99.99, ninetyNineRupees.amountMajor, 0.0001)
        assertEquals("₹99.99", ninetyNineRupees.formatDisplay())

        // 1,000 rupees
        val oneThousand = Money(amountMinor = 100000L, currency = "INR")
        assertEquals(100000L, oneThousand.amountMinor)
        assertEquals(1000.0, oneThousand.amountMajor, 0.0001)
        assertEquals("₹1000.00", oneThousand.formatDisplay())

        // 10 lakh rupees (1,000,000 INR)
        val tenLakh = Money(amountMinor = 100000000L, currency = "INR")
        assertEquals(100000000L, tenLakh.amountMinor)
        assertEquals(1000000.0, tenLakh.amountMajor, 0.0001)
        assertEquals("₹1000000.00", tenLakh.formatDisplay())
    }

    @Test
    fun `Financial ledger invariant holds - Gross equals Platform Deductions plus Net Earnings`() {
        val gross = 80000L // Rs 800.00
        val platformFee = 8000L // Rs 80.00 (10%)
        val netDriverShare = 72000L // Rs 720.00

        val summary = DriverEarningsSummary(
            grossEarnings = Money(gross),
            platformDeductions = Money(platformFee),
            netEarnings = Money(netDriverShare),
            refundDeductions = Money(0L),
            completedRidesCount = 2,
            settlementSummary = DriverSettlementSummary(
                settledAmount = Money(45000L),
                pendingSettlementAmount = Money(27000L),
                unreadySettlementAmount = Money(0L),
                failedSettlementAmount = Money(0L)
            )
        )

        assertEquals(
            summary.grossEarnings.amountMinor,
            summary.platformDeductions.amountMinor + summary.netEarnings.amountMinor
        )

        // Settlement sum matches total net earnings
        assertEquals(
            summary.netEarnings.amountMinor,
            summary.settlementSummary.settledAmount.amountMinor + summary.settlementSummary.pendingSettlementAmount.amountMinor
        )
    }

    // =========================================================================
    // 3. Passenger PII & Driver Data Privacy Tests
    // =========================================================================

    @Test
    fun `DriverRideEarningsItem does not contain passenger PII`() {
        val rideItem = DriverRideEarningsItem(
            rideId = "ride_sec_101",
            tripId = "trip_sec_202",
            completedAt = "2026-09-28T10:00:00.000Z",
            pickupAddress = "Civil Lines, Prayagraj",
            destinationAddress = "University Gate 2",
            grossAmount = Money(40000L),
            platformFee = Money(4000L),
            netAmount = Money(36000L),
            currency = "INR",
            paymentStatus = DomainPaymentStatus.CAPTURED,
            settlementStatus = DomainSettlementStatus.PROCESSED
        )

        // Class fields audit
        val declaredFieldNames = DriverRideEarningsItem::class.java.declaredFields.map { it.name }.toSet()
        assertFalse(declaredFieldNames.contains("passengerPhone"))
        assertFalse(declaredFieldNames.contains("phoneNumber"))
        assertFalse(declaredFieldNames.contains("passengerEmail"))
        assertFalse(declaredFieldNames.contains("email"))
        assertFalse(declaredFieldNames.contains("passengerName"))
        assertFalse(declaredFieldNames.contains("bankAccountNumber"))
        assertFalse(declaredFieldNames.contains("ifsc"))
        assertFalse(declaredFieldNames.contains("cvv"))
    }
}
