package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.voice.VoiceInputClient
import com.ishara.app.core.voice.VoiceInputError
import com.ishara.app.core.voice.VoiceRecognitionState
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.model.VoiceDraftEndpoint
import com.ishara.app.domain.model.VoiceTripDraft
import com.ishara.app.domain.model.VoiceTripDraftStatus
import com.ishara.app.domain.repository.LocationRepository
import com.ishara.app.domain.repository.VoiceTripRepository
import com.ishara.app.domain.usecase.CancelVoiceTripDraftUseCase
import com.ishara.app.domain.usecase.CreateVoiceTripDraftUseCase
import com.ishara.app.domain.usecase.SearchLocationsUseCase
import com.ishara.app.feature.student.voice.ClarificationEndpointType
import com.ishara.app.feature.student.voice.VoiceTripStage
import com.ishara.app.feature.student.voice.VoiceTripViewModel
import com.ishara.app.navigation.IshaaraDestination
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class VoiceTripViewModelTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private class FakeVoiceInputClient : VoiceInputClient {
        private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
        override val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

        var isAvailableFlag = true
        var startListeningCallCount = 0
        var stopListeningCallCount = 0
        var cancelCallCount = 0
        var destroyCallCount = 0
        var lastLanguageHint: String? = null

        override fun isAvailable(): Boolean = isAvailableFlag

        override fun startListening(languageHint: String?) {
            startListeningCallCount++
            lastLanguageHint = languageHint
            _state.value = VoiceRecognitionState.Listening
        }

        override fun stopListening() {
            stopListeningCallCount++
        }

        override fun cancel() {
            cancelCallCount++
            _state.value = VoiceRecognitionState.Cancelled
        }

        override fun destroy() {
            destroyCallCount++
            _state.value = VoiceRecognitionState.Idle
        }

        fun emitState(newState: VoiceRecognitionState) {
            _state.value = newState
        }
    }

    private class FakeVoiceTripRepository : VoiceTripRepository {
        var shouldFail = false
        var failureError: IshaaraError = IshaaraError.Server(500, "Server error")
        var createDraftCallCount = AtomicInteger(0)
        var cancelDraftCallCount = AtomicInteger(0)

        override suspend fun createTripDraft(
            transcript: String,
            languageHint: String?
        ): IshaaraResult<VoiceTripDraft> {
            createDraftCallCount.incrementAndGet()
            if (shouldFail) return IshaaraResult.Failure(failureError)

            val draft = VoiceTripDraft(
                id = "mock_draft_001",
                originalTranscript = transcript,
                normalizedTranscript = transcript,
                intent = "CREATE_TRIP",
                origin = VoiceDraftEndpoint(
                    query = "Jhansi Railway Station",
                    displayName = "Jhansi Railway Station",
                    formattedAddress = "Station Road, Jhansi, UP",
                    coordinates = LocationCoordinates(25.4484, 78.5685)
                ),
                destination = VoiceDraftEndpoint(
                    query = "SRGI College",
                    displayName = "SRGI College",
                    formattedAddress = "Gwalior Road, Jhansi, UP",
                    coordinates = LocationCoordinates(25.4984, 78.6085)
                ),
                status = VoiceTripDraftStatus.CREATED,
                expiresAt = "2026-09-27T16:15:00.000Z"
            )
            return IshaaraResult.Success(draft)
        }

        override suspend fun cancelTripDraft(draftId: String): IshaaraResult<Unit> {
            cancelDraftCallCount.incrementAndGet()
            return IshaaraResult.Success(Unit)
        }
    }

    private class FakeLocationRepository : LocationRepository {
        var mockLocations = listOf(
            SearchResultLocation(
                id = "loc_1",
                name = "SRGI College Campus",
                formattedAddress = "Gwalior Highway, Jhansi",
                city = "Jhansi",
                state = "UP",
                country = "India",
                coordinates = LocationCoordinates(25.4984, 78.6085)
            )
        )

        override suspend fun searchLocations(
            query: String,
            latitude: Double?,
            longitude: Double?,
            radius: Double?,
            limit: Int
        ): IshaaraResult<List<SearchResultLocation>> {
            return IshaaraResult.Success(mockLocations)
        }

        override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
            return IshaaraResult.Success(LocationCoordinates(25.4484, 78.5685))
        }

        override suspend fun resolveHumanReadableLocation(
            coordinates: LocationCoordinates
        ): IshaaraResult<CurrentLocationDisplay> {
            return IshaaraResult.Success(
                CurrentLocationDisplay("Jhansi", "Uttar Pradesh", coordinates)
            )
        }

        override fun getRecentDestinations(): kotlinx.coroutines.flow.Flow<List<StudentDestination>> {
            return MutableStateFlow(emptyList())
        }

        override suspend fun saveRecentDestination(destination: StudentDestination): IshaaraResult<Unit> {
            return IshaaraResult.Success(Unit)
        }

        override suspend fun clearRecentDestinations(): IshaaraResult<Unit> {
            return IshaaraResult.Success(Unit)
        }
    }

    @Test
    fun initialState_isIdleAndNotListening() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        val state = viewModel.uiState.value
        assertTrue(state.stage is VoiceTripStage.Idle)
        assertFalse(state.isMicrophoneActive)
        assertFalse(state.isSubmittingDraft)
        assertFalse(state.isConfirmed)
    }

    @Test
    fun startListening_triggersVoiceClientAndUpdatesState() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening("en-IN")

        assertEquals(1, voiceClient.startListeningCallCount)
        assertEquals("en-IN", voiceClient.lastLanguageHint)
        assertTrue(viewModel.uiState.value.isMicrophoneActive)
        assertTrue(viewModel.uiState.value.stage is VoiceTripStage.Listening)
    }

    @Test
    fun partialResult_updatesInterimTranscript() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(VoiceRecognitionState.PartialResult("Take me from Jhansi"))

        val state = viewModel.uiState.value
        val stage = state.stage as VoiceTripStage.Listening
        assertEquals("Take me from Jhansi", stage.interimTranscript)
    }

    @Test
    fun finalResult_automaticallySubmitsDraftAndReachesReviewState() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(
            VoiceRecognitionState.FinalResult("Take me from Jhansi Railway Station to SRGI College")
        )

        val state = viewModel.uiState.value
        assertFalse(state.isMicrophoneActive)
        assertFalse(state.isSubmittingDraft)
        assertTrue(state.stage is VoiceTripStage.ReviewDraft)

        val draftStage = state.stage as VoiceTripStage.ReviewDraft
        assertEquals("Jhansi Railway Station", draftStage.draft.origin.displayName)
        assertEquals("SRGI College", draftStage.draft.destination.displayName)
        assertEquals(1, repo.createDraftCallCount.get())
    }

    @Test
    fun confirmDraft_invokesCallbackWithDiscoveryQueryAndProtectsDuplicateTap() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(VoiceRecognitionState.FinalResult("Take me from Jhansi to SRGI"))

        val callbackInvokedCount = AtomicInteger(0)
        var capturedQuery: DiscoveryQuery? = null

        // First confirmation
        viewModel.onConfirmDraft { query ->
            callbackInvokedCount.incrementAndGet()
            capturedQuery = query
        }

        assertEquals(1, callbackInvokedCount.get())
        assertNotNull(capturedQuery)
        assertEquals(25.4484, capturedQuery?.originLatitude ?: 0.0, 0.0001)
        assertEquals(25.4984, capturedQuery?.destinationLatitude ?: 0.0, 0.0001)
        assertTrue(viewModel.uiState.value.isConfirmed)

        // Duplicate confirmation tap (e.g. rapid taps by student)
        viewModel.onConfirmDraft {
            callbackInvokedCount.incrementAndGet()
        }

        // Must still be 1 (duplicate tap guarded)
        assertEquals(1, callbackInvokedCount.get())
    }

    @Test
    fun cancelSession_cancelsVoiceClientAndResetsState() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        viewModel.cancelSession()

        assertEquals(1, voiceClient.cancelCallCount)
        assertTrue(viewModel.uiState.value.stage is VoiceTripStage.Idle)
        assertFalse(viewModel.uiState.value.isMicrophoneActive)
    }

    @Test
    fun cancelSession_afterDraftCreated_triggersBackendCancellation() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(VoiceRecognitionState.FinalResult("Jhansi to SRGI"))

        assertTrue(viewModel.uiState.value.stage is VoiceTripStage.ReviewDraft)

        // User decides to cancel after reviewing
        viewModel.cancelSession()

        assertEquals(1, repo.cancelDraftCallCount.get())
        assertTrue(viewModel.uiState.value.stage is VoiceTripStage.Idle)
    }

    @Test
    fun duplicateMicrophoneTap_isIgnoredWhileListening() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        assertEquals(1, voiceClient.startListeningCallCount)

        // Second tap while listening
        viewModel.startListening()
        assertEquals(1, voiceClient.startListeningCallCount) // Guarded
    }

    @Test
    fun speechError_transitionsToErrorStage() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(VoiceRecognitionState.Error(VoiceInputError.NO_SPEECH_DETECTED))

        val state = viewModel.uiState.value
        assertFalse(state.isMicrophoneActive)
        assertTrue(state.stage is VoiceTripStage.Error)
        val errorStage = state.stage as VoiceTripStage.Error
        assertTrue(errorStage.canRetry)
        assertEquals("NO_SPEECH_DETECTED", errorStage.errorCode)
    }

    @Test
    fun backendLocationNotFoundError_entersClarificationStage() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository().apply {
            shouldFail = true
            failureError = IshaaraError.NotFound(
                message = "Location not found for destination: \"SRGI College\". Please try a more specific landmark."
            )
        }
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(VoiceRecognitionState.FinalResult("Take me from Jhansi Station to SRGI College"))

        val state = viewModel.uiState.value
        assertTrue(state.stage is VoiceTripStage.Clarification)
        val clarStage = state.stage as VoiceTripStage.Clarification
        assertEquals(ClarificationEndpointType.DESTINATION, clarStage.endpointType)
        assertEquals(1, clarStage.candidateLocations.size)
        assertEquals("SRGI College Campus", clarStage.candidateLocations[0].name)
    }

    @Test
    fun selectingClarifiedLocation_resolvesDraft() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository().apply {
            shouldFail = true
            failureError = IshaaraError.NotFound("Location not found for destination")
        }
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.startListening()
        voiceClient.emitState(VoiceRecognitionState.FinalResult("Jhansi to SRGI"))

        // Now in clarification
        assertTrue(viewModel.uiState.value.stage is VoiceTripStage.Clarification)

        // Repo now succeeds
        repo.shouldFail = false

        // Select candidate
        val selectedPlace = SearchResultLocation(
            id = "srgi_1",
            name = "SRGI College Campus",
            formattedAddress = "Gwalior Highway, Jhansi",
            city = "Jhansi",
            state = "UP",
            country = "India",
            coordinates = LocationCoordinates(25.4984, 78.6085)
        )
        viewModel.onSelectClarifiedLocation(selectedPlace)

        // Re-triggers submission with clarified place
        assertTrue(viewModel.uiState.value.stage is VoiceTripStage.ReviewDraft)
    }

    @Test
    fun manualSearchFallback_navigatesToStudentSearch() {
        val voiceClient = FakeVoiceInputClient()
        val repo = FakeVoiceTripRepository()
        val locationRepo = FakeLocationRepository()
        val navManager = NavigationManager()

        val viewModel = VoiceTripViewModel(
            voiceInputClient = voiceClient,
            createVoiceTripDraftUseCase = CreateVoiceTripDraftUseCase(repo),
            cancelVoiceTripDraftUseCase = CancelVoiceTripDraftUseCase(repo),
            searchLocationsUseCase = SearchLocationsUseCase(locationRepo),
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.onNavigateToManualSearch()

        val command = navManager.commands.replayCache.lastOrNull()
        assertTrue(command is NavigationCommand.NavigateTo)
        assertEquals(IshaaraDestination.StudentSearch.route, (command as NavigationCommand.NavigateTo).route)
    }
}
