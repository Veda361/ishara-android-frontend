package com.ishara.app.data.remote.datasource

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DriverIdentityDto
import com.ishara.app.data.remote.dto.DriverOperationalContextResponseDto
import com.ishara.app.data.remote.dto.DriverProfileResponseDto
import com.ishara.app.data.remote.dto.DriverTodayStatsDto
import com.ishara.app.data.remote.dto.DriverTripDto
import com.ishara.app.data.remote.dto.DriverTripLocationDto
import com.ishara.app.data.remote.dto.DriverGeoJsonPointDto

import com.ishara.app.data.remote.dto.CleanDriverProfileResponseDto
import com.ishara.app.data.remote.dto.CleanDriverVerificationResponseDto
import com.ishara.app.data.remote.dto.CleanEmergencyContactDto
import com.ishara.app.data.remote.dto.CreateDriverProfileRequestDto
import com.ishara.app.data.remote.dto.SubmitDriverVerificationRequestDto
import com.ishara.app.data.remote.dto.UpdateDriverProfileRequestDto

interface DriverRemoteDataSource {
    suspend fun getOperationalContext(timezone: String?, token: String): IshaaraResult<DriverOperationalContextResponseDto>
    suspend fun createDriverProfile(licenseNumber: String, token: String): IshaaraResult<DriverProfileResponseDto>
    suspend fun getDriverProfile(token: String): IshaaraResult<CleanDriverProfileResponseDto>
    suspend fun createDriverProfileFull(request: CreateDriverProfileRequestDto, token: String): IshaaraResult<CleanDriverProfileResponseDto>
    suspend fun updateDriverProfile(request: UpdateDriverProfileRequestDto, token: String): IshaaraResult<CleanDriverProfileResponseDto>
    suspend fun getVerificationStatus(token: String): IshaaraResult<CleanDriverVerificationResponseDto>
    suspend fun submitVerification(notes: String?, token: String): IshaaraResult<CleanDriverVerificationResponseDto>
    suspend fun setOnline(token: String): IshaaraResult<DriverProfileResponseDto>
    suspend fun setOffline(token: String): IshaaraResult<DriverProfileResponseDto>
    suspend fun startTrip(tripId: String, token: String): IshaaraResult<DriverTripDto>
    suspend fun completeTrip(tripId: String, token: String): IshaaraResult<DriverTripDto>
    suspend fun cancelTrip(tripId: String, token: String): IshaaraResult<DriverTripDto>
    suspend fun updateLocation(location: LocationCoordinates, token: String): IshaaraResult<Unit>
}

class DriverRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DriverRemoteDataSource {

