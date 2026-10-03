package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.RegisterPushTokenRequestDto
import com.ishara.app.data.remote.dto.RegisterPushTokenResponseDto
import com.ishara.app.data.remote.dto.RemovePushTokenRequestDto
import com.ishara.app.data.remote.dto.RemovePushTokenResponseDto

/**
 * Remote data source for device push token registration and removal.
 * Backend Routes:
 * - POST   /api/v1/devices/push-token
 * - DELETE /api/v1/devices/push-token
 */
interface DeviceTokenRemoteDataSource {

    suspend fun registerPushToken(
        request: RegisterPushTokenRequestDto,
        token: String?
    ): IshaaraResult<RegisterPushTokenResponseDto>

    suspend fun removePushToken(
        request: RemovePushTokenRequestDto,
        token: String?
    ): IshaaraResult<RemovePushTokenResponseDto>
}

class DeviceTokenRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DeviceTokenRemoteDataSource {

    private val tag = "DeviceTokenRemoteDS"

    override suspend fun registerPushToken(
        request: RegisterPushTokenRequestDto,
        token: String?
    ): IshaaraResult<RegisterPushTokenResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val bodyJson = serializeRegisterRequest(request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/devices/push-token",
            method = HttpMethod.POST,
            headers = headers,
            body = bodyJson
        )

        return httpClient.execute(httpRequest).map { response ->
            parseRegisterResponse(response.body)
        }
    }

    override suspend fun removePushToken(
        request: RemovePushTokenRequestDto,
        token: String?
    ): IshaaraResult<RemovePushTokenResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val bodyJson = serializeRemoveRequest(request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/devices/push-token",
            method = HttpMethod.DELETE,
            headers = headers,
            body = bodyJson
        )

        return httpClient.execute(httpRequest).map { response ->
            parseRemoveResponse(response.body)
        }
    }

    fun serializeRegisterRequest(dto: RegisterPushTokenRequestDto): String {
        val sb = StringBuilder("{")
        sb.append("\"token\":").append(escapeJson(dto.token))
        sb.append(",\"platform\":").append(escapeJson(dto.platform.lowercase()))
        if (!dto.deviceId.isNullOrBlank()) {
            sb.append(",\"deviceId\":").append(escapeJson(dto.deviceId))
        }
        if (!dto.appVersion.isNullOrBlank()) {
            sb.append(",\"appVersion\":").append(escapeJson(dto.appVersion))
        }
        sb.append("}")
        return sb.toString()
    }

    fun serializeRemoveRequest(dto: RemovePushTokenRequestDto): String {
        return "{\"token\":${escapeJson(dto.token)}}"
    }

    fun parseRegisterResponse(json: String): RegisterPushTokenResponseDto {
        val dataJson = extractJsonField(json, "data") ?: json
        val tokenMasked = extractJsonString(dataJson, "tokenMasked") ?: ""
        val platform = extractJsonString(dataJson, "platform") ?: "android"
        val isActive = extractJsonBoolean(dataJson, "isActive") ?: true
        val lastSeenAt = extractJsonString(dataJson, "lastSeenAt") ?: ""
        val success = extractJsonBoolean(json, "success") ?: true
        val registered = extractJsonBoolean(json, "registered") ?: true

        return RegisterPushTokenResponseDto(
            tokenMasked = tokenMasked,
            platform = platform,
            isActive = isActive,
            lastSeenAt = lastSeenAt,
            success = success,
            registered = registered
        )
    }

    fun parseRemoveResponse(json: String): RemovePushTokenResponseDto {
        val dataJson = extractJsonField(json, "data") ?: json
        val removed = extractJsonBoolean(dataJson, "removed")
            ?: extractJsonBoolean(json, "removed")
            ?: true
        val success = extractJsonBoolean(json, "success") ?: true
        return RemovePushTokenResponseDto(success = success, removed = removed)
    }

    private fun escapeJson(str: String): String {
        val escaped = str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
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
