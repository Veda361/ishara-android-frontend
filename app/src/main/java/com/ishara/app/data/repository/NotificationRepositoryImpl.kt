package com.ishara.app.data.repository

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.NotificationMapper
import com.ishara.app.data.remote.datasource.NotificationRemoteDataSource
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationItem
import com.ishara.app.domain.model.NotificationStatus
import com.ishara.app.domain.model.PaginatedNotifications
import com.ishara.app.domain.repository.NotificationRepository
import kotlinx.coroutines.withContext

class NotificationRepositoryImpl(
    private val remoteDataSource: NotificationRemoteDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider
) : NotificationRepository {

    override suspend fun getNotifications(
        page: Int,
        limit: Int,
        category: NotificationCategory?,
        status: NotificationStatus?
    ): IshaaraResult<PaginatedNotifications> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val authToken = session?.token
        if (authToken.isNullOrBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Authentication(message = "Authentication required to view notifications.")
            )
        }

        val clampedPage = if (page < 1) 1 else page
        val clampedLimit = limit.coerceIn(1, 50)
        val categoryParam = category?.backendValue
        val statusParam = status?.name

        remoteDataSource.getNotifications(clampedPage, clampedLimit, categoryParam, statusParam, authToken).map { dto ->
            NotificationMapper.toDomain(dto)
        }
    }

    override suspend fun getUnreadCount(): IshaaraResult<Int> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val authToken = session?.token
        if (authToken.isNullOrBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Authentication(message = "Authentication required to get unread count.")
            )
        }

        remoteDataSource.getUnreadCount(authToken).map { dto ->
            dto.unreadCount
        }
    }

    override suspend fun markAsRead(
        notificationId: String
    ): IshaaraResult<NotificationItem> = withContext(dispatchers.io) {
        val trimmedId = notificationId.trim()
        if (trimmedId.isBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Validation(field = "notificationId", message = "Notification ID is required.")
            )
        }

        val session = sessionStore.getSession()
        val authToken = session?.token
        if (authToken.isNullOrBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Authentication(message = "Authentication required to mark notification read.")
            )
        }

        remoteDataSource.markAsRead(trimmedId, authToken).map { dto ->
            NotificationMapper.toDomain(dto)
        }
    }

    override suspend fun markAllAsRead(): IshaaraResult<Int> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val authToken = session?.token
        if (authToken.isNullOrBlank()) {
            return@withContext IshaaraResult.Failure(
                IshaaraError.Authentication(message = "Authentication required to mark all notifications read.")
            )
        }

        remoteDataSource.markAllAsRead(authToken).map { dto ->
            dto.markedCount
        }
    }
}
