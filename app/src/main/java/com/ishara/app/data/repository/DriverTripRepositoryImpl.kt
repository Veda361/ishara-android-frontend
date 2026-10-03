package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.TripMapper
import com.ishara.app.data.remote.datasource.TripRemoteDataSource
import com.ishara.app.data.remote.dto.CancelTripRequestDto
import com.ishara.app.domain.model.CreateTripParams
import com.ishara.app.domain.model.Trip
import com.ishara.app.domain.model.TripStatus
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.DriverTripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of [DriverTripRepository].
 *
 * Core Guarantees:
 * 1. Backend Source of Truth: All mutations and states are determined authoritatively by the backend.
 * 2. Strict Role Boundary: Only DRIVER_CONDUCTOR sessions may access driver trip resources.
 * 3. Transient Caching: In-memory cache is used strictly for UI responsiveness, never as local authorization.
 * 4. Data Isolation & Account Switching: Clears cached state immediately on sign out or account switch.
 * 5. Concurrency Safety: Mutex protection prevents duplicate parallel network mutations.
 */
class DriverTripRepositoryImpl(
    private val remoteDataSource: TripRemoteDataSource,
    private val localDataSource: SessionLocalDataSource
) : DriverTripRepository {

    private val mutex = Mutex()
    private val _driverTripsFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val _activeTripFlow = MutableStateFlow<Trip?>(null)

    override fun observeDriverTrips(): StateFlow<List<Trip>> = _driverTripsFlow.asStateFlow()

    override fun observeActiveTrip(): StateFlow<Trip?> = _activeTripFlow.asStateFlow()

    override suspend fun getDriverTrips(
        status: TripStatus?,
        page: Int,
        limit: Int,
        forceRefresh: Boolean
    ): IshaaraResult<List<Trip>> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Trip operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        if (!forceRefresh) {
            val cached = _driverTripsFlow.value
            if (cached.isNotEmpty()) {
                val filtered = if (status != null) cached.filter { it.status == status } else cached
                return IshaaraResult.success(filtered)
            }
        }

        return fetchDriverTripsFromBackend(
            token = session.token,
            page = page,
            limit = limit,
            status = status
        )
    }

    override suspend fun refreshDriverTrips(status: TripStatus?): IshaaraResult<List<Trip>> {
        return getDriverTrips(status = status, page = 1, limit = 50, forceRefresh = true)
    }

    private suspend fun fetchDriverTripsFromBackend(
        token: String,
        page: Int,
        limit: Int,
        status: TripStatus?
    ): IshaaraResult<List<Trip>> {
        val result = remoteDataSource.listDriverTrips(
            token = token,
            page = page,
            limit = limit,
            status = status?.name
        )

        return when (result) {
            is IshaaraResult.Success -> {
                val trips = result.data.map { TripMapper.toDomain(it) }
                _driverTripsFlow.value = trips

                // Update active trip cache (strictly ACTIVE state)
                val active = trips.firstOrNull { it.status == TripStatus.ACTIVE }
                _activeTripFlow.value = active

                IshaaraLogger.d(TAG, "Fetched ${trips.size} trips for driver. Active trip: ${active?.id}")
                IshaaraResult.success(trips)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to fetch driver trips: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun getTripDetails(
        tripId: String,
        forceRefresh: Boolean
    ): IshaaraResult<Trip> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Trip operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        if (!forceRefresh) {
            val cached = _driverTripsFlow.value.find { it.id == tripId }
            if (cached != null) {
                return IshaaraResult.success(cached)
            }
        }

        val result = remoteDataSource.getDriverTripDetails(tripId = tripId, token = session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val trip = TripMapper.toDomain(result.data)
                updateTripInCache(trip)
                IshaaraResult.success(trip)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to retrieve trip $tripId: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun createTrip(params: CreateTripParams): IshaaraResult<Trip> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Trip operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        val requestDto = TripMapper.toCreateRequestDto(params)
        val result = remoteDataSource.createDriverTrip(request = requestDto, token = session.token)

        return when (result) {
            is IshaaraResult.Success -> {
                val createdTrip = TripMapper.toDomain(result.data)
                updateTripInCache(createdTrip)
                IshaaraLogger.i(TAG, "Trip created successfully: ${createdTrip.id}")
                IshaaraResult.success(createdTrip)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to create trip: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun startTrip(tripId: String): IshaaraResult<Trip> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Trip operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        if (session.token.isBlank()) {
            return IshaaraResult.failure(IshaaraError.Authentication(message = "Authentication required. Please sign in."))
        }

        val cleanToken = session.token.trim()
        IshaaraLogger.d(
            TAG,
            "[SafeDiagnostic] Executing startTrip($tripId) | Authorization present = true | token length = ${cleanToken.length} | token source = EncryptedSessionStore"
        )

        val result = remoteDataSource.startDriverTrip(tripId = tripId, token = cleanToken)
        return when (result) {
            is IshaaraResult.Success -> {
                val updatedTrip = TripMapper.toDomain(result.data)
                _activeTripFlow.value = updatedTrip
                updateTripInCache(updatedTrip)
                IshaaraLogger.i(TAG, "Trip started successfully: ${updatedTrip.id} (status: ${updatedTrip.status})")
                IshaaraResult.success(updatedTrip)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to start trip $tripId: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun completeTrip(tripId: String): IshaaraResult<Trip> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Trip operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        val cleanToken = session.token.trim()
        val result = remoteDataSource.completeDriverTrip(tripId = tripId, token = cleanToken)
        return when (result) {
            is IshaaraResult.Success -> {
                val updatedTrip = TripMapper.toDomain(result.data)
                if (_activeTripFlow.value?.id == tripId) {
                    _activeTripFlow.value = null
                }
                updateTripInCache(updatedTrip)
                IshaaraLogger.i(TAG, "Trip completed successfully: ${updatedTrip.id}")
                IshaaraResult.success(updatedTrip)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to complete trip $tripId: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun cancelTrip(tripId: String, reason: String?): IshaaraResult<Trip> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        if (session.role != UserRole.DRIVER_CONDUCTOR) {
            return IshaaraResult.failure(
                IshaaraError.Forbidden(
                    message = "Trip operations are restricted to DRIVER_CONDUCTOR role.",
                    errorCode = "FORBIDDEN"
                )
            )
        }

        val cleanToken = session.token.trim()
        val requestDto = if (!reason.isNullOrBlank()) CancelTripRequestDto(reason = reason.trim()) else null
        val result = remoteDataSource.cancelDriverTrip(tripId = tripId, request = requestDto, token = cleanToken)
        return when (result) {
            is IshaaraResult.Success -> {
                val updatedTrip = TripMapper.toDomain(result.data)
                if (_activeTripFlow.value?.id == tripId) {
                    _activeTripFlow.value = null
                }
                updateTripInCache(updatedTrip)
                IshaaraLogger.i(TAG, "Trip cancelled successfully: ${updatedTrip.id}")
                IshaaraResult.success(updatedTrip)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.e(TAG, "Failed to cancel trip $tripId: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override fun clearTripState() {
        _activeTripFlow.value = null
        _driverTripsFlow.value = emptyList()
        IshaaraLogger.d(TAG, "Flushed all in-memory trip caches on account switch/sign out.")
    }

    private fun updateTripInCache(trip: Trip) {
        val currentList = _driverTripsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == trip.id }
        if (index != -1) {
            currentList[index] = trip
        } else {
            currentList.add(0, trip)
        }
        _driverTripsFlow.value = currentList

        if (trip.status == TripStatus.ACTIVE) {
            _activeTripFlow.value = trip
        } else if (_activeTripFlow.value?.id == trip.id && trip.isTerminal) {
            _activeTripFlow.value = null
        }
    }

    companion object {
        private const val TAG = "DriverTripRepo"
    }
}