    override suspend fun getDriverProfile(token: String): IshaaraResult<CleanDriverProfileResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            parseCleanDriverProfileResponse(response.body)
        }
    }

    override suspend fun createDriverProfileFull(
        request: CreateDriverProfileRequestDto,
        token: String
    ): IshaaraResult<CleanDriverProfileResponseDto> {
        val fields = mutableListOf<String>()
        fields.add("\"licenseNumber\":\"${request.licenseNumber.trim()}\"")
        request.yearsOfExperience?.let { fields.add("\"yearsOfExperience\":$it") }
        request.emergencyContact?.let { ec ->
            val ecFields = mutableListOf(
                "\"name\":\"${ec.name.trim()}\"",
                "\"phoneNumber\":\"${ec.phoneNumber.trim()}\""
            )
            ec.relationship?.let { ecFields.add("\"relationship\":\"${it.trim()}\"") }
            fields.add("\"emergencyContact\":{${ecFields.joinToString(",")}}")
        }
        request.operatingType?.let { fields.add("\"operatingType\":\"$it\"") }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = "{${fields.joinToString(",")}}"
        )
        return httpClient.execute(httpRequest).map { response ->
            parseCleanDriverProfileResponse(response.body)
        }
    }

    override suspend fun updateDriverProfile(
        request: UpdateDriverProfileRequestDto,
        token: String
    ): IshaaraResult<CleanDriverProfileResponseDto> {
        val fields = mutableListOf<String>()
        request.licenseNumber?.let { fields.add("\"licenseNumber\":\"${it.trim()}\"") }
        request.yearsOfExperience?.let { fields.add("\"yearsOfExperience\":$it") }
        request.emergencyContact?.let { ec ->
            val ecFields = mutableListOf(
                "\"name\":\"${ec.name.trim()}\"",
                "\"phoneNumber\":\"${ec.phoneNumber.trim()}\""
            )
            ec.relationship?.let { ecFields.add("\"relationship\":\"${it.trim()}\"") }
            fields.add("\"emergencyContact\":{${ecFields.joinToString(",")}}")
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = "{${fields.joinToString(",")}}"
        )
        return httpClient.execute(httpRequest).map { response ->
            parseCleanDriverProfileResponse(response.body)
        }
    }

    override suspend fun getVerificationStatus(token: String): IshaaraResult<CleanDriverVerificationResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/verification",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            parseCleanDriverVerificationResponse(response.body)
        }
    }

    override suspend fun submitVerification(
        notes: String?,
        token: String
    ): IshaaraResult<CleanDriverVerificationResponseDto> {
        val body = if (!notes.isNullOrBlank()) {
            "{\"notes\":\"${notes.trim()}\"}"
        } else {
            "{}"
        }
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/verification",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = body
        )
        return httpClient.execute(request).map { response ->
            parseCleanDriverVerificationResponse(response.body)
        }
    }

    override suspend fun createDriverProfile(
        licenseNumber: String,
        token: String
    ): IshaaraResult<DriverProfileResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/profile",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = """{"licenseNumber":"$licenseNumber"}"""
        )
        return httpClient.execute(request).map { response ->
            parseDriverProfile(response.body, "OFFLINE")
        }
    }

    override suspend fun getOperationalContext(
        timezone: String?,
        token: String
    ): IshaaraResult<DriverOperationalContextResponseDto> {
        val queryParams = if (!timezone.isNullOrBlank()) mapOf("timezone" to timezone) else emptyMap()
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/operations/context",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token"),
            queryParams = queryParams
        )
        return httpClient.execute(request).map { response ->
            parseOperationalContext(response.body)
        }
    }

    override suspend fun setOnline(token: String): IshaaraResult<DriverProfileResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/status/online",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token"),
            body = "{}"
        )
        return httpClient.execute(request).map { response ->
            parseDriverProfile(response.body, "ONLINE")
        }
    }

    override suspend fun setOffline(token: String): IshaaraResult<DriverProfileResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/status/offline",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token"),
            body = "{}"
        )
        return httpClient.execute(request).map { response ->
            parseDriverProfile(response.body, "OFFLINE")
        }
    }

    override suspend fun startTrip(tripId: String, token: String): IshaaraResult<DriverTripDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/start",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )
        return httpClient.execute(request).map { response ->
            parseTrip(response.body, tripId, "ACTIVE")
        }
    }

    override suspend fun completeTrip(tripId: String, token: String): IshaaraResult<DriverTripDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/complete",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )
        return httpClient.execute(request).map { response ->
            parseTrip(response.body, tripId, "COMPLETED")
        }
    }

    override suspend fun cancelTrip(tripId: String, token: String): IshaaraResult<DriverTripDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/cancel",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )
        return httpClient.execute(request).map { response ->
            parseTrip(response.body, tripId, "CANCELLED")
        }
    }

    override suspend fun updateLocation(
        location: LocationCoordinates,
        token: String
    ): IshaaraResult<Unit> {
        val body = """
            {
                "coordinates": [${location.longitude}, ${location.latitude}],
                "heading": ${location.headingDegrees ?: 0.0},
                "speed": ${location.speedMps ?: 0.0},
                "accuracy": ${location.accuracyMeters ?: 5.0},
                "recordedAt": "${location.toIso8601Utc()}"
            }
        """.trimIndent()

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/location",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = body
        )
        return httpClient.execute(request).map { }
    }

    private fun parseOperationalContext(json: String): DriverOperationalContextResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        val driverJson = extractJsonObject(dataObj, "driver") ?: dataObj
        val id = extractJsonField(driverJson, "id") ?: ""
        val userId = extractJsonField(driverJson, "userId") ?: ""
        val verificationStatus = extractJsonField(driverJson, "verificationStatus") ?: "VERIFIED"
        val status = extractJsonField(driverJson, "status") ?: "ONLINE"
        val licenseNumberMasked = extractJsonField(driverJson, "licenseNumberMasked")
        val licenseVerifiedAt = extractJsonField(driverJson, "licenseVerifiedAt")

        val driver = DriverIdentityDto(
            id = id,
            userId = userId,
            verificationStatus = verificationStatus,
            status = status,
            licenseNumberMasked = licenseNumberMasked,
            licenseVerifiedAt = licenseVerifiedAt
        )

        val statsJson = extractJsonObject(dataObj, "todayStats")
        val completedCount = extractJsonField(statsJson ?: "", "completedRidesCount")?.toIntOrNull() ?: 0
        val isOnlineBool = extractJsonField(statsJson ?: "", "isOnline")?.toBooleanStrictOrNull()
            ?: status.equals("ONLINE", ignoreCase = true)
        val todayDate = extractJsonField(statsJson ?: "", "currentDate") ?: "2026-09-26"
        val tz = extractJsonField(statsJson ?: "", "timezone") ?: "Asia/Kolkata"

        return DriverOperationalContextResponseDto(
            driver = driver,
            vehicle = null,
            activeTrip = null,
            activeRidesCount = 0,
            todayStats = DriverTodayStatsDto(
                completedRidesCount = completedCount,
                isOnline = isOnlineBool,
                currentDate = todayDate,
                timezone = tz
            )
        )
    }

    private fun parseCleanDriverProfileResponse(json: String): CleanDriverProfileResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        val ecJson = extractJsonObject(dataObj, "emergencyContact")
        val ec = if (ecJson != null) {
            val name = extractJsonField(ecJson, "name") ?: ""
            val phone = extractJsonField(ecJson, "phoneNumberMasked") ?: extractJsonField(ecJson, "phoneNumber")
            val rel = extractJsonField(ecJson, "relationship")
            CleanEmergencyContactDto(name, phone, rel)
        } else null

        val isSusp = extractJsonField(dataObj, "isSuspended")?.toBooleanStrictOrNull() ?: false

        return CleanDriverProfileResponseDto(
            id = extractJsonField(dataObj, "id") ?: extractJsonField(dataObj, "_id") ?: "",
            userId = extractJsonField(dataObj, "userId") ?: "",
            verificationStatus = extractJsonField(dataObj, "verificationStatus") ?: "PENDING",
            status = extractJsonField(dataObj, "status") ?: "OFFLINE",
            licenseNumberMasked = extractJsonField(dataObj, "licenseNumberMasked"),
            licenseVerifiedAt = extractJsonField(dataObj, "licenseVerifiedAt"),
            submittedAt = extractJsonField(dataObj, "submittedAt"),
            reviewedAt = extractJsonField(dataObj, "reviewedAt"),
            reviewedBy = extractJsonField(dataObj, "reviewedBy"),
            rejectionReason = extractJsonField(dataObj, "rejectionReason"),
            yearsOfExperience = extractJsonField(dataObj, "yearsOfExperience")?.toIntOrNull(),
            emergencyContact = ec,
            operatingType = extractJsonField(dataObj, "operatingType"),
            isSuspended = isSusp,
            suspensionReason = extractJsonField(dataObj, "suspensionReason"),
            createdAt = extractJsonField(dataObj, "createdAt"),
            updatedAt = extractJsonField(dataObj, "updatedAt")
        )
    }

    private fun parseCleanDriverVerificationResponse(json: String): CleanDriverVerificationResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        return CleanDriverVerificationResponseDto(
            driverId = extractJsonField(dataObj, "driverId") ?: extractJsonField(dataObj, "id") ?: "",
            userId = extractJsonField(dataObj, "userId") ?: "",
            verificationStatus = extractJsonField(dataObj, "verificationStatus") ?: "PENDING",
            submittedAt = extractJsonField(dataObj, "submittedAt"),
            reviewedAt = extractJsonField(dataObj, "reviewedAt"),
            reviewedBy = extractJsonField(dataObj, "reviewedBy"),
            rejectionReason = extractJsonField(dataObj, "rejectionReason"),
            licenseNumberMasked = extractJsonField(dataObj, "licenseNumberMasked"),
            operatingType = extractJsonField(dataObj, "operatingType"),
            createdAt = extractJsonField(dataObj, "createdAt"),
            updatedAt = extractJsonField(dataObj, "updatedAt")
        )
    }

    private fun parseDriverProfile(json: String, fallbackStatus: String): DriverProfileResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        return DriverProfileResponseDto(
            id = extractJsonField(dataObj, "id") ?: "",
            userId = extractJsonField(dataObj, "userId") ?: "",
            verificationStatus = extractJsonField(dataObj, "verificationStatus") ?: "VERIFIED",
            status = extractJsonField(dataObj, "status") ?: fallbackStatus,
            licenseNumberMasked = extractJsonField(dataObj, "licenseNumberMasked"),
            licenseVerifiedAt = extractJsonField(dataObj, "licenseVerifiedAt")
        )
    }

    private fun parseTrip(json: String, tripId: String, status: String): DriverTripDto {
        return DriverTripDto(
            id = extractJsonField(json, "id") ?: tripId,
            driverId = extractJsonField(json, "driverId") ?: "",
            vehicleId = extractJsonField(json, "vehicleId") ?: "",
            origin = DriverTripLocationDto(
                formattedAddress = "Origin",
                coordinates = DriverGeoJsonPointDto(coordinates = doubleArrayOf(0.0, 0.0))
            ),
            destination = DriverTripLocationDto(
                formattedAddress = "Destination",
                coordinates = DriverGeoJsonPointDto(coordinates = doubleArrayOf(0.0, 0.0))
            ),
            status = extractJsonField(json, "status") ?: status
        )
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = Regex("\"$field\"\\s*:\\s*\"?([^,\"}]+)\"?")
        return pattern.find(json)?.groupValues?.get(1)?.trim()
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null
        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null
        val braceIndex = json.indexOf('{', colonIndex)
        if (braceIndex == -1) return null
        val endIndex = findClosingChar(json, braceIndex, '{', '}')
        if (endIndex == -1) return null
        return json.substring(braceIndex, endIndex + 1)
    }

    private fun findClosingChar(text: String, startIndex: Int, openChar: Char, closeChar: Char): Int {
        var depth = 0
        var inQuotes = false
        var isEscaped = false

        for (i in startIndex until text.length) {
            val char = text[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == openChar) depth++
                if (char == closeChar) {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
