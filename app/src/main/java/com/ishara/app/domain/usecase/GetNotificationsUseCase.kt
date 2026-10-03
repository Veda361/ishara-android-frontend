package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.PaginatedNotifications
import com.ishara.app.domain.repository.NotificationRepository

class GetNotificationsUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(
        page: Int = 1,
        limit: Int = 20,
        category: NotificationCategory? = null,
        status: NotificationStatus? = null
    ): IshaaraResult<PaginatedNotifications> {
        return repository.getNotifications(page, limit, category, status)
    }
}
