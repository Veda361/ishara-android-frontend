package com.ishara.app

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.DeviceTokenMapper
import com.ishara.app.data.mapper.NotificationMapper
import com.ishara.app.data.remote.datasource.DeviceTokenRemoteDataSourceImpl
import com.ishara.app.data.remote.datasource.NotificationRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.MarkAllAsReadResponseDto
import com.ishara.app.data.remote.dto.NotificationItemDto
import com.ishara.app.data.remote.dto.PaginatedNotificationsResponseDto
import com.ishara.app.data.remote.dto.RegisterPushTokenRequestDto
import com.ishara.app.data.remote.dto.RegisterPushTokenResponseDto
import com.ishara.app.data.remote.dto.RemovePushTokenRequestDto
import com.ishara.app.data.remote.dto.RemovePushTokenResponseDto
import com.ishara.app.data.remote.dto.UnreadCountResponseDto
import com.ishara.app.domain.model.NotificationCategory
import com.ishara.app.domain.model.NotificationPriority
import com.ishara.app.domain.model.NotificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strict backend contract test suite for Phase A15: Notifications & Device Tokens.
 * Validates request serialization, response parsing, and domain mapping against backend contract.
 */
class NotificationStrictContractTest {

    private val sampleNotificationJson = """
        {
          "status": "success",
          "data": {
            "id": "notif_001",
            "userId": "usr_abc123",
            "type": "RIDE_COMPLETED",
            "title": "Ride Completed",
            "body": "Your trip to City Center has arrived.",
            "data": {
              "rideId": "ride_999",
              "tripId": "trip_456"
            },
            "sourceEventId": "evt_777",
            "aggregateType": "Ride",
            "aggregateId": "ride_999",
            "status": "UNREAD",
            "priority": "HIGH",
            "category": "rideUpdates",
            "readAt": null,
            "createdAt": "2026-10-01T14:30:00.000Z",
            "updatedAt": "2026-10-01T14:30:00.000Z"
          }
        }
    """.trimIndent()

    private val samplePaginatedNotificationsJson = """
        {
          "status": "success",
          "data": {
            "items": [
              {
                "id": "notif_001",
                "userId": "usr_abc123",
                "type": "RIDE_REQUEST_ACCEPTED",
                "title": "Ride Accepted",
                "body": "Driver is on the way.",
                "data": {
                  "rideId": "ride_101",
                  "requestId": "req_202"
                },
                "sourceEventId": "evt_888",
                "aggregateType": "RideRequest",
                "aggregateId": "req_202",
                "status": "UNREAD",
                "priority": "HIGH",
                "category": "rideUpdates",
                "readAt": null,
                "createdAt": "2026-10-01T15:00:00.000Z",
                "updatedAt": "2026-10-01T15:00:00.000Z"
              },
              {
                "id": "notif_002",
                "userId": "usr_abc123",
                "type": "PAYMENT_CAPTURED",
                "title": "Payment Confirmed",
                "body": "Fare of ₹45 captured successfully.",
                "data": {
                  "paymentId": "pay_303",
                  "rideId": "ride_101"
                },
                "sourceEventId": "evt_999",
                "aggregateType": "Payment",
                "aggregateId": "pay_303",
                "status": "READ",
                "priority": "NORMAL",
                "category": "system",
                "readAt": "2026-10-01T15:10:00.000Z",
                "createdAt": "2026-10-01T15:05:00.000Z",
                "updatedAt": "2026-10-01T15:10:00.000Z"
              }
            ],
            "total": 2,
            "page": 1,
            "limit": 20,
            "hasMore": false,
            "unreadCount": 1
          }
        }
    """.trimIndent()

    private val sampleUnreadCountJson = """
        {
          "status": "success",
          "data": {
            "unreadCount": 3
          }
        }
    """.trimIndent()

    private val sampleMarkAllAsReadJson = """
        {
          "status": "success",
          "data": {
            "markedCount": 5
          }
        }
    """.trimIndent()

    private val sampleRegisterPushTokenSuccessJson = """
        {
          "success": true,
          "registered": true,
          "data": {
            "tokenMasked": "fcm_tok...xyz",
            "platform": "android",
            "isActive": true,
            "lastSeenAt": "2026-10-01T16:00:00.000Z"
          }
        }
    """.trimIndent()

    private val sampleRemovePushTokenSuccessJson = """
        {
          "success": true,
          "removed": true
        }
    """.trimIndent()

    private val networkConfig = NetworkConfig()

