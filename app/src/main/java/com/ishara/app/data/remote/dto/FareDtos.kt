package com.ishara.app.data.remote.dto

/**
 * Backend wire DTOs for Phase A12 — Fare & Authoritative Pricing Snapshot.
 * Matches backend `GET /api/v1/rides/:rideId/fare` contract (`RideFareResponse`).
 *
 * All financial amounts are integer minor currency units (paise in INR).
 * Floating-point representation for money is prohibited across the platform.
 */
data class RideFareResponseDto(
    val rideId: String,
    val status: String,
    val currency: String,
    val currentFareMinor: Long,
    val isFinal: Boolean,
    val fareEstimate: FareEstimateDto? = null,
    val fareSnapshot: FareSnapshotDto? = null
)

/**
 * Pre-ride or in-flight estimated fare projection from backend pricing policy.
 */
data class FareEstimateDto(
    val currency: String,
    val pricingPolicyVersion: String,
    val distanceMeters: Long,
    val estimatedDurationSeconds: Long? = null,
    val baseFareMinor: Long,
    val distanceComponentMinor: Long,
    val timeComponentMinor: Long = 0L,
    val subtotalMinor: Long,
    val serviceFeeMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long,
    val providerAmountMinor: Long = 0L,
    val isEstimate: Boolean = true,
    val calculatedAt: String
)

/**
 * Authoritative, immutable post-ride billing snapshot stamped upon ride completion.
 */
data class FareSnapshotDto(
    val currency: String,
    val pricingPolicyVersion: String,
    val distanceMeters: Long,
    val actualDurationSeconds: Long? = null,
    val baseFareMinor: Long,
    val distanceComponentMinor: Long,
    val timeComponentMinor: Long = 0L,
    val subtotalMinor: Long,
    val serviceFeeMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val totalMinor: Long,
    val providerAmountMinor: Long = 0L,
    val isEstimate: Boolean = false,
    val calculatedAt: String
)
