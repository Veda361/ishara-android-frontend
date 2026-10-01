package com.ishara.app.core.di

import android.content.Context
import android.util.Log
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.realtime.RealtimeClient
import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
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
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.ErrorNormalizer
import kotlinx.serialization.json.Json

/**
 * Pure Dependency Injection Container for the Ishaara Android application.
 * Wired to the Ishaara Backend API (Phase 00-17).
 */
interface AppContainer {
    val dispatchers: DispatcherProvider
    val networkConfig: NetworkConfig
    val httpClient: IshaaraHttpClient
    val sessionStore: SessionStore
    val navigationManager: NavigationManager
    val locationProvider: LocationProvider
    val realtimeClient: RealtimeClient

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

    override val httpClient: IshaaraHttpClient by lazy {
        DefaultIshaaraHttpClient(networkConfig)
    }

    override val sessionStore: SessionStore by lazy {
        InMemorySessionStore()
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

/**
 * Standard baseline HTTP client using java.net.HttpURLConnection (zero external dependency).
 */
private class DefaultIshaaraHttpClient(
    private val config: NetworkConfig
) : IshaaraHttpClient {

    override suspend fun execute(
        request: HttpRequest
    ): IshaaraResult<HttpResponse> {

        val tag = "IshaaraHttpClient"
        Log.d(tag, "--> ${request.method} ${request.url}")
        request.headers.forEach { (k, v) -> Log.d(tag, "$k: $v") }
        request.body?.let { Log.d(tag, "Body: $it") }

        return try {

            val finalUrl = buildUrl(
                request.url,
                request.queryParams
            )

            val connection =
                (java.net.URL(finalUrl).openConnection() as java.net.HttpURLConnection).apply {

                    requestMethod = request.method.name
                    connectTimeout = config.connectTimeoutMillis.toInt()
                    readTimeout = config.readTimeoutMillis.toInt()
                    doInput = true

                    request.headers.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }

                    if (
                        request.body != null &&
                        request.method != HttpMethod.GET
                    ) {
                        doOutput = true

                        outputStream.use {
                            it.write(request.body.toByteArray(Charsets.UTF_8))
                            it.flush()
                        }
                    }
                }

            val statusCode = connection.responseCode

            val responseBody =
                (
                        if (statusCode in 200..299)
                            connection.inputStream
                        else
                            connection.errorStream
                        )?.bufferedReader()?.use { it.readText() }.orEmpty()

            val responseHeaders =
                connection.headerFields
                    .filterKeys { it != null }
                    .mapValues { (_, values) ->
                        values.joinToString(",")
                    }

            Log.d(tag, "<-- $statusCode ${request.url}")
            Log.d(tag, "Response: $responseBody")

            connection.disconnect()

            if (statusCode in 200..299) {

                IshaaraResult.success(
                    HttpResponse(
                        statusCode = statusCode,
                        headers = responseHeaders,
                        body = responseBody
                    )
                )

            } else {

                IshaaraResult.failure(
                    ErrorNormalizer.normalize(
                        statusCode = statusCode,
                        body = responseBody
                    )
                )
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(tag, "Timeout: ${e.message}")
            IshaaraResult.failure(
                ErrorNormalizer.normalize(
                    statusCode = 408,
                    throwable = e,
                    body = null
                )
            )

        } catch (e: java.io.IOException) {
            Log.e(tag, "IO Error: ${e.message}")
            IshaaraResult.failure(
                IshaaraError.Network(
                    message = e.localizedMessage ?: "Network error",
                    cause = e
                )
            )

        } catch (e: Exception) {
            Log.e(tag, "Unknown Error: ${e.message}")
            IshaaraResult.failure(
                IshaaraError.Unknown(
                    message = e.localizedMessage ?: "Unknown error",
                    cause = e
                )
            )
        }
    }

    private fun buildUrl(
        base: String,
        queryParams: Map<String, String>
    ): String {

        if (queryParams.isEmpty()) return base

        val query = queryParams.entries.joinToString("&") {

            "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${
                java.net.URLEncoder.encode(it.value, "UTF-8")
            }"

        }

        return "$base?$query"
    }
}

/**
 * Default fallback LocationProvider.
 */
private class DefaultLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
        return IshaaraResult.success(
            LocationCoordinates(latitude = 18.5204, longitude = 73.8567)
        )
    }

    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = emptyFlow()

    override fun isLocationAvailable(): Boolean = true
}

/**
 * Default fallback RealtimeClient.
 */
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
