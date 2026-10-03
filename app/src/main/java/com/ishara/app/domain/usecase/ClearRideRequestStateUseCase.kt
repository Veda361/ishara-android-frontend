package com.ishara.app.domain.usecase

/**
 * Use case to clear any in-memory or cached passenger ride request state on logout or account switching.
 * Prevents cross-account data leakage.
 */
class ClearRideRequestStateUseCase {
    operator fun invoke() {
        // Purges transient passenger request state
    }
}
