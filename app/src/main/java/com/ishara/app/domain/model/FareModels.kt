package com.ishara.app.domain.model

/**
 * Authoritative ride fare domain entity aggregating server-side pricing data.
 * Pure consumer of backend pricing calculations.
 */
data class RideFare(
    val rideId: String,
    val status: String,
    val currentFare: Money,
    val isFinal: Boolean,
    val estimate: FareEstimate?,
    val snapshot: FareSnapshot?
) {
    /**
     * Returns the active breakdown if available (preferring immutable snapshot over estimate).
     */
    val effectiveBreakdown: FareBreakdownDetails?
        get() = snapshot?.toBreakdownDetails() ?: estimate?.toBreakdownDetails()
}

/**
 * Unified breakdown details for UI rendering without client-side financial recalculation.
 */
data class FareBreakdownDetails(
    val isSnapshot: Boolean,
    val currency: String,
    val pricingPolicyVersion: String,
    val distanceMeters: Long,
    val durationSeconds: Long?,
    val baseFare: Money,
    val distanceComponent: Money,
    val timeComponent: Money,
    val subtotal: Money,
    val serviceFee: Money,
    val tax: Money,
    val discount: Money,
    val total: Money,
    val calculatedAt: String
) {
    val distanceKmFormatted: String
        get() = String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000.0)

    val durationMinutesFormatted: String?
        get() = durationSeconds?.let { "${it / 60} mins" }
}

/**
 * Pre-ride or in-flight estimated fare projection domain model.
 */
data class FareEstimate(
    val currency: String,
    val pricingPolicyVersion: String,
    val distanceMeters: Long,
    val estimatedDurationSeconds: Long?,
    val baseFare: Money,
    val distanceComponent: Money,
    val timeComponent: Money,
    val subtotal: Money,
    val serviceFee: Money,
    val tax: Money,
    val total: Money,
    val calculatedAt: String
) {
    fun toBreakdownDetails() = FareBreakdownDetails(
        isSnapshot = false,
        currency = currency,
        pricingPolicyVersion = pricingPolicyVersion,
        distanceMeters = distanceMeters,
        durationSeconds = estimatedDurationSeconds,
        baseFare = baseFare,
        distanceComponent = distanceComponent,
        timeComponent = timeComponent,
        subtotal = subtotal,
        serviceFee = serviceFee,
        tax = tax,
        discount = Money(0L, currency),
        total = total,
        calculatedAt = calculatedAt
    )
}

/**
 * Authoritative, immutable post-ride billing snapshot domain model.
 */
data class FareSnapshot(
    val currency: String,
    val pricingPolicyVersion: String,
    val distanceMeters: Long,
    val actualDurationSeconds: Long?,
    val baseFare: Money,
    val distanceComponent: Money,
    val timeComponent: Money,
    val subtotal: Money,
    val serviceFee: Money,
    val tax: Money,
    val discount: Money,
    val total: Money,
    val calculatedAt: String
) {
    fun toBreakdownDetails() = FareBreakdownDetails(
        isSnapshot = true,
        currency = currency,
        pricingPolicyVersion = pricingPolicyVersion,
        distanceMeters = distanceMeters,
        durationSeconds = actualDurationSeconds,
        baseFare = baseFare,
        distanceComponent = distanceComponent,
        timeComponent = timeComponent,
        subtotal = subtotal,
        serviceFee = serviceFee,
        tax = tax,
        discount = discount,
        total = total,
        calculatedAt = calculatedAt
    )
}
