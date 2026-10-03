package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyEvent
import com.ishara.app.domain.model.EmergencyStatus
import com.ishara.app.domain.model.EmergencyType
import com.ishara.app.domain.model.SafetyLocationSnapshot
import com.ishara.app.domain.repository.SafetyRepository
import com.ishara.app.domain.usecase.CancelSosUseCase
import com.ishara.app.domain.usecase.GetActiveSosUseCase
import com.ishara.app.domain.usecase.GetEmergencyContactsUseCase
import com.ishara.app.domain.usecase.TriggerSosUseCase
import com.ishara.app.feature.safety.SafetyUiState
import com.ishara.app.feature.safety.SafetyViewModel
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyViewModelTest {

    private val testDispatcher = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeSafetyRepository : SafetyRepository {
        var triggerSosResult: IshaaraResult<EmergencyEvent>? = null
        var getActiveSosResult: IshaaraResult<EmergencyEvent?>? = null
        var cancelSosResult: IshaaraResult<EmergencyEvent>? = null
        var listContactsResult: IshaaraResult<List<EmergencyContact>>? = null

        var triggerCallCount = 0

        override suspend fun triggerSos(
            rideId: String,
            emergencyType: EmergencyType,
            idempotencyKey: String?
        ): IshaaraResult<EmergencyEvent> {
            triggerCallCount++
            return triggerSosResult ?: throw IllegalStateException("triggerSosResult not set")
        }

        override suspend fun getActiveSosForRide(rideId: String): IshaaraResult<EmergencyEvent?> {
            return getActiveSosResult ?: IshaaraResult.success(null)
        }

        override suspend fun listEventsForRide(
            rideId: String,
            limit: Int,
            skip: Int
        ): IshaaraResult<List<EmergencyEvent>> = IshaaraResult.success(emptyList())

        override suspend fun cancelSosByRide(
            rideId: String,
            reason: String?
        ): IshaaraResult<EmergencyEvent> {
            return cancelSosResult ?: throw IllegalStateException("cancelSosResult not set")
        }

        override suspend fun getSosById(eventId: String): IshaaraResult<EmergencyEvent> = throw NotImplementedError()

        override suspend fun cancelSosById(eventId: String, reason: String?): IshaaraResult<EmergencyEvent> = throw NotImplementedError()

        override suspend fun listEmergencyContacts(): IshaaraResult<List<EmergencyContact>> {
            return listContactsResult ?: IshaaraResult.success(emptyList())
        }

        override suspend fun createEmergencyContact(
            name: String,
            phoneNumber: String,
            relationship: EmergencyContactRelationship
        ): IshaaraResult<EmergencyContact> = throw NotImplementedError()

        override suspend fun updateEmergencyContact(
            contactId: String,
            name: String?,
            phoneNumber: String?,
            relationship: EmergencyContactRelationship?,
            isActive: Boolean?
        ): IshaaraResult<EmergencyContact> = throw NotImplementedError()

        override suspend fun deleteEmergencyContact(contactId: String): IshaaraResult<Unit> = IshaaraResult.success(Unit)
    }

    private val sampleEvent = EmergencyEvent(
        id = "doc_1",
        eventId = "se_vm_test",
        rideId = "ride_vm_1",
        tripId = null,
        triggeredByUserId = "user_1",
        triggeredByRole = "USER",
        driverId = "drv_1",
        passengerUserId = "user_1",
        emergencyType = EmergencyType.SOS,
        status = EmergencyStatus.ACTIVE,
        locationSnapshot = SafetyLocationSnapshot(
            coordinates = listOf(77.5, 12.9),
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

    private val sampleContact = EmergencyContact(
        id = "contact_1",
        name = "Father",
        phoneNumber = "+919876543210",
        relationship = EmergencyContactRelationship.PARENT,
        isVerified = false,
        isActive = true,
        createdAt = "2026-09-28T10:00:00Z",
        updatedAt = "2026-09-28T10:00:00Z"
    )

    @Test
    fun `initial state reconciles active SOS and loads contacts`() = runBlocking {
        val repo = FakeSafetyRepository()
        repo.getActiveSosResult = IshaaraResult.success(sampleEvent)
        repo.listContactsResult = IshaaraResult.success(listOf(sampleContact))

        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        val state = viewModel.uiState.value
        assertTrue(state is SafetyUiState.Content)
        val content = state as SafetyUiState.Content
        assertEquals("ride_vm_1", content.rideId)
        assertNotNull(content.activeEvent)
        assertEquals("se_vm_test", content.activeEvent?.eventId)
        assertTrue(content.hasActiveAlert)
        assertEquals(1, content.emergencyContacts.size)
        assertEquals("Father", content.emergencyContacts[0].name)
    }

    @Test
    fun `confirmation dialog open and dismiss toggles state`() = runBlocking {
        val repo = FakeSafetyRepository()
        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        var content = viewModel.uiState.value as SafetyUiState.Content
        assertFalse(content.showConfirmationDialog)

        viewModel.onOpenConfirmationDialog()
        content = viewModel.uiState.value as SafetyUiState.Content
        assertTrue(content.showConfirmationDialog)

        viewModel.onDismissConfirmationDialog()
        content = viewModel.uiState.value as SafetyUiState.Content
        assertFalse(content.showConfirmationDialog)
    }

    @Test
    fun `onConfirmSos sets active event on success and prevents duplicate taps`() = runBlocking {
        val repo = FakeSafetyRepository()
        repo.triggerSosResult = IshaaraResult.success(sampleEvent)

        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        viewModel.onOpenConfirmationDialog()
        viewModel.onConfirmSos()

        val content = viewModel.uiState.value as SafetyUiState.Content
        assertFalse(content.isSubmitting)
        assertFalse(content.showConfirmationDialog)
        assertNotNull(content.activeEvent)
        assertEquals("se_vm_test", content.activeEvent?.eventId)
        assertTrue(content.hasActiveAlert)
        assertNotNull(content.bannerMessage)
        assertEquals(1, repo.triggerCallCount)
    }

    @Test
    fun `onConfirmSos reconciles active event when 409 conflict occurs`() = runBlocking {
        val repo = FakeSafetyRepository()
        repo.triggerSosResult = IshaaraResult.failure(
            IshaaraError.Server(code = 409, message = "SOS_ALREADY_ACTIVE")
        )
        repo.getActiveSosResult = IshaaraResult.success(sampleEvent)

        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        viewModel.onConfirmSos()

        val content = viewModel.uiState.value as SafetyUiState.Content
        assertFalse(content.isSubmitting)
        assertEquals("se_vm_test", content.activeEvent?.eventId)
        assertEquals("A safety alert is already active for this ride.", content.bannerMessage)
    }

    @Test
    fun `onConfirmSos handles network error calmly`() = runBlocking {
        val repo = FakeSafetyRepository()
        repo.triggerSosResult = IshaaraResult.failure(IshaaraError.Network("Offline"))

        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        viewModel.onConfirmSos()

        val content = viewModel.uiState.value as SafetyUiState.Content
        assertFalse(content.isSubmitting)
        assertNotNull(content.errorMessage)
        assertTrue(content.errorMessage!!.contains("offline"))
    }

    @Test
    fun `cancellation flow updates active event and clears active alert state`() = runBlocking {
        val repo = FakeSafetyRepository()
        repo.getActiveSosResult = IshaaraResult.success(sampleEvent)
        val cancelledEvent = sampleEvent.copy(
            status = EmergencyStatus.CANCELLED,
            cancelledAt = "2026-09-28T12:05:00Z",
            cancellationReason = "False alarm"
        )
        repo.cancelSosResult = IshaaraResult.success(cancelledEvent)

        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        viewModel.onOpenCancelDialog()
        var content = viewModel.uiState.value as SafetyUiState.Content
        assertTrue(content.showCancelDialog)

        viewModel.onCancellationReasonChanged("False alarm")
        viewModel.onConfirmCancel()

        content = viewModel.uiState.value as SafetyUiState.Content
        assertFalse(content.showCancelDialog)
        assertFalse(content.isCancelling)
        assertEquals(EmergencyStatus.CANCELLED, content.activeEvent?.status)
        assertFalse(content.hasActiveAlert)
        assertEquals("Safety alert has been cancelled.", content.bannerMessage)
    }

    @Test
    fun `onNavigateBack emits NavigateUp command`() = runBlocking {
        val repo = FakeSafetyRepository()
        val nav = NavigationManager()
        val viewModel = SafetyViewModel(
            rideId = "ride_vm_1",
            triggerSosUseCase = TriggerSosUseCase(repo),
            getActiveSosUseCase = GetActiveSosUseCase(repo),
            cancelSosUseCase = CancelSosUseCase(repo),
            getEmergencyContactsUseCase = GetEmergencyContactsUseCase(repo),
            navigationManager = nav,
            dispatchers = testDispatcher
        )

        viewModel.onNavigateBack()
        val command = nav.commands.first()
        assertEquals(NavigationCommand.NavigateUp, command)
    }
}
