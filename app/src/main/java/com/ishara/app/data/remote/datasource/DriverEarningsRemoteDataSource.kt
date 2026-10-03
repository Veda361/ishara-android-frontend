package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DriverEarningsPaginationDto
import com.ishara.app.data.remote.dto.DriverEarningsPeriodDto
import com.ishara.app.data.remote.dto.DriverEarningsResponseDto
import com.ishara.app.data.remote.dto.DriverEarningsSummaryDto
import com.ishara.app.data.remote.dto.DriverRideEarningsItemDto
import com.ishara.app.data.remote.dto.DriverSettlementSummaryDto

interface DriverEarningsRemoteDataSource {
    suspend fun getDriverEarnings(
        period: String,
        from: String?,
        to: String?,
        timezone: String,
        page: Int,
        limit: Int,
        token: String
    ): IshaaraResult<DriverEarningsResponseDto>

    suspend fun getDriverRidesWithFinancials(
        status: String?,
        tripId: String?,
        period: String?,
        from: String?,
        to: String?,
        timezone: String,
        page: Int,
        limit: Int,
        token: String
    ): IshaaraResult<DriverEarningsResponseDto>
}

class DriverEarningsRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DriverEarningsRemoteDataSource {

    override suspend fun getDriverEarnings(
        period: String,
        from: String?,
        to: String?,
        timezone: String,
        page: Int,
        limit: Int,
        token: String
    ): IshaaraResult<DriverEarningsResponseDto> {
        val queryParams = mutableMapOf(
            "period" to period,
            "timezone" to timezone,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        if (!from.isNullOrBlank()) queryParams["from"] = from
        if (!to.isNullOrBlank()) queryParams["to"] = to

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/earnings",
            method = HttpMethod.GET,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            ),
            queryParams = queryParams
        )

