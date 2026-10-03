package com.ishara.app.domain.usecase

import com.ishara.app.core.realtime.RealtimeConnectionState
import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.RideTrackingRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe realtime live ride tracking events and connection states.
 */
class ObserveRideTrackingUseCase(
    private val repository: RideTrackingRepository
) {
    fun observeEvents(rideId: String): Flow<RealtimeEvent> {
        return repository.observeRideTrackingEvents(rideId)
    }

    fun observeConnectionState(): Flow<RealtimeConnectionState> {
        return repository.observeConnectionState()
    }

    suspend fun subscribe(rideId: String): IshaaraResult<Unit> {
        return repository.subscribeToRideTracking(rideId)
    }

    suspend fun unsubscribe(rideId: String): IshaaraResult<Unit> {
        return repository.unsubscribeFromRideTracking(rideId)
    }
}
