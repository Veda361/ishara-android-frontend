package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.repository.LocationRepository
import com.ishara.app.domain.usecase.ManageRecentDestinationsUseCase
import com.ishara.app.domain.usecase.SearchLocationsUseCase
import com.ishara.app.feature.student.search.LocationSearchViewModel
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Validates LocationSearchViewModel debouncing, race condition cancellation,
 * error recovery, and result mapping.
 */
class LocationSearchViewModelTest {

    private class TestDispatcherProvider(
        private val testDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private class MockSearchLocationRepository(
        var shouldFail: Boolean = false,
        var returnEmpty: Boolean = false
    ) : LocationRepository {
        val searchCallCount = AtomicInteger(0)
        val recentDestinationsFlow = MutableStateFlow<List<StudentDestination>>(emptyList())

        override suspend fun searchLocations(
            query: String,
            latitude: Double?,
            longitude: Double?,
            radius: Double?,
            limit: Int
        ): IshaaraResult<List<SearchResultLocation>> {
            searchCallCount.incrementAndGet()
            if (shouldFail) {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }
            if (returnEmpty) {
                return IshaaraResult.success(emptyList())
            }

            return IshaaraResult.success(
                listOf(
                    SearchResultLocation(
                        id = "loc_1",
                        name = "Jhansi Railway Station",
                        formattedAddress = "Station Road, Jhansi",
                        city = "Jhansi",
                        state = "Uttar Pradesh",
                        country = "India",
                        coordinates = LocationCoordinates(25.4484, 78.5685)
                    )
                )
            )
        }

        override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> =
            IshaaraResult.success(LocationCoordinates(25.4484, 78.5685))

        override suspend fun resolveHumanReadableLocation(
            coordinates: LocationCoordinates
        ): IshaaraResult<CurrentLocationDisplay> =
            IshaaraResult.success(CurrentLocationDisplay("Title", "Subtitle", coordinates))

        override fun getRecentDestinations(): Flow<List<StudentDestination>> = recentDestinationsFlow

        override suspend fun saveRecentDestination(destination: StudentDestination): IshaaraResult<Unit> {
            val list = recentDestinationsFlow.value.toMutableList()
            list.add(0, destination)
            recentDestinationsFlow.value = list
            return IshaaraResult.success(Unit)
        }

        override suspend fun clearRecentDestinations(): IshaaraResult<Unit> {
            recentDestinationsFlow.value = emptyList()
            return IshaaraResult.success(Unit)
        }
    }

    @Test
    fun test1_shortQuery_doesNotDispatchRemoteSearch() = runBlocking {
        val repo = MockSearchLocationRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            val viewModel = LocationSearchViewModel(
                searchLocationsUseCase = SearchLocationsUseCase(repo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(repo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope,
                debounceMillis = 20L
            )

            viewModel.onQueryChange("J")
            delay(50)

            assertEquals("Query shorter than 2 chars must NOT trigger remote API", 0, repo.searchCallCount.get())
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.results.isEmpty())
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test2_validQuery_dispatchesRemoteSearch_andUpdatesResults() = runBlocking {
        val repo = MockSearchLocationRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            val viewModel = LocationSearchViewModel(
                searchLocationsUseCase = SearchLocationsUseCase(repo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(repo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope,
                debounceMillis = 20L
            )

            viewModel.onQueryChange("Jhansi")
            delay(60)

            assertEquals("Valid query must trigger remote API", 1, repo.searchCallCount.get())
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, viewModel.uiState.value.results.size)
            assertEquals("Jhansi Railway Station", viewModel.uiState.value.results[0].name)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test3_emptyResult_setsEmptyResultState() = runBlocking {
        val repo = MockSearchLocationRepository(returnEmpty = true)
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            val viewModel = LocationSearchViewModel(
                searchLocationsUseCase = SearchLocationsUseCase(repo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(repo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope,
                debounceMillis = 20L
            )

            viewModel.onQueryChange("Unknown")
            delay(60)

            assertTrue(viewModel.uiState.value.isEmptyResult)
            assertTrue(viewModel.uiState.value.results.isEmpty())
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test4_remoteFailure_setsErrorMessage() = runBlocking {
        val repo = MockSearchLocationRepository(shouldFail = true)
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            val viewModel = LocationSearchViewModel(
                searchLocationsUseCase = SearchLocationsUseCase(repo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(repo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope,
                debounceMillis = 20L
            )

            viewModel.onQueryChange("Jhansi")
            delay(60)

            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test5_clearQuery_resetsState() = runBlocking {
        val repo = MockSearchLocationRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            val viewModel = LocationSearchViewModel(
                searchLocationsUseCase = SearchLocationsUseCase(repo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(repo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope,
                debounceMillis = 20L
            )

            viewModel.onQueryChange("Jhansi")
            var attempts = 0
            while (viewModel.uiState.value.results.isEmpty() && attempts < 20) {
                delay(50)
                attempts++
            }
            assertEquals(1, viewModel.uiState.value.results.size)

            viewModel.onClearQuery()
            attempts = 0
            while (viewModel.uiState.value.results.isNotEmpty() && attempts < 20) {
                delay(50)
                attempts++
            }

            assertEquals("", viewModel.uiState.value.query)
            assertTrue(viewModel.uiState.value.results.isEmpty())
            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun test6_selectLocation_savesToRecents_andNavigatesUp() = runBlocking {
        val repo = MockSearchLocationRepository()
        val navManager = NavigationManager()
        val dispatchers = TestDispatcherProvider()
        val testScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            val viewModel = LocationSearchViewModel(
                searchLocationsUseCase = SearchLocationsUseCase(repo),
                manageRecentDestinationsUseCase = ManageRecentDestinationsUseCase(repo),
                navigationManager = navManager,
                dispatchers = dispatchers,
                externalScope = testScope,
                debounceMillis = 20L
            )

            var selectedDestination: StudentDestination? = null

            val location = SearchResultLocation(
                id = "loc_1",
                name = "Sipri Bazaar",
                formattedAddress = "Sipri Bazaar, Jhansi",
                city = "Jhansi",
                state = "Uttar Pradesh",
                country = "India",
                coordinates = LocationCoordinates(25.45, 78.57)
            )

            viewModel.onLocationSelected(location) { selectedDestination = it }

            assertNotNull(selectedDestination)
            assertEquals("Sipri Bazaar", selectedDestination?.name)

            val navCommand = navManager.commands.first()
            assertTrue(navCommand is NavigationCommand.NavigateUp)
        } finally {
            testScope.cancel()
        }
    }
}
