package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyStatus
import com.ishara.app.domain.model.EmergencyType
import com.ishara.app.domain.model.SafetyLocationSnapshot
import com.ishara.app.domain.repository.SafetyRepository
import com.ishara.app.domain.usecase.CancelSosUseCase
import com.ishara.app.domain.usecase.CreateEmergencyContactUseCase
import com.ishara.app.domain.usecase.DeleteEmergencyContactUseCase
import com.ishara.app.domain.usecase.GetActiveSosUseCase
import com.ishara.app.domain.usecase.GetEmergencyContactsUseCase
import com.ishara.app.domain.usecase.TriggerSosUseCase
import com.ishara.app.feature.safety.SafetyUiState
import com.ishara.app.feature.safety.SafetyViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyContactManagementTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private val testDispatchers = TestDispatcherProvider()

    @Test
    fun `adding contact with blank name sets contactErrorMessage`() = runBlocking {
        val fakeRepo = FakeSafetyRepository()
        val viewModel = createViewModel(fakeRepo)

        viewModel.onAddContact(
            name = "   ",
            phoneNumber = "+919876543210",
            relationship = EmergencyContactRelationship.PARENT
        )

        val state = viewModel.uiState.value as SafetyUiState.Content
        assertEquals("Name is required", state.contactErrorMessage)
        assertFalse(state.isAddingContact)
    }

    @Test
    fun `adding contact with invalid phone format sets contactErrorMessage`() = runBlocking {
        val fakeRepo = FakeSafetyRepository()
        val viewModel = createViewModel(fakeRepo)

        viewModel.onAddContact(
            name = "Jane Doe",
            phoneNumber = "abc-123",
            relationship = EmergencyContactRelationship.FRIEND
        )

        val state = viewModel.uiState.value as SafetyUiState.Content
        assertEquals("Phone number must be 7–15 digits (optional leading +)", state.contactErrorMessage)
        assertFalse(state.isAddingContact)
    }

    @Test
    fun `adding contact beyond 5 contacts limit rejects with error`() = runBlocking {
        val existing5 = (1..5).map {
            EmergencyContact(
                id = "c_$it",
                name = "Contact $it",
                phoneNumber = "+91987654321$it",
                relationship = EmergencyContactRelationship.OTHER,
                isVerified = false,
                isActive = true,
                createdAt = "2026-10-01",
                updatedAt = "2026-10-01"
            )
        }
        val fakeRepo = FakeSafetyRepository(contacts = existing5.toMutableList())
        val viewModel = createViewModel(fakeRepo)

        viewModel.onAddContact(
            name = "Contact 6",
            phoneNumber = "+919876543216",
            relationship = EmergencyContactRelationship.SIBLING
        )

        val state = viewModel.uiState.value as SafetyUiState.Content
        assertEquals("Maximum limit of 5 emergency contacts reached.", state.contactErrorMessage)
    }

    @Test
    fun `adding contact with valid details succeeds and updates list`() = runBlocking {
        val fakeRepo = FakeSafetyRepository()
        val viewModel = createViewModel(fakeRepo)

        viewModel.onAddContact(
            name = "Mom",
            phoneNumber = "+919876543210",
            relationship = EmergencyContactRelationship.PARENT
        )

        val state = viewModel.uiState.value as SafetyUiState.Content
        assertEquals(1, state.emergencyContacts.size)
        assertEquals("Mom", state.emergencyContacts.first().name)
        assertEquals("+919876543210", state.emergencyContacts.first().phoneNumber)
        assertEquals(EmergencyContactRelationship.PARENT, state.emergencyContacts.first().relationship)
        assertFalse(state.showAddContactDialog)
        assertEquals("Emergency contact added successfully.", state.bannerMessage)
    }

    @Test
    fun `deleting contact removes contact from state list`() = runBlocking {
        val initial = mutableListOf(
            EmergencyContact(
                id = "contact_del_1",
                name = "Friend",
                phoneNumber = "+919876543211",
                relationship = EmergencyContactRelationship.FRIEND,
                isVerified = false,
                isActive = true,
                createdAt = "2026-10-01",
                updatedAt = "2026-10-01"
            )
        )
        val fakeRepo = FakeSafetyRepository(contacts = initial)
        val viewModel = createViewModel(fakeRepo)

        viewModel.onDeleteContact("contact_del_1")

        val state = viewModel.uiState.value as SafetyUiState.Content
        assertTrue(state.emergencyContacts.isEmpty())
        assertNull(state.isDeletingContactId)
        assertEquals("Emergency contact removed.", state.bannerMessage)
    }

    private fun createViewModel(repo: SafetyRepository): SafetyViewModel {
        return SafetyViewModel(
            rideId = "ride_safety_101",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            createEmergencyContactUseCase = CreateEmergencyContactUseCase(repo),
            deleteEmergencyContactUseCase = DeleteEmergencyContactUseCase(repo),
            navigationManager = NavigationManager(),
            dispatchers = testDispatchers
        )
    }

    private class FakeSafetyRepository(
        var contacts: MutableList<EmergencyContact> = mutableListOf()
    ) : SafetyRepository {

        override suspend fun triggerSos(
            rideId: String,
            emergencyType: EmergencyType,
            idempotencyKey: String?
        ): IshaaraResult<EmergencyEvent> {
            return IshaaraResult.Success(
                EmergencyEvent(
                    id = "ev_1",
                    eventId = "se_test",
                    rideId = rideId,
                    tripId = null,
                    triggeredByUserId = "u1",
                    triggeredByRole = "USER",
                    driverId = "d1",
                    passengerUserId = "u1",
                    emergencyType = emergencyType,
                    status = EmergencyStatus.ACTIVE,
                    locationSnapshot = SafetyLocationSnapshot(null, null, null, null, false, null, "driver_profile"),
                    triggeredAt = "2026-10-01",
                    acknowledgedAt = null,
                    resolvedAt = null,
                    cancelledAt = null,
                    cancellationReason = null,
                    createdAt = "2026-10-01",
                    updatedAt = "2026-10-01"
                )
            )
        }

        override suspend fun getActiveSosForRide(rideId: String): IshaaraResult<EmergencyEvent?> {
            return IshaaraResult.Success(null)
        }

        override suspend fun listEventsForRide(rideId: String, limit: Int, skip: Int): IshaaraResult<List<EmergencyEvent>> {
            return IshaaraResult.Success(emptyList())
        }

        override suspend fun cancelSosByRide(rideId: String, reason: String?): IshaaraResult<EmergencyEvent> {
            throw NotImplementedError()
        }

        override suspend fun getSosById(eventId: String): IshaaraResult<EmergencyEvent> {
            throw NotImplementedError()
        }

        override suspend fun cancelSosById(eventId: String, reason: String?): IshaaraResult<EmergencyEvent> {
            throw NotImplementedError()
        }

        override suspend fun listEmergencyContacts(): IshaaraResult<List<EmergencyContact>> {
            return IshaaraResult.Success(contacts.toList())
        }

        override suspend fun createEmergencyContact(
            name: String,
            phoneNumber: String,
            relationship: EmergencyContactRelationship
        ): IshaaraResult<EmergencyContact> {
            val contact = EmergencyContact(
                id = "c_${System.currentTimeMillis()}",
                name = name,
                phoneNumber = phoneNumber,
                relationship = relationship,
                isVerified = false,
                isActive = true,
                createdAt = "2026-10-01",
                updatedAt = "2026-10-01"
            )
            contacts.add(0, contact)
            return IshaaraResult.Success(contact)
        }

        override suspend fun updateEmergencyContact(
            contactId: String,
            name: String?,
            phoneNumber: String?,
            relationship: EmergencyContactRelationship?,
            isActive: Boolean?
        ): IshaaraResult<EmergencyContact> {
            throw NotImplementedError()
        }

        override suspend fun deleteEmergencyContact(contactId: String): IshaaraResult<Unit> {
            contacts.removeAll { it.id == contactId }
            return IshaaraResult.Success(Unit)
        }
    }
}
