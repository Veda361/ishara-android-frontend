package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.MarkAllAsReadResponseDto
import com.ishara.app.data.remote.dto.NotificationItemDto
import com.ishara.app.data.remote.dto.PaginatedNotificationsResponseDto
import com.ishara.app.data.remote.dto.UnreadCountResponseDto

/**
 * Remote data source for notifications.
 * Backend Routes:
 * - GET  /api/v1/notifications?page=1&limit=20&status=UNREAD
 * - GET  /api/v1/notifications/unread-count
 * - POST /api/v1/notifications/:notificationId/read
 * - POST /api/v1/notifications/read-all
 */
interface NotificationRemoteDataSource {

    suspend fun getNotifications(
        page: Int,
        limit: Int,
        category: String? = null,
        status: String? = null,
        token: String?
    ): IshaaraResult<PaginatedNotificationsResponseDto>

    suspend fun getUnreadCount(
        token: String?
    ): IshaaraResult<UnreadCountResponseDto>

    suspend fun markAsRead(
        notificationId: String,
        token: String?
    ): IshaaraResult<NotificationItemDto>

    suspend fun markAllAsRead(
        token: String?
    ): IshaaraResult<MarkAllAsReadResponseDto>
}

class NotificationRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : NotificationRemoteDataSource {

    private val tag = "NotificationRemoteDS"

