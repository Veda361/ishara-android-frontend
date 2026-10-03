package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.ResolvedLocationDto

/**
 * Remote data source for locations and geocoding API endpoints:
 * - GET /api/v1/locations/search
 */
interface LocationRemoteDataSource {
    suspend fun searchLocations(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Double? = null,
        limit: Int = 5,
        token: String? = null
    ): IshaaraResult<List<ResolvedLocationDto>>
}

class LocationRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : LocationRemoteDataSource {

    override suspend fun searchLocations(
        query: String,
        latitude: Double?,
        longitude: Double?,
        radius: Double?,
        limit: Int,
        token: String?
    ): IshaaraResult<List<ResolvedLocationDto>> {
        val queryParams = mutableMapOf(
            "q" to query,
            "limit" to limit.toString()
        )
        if (latitude != null) queryParams["latitude"] = latitude.toString()
        if (longitude != null) queryParams["longitude"] = longitude.toString()
        if (radius != null) queryParams["radius"] = radius.toString()

        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/locations/search",
            method = HttpMethod.GET,
            headers = headers,
            queryParams = queryParams
        )

        return httpClient.execute(request).map { response ->
            parseLocationList(response.body)
        }
    }

    /**
     * Pure Kotlin JSON parser for location arrays.
     * Guarantees identical execution across Android runtimes and JVM unit test runners.
     */
    internal fun parseLocationList(json: String): List<ResolvedLocationDto> {
        if (json.isBlank()) return emptyList()

        // Locate data array
        val dataIndex = json.indexOf("\"data\"")
        if (dataIndex == -1) return emptyList()

        val arrayStartIndex = json.indexOf('[', dataIndex)
        if (arrayStartIndex == -1) return emptyList()

        val arrayEndIndex = json.lastIndexOf(']')
        if (arrayEndIndex == -1 || arrayEndIndex <= arrayStartIndex) return emptyList()

        val arrayContent = json.substring(arrayStartIndex + 1, arrayEndIndex).trim()
        if (arrayContent.isEmpty()) return emptyList()

        // Extract individual JSON objects by tracking brace depth
        val objects = mutableListOf<String>()
        var depth = 0
        var startIndex = -1
        var inString = false
        var isEscaped = false

        for (i in arrayContent.indices) {
            val char = arrayContent[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (char == '{') {
                    if (depth == 0) startIndex = i
                    depth++
                } else if (char == '}') {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        objects.add(arrayContent.substring(startIndex, i + 1))
                        startIndex = -1
                    }
                }
            }
        }

        return objects.mapNotNull { objJson ->
            parseResolvedLocation(objJson)
        }
    }

    private fun parseResolvedLocation(json: String): ResolvedLocationDto? {
        val latStr = extractNumericField(json, "latitude") ?: return null
        val lngStr = extractNumericField(json, "longitude") ?: return null
        val lat = latStr.toDoubleOrNull() ?: return null
        val lng = lngStr.toDoubleOrNull() ?: return null

        val formattedAddress = extractStringField(json, "formattedAddress") ?: ""
        val displayName = extractStringField(json, "displayName")
        val provider = extractStringField(json, "provider")
        val googlePlaceId = extractStringField(json, "googlePlaceId")
        val serpApiDataId = extractStringField(json, "serpApiDataId")
        val serpApiDataCid = extractStringField(json, "serpApiDataCid")
        val city = extractStringField(json, "city")
        val state = extractStringField(json, "state")
        val country = extractStringField(json, "country")

        return ResolvedLocationDto(
            latitude = lat,
            longitude = lng,
            formattedAddress = formattedAddress,
            displayName = displayName,
            provider = provider,
            googlePlaceId = googlePlaceId,
            serpApiDataId = serpApiDataId,
            serpApiDataCid = serpApiDataCid,
            city = city,
            state = state,
            country = country
        )
    }

    private fun extractStringField(json: String, field: String): String? {
        val pattern = Regex("\"$field\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        return pattern.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"")
    }

    private fun extractNumericField(json: String, field: String): String? {
        val pattern = Regex("\"$field\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)")
        return pattern.find(json)?.groupValues?.get(1)
    }
}