    private class MockHttpClient(
        private val handler: (HttpRequest) -> HttpResponse
    ) : IshaaraHttpClient {
        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            return try {
                IshaaraResult.success(handler(request))
            } catch (e: Exception) {
                IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Network("Network failure", e))
            }
        }
    }

    @Test
    fun `parseNotificationItem parses full DTO accurately`() {
        val dataSource = NotificationRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val dto = dataSource.parseNotificationItemResponse(sampleNotificationJson)

        assertEquals("notif_001", dto.id)
        assertEquals("usr_abc123", dto.userId)
        assertEquals("RIDE_COMPLETED", dto.type)
        assertEquals("Ride Completed", dto.title)
        assertEquals("Your trip to City Center has arrived.", dto.body)
        assertEquals("ride_999", dto.data["rideId"])
        assertEquals("trip_456", dto.data["tripId"])
        assertEquals("evt_777", dto.sourceEventId)
        assertEquals("Ride", dto.aggregateType)
        assertEquals("ride_999", dto.aggregateId)
        assertEquals("UNREAD", dto.status)
        assertEquals("HIGH", dto.priority)
        assertEquals("rideUpdates", dto.category)
        assertNull(dto.readAt)
        assertEquals("2026-10-01T14:30:00.000Z", dto.createdAt)
    }

    @Test
    fun `NotificationMapper maps DTO to domain model correctly`() {
        val dto = NotificationItemDto(
            id = "notif_001",
            userId = "usr_abc123",
            type = "RIDE_COMPLETED",
            title = "Ride Completed",
            body = "Your trip arrived.",
            data = mapOf("rideId" to "ride_999"),
            sourceEventId = "evt_777",
            aggregateType = "Ride",
            aggregateId = "ride_999",
            status = "UNREAD",
            priority = "HIGH",
            readAt = null,
            createdAt = "2026-10-01T14:30:00.000Z",
            updatedAt = "2026-10-01T14:30:00.000Z",
            category = "rideUpdates"
        )

        val domain = NotificationMapper.toDomain(dto)

        assertEquals("notif_001", domain.id)
        assertEquals(NotificationStatus.UNREAD, domain.status)
        assertEquals(NotificationPriority.HIGH, domain.priority)
        assertEquals(NotificationCategory.RIDE_UPDATES, domain.category)
        assertFalse(domain.isRead)
        assertEquals("ride_999", domain.rideId)
    }

    @Test
    fun `parsePaginatedNotificationsResponse parses multi-item array and counts`() {
        val dataSource = NotificationRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val paginated = dataSource.parsePaginatedNotificationsResponse(samplePaginatedNotificationsJson)

        assertEquals(2, paginated.total)
        assertEquals(1, paginated.page)
        assertEquals(20, paginated.limit)
        assertFalse(paginated.hasMore)
        assertEquals(1, paginated.unreadCount)
        assertEquals(2, paginated.items.size)

        val item1 = paginated.items[0]
        assertEquals("notif_001", item1.id)
        assertEquals("RIDE_REQUEST_ACCEPTED", item1.type)
        assertEquals("UNREAD", item1.status)

        val item2 = paginated.items[1]
        assertEquals("notif_002", item2.id)
        assertEquals("PAYMENT_CAPTURED", item2.type)
        assertEquals("READ", item2.status)
        assertEquals("system", item2.category)
    }

    @Test
    fun `parseUnreadCountResponse correctly parses count`() {
        val dataSource = NotificationRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val res = dataSource.parseUnreadCountResponse(sampleUnreadCountJson)
        assertEquals(3, res.unreadCount)
    }

    @Test
    fun `parseMarkAllAsReadResponse correctly parses marked count`() {
        val dataSource = NotificationRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val res = dataSource.parseMarkAllAsReadResponse(sampleMarkAllAsReadJson)
        assertEquals(5, res.markedCount)
    }

    @Test
    fun `serializeRegisterRequest enforces android platform and valid JSON`() {
        val dataSource = DeviceTokenRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val req = RegisterPushTokenRequestDto(
            token = "fcm_test_token_abc_123",
            platform = "android",
            deviceId = "dev_pxl_8",
            appVersion = "1.0.0"
        )
        val json = dataSource.serializeRegisterRequest(req)

        assertTrue(json.contains("\"token\":\"fcm_test_token_abc_123\""))
        assertTrue(json.contains("\"platform\":\"android\""))
        assertTrue(json.contains("\"deviceId\":\"dev_pxl_8\""))
        assertTrue(json.contains("\"appVersion\":\"1.0.0\""))
    }

    @Test
    fun `serializeRemoveRequest correctly serializes token`() {
        val dataSource = DeviceTokenRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val req = RemovePushTokenRequestDto(token = "fcm_delete_token")
        val json = dataSource.serializeRemoveRequest(req)
        assertEquals("{\"token\":\"fcm_delete_token\"}", json)
    }

    @Test
    fun `parseRegisterResponse parses tokenMasked, success, and registered fields`() {
        val dataSource = DeviceTokenRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val res = dataSource.parseRegisterResponse(sampleRegisterPushTokenSuccessJson)

        assertEquals("fcm_tok...xyz", res.tokenMasked)
        assertEquals("android", res.platform)
        assertTrue(res.isActive)
        assertTrue(res.success)
        assertTrue(res.registered)
    }

    @Test
    fun `parseRemoveResponse parses removed boolean`() {
        val dataSource = DeviceTokenRemoteDataSourceImpl(MockHttpClient { HttpResponse(200, "") }, networkConfig)
        val res = dataSource.parseRemoveResponse(sampleRemovePushTokenSuccessJson)

        assertTrue(res.success)
        assertTrue(res.removed)
    }

    @Test
    fun `NotificationCategory maps authoritative backend strings`() {
        assertEquals(NotificationCategory.RIDE_UPDATES, NotificationCategory.fromBackend("rideUpdates"))
        assertEquals(NotificationCategory.ACCOUNT, NotificationCategory.fromBackend("account"))
        assertEquals(NotificationCategory.SYSTEM, NotificationCategory.fromBackend("system"))
        // Fallback for unknown
        assertEquals(NotificationCategory.SYSTEM, NotificationCategory.fromBackend("unknownCategory"))
    }

    @Test
    fun `DeviceTokenMapper maps DTO to domain model`() {
        val dto = RegisterPushTokenResponseDto(
            tokenMasked = "fcm_mask_99",
            platform = "android",
            isActive = true,
            lastSeenAt = "2026-10-01T12:00:00Z",
            success = true,
            registered = true
        )
        val domain = DeviceTokenMapper.toDomain(dto)

        assertEquals("fcm_mask_99", domain.tokenMasked)
        assertEquals("android", domain.platform)
        assertTrue(domain.isActive)
        assertTrue(domain.registered)
    }
}