    override suspend fun getNotifications(
        page: Int,
        limit: Int,
        category: String?,
        status: String?,
        token: String?
    ): IshaaraResult<PaginatedNotificationsResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val queryParams = mutableListOf("page=$page", "limit=$limit")
        if (!category.isNullOrBlank()) {
            queryParams.add("category=$category")
        }
        if (!status.isNullOrBlank()) {
            queryParams.add("status=$status")
        }
        val queryString = queryParams.joinToString("&")

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/notifications?$queryString",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parsePaginatedNotificationsResponse(response.body)
        }
    }

    override suspend fun getUnreadCount(
        token: String?
    ): IshaaraResult<UnreadCountResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/notifications/unread-count",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseUnreadCountResponse(response.body)
        }
    }

    override suspend fun markAsRead(
        notificationId: String,
        token: String?
    ): IshaaraResult<NotificationItemDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/notifications/$notificationId/read",
            method = HttpMethod.POST,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseNotificationItemResponse(response.body)
        }
    }

    override suspend fun markAllAsRead(
        token: String?
    ): IshaaraResult<MarkAllAsReadResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/notifications/read-all",
            method = HttpMethod.POST,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseMarkAllAsReadResponse(response.body)
        }
    }

    fun parsePaginatedNotificationsResponse(json: String): PaginatedNotificationsResponseDto {
        val dataJson = extractJsonField(json, "data") ?: json

        val total = extractJsonInt(dataJson, "total") ?: 0
        val page = extractJsonInt(dataJson, "page") ?: 1
        val limit = extractJsonInt(dataJson, "limit") ?: 20
        val hasMore = extractJsonBoolean(dataJson, "hasMore") ?: false
        val unreadCount = extractJsonInt(dataJson, "unreadCount") ?: 0

        val items = mutableListOf<NotificationItemDto>()
        val itemsArrayJson = extractJsonField(dataJson, "items")
        if (!itemsArrayJson.isNullOrBlank() && itemsArrayJson.startsWith("[") && itemsArrayJson.endsWith("]")) {
            val content = itemsArrayJson.substring(1, itemsArrayJson.length - 1).trim()
            if (content.isNotBlank()) {
                val objectStrings = splitJsonObjects(content)
                for (obj in objectStrings) {
                    items.add(parseNotificationItem(obj))
                }
            }
        }

        return PaginatedNotificationsResponseDto(
            items = items,
            total = total,
            page = page,
            limit = limit,
            hasMore = hasMore,
            unreadCount = unreadCount
        )
    }

    fun parseUnreadCountResponse(json: String): UnreadCountResponseDto {
        val dataJson = extractJsonField(json, "data") ?: json
        val count = extractJsonInt(dataJson, "unreadCount") ?: 0
        return UnreadCountResponseDto(unreadCount = count)
    }

    fun parseNotificationItemResponse(json: String): NotificationItemDto {
        val dataJson = extractJsonField(json, "data") ?: json
        return parseNotificationItem(dataJson)
    }

    fun parseMarkAllAsReadResponse(json: String): MarkAllAsReadResponseDto {
        val dataJson = extractJsonField(json, "data") ?: json
        val count = extractJsonInt(dataJson, "markedCount") ?: 0
        return MarkAllAsReadResponseDto(markedCount = count)
    }

    fun parseNotificationItem(objJson: String): NotificationItemDto {
        val id = extractJsonString(objJson, "id") ?: extractJsonString(objJson, "_id") ?: ""
        val userId = extractJsonString(objJson, "userId") ?: ""
        val type = extractJsonString(objJson, "type") ?: ""
        val title = extractJsonString(objJson, "title") ?: ""
        val body = extractJsonString(objJson, "body") ?: ""
        val sourceEventId = extractJsonString(objJson, "sourceEventId") ?: ""
        val aggregateType = extractJsonString(objJson, "aggregateType") ?: ""
        val aggregateId = extractJsonString(objJson, "aggregateId") ?: ""
        val status = extractJsonString(objJson, "status") ?: "UNREAD"
        val priority = extractJsonString(objJson, "priority") ?: "NORMAL"
        val category = extractJsonString(objJson, "category")
        val readAt = extractJsonString(objJson, "readAt")
        val createdAt = extractJsonString(objJson, "createdAt") ?: ""
        val updatedAt = extractJsonString(objJson, "updatedAt") ?: ""

        val dataMap = mutableMapOf<String, String>()
        val dataFieldJson = extractJsonField(objJson, "data")
        if (!dataFieldJson.isNullOrBlank() && dataFieldJson.startsWith("{") && dataFieldJson.endsWith("}")) {
            val pairs = extractKeyValuePairs(dataFieldJson)
            dataMap.putAll(pairs)
        }

        return NotificationItemDto(
            id = id,
            userId = userId,
            type = type,
            title = title,
            body = body,
            data = dataMap,
            sourceEventId = sourceEventId,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            status = status,
            priority = priority,
            readAt = readAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            category = category
        )
    }

    private fun extractKeyValuePairs(jsonObj: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pattern = "\"([^\"]+)\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"".toRegex()
        pattern.findAll(jsonObj).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            result[key] = value
        }
        return result
    }

    private fun splitJsonObjects(arrayContent: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var isEscaped = false

        for (i in arrayContent.indices) {
            val c = arrayContent[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && start != -1) {
                        result.add(arrayContent.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return result
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*(?:\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"|null)".toRegex()
        val match = pattern.find(json) ?: return null
        val group = match.groups[1] ?: return null
        return group.value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toBoolean()
    }

    private fun extractJsonField(json: String, key: String): String? {
        val startIndex = json.indexOf("\"$key\"")
        if (startIndex == -1) return null

        val colonIndex = json.indexOf(':', startIndex)
        if (colonIndex == -1) return null

        var i = colonIndex + 1
        while (i < json.length && json[i].isWhitespace()) {
            i++
        }
        if (i >= json.length) return null

        return when (json[i]) {
            '{' -> {
                var depth = 0
                val start = i
                while (i < json.length) {
                    if (json[i] == '{') depth++
                    else if (json[i] == '}') {
                        depth--
                        if (depth == 0) return json.substring(start, i + 1)
                    }
                    i++
                }
                null
            }
            '[' -> {
                var depth = 0
                val start = i
                while (i < json.length) {
                    if (json[i] == '[') depth++
                    else if (json[i] == ']') {
                        depth--
                        if (depth == 0) return json.substring(start, i + 1)
                    }
                    i++
                }
                null
            }
            '"' -> {
                val start = i + 1
                val end = json.indexOf('"', start)
                if (end != -1) json.substring(start, end) else null
            }
            else -> {
                val start = i
                while (i < json.length && json[i] != ',' && json[i] != '}' && json[i] != ']') {
                    i++
                }
                json.substring(start, i).trim()
            }
        }
    }
}