        return httpClient.execute(request).map { response ->
            parseEarningsResponse(response.body)
        }
    }

    override suspend fun getDriverRidesWithFinancials(
        status: String?,
        tripId: String?,
        period: String?,
        from: String?,
        to: String?,
        timezone: String,
        page: Int,
        limit: Int,
        token: String
    ): IshaaraResult<DriverEarningsResponseDto> {
        val queryParams = mutableMapOf(
            "withFinancials" to "true",
            "timezone" to timezone,
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        if (!status.isNullOrBlank()) queryParams["status"] = status
        if (!tripId.isNullOrBlank()) queryParams["tripId"] = tripId
        if (!period.isNullOrBlank()) queryParams["period"] = period
        if (!from.isNullOrBlank()) queryParams["from"] = from
        if (!to.isNullOrBlank()) queryParams["to"] = to

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/rides",
            method = HttpMethod.GET,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            ),
            queryParams = queryParams
        )

        return httpClient.execute(request).map { response ->
            parseRidesListResponse(response.body, period ?: "custom")
        }
    }

    // =========================================================================
    // JSON Parsing Logic (Decoupled from Android Framework for Pure Unit Tests)
    // =========================================================================

    fun parseEarningsResponse(json: String): DriverEarningsResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json

        val periodObj = extractJsonObject(dataObj, "period")
        val periodDto = if (periodObj != null) parsePeriod(periodObj) else DriverEarningsPeriodDto()

        val summaryObj = extractJsonObject(dataObj, "summary")
        val summaryDto = if (summaryObj != null) parseSummary(summaryObj) else DriverEarningsSummaryDto()

        val itemsArrayStr = extractJsonArray(dataObj, "items")
        val itemsList = if (itemsArrayStr != null) {
            val itemJsonList = splitJsonObjectsInArray(itemsArrayStr)
            itemJsonList.map { parseRideEarningsItem(it) }
        } else {
            emptyList()
        }

        val paginationObj = extractJsonObject(dataObj, "pagination")
        val paginationDto = if (paginationObj != null) parsePagination(paginationObj) else DriverEarningsPaginationDto(
            total = itemsList.size,
            page = 1,
            limit = 20,
            hasMore = false
        )

        return DriverEarningsResponseDto(
            period = periodDto,
            summary = summaryDto,
            items = itemsList,
            pagination = paginationDto
        )
    }

    private fun parsePeriod(obj: String): DriverEarningsPeriodDto {
        return DriverEarningsPeriodDto(
            period = extractStringValue(obj, "period") ?: "today",
            from = extractStringValue(obj, "from") ?: "",
            to = extractStringValue(obj, "to") ?: "",
            timezone = extractStringValue(obj, "timezone") ?: "Asia/Kolkata"
        )
    }

    private fun parseSummary(obj: String): DriverEarningsSummaryDto {
        val gross = extractLongValue(obj, "grossEarningsMinor") ?: 0L
        val platformFee = extractLongValue(obj, "platformDeductionsMinor") ?: 0L
        val net = extractLongValue(obj, "netEarningsMinor") ?: 0L
        val refunds = extractLongValue(obj, "refundDeductionsMinor") ?: 0L
        val count = extractIntValue(obj, "completedRidesCount") ?: 0
        val currency = extractStringValue(obj, "currency") ?: "INR"

        val settlementObj = extractJsonObject(obj, "settlementSummary")
        val settlementDto = if (settlementObj != null) parseSettlementSummary(settlementObj) else DriverSettlementSummaryDto()

        return DriverEarningsSummaryDto(
            grossEarningsMinor = gross,
            platformDeductionsMinor = platformFee,
            netEarningsMinor = net,
            refundDeductionsMinor = refunds,
            completedRidesCount = count,
            settlementSummary = settlementDto,
            currency = currency
        )
    }

    private fun parseSettlementSummary(obj: String): DriverSettlementSummaryDto {
        return DriverSettlementSummaryDto(
            settledAmountMinor = extractLongValue(obj, "settledAmountMinor") ?: 0L,
            pendingSettlementAmountMinor = extractLongValue(obj, "pendingSettlementAmountMinor") ?: 0L,
            unreadySettlementAmountMinor = extractLongValue(obj, "unreadySettlementAmountMinor") ?: 0L,
            failedSettlementAmountMinor = extractLongValue(obj, "failedSettlementAmountMinor") ?: 0L
        )
    }

    private fun parseRideEarningsItem(obj: String): DriverRideEarningsItemDto {
        return DriverRideEarningsItemDto(
            rideId = extractStringValue(obj, "rideId") ?: "",
            tripId = extractStringValue(obj, "tripId") ?: "",
            completedAt = extractStringValue(obj, "completedAt"),
            pickupAddress = extractStringValue(obj, "pickupAddress") ?: "",
            destinationAddress = extractStringValue(obj, "destinationAddress") ?: "",
            grossAmountMinor = extractLongValue(obj, "grossAmountMinor") ?: 0L,
            platformFeeMinor = extractLongValue(obj, "platformFeeMinor") ?: 0L,
            netAmountMinor = extractLongValue(obj, "netAmountMinor") ?: 0L,
            currency = extractStringValue(obj, "currency") ?: "INR",
            paymentStatus = extractStringValue(obj, "paymentStatus") ?: "PENDING",
            settlementStatus = extractStringValue(obj, "settlementStatus") ?: "UNSETTLED"
        )
    }

    private fun parsePagination(obj: String): DriverEarningsPaginationDto {
        return DriverEarningsPaginationDto(
            total = extractIntValue(obj, "total") ?: 0,
            page = extractIntValue(obj, "page") ?: 1,
            limit = extractIntValue(obj, "limit") ?: 20,
            hasMore = extractBooleanValue(obj, "hasMore") ?: false
        )
    }

    private fun parseRidesListResponse(json: String, periodStr: String): DriverEarningsResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        val itemsArrayStr = extractJsonArray(dataObj, "items")
        val items = if (itemsArrayStr != null) {
            val itemJsonList = splitJsonObjectsInArray(itemsArrayStr)
            itemJsonList.map { parseRideFromRidesApi(it) }
        } else {
            emptyList()
        }

        val total = extractIntValue(dataObj, "total") ?: items.size
        val page = extractIntValue(dataObj, "page") ?: 1
        val limit = extractIntValue(dataObj, "limit") ?: 20
        val hasMore = extractBooleanValue(dataObj, "hasMore") ?: false

        return DriverEarningsResponseDto(
            period = DriverEarningsPeriodDto(period = periodStr),
            summary = DriverEarningsSummaryDto(
                completedRidesCount = total
            ),
            items = items,
            pagination = DriverEarningsPaginationDto(
                total = total,
                page = page,
                limit = limit,
                hasMore = hasMore
            )
        )
    }

    private fun parseRideFromRidesApi(obj: String): DriverRideEarningsItemDto {
        val rideId = extractStringValue(obj, "id") ?: extractStringValue(obj, "rideId") ?: ""
        val tripId = extractStringValue(obj, "tripId") ?: ""
        val completedAt = extractStringValue(obj, "completedAt")

        val pickupObj = extractJsonObject(obj, "pickup")
        val pickupAddress = if (pickupObj != null) {
            extractStringValue(pickupObj, "formattedAddress") ?: ""
        } else {
            ""
        }

        val destObj = extractJsonObject(obj, "destination")
        val destAddress = if (destObj != null) {
            extractStringValue(destObj, "formattedAddress") ?: ""
        } else {
            ""
        }

        val financialStatusObj = extractJsonObject(obj, "financialStatus")
        val gross = if (financialStatusObj != null) extractLongValue(financialStatusObj, "grossAmountMinor") ?: 0L else 0L
        val fee = if (financialStatusObj != null) extractLongValue(financialStatusObj, "platformFeeMinor") ?: 0L else 0L
        val net = if (financialStatusObj != null) extractLongValue(financialStatusObj, "netAmountMinor") ?: 0L else 0L
        val curr = if (financialStatusObj != null) extractStringValue(financialStatusObj, "currency") ?: "INR" else "INR"
        val payStatus = if (financialStatusObj != null) extractStringValue(financialStatusObj, "paymentStatus") ?: "PENDING" else "PENDING"
        val setStatus = if (financialStatusObj != null) extractStringValue(financialStatusObj, "settlementStatus") ?: "UNSETTLED" else "UNSETTLED"

        return DriverRideEarningsItemDto(
            rideId = rideId,
            tripId = tripId,
            completedAt = completedAt,
            pickupAddress = pickupAddress,
            destinationAddress = destAddress,
            grossAmountMinor = gross,
            platformFeeMinor = fee,
            netAmountMinor = net,
            currency = curr,
            paymentStatus = payStatus,
            settlementStatus = setStatus
        )
    }

    // Helper functions
    private fun extractStringValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractLongValue(json: String, key: String): Long? {
        val pattern = "\"$key\"\\s*:\\s*([0-9-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractIntValue(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*([0-9-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractBooleanValue(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val braceStartIndex = json.indexOf('{', colonIndex)
        if (braceStartIndex == -1) return null

        var depth = 0
        var inString = false
        var isEscaped = false

        for (i in braceStartIndex until json.length) {
            val char = json[i]
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
                if (char == '{') depth++
                if (char == '}') {
                    depth--
                    if (depth == 0) {
                        return json.substring(braceStartIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun extractJsonArray(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val bracketStartIndex = json.indexOf('[', colonIndex)
        if (bracketStartIndex == -1) return null

        var depth = 0
        var inString = false
        var isEscaped = false

        for (i in bracketStartIndex until json.length) {
            val char = json[i]
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
                if (char == '[') depth++
                if (char == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(bracketStartIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun splitJsonObjectsInArray(arrayJson: String): List<String> {
        val trimmed = arrayJson.trim()
        val startIndex = trimmed.indexOf('[')
        val endIndex = trimmed.lastIndexOf(']')
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) return emptyList()

        val content = trimmed.substring(startIndex + 1, endIndex)
        val objects = mutableListOf<String>()
        var depth = 0
        var objStart = -1
        var inString = false
        var isEscaped = false

        for (i in content.indices) {
            val char = content[i]
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
                    if (depth == 0) objStart = i
                    depth++
                } else if (char == '}') {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        objects.add(content.substring(objStart, i + 1).trim())
                        objStart = -1
                    }
                }
            }
        }
        return objects
    }
}
