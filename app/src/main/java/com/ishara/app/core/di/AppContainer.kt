package com.ishara.app.core.di

import android.content.Context
import android.util.Log
import com.ishara.app.BuildConfig
import com.ishara.app.core.auth.GoogleSignInManager
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.network.*
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SharedPreferencesSessionStore
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.remote.datasource.*
import com.ishara.app.data.repository.*
import com.ishara.app.domain.repository.*
import com.ishara.app.domain.usecase.*
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json

interface AppContainer {
    val dispatchers: DispatcherProvider
    val networkConfig: NetworkConfig
    val httpClient: IshaaraHttpClient
    val sessionStore: SessionStore
    val navigationManager: NavigationManager
    val locationProvider: LocationProvider
    val realtimeClient: RealtimeClient
    val googleSignInManager: GoogleSignInManager

    val authRepository: AuthRepository
    val tripRepository: TripRepository
    val driverRepository: DriverRepository
    val rideRepository: RideRepository

    val getAuthSessionUseCase: GetAuthSessionUseCase
    val signInWithGoogleUseCase: SignInWithGoogleUseCase
    val signOutUseCase: SignOutUseCase
    val discoverTripsUseCase: DiscoverTripsUseCase
    val updateDriverLocationUseCase: UpdateDriverLocationUseCase
}

class DefaultAppContainer(private val appContext: Context) : AppContainer {

    init {
        Log.d("AUTH_DEBUG", "DefaultAppContainer: Initializing")
        Log.d("AUTH_DEBUG", "DefaultAppContainer: BuildConfig.BASE_URL = ${BuildConfig.BASE_URL}")
        Log.d("AUTH_DEBUG", "DefaultAppContainer: BuildConfig.GOOGLE_WEB_CLIENT_ID = ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
        
        if (BuildConfig.BASE_URL.isBlank()) {
            Log.e("AUTH_DEBUG", "DefaultAppContainer: BASE_URL is empty! Check local.properties and app build.gradle")
        }
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            Log.e("AUTH_DEBUG", "DefaultAppContainer: GOOGLE_WEB_CLIENT_ID is empty! Google Sign-In will fail.")
        }
    }

    override val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    override val networkConfig: NetworkConfig by lazy {
        NetworkConfig()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val sessionStore: SessionStore by lazy {
        SharedPreferencesSessionStore(appContext)
    }

    override val httpClient: IshaaraHttpClient by lazy {
        KtorHttpClient(sessionStore)
    }

    override val navigationManager: NavigationManager by lazy {
        NavigationManager()
    }

    override val locationProvider: LocationProvider by lazy {
        DefaultLocationProvider()
    }

    override val realtimeClient: RealtimeClient by lazy {
        DefaultRealtimeClient()
    }

    override val googleSignInManager: GoogleSignInManager by lazy {
        GoogleSignInManager(appContext)
    }

    // Remote Data Sources
    private val authRemoteDataSource: AuthRemoteDataSource by lazy {
        AuthRemoteDataSourceImpl(httpClient, networkConfig, json)
    }

    private val tripRemoteDataSource: TripRemoteDataSource by lazy {
        TripRemoteDataSourceImpl(httpClient, networkConfig, json)
    }

    private val driverRemoteDataSource: DriverRemoteDataSource by lazy {
        DriverRemoteDataSourceImpl(httpClient, networkConfig, json)
    }

    private val rideRemoteDataSource: RideRemoteDataSource by lazy {
        RideRemoteDataSourceImpl(httpClient, networkConfig, json)
    }

    // Repositories
    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authRemoteDataSource, sessionStore)
    }

    override val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(tripRemoteDataSource, sessionStore)
    }

    override val driverRepository: DriverRepository by lazy {
        DriverRepositoryImpl(driverRemoteDataSource, sessionStore)
    }

    override val rideRepository: RideRepository by lazy {
        RideRepositoryImpl(rideRemoteDataSource, sessionStore)
    }

    // Use Cases
    override val getAuthSessionUseCase: GetAuthSessionUseCase by lazy {
        GetAuthSessionUseCase(authRepository)
    }

    override val signInWithGoogleUseCase: SignInWithGoogleUseCase by lazy {
        SignInWithGoogleUseCase(authRepository)
    }

    override val signOutUseCase: SignOutUseCase by lazy {
        SignOutUseCase(authRepository)
    }

    override val discoverTripsUseCase: DiscoverTripsUseCase by lazy {
        DiscoverTripsUseCase(tripRepository)
    }

    override val updateDriverLocationUseCase: UpdateDriverLocationUseCase by lazy {
        UpdateDriverLocationUseCase(driverRepository)
    }
}

private class DefaultLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
        return IshaaraResult.success(
            LocationCoordinates(latitude = 18.5204, longitude = 73.8567)
        )
    }
    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = emptyFlow()
    override fun isLocationAvailable(): Boolean = true
}

private class DefaultRealtimeClient : RealtimeClient {
    private val stateFlow = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Disconnected)
    private val eventsFlow = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 16)
    override suspend fun connect(endpoint: String, token: String): IshaaraResult<Unit> {
        stateFlow.value = RealtimeConnectionState.Connected
        return IshaaraResult.success(Unit)
    }
    override suspend fun disconnect() {
        stateFlow.value = RealtimeConnectionState.Disconnected
    }
    override suspend fun send(message: String): IshaaraResult<Unit> {
        return IshaaraResult.success(Unit)
    }
    override fun observeConnectionState(): Flow<RealtimeConnectionState> = stateFlow.asStateFlow()
    override fun observeEvents(): Flow<RealtimeEvent> = eventsFlow.asSharedFlow()
}
