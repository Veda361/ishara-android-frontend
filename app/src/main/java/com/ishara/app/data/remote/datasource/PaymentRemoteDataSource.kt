package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CheckoutSessionDto
import com.ishara.app.data.remote.dto.CreatePaymentOrderRequestDto
import com.ishara.app.data.remote.dto.PaymentRecordDto
import com.ishara.app.data.remote.dto.VerifyPaymentRequestDto

interface PaymentRemoteDataSource {
    suspend fun createPaymentOrder(
        rideId: String,
        request: CreatePaymentOrderRequestDto,
        idempotencyKey: String?,
        token: String?
    ): IshaaraResult<CheckoutSessionDto>

    suspend fun getPaymentByRideId(
        rideId: String,
        token: String?
    ): IshaaraResult<PaymentRecordDto>

    suspend fun verifyPayment(
        paymentId: String,
        request: VerifyPaymentRequestDto,
        token: String?
    ): IshaaraResult<PaymentRecordDto>
}

class PaymentRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : PaymentRemoteDataSource {

    override suspend fun createPaymentOrder(
        rideId: String,
        request: CreatePaymentOrderRequestDto,
        idempotencyKey: String?,
        token: String?
    ): IshaaraResult<CheckoutSessionDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        if (!idempotencyKey.isNullOrBlank()) {
            headers["Idempotency-Key"] = idempotencyKey
        }

        val jsonBody = serializeCreateOrderRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/payment",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseCheckoutSessionResponse(response.body)
        }
    }

    override suspend fun getPaymentByRideId(
        rideId: String,
        token: String?
    ): IshaaraResult<PaymentRecordDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/payment",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parsePaymentRecordResponse(response.body)
        }
    }

    override suspend fun verifyPayment(
        paymentId: String,
        request: VerifyPaymentRequestDto,
        token: String?
    ): IshaaraResult<PaymentRecordDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val jsonBody = serializeVerifyRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/payments/$paymentId/verify",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parsePaymentRecordResponse(response.body)
        }
    }

    private fun serializeCreateOrderRequest(dto: CreatePaymentOrderRequestDto): String {
        return if (!dto.idempotencyKey.isNullOrBlank()) {
            """{"idempotencyKey":"${escapeJson(dto.idempotencyKey)}"}"""
        } else {
            "{}"
        }
    }

    private fun serializeVerifyRequest(dto: VerifyPaymentRequestDto): String {
        return """{"providerOrderId":"${escapeJson(dto.providerOrderId)}","providerPaymentId":"${escapeJson(dto.providerPaymentId)}","signature":"${escapeJson(dto.signature)}"}"""
    }

    fun parseCheckoutSessionResponse(json: String): CheckoutSessionDto {
        try {
            val dataObj = extractJsonObject(json, "data") ?: json

            val paymentId = extractStringValue(dataObj, "paymentId")
                ?: throw IllegalArgumentException("Missing paymentId in checkout session")
            val rideId = extractStringValue(dataObj, "rideId")
                ?: throw IllegalArgumentException("Missing rideId in checkout session")
            val grossAmountMinor = extractLongValue(dataObj, "grossAmountMinor")
                ?: throw IllegalArgumentException("Missing grossAmountMinor in checkout session")
            val currency = extractStringValue(dataObj, "currency") ?: "INR"
            val provider = extractStringValue(dataObj, "provider") ?: "razorpay"
            val providerOrderId = extractStringValue(dataObj, "providerOrderId")
                ?: throw IllegalArgumentException("Missing providerOrderId in checkout session")
            val keyId = extractStringValue(dataObj, "keyId")
            val qrPayload = extractStringValue(dataObj, "qrPayload")
            val expiresAt = extractStringValue(dataObj, "expiresAt") ?: ""

            return CheckoutSessionDto(
                paymentId = paymentId,
                rideId = rideId,
                grossAmountMinor = grossAmountMinor,
                currency = currency,
                provider = provider,
                providerOrderId = providerOrderId,
                keyId = keyId,
                qrPayload = qrPayload,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse checkout session response: ${e.message}", e)
        }
    }

    fun parsePaymentRecordResponse(json: String): PaymentRecordDto {
        try {
            val dataObj = extractJsonObject(json, "data") ?: json

            val id = extractStringValue(dataObj, "id")
                ?: throw IllegalArgumentException("Missing id in payment record")
            val rideId = extractStringValue(dataObj, "rideId")
                ?: throw IllegalArgumentException("Missing rideId in payment record")
            val userId = extractStringValue(dataObj, "userId") ?: ""
            val driverId = extractStringValue(dataObj, "driverId") ?: ""
            val grossAmountMinor = extractLongValue(dataObj, "grossAmountMinor") ?: 0L
            val platformFeeMinor = extractLongValue(dataObj, "platformFeeMinor") ?: 0L
            val providerAmountMinor = extractLongValue(dataObj, "providerAmountMinor")
                ?: (grossAmountMinor - platformFeeMinor)
            val refundedAmountMinor = extractLongValue(dataObj, "refundedAmountMinor") ?: 0L
            val currency = extractStringValue(dataObj, "currency") ?: "INR"
            val status = extractStringValue(dataObj, "status") ?: "CREATED"
            val provider = extractStringValue(dataObj, "provider") ?: "razorpay"
            val providerOrderId = extractStringValue(dataObj, "providerOrderId") ?: ""
            val providerPaymentId = extractStringValue(dataObj, "providerPaymentId")
            val providerSignature = extractStringValue(dataObj, "providerSignature")
            val idempotencyKey = extractStringValue(dataObj, "idempotencyKey")
            val capturedAt = extractStringValue(dataObj, "capturedAt")
            val expiresAt = extractStringValue(dataObj, "expiresAt") ?: ""
            val createdAt = extractStringValue(dataObj, "createdAt") ?: ""
            val updatedAt = extractStringValue(dataObj, "updatedAt") ?: ""

            return PaymentRecordDto(
                id = id,
                rideId = rideId,
                userId = userId,
                driverId = driverId,
                grossAmountMinor = grossAmountMinor,
                platformFeeMinor = platformFeeMinor,
                providerAmountMinor = providerAmountMinor,
                refundedAmountMinor = refundedAmountMinor,
                currency = currency,
                status = status,
                provider = provider,
                providerOrderId = providerOrderId,
                providerPaymentId = providerPaymentId,
                providerSignature = providerSignature,
                idempotencyKey = idempotencyKey,
                capturedAt = capturedAt,
                expiresAt = expiresAt,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse payment record response: ${e.message}", e)
        }
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractStringValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractLongValue(json: String, key: String): Long? {
        val pattern = "\"$key\"\\s*:\\s*([0-9-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
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
}
