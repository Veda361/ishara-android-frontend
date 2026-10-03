package com.ishara.app.domain.usecase

import com.ishara.app.domain.repository.FareRepository

/**
 * Use case clearing cached fare states during logout or role/account switching.
 */
class ClearFareStateUseCase(
    private val fareRepository: FareRepository
) {
    suspend operator fun invoke() {
        fareRepository.clearFareState()
    }
}
