package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitCacheMetadata
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.model.TransitStop
import com.ishara.app.domain.model.TransitStopType
import com.ishara.app.domain.model.TransitVehicleType
import com.ishara.app.domain.repository.TransitRepository
import com.ishara.app.domain.repository.TransitResource
import com.ishara.app.domain.usecase.GetRouteDetailsUseCase
import com.ishara.app.domain.usecase.GetTransitRoutesUseCase
import com.ishara.app.feature.student.transit.RouteDetailsViewModel
import com.ishara.app.feature.student.transit.TransitRoutesViewModel
import com.ishara.app.navigation.NavigationCommand
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitViewModelTest {

    private class TestDispatcherProvider(
        override val main: CoroutineDispatcher = Dispatchers.Unconfined,
        override val io: CoroutineDispatcher = Dispatchers.Unconfined,
        override val default: CoroutineDispatcher = Dispatchers.Unconfined,
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider

    private val testStop1 = TransitStop(
        id = "s1",
        name = "Stop One",
        formattedAddress = "Address 1",
        coordinates = LocationCoordinates(25.44, 78.58),
        sequence = 0,
        stopType = TransitStopType.ORIGIN
    )

    private val testStop2 = TransitStop(
        id = "s2",
        name = "Stop Two",
        formattedAddress = "Address 2",
        coordinates = LocationCoordinates(25.46, 78.60),
        sequence = 1,
        stopType = TransitStopType.DESTINATION
    )

    private val testRoute = TransitRoute(
        id = "route_test_1",
        name = "Stop One ↔ Stop Two",
        originStop = testStop1,
        destinationStop = testStop2,
        stops = listOf(testStop1, testStop2),
        geometry = listOf(LocationCoordinates(25.44, 78.58), LocationCoordinates(25.46, 78.60)),
        distanceMeters = 3000.0,
        durationSeconds = 600L,
        vehicleType = TransitVehicleType.BUS,
        vehiclePlate = "UP93B9999",
        driverName = "Ram Singh",
        operatingStatus = TransitOperatingStatus.ACTIVE,
        startedAtEpochMillis = 1000L,
        cachedAtEpochMillis = 2000L
    )

    private class FakeTransitRepository(
        var routesFlowToReturn: Flow<TransitResource<List<TransitRoute>>> = flowOf(),
        var singleRouteFlowToReturn: Flow<TransitResource<TransitRoute>> = flowOf()
    ) : TransitRepository {
        override fun getRoutes(forceRefresh: Boolean): Flow<TransitResource<List<TransitRoute>>> = routesFlowToReturn
        override fun getRouteById(routeId: String, forceRefresh: Boolean): Flow<TransitResource<TransitRoute>> = singleRouteFlowToReturn
        override suspend fun refreshRoutes(): IshaaraResult<List<TransitRoute>> = IshaaraResult.success(emptyList())
        override suspend fun getCacheMetadata(): TransitCacheMetadata? = null
        override suspend fun clearCache() {}
    }

    @Test
    fun testTransitRoutesViewModelSuccessState() {
        val fakeRepo = FakeTransitRepository(
            routesFlowToReturn = flowOf(
                TransitResource.Success(
                    data = listOf(testRoute),
                    freshness = TransitCacheFreshness.FRESH,
                    isOffline = false,
                    lastRefreshedAt = 5000L
                )
            )
        )
        val navManager = NavigationManager()
        val getRoutesUseCase = GetTransitRoutesUseCase(fakeRepo)

        val viewModel = TransitRoutesViewModel(
            getTransitRoutesUseCase = getRoutesUseCase,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertFalse(state.isOffline)
        assertEquals(1, state.routes.size)
        assertEquals("route_test_1", state.routes[0].id)
        assertEquals(TransitCacheFreshness.FRESH, state.freshness)
        assertEquals(5000L, state.lastRefreshedAtMillis)
    }

    @Test
    fun testTransitRoutesViewModelOfflineWithCachedData() {
        val fakeRepo = FakeTransitRepository(
            routesFlowToReturn = flowOf(
                TransitResource.Error(
                    message = "You are offline",
                    cachedData = listOf(testRoute),
                    isOffline = true
                )
            )
        )
        val navManager = NavigationManager()
        val getRoutesUseCase = GetTransitRoutesUseCase(fakeRepo)

        val viewModel = TransitRoutesViewModel(
            getTransitRoutesUseCase = getRoutesUseCase,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isOffline)
        assertEquals(1, state.routes.size)
        assertEquals("route_test_1", state.routes[0].id)
    }

    @Test
    fun testTransitRoutesViewModelNavigation() {
        val fakeRepo = FakeTransitRepository(
            routesFlowToReturn = flowOf(
                TransitResource.Success(
                    data = listOf(testRoute),
                    freshness = TransitCacheFreshness.FRESH,
                    isOffline = false,
                    lastRefreshedAt = 1000L
                )
            )
        )
        val navManager = NavigationManager()
        val getRoutesUseCase = GetTransitRoutesUseCase(fakeRepo)

        val viewModel = TransitRoutesViewModel(
            getTransitRoutesUseCase = getRoutesUseCase,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        viewModel.onRouteSelected("route_test_1")
        assertEquals(
            "student/transit/routes/route_test_1",
            (navManager.commands.replayCache.lastOrNull() as? NavigationCommand.NavigateTo)?.route
        )

        viewModel.onNavigateBack()
        assertEquals(NavigationCommand.NavigateUp, navManager.commands.replayCache.lastOrNull())
    }

    @Test
    fun testRouteDetailsViewModelSuccessState() {
        val fakeRepo = FakeTransitRepository(
            singleRouteFlowToReturn = flowOf(
                TransitResource.Success(
                    data = testRoute,
                    freshness = TransitCacheFreshness.FRESH,
                    isOffline = false,
                    lastRefreshedAt = 5000L
                )
            )
        )
        val navManager = NavigationManager()
        val getRouteDetailsUseCase = GetRouteDetailsUseCase(fakeRepo)

        val viewModel = RouteDetailsViewModel(
            routeId = "route_test_1",
            getRouteDetailsUseCase = getRouteDetailsUseCase,
            navigationManager = navManager,
            dispatchers = TestDispatcherProvider()
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isOffline)
        assertNotNull(state.route)
        assertEquals("route_test_1", state.route?.id)
        assertNotNull(state.schedule)
        assertEquals(TransitOperatingStatus.ACTIVE, state.schedule?.status)
        assertEquals(TransitCacheFreshness.FRESH, state.freshness)

        viewModel.onNavigateBack()
        assertEquals(NavigationCommand.NavigateUp, navManager.commands.replayCache.lastOrNull())
    }
}
