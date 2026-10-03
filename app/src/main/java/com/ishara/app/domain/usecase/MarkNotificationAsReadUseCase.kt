package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.repository.NotificationRepository

class MarkNotificationAsReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notificationId: String): IshaaraResult<NotificationItem> {
        return repository.markAsRead(notificationId)
    }
}
