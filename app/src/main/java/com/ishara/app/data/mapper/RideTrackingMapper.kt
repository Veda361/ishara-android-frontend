package com.ishara.app.data.mapper

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.data.remote.dto.RideDriverLocationResponseDto
import com.ishara.app.data.remote.dto.RideTrackingUpdatedPayloadDto
import com.ishara.app.data.remote.dto.TrackingCoordinatesDto
import com.ishara.app.data.remote.dto.TrackingDriverInfoDto
import com.ishara.app.data.remote.dto.TrackingEtaInfoDto
import com.ishara.app.data.remote.dto.TrackingResponseDto
import com.ishara.app.data.remote.dto.TrackingRouteInfoDto
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingDriverLocation
import com.ishara.app.domain.model.TrackingEta
import com.ishara.app.domain.model.TrackingFreshness
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.model.TrackingRouteProgress
import com.ishara.app.domain.model.TrackingState

object RideTrackingMapper {

    fun mapCoordinates(dto: TrackingCoordinatesDto?): LocationCoordinates? {
        if (dto == null) return null
        if (!isValidCoordinate(dto.latitude, dto.longitude)) return null
        return LocationCoordinates(
            latitude = dto.latitude,
            longitude = dto.longitude
        )
    }

    fun mapDriverInfo(dto: TrackingDriverInfoDto?): TrackingDriverLocation? {
        if (dto == null) return null
        val coords = mapCoordinates(dto.location) ?: return null

        return TrackingDriverLocation(
            coordinates = coords,
            accuracyMeters = dto.accuracyMeters?.toFloat(),
            headingDegrees = dto.headingDegrees?.toFloat(),
            speedMps = dto.speedMps?.toFloat(),
            recordedAt = dto.recordedAt,
            receivedAt = dto.receivedAt,
            freshness = TrackingFreshness.fromBackend(dto.freshness)
        )
    }

    fun mapDriverLocationResponse(dto: RideDriverLocationResponseDto): TrackingDriverLocation? {
        val coords = mapCoordinates(dto.location) ?: return null
        return TrackingDriverLocation(
            coordinates = coords,
            accuracyMeters = dto.accuracyMeters?.toFloat(),
            headingDegrees = dto.headingDegrees?.toFloat(),
            speedMps = dto.speedMps?.toFloat(),
            recordedAt = dto.recordedAt,
            receivedAt = dto.receivedAt,
            freshness = TrackingFreshness.fromBackend(dto.status)
        )
    }

    fun mapRouteInfo(dto: TrackingRouteInfoDto?): TrackingRouteProgress? {
        if (dto == null) return null
        return TrackingRouteProgress(
            totalDistanceMeters = dto.distanceMeters.toInt(),
            completedDistanceMeters = dto.completedDistanceMeters.toInt(),
            remainingDistanceMeters = dto.remainingDistanceMeters.toInt(),
            progressPercent = dto.progressPercent.toFloat(),
            distanceFromRouteMeters = dto.distanceFromRouteMeters.toFloat(),
            isOffRoute = dto.isOffRoute
        )
    }

    fun mapEta(dto: TrackingEtaInfoDto?): TrackingEta {
        if (dto == null) return TrackingEta(available = false)
        return TrackingEta(
            available = dto.available,
            seconds = dto.seconds,
            source = dto.source,
            confidence = dto.confidence
        )
    }

    fun mapTrackingResponse(
        dto: TrackingResponseDto,
        pickupAddress: String? = null,
        destinationAddress: String? = null,
        pickupCoords: LocationCoordinates? = null,
        destinationCoords: LocationCoordinates? = null
    ): RideTrackingSnapshot {
        return RideTrackingSnapshot(
            rideId = dto.rideId,
            status = TrackingRideStatus.fromBackend(dto.status),
            trackingState = TrackingState.fromBackend(dto.trackingState),
            driverLocation = mapDriverInfo(dto.driver),
            routeProgress = mapRouteInfo(dto.route),
            distanceToPickupMeters = dto.distanceToPickupMeters?.toInt(),
            distanceToDestinationMeters = dto.distanceToDestinationMeters?.toInt(),
            eta = mapEta(dto.eta),
            updatedAt = dto.updatedAt,
            pickupAddress = pickupAddress,
            destinationAddress = destinationAddress,
            pickupCoordinates = pickupCoords,
            destinationCoordinates = destinationCoords
        )
    }

    /**
     * Applies a realtime update payload onto an existing authoritative snapshot.
     * Reconciles freshness, coordinates, distances, and ETA safely.
     */
    fun reconcileWithRealtimeUpdate(
        current: RideTrackingSnapshot,
        update: RideTrackingUpdatedPayloadDto
    ): RideTrackingSnapshot {
        val updatedDriverLocation = if (update.driverLocation != null) {
            val coords = mapCoordinates(update.driverLocation)
            if (coords != null) {
                current.driverLocation?.copy(
                    coordinates = coords,
                    freshness = TrackingFreshness.fromBackend(update.freshness),
                    recordedAt = update.recordedAt ?: current.driverLocation.recordedAt
                ) ?: TrackingDriverLocation(
                    coordinates = coords,
                    freshness = TrackingFreshness.fromBackend(update.freshness),
                    recordedAt = update.recordedAt
                )
            } else {
                current.driverLocation
            }
        } else {
            current.driverLocation
        }

        val updatedRouteProgress = if (update.routeProgress != null) {
            current.routeProgress?.copy(
                completedDistanceMeters = update.routeProgress.completedDistanceMeters.toInt(),
                remainingDistanceMeters = update.routeProgress.remainingDistanceMeters.toInt(),
                progressPercent = update.routeProgress.progressPercent.toFloat()
            ) ?: TrackingRouteProgress(
                totalDistanceMeters = (update.routeProgress.completedDistanceMeters + update.routeProgress.remainingDistanceMeters).toInt(),
                completedDistanceMeters = update.routeProgress.completedDistanceMeters.toInt(),
                remainingDistanceMeters = update.routeProgress.remainingDistanceMeters.toInt(),
                progressPercent = update.routeProgress.progressPercent.toFloat()
            )
        } else {
            current.routeProgress
        }

        return current.copy(
            trackingState = update.trackingState?.let { TrackingState.fromBackend(it) } ?: current.trackingState,
            driverLocation = updatedDriverLocation,
            routeProgress = updatedRouteProgress,
            distanceToPickupMeters = update.distanceToPickupMeters?.toInt() ?: current.distanceToPickupMeters,
            distanceToDestinationMeters = update.distanceToDestinationMeters?.toInt() ?: current.distanceToDestinationMeters,
            eta = update.eta?.let { mapEta(it) } ?: current.eta,
            updatedAt = update.recordedAt ?: current.updatedAt
        )
    }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0 && (lat != 0.0 || lng != 0.0)
    }
}
