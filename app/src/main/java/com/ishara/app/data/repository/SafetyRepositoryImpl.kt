package com.ishara.app.data.repository

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.SafetyMapper
import com.ishara.app.data.remote.datasource.SafetyRemoteDataSource
import com.ishara.app.data.remote.dto.CancelSosRequestDto
import com.ishara.app.data.remote.dto.CreateEmergencyContactRequestDto
import com.ishara.app.data.remote.dto.CreateSosRequestDto
import com.ishara.app.data.remote.dto.UpdateEmergencyContactRequestDto
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyType
import com.ishara.app.domain.repository.SafetyRepository
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production implementation of [SafetyRepository].
 * Handles session token injection, thread dispatching, and DTO-to-Domain mapping.
 */
class SafetyRepositoryImpl(
    private val remoteDataSource: SafetyRemoteDataSource,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : SafetyRepository {

    override suspend fun triggerSos(
        rideId: String,
        emergencyType: EmergencyType,
        idempotencyKey: String?
    ): IshaaraResult<EmergencyEvent> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token
        val key = idempotencyKey ?: UUID.randomUUID().toString()

        val request = CreateSosRequestDto(
            emergencyType = if (emergencyType == EmergencyType.SAFETY_CONCERN) "SAFETY_CONCERN" else "SOS"
        )

        remoteDataSource.triggerSos(rideId, request, key, token).map { dto ->
            SafetyMapper.toDomain(dto)
        }
    }

    override suspend fun getActiveSosForRide(
        rideId: String
    ): IshaaraResult<EmergencyEvent?> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.getActiveSosForRide(rideId, token).map { dto ->
            dto?.let { SafetyMapper.toDomain(it) }
        }
    }

    override suspend fun listEventsForRide(
        rideId: String,
        limit: Int,
        skip: Int
    ): IshaaraResult<List<EmergencyEvent>> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.listEventsForRide(rideId, limit, skip, token).map { dtos ->
            dtos.map { SafetyMapper.toDomain(it) }
        }
    }

    override suspend fun cancelSosByRide(
        rideId: String,
        reason: String?
    ): IshaaraResult<EmergencyEvent> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val request = CancelSosRequestDto(reason = reason)

        remoteDataSource.cancelSosByRide(rideId, request, token).map { dto ->
            SafetyMapper.toDomain(dto)
        }
    }

    override suspend fun getSosById(
        eventId: String
    ): IshaaraResult<EmergencyEvent> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.getSosById(eventId, token).map { dto ->
            SafetyMapper.toDomain(dto)
        }
    }

    override suspend fun cancelSosById(
        eventId: String,
        reason: String?
    ): IshaaraResult<EmergencyEvent> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val request = CancelSosRequestDto(reason = reason)

        remoteDataSource.cancelSosById(eventId, request, token).map { dto ->
            SafetyMapper.toDomain(dto)
        }
    }

    override suspend fun listEmergencyContacts(): IshaaraResult<List<EmergencyContact>> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.listEmergencyContacts(token).map { dtos ->
            dtos.map { SafetyMapper.toDomain(it) }
        }
    }

    override suspend fun createEmergencyContact(
        name: String,
        phoneNumber: String,
        relationship: EmergencyContactRelationship
    ): IshaaraResult<EmergencyContact> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val relString = when (relationship) {
            EmergencyContactRelationship.PARENT -> "PARENT"
            EmergencyContactRelationship.SPOUSE -> "SPOUSE"
            EmergencyContactRelationship.SIBLING -> "SIBLING"
            EmergencyContactRelationship.FRIEND -> "FRIEND"
            EmergencyContactRelationship.GUARDIAN -> "GUARDIAN"
            else -> "OTHER"
        }

        val request = CreateEmergencyContactRequestDto(
            name = name,
            phoneNumber = phoneNumber,
            relationship = relString
        )

        remoteDataSource.createEmergencyContact(request, token).map { dto ->
            SafetyMapper.toDomain(dto)
        }
    }

    override suspend fun updateEmergencyContact(
        contactId: String,
        name: String?,
        phoneNumber: String?,
        relationship: EmergencyContactRelationship?,
        isActive: Boolean?
    ): IshaaraResult<EmergencyContact> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        val relString = relationship?.let {
            when (it) {
                EmergencyContactRelationship.PARENT -> "PARENT"
                EmergencyContactRelationship.SPOUSE -> "SPOUSE"
                EmergencyContactRelationship.SIBLING -> "SIBLING"
                EmergencyContactRelationship.FRIEND -> "FRIEND"
                EmergencyContactRelationship.GUARDIAN -> "GUARDIAN"
                else -> "OTHER"
            }
        }

        val request = UpdateEmergencyContactRequestDto(
            name = name,
            phoneNumber = phoneNumber,
            relationship = relString,
            isActive = isActive
        )

        remoteDataSource.updateEmergencyContact(contactId, request, token).map { dto ->
            SafetyMapper.toDomain(dto)
        }
    }

    override suspend fun deleteEmergencyContact(
        contactId: String
    ): IshaaraResult<Unit> = withContext(dispatchers.io) {
        val session = sessionStore.getSession()
        val token = session?.token

        remoteDataSource.deleteEmergencyContact(contactId, token)
    }
}
