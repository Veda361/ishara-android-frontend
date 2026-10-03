package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.remote.datasource.SafetyRemoteDataSource
import com.ishara.app.data.remote.dto.CancelSosRequestDto
import com.ishara.app.data.remote.dto.CreateEmergencyContactRequestDto
import com.ishara.app.data.remote.dto.CreateSosRequestDto
import com.ishara.app.data.remote.dto.EmergencyContactResponseDto
import com.ishara.app.data.remote.dto.EmergencyEventResponseDto
import com.ishara.app.data.remote.dto.SafetyLocationSnapshotDto
import com.ishara.app.data.remote.dto.UpdateEmergencyContactRequestDto
import com.ishara.app.data.repository.SafetyRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyStatus
import com.ishara.app.domain.model.EmergencyType
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyRepositoryTest {

    private val testDispatcher = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeSafetyRemoteDataSource : SafetyRemoteDataSource {
        var triggerSosResult: IshaaraResult<EmergencyEventResponseDto>? = null
        var getActiveSosResult: IshaaraResult<EmergencyEventResponseDto?>? = null
        var cancelSosResult: IshaaraResult<EmergencyEventResponseDto>? = null
        var listContactsResult: IshaaraResult<List<EmergencyContactResponseDto>>? = null
        var createContactResult: IshaaraResult<EmergencyContactResponseDto>? = null
        var deleteContactResult: IshaaraResult<Unit>? = null

        var lastIdempotencyKey: String? = null
        var lastToken: String? = null
        var lastRideId: String? = null

        override suspend fun triggerSos(
            rideId: String,
            request: CreateSosRequestDto,
            idempotencyKey: String?,
            token: String?
        ): IshaaraResult<EmergencyEventResponseDto> {
            lastRideId = rideId
            lastIdempotencyKey = idempotencyKey
            lastToken = token
            return triggerSosResult ?: throw IllegalStateException("triggerSosResult not set")
        }

        override suspend fun getActiveSosForRide(
            rideId: String,
            token: String?
        ): IshaaraResult<EmergencyEventResponseDto?> {
            lastRideId = rideId
            lastToken = token
            return getActiveSosResult ?: throw IllegalStateException("getActiveSosResult not set")
        }

        override suspend fun listEventsForRide(
            rideId: String,
            limit: Int,
            skip: Int,
            token: String?
        ): IshaaraResult<List<EmergencyEventResponseDto>> = IshaaraResult.success(emptyList())

        override suspend fun cancelSosByRide(
            rideId: String,
            request: CancelSosRequestDto,
            token: String?
        ): IshaaraResult<EmergencyEventResponseDto> {
            lastRideId = rideId
            lastToken = token
            return cancelSosResult ?: throw IllegalStateException("cancelSosResult not set")
        }

        override suspend fun getSosById(
            eventId: String,
            token: String?
        ): IshaaraResult<EmergencyEventResponseDto> = throw NotImplementedError()

        override suspend fun cancelSosById(
            eventId: String,
            request: CancelSosRequestDto,
            token: String?
        ): IshaaraResult<EmergencyEventResponseDto> = throw NotImplementedError()

        override suspend fun listEmergencyContacts(
            token: String?
        ): IshaaraResult<List<EmergencyContactResponseDto>> {
            lastToken = token
            return listContactsResult ?: throw IllegalStateException("listContactsResult not set")
        }

        override suspend fun createEmergencyContact(
            request: CreateEmergencyContactRequestDto,
            token: String?
        ): IshaaraResult<EmergencyContactResponseDto> {
            lastToken = token
            return createContactResult ?: throw IllegalStateException("createContactResult not set")
        }

        override suspend fun updateEmergencyContact(
            contactId: String,
            request: UpdateEmergencyContactRequestDto,
            token: String?
        ): IshaaraResult<EmergencyContactResponseDto> = throw NotImplementedError()

        override suspend fun deleteEmergencyContact(
            contactId: String,
            token: String?
        ): IshaaraResult<Unit> {
            lastToken = token
            return deleteContactResult ?: IshaaraResult.success(Unit)
        }
    }

    private val fakeEventDto = EmergencyEventResponseDto(
        id = "doc_123",
        eventId = "se_test_001",
        rideId = "ride_456",
        tripId = null,
        triggeredByUserId = "user_789",
        triggeredByRole = "USER",
        driverId = "drv_111",
        passengerUserId = "user_789",
        emergencyType = "SOS",
        status = "ACTIVE",
        locationSnapshot = SafetyLocationSnapshotDto(
            coordinates = listOf(77.6, 12.9),
            accuracyMeters = 5.0,
            headingDegrees = null,
            speedMps = null,
            isStale = false,
            capturedAt = "2026-09-28T12:00:00Z",
            provider = "driver_profile"
        ),
        triggeredAt = "2026-09-28T12:00:00Z",
        acknowledgedAt = null,
        resolvedAt = null,
        cancelledAt = null,
        cancellationReason = null,
        createdAt = "2026-09-28T12:00:00Z",
        updatedAt = "2026-09-28T12:00:00Z"
    )

    @Test
    fun `triggerSos attaches auth token and generates idempotency key`() = runBlocking {
        val remoteDataSource = FakeSafetyRemoteDataSource()
        remoteDataSource.triggerSosResult = IshaaraResult.success(fakeEventDto)

        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token_xyz", userId = "user_789", role = UserRole.USER)
        )

        val repository = SafetyRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            dispatchers = testDispatcher
        )

        val result = repository.triggerSos(rideId = "ride_456")

        assertTrue(result is IshaaraResult.Success)
        val event = (result as IshaaraResult.Success).data
        assertEquals("se_test_001", event.eventId)
        assertEquals(EmergencyStatus.ACTIVE, event.status)
        assertEquals("session_token_xyz", remoteDataSource.lastToken)
        assertNotNull(remoteDataSource.lastIdempotencyKey)
        assertTrue(remoteDataSource.lastIdempotencyKey!!.isNotBlank())
    }

    @Test
    fun `triggerSos propagates server conflict error when SOS is already active`() = runBlocking {
        val remoteDataSource = FakeSafetyRemoteDataSource()
        remoteDataSource.triggerSosResult = IshaaraResult.failure(
            IshaaraError.Server(code = 409, message = "SOS_ALREADY_ACTIVE")
        )

        val sessionStore = InMemorySessionStore()
        val repository = SafetyRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            dispatchers = testDispatcher
        )

        val result = repository.triggerSos(rideId = "ride_456")
        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Server)
        assertEquals(409, (error as IshaaraError.Server).code)
    }

    @Test
    fun `getActiveSosForRide returns null when no active SOS exists`() = runBlocking {
        val remoteDataSource = FakeSafetyRemoteDataSource()
        remoteDataSource.getActiveSosResult = IshaaraResult.success(null)

        val repository = SafetyRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionStore = InMemorySessionStore(),
            dispatchers = testDispatcher
        )

        val result = repository.getActiveSosForRide(rideId = "ride_456")
        assertTrue(result is IshaaraResult.Success)
        assertNull((result as IshaaraResult.Success).data)
    }

    @Test
    fun `cancelSosByRide returns cancelled domain event`() = runBlocking {
        val remoteDataSource = FakeSafetyRemoteDataSource()
        val cancelledDto = fakeEventDto.copy(
            status = "CANCELLED",
            cancelledAt = "2026-09-28T12:05:00Z",
            cancellationReason = "Situation resolved"
        )
        remoteDataSource.cancelSosResult = IshaaraResult.success(cancelledDto)

        val repository = SafetyRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionStore = InMemorySessionStore(),
            dispatchers = testDispatcher
        )

        val result = repository.cancelSosByRide(rideId = "ride_456", reason = "Situation resolved")
        assertTrue(result is IshaaraResult.Success)
        val event = (result as IshaaraResult.Success).data
        assertEquals(EmergencyStatus.CANCELLED, event.status)
        assertEquals("Situation resolved", event.cancellationReason)
    }

    @Test
    fun `listEmergencyContacts returns mapped contacts`() = runBlocking {
        val remoteDataSource = FakeSafetyRemoteDataSource()
        val contactDto = EmergencyContactResponseDto(
            id = "c_1",
            name = "Father",
            phoneNumber = "+919876543210",
            relationship = "PARENT",
            isVerified = false,
            isActive = true,
            createdAt = "2026-09-28T10:00:00Z",
            updatedAt = "2026-09-28T10:00:00Z"
        )
        remoteDataSource.listContactsResult = IshaaraResult.success(listOf(contactDto))

        val repository = SafetyRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionStore = InMemorySessionStore(),
            dispatchers = testDispatcher
        )

        val result = repository.listEmergencyContacts()
        assertTrue(result is IshaaraResult.Success)
        val contacts = (result as IshaaraResult.Success).data
        assertEquals(1, contacts.size)
        assertEquals("Father", contacts[0].name)
        assertEquals(EmergencyContactRelationship.PARENT, contacts[0].relationship)
    }
}
