package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.NotificationRepository

class MarkAllNotificationsAsReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(): IshaaraResult<Int> {
        return repository.markAllAsRead()
    }
}
