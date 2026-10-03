package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideFare

/**
 * Repository interface for Phase A12 — Fare & Authoritative Pricing.
 */
interface FareRepository {

    /**
     * Retrieves the authoritative server-side fare calculation for a specific ride.
     * Before ride completion, returns estimated fare.
     * Upon completion, returns the immutable billing snapshot.
     */
    suspend fun getRideFare(rideId: String): IshaaraResult<RideFare>

    /**
     * Clears all cached fare state on logout or account switching.
     */
    suspend fun clearFareState()
}
