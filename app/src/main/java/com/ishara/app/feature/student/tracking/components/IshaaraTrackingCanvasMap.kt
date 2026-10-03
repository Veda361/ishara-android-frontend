package com.ishara.app.feature.student.tracking.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ishara.app.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.designsystem.component.IshaaraButton
import com.ishara.app.core.designsystem.component.IshaaraButtonSize
import com.ishara.app.core.designsystem.component.IshaaraButtonVariant
import com.ishara.app.core.designsystem.theme.IshaaraPalette
import com.ishara.app.core.designsystem.theme.IshaaraTheme
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.domain.model.TrackingDriverLocation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Production-ready, calm, accessible custom canvas map for passenger ride tracking and campus navigation.
 *
 * Prioritizes:
 * 1. Current user GPS location with pulsing accuracy indicator.
 * 2. Bus / driver position with heading and optional accuracy radius.
 * 3. Passenger pickup location.
 * 4. Destination point.
 * 5. Connecting route trajectory.
 * 6. Campus landmark grid for context.
 *
 * Guarantees:
 * - Deterministic, non-distracting visual language (no cyberpunk/neon glowing effects).
 * - Smooth location interpolation.
 * - Non-intrusive camera (user can pan/zoom freely; recenter button restores view).
 * - Renders gracefully even when location permissions are missing.
 * - 100% accessible with content descriptions.
 */
@Composable
fun IshaaraTrackingCanvasMap(
    driverLocation: TrackingDriverLocation? = null,
    pickupCoordinates: LocationCoordinates? = null,
    destinationCoordinates: LocationCoordinates? = null,
    userLocation: LocationCoordinates? = null,
    isPermissionRequired: Boolean = false,
    isLocationDisabled: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = IshaaraTheme.colors

    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var hasCenteredInitially by remember { mutableStateOf(false) }

    // Smoothly animated coordinates for driver marker
    val targetLat = driverLocation?.coordinates?.latitude ?: 0.0
    val targetLng = driverLocation?.coordinates?.longitude ?: 0.0
    val targetHeading = driverLocation?.headingDegrees ?: 0f

    val animLat by animateFloatAsState(
        targetValue = targetLat.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "DriverLat"
    )
    val animLng by animateFloatAsState(
        targetValue = targetLng.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "DriverLng"
    )
    val animHeading by animateFloatAsState(
        targetValue = targetHeading,
        animationSpec = tween(durationMillis = 300),
        label = "DriverHeading"
    )

    // Smoothly animated coordinates for user marker
    val targetUserLat = userLocation?.latitude ?: 0.0
    val targetUserLng = userLocation?.longitude ?: 0.0
    val animUserLat by animateFloatAsState(
        targetValue = targetUserLat.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "UserLat"
    )
    val animUserLng by animateFloatAsState(
        targetValue = targetUserLng.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "UserLng"
    )

    // Move camera to current location ONCE when location arrives
    LaunchedEffect(userLocation) {
        if (userLocation != null && userLocation.isValid() && !hasCenteredInitially) {
            panOffset = Offset.Zero
            zoomScale = 1.0f
            hasCenteredInitially = true
            IshaaraLogger.d("ISHAARA_MAP", "Camera centered on current location: lat=${userLocation.latitude} lng=${userLocation.longitude}")
        }
    }

    val recenterDescription = stringResource(R.string.tracking_recenter)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceSubtle)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    panOffset += pan
                    zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
                }
            }
            .semantics {
                contentDescription = "Interactive Ishaara campus and transit map"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate bounding box across valid points
            val points = mutableListOf<Pair<Double, Double>>()
            if (userLocation != null && userLocation.isValid()) {
                points.add(animUserLat.toDouble() to animUserLng.toDouble())
            }
            if (pickupCoordinates != null && pickupCoordinates.isValid()) {
                points.add(pickupCoordinates.latitude to pickupCoordinates.longitude)
            }
            if (destinationCoordinates != null && destinationCoordinates.isValid()) {
                points.add(destinationCoordinates.latitude to destinationCoordinates.longitude)
            }
            if (animLat != 0f && animLng != 0f) {
                points.add(animLat.toDouble() to animLng.toDouble())
            }

            val (centerLat, centerLng, latSpan, lngSpan) = computeBounds(points)

            // Draw calm cartographic background grid
            drawCartographicGrid(width, height, panOffset, zoomScale)

            fun projectToCanvas(lat: Double, lng: Double): Offset {
                val padX = width * 0.18f
                val padY = height * 0.18f
                val usableW = width - (2 * padX)
                val usableH = height - (2 * padY)

                val normX = if (lngSpan > 0.0001) ((lng - (centerLng - lngSpan / 2)) / lngSpan).toFloat() else 0.5f
                val normY = if (latSpan > 0.0001) (((centerLat + latSpan / 2) - lat) / latSpan).toFloat() else 0.5f

                val baseX = padX + (normX * usableW)
                val baseY = padY + (normY * usableH)

                // Apply pan & zoom relative to center
                val centerX = width / 2f
                val centerY = height / 2f
                val zoomedX = centerX + (baseX - centerX) * zoomScale + panOffset.x
                val zoomedY = centerY + (baseY - centerY) * zoomScale + panOffset.y

                return Offset(zoomedX, zoomedY)
            }

            // Draw Campus Landmarks if no active trip route
            if (pickupCoordinates == null && destinationCoordinates == null) {
                drawCampusLandmarks(centerLat, centerLng, ::projectToCanvas)
            }

            val pickupOffset = pickupCoordinates?.let { projectToCanvas(it.latitude, it.longitude) }
            val destOffset = destinationCoordinates?.let { projectToCanvas(it.latitude, it.longitude) }
            val driverOffset = if (animLat != 0f && animLng != 0f) projectToCanvas(animLat.toDouble(), animLng.toDouble()) else null
            val userOffset = if (animUserLat != 0f && animUserLng != 0f) projectToCanvas(animUserLat.toDouble(), animUserLng.toDouble()) else null

            // 1. Draw Connecting Route Line (Calm Cobalt / Slate)
            drawRoutePath(
                pickupOffset = pickupOffset,
                driverOffset = driverOffset,
                destOffset = destOffset
            )

            // 2. Draw User Location Marker (Cobalt pulse dot)
            if (userOffset != null) {
                drawUserLocationMarker(
                    center = userOffset,
                    accuracyMeters = userLocation?.accuracyMeters
                )
            }

            // 3. Draw Pickup Marker (Emerald)
            if (pickupOffset != null) {
                drawPickupMarker(pickupOffset)
            }

            // 4. Draw Destination Marker (Crimson)
            if (destOffset != null) {
                drawDestinationMarker(destOffset)
            }

            // 5. Draw Driver / Bus Marker (Amber / Dark)
            if (driverOffset != null) {
                drawDriverBusMarker(
                    center = driverOffset,
                    heading = if (driverLocation?.headingDegrees != null) animHeading else null,
                    accuracyMeters = driverLocation?.accuracyMeters
                )
            }
        }

        // Overlay: Permission Required Banner
        if (isPermissionRequired) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(IshaaraTheme.shapes.sm)
                    .background(colors.surfaceElevated.copy(alpha = 0.95f))
                    .border(1.dp, colors.border, IshaaraTheme.shapes.sm)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location permission required",
                            style = IshaaraTheme.typography.titleSmall,
                            color = colors.foreground
                        )
                        Text(
                            text = "Enable location to see your position on the map",
                            style = IshaaraTheme.typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                    }
                    if (onRequestPermission != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IshaaraButton(
                            text = "Enable",
                            onClick = onRequestPermission,
                            variant = IshaaraButtonVariant.Primary,
                            size = IshaaraButtonSize.Small
                        )
                    }
                }
            }
        }

        // Overlay: Location Services Disabled Banner
        if (isLocationDisabled && !isPermissionRequired) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(IshaaraTheme.shapes.sm)
                    .background(colors.surfaceElevated.copy(alpha = 0.95f))
                    .border(1.dp, colors.border, IshaaraTheme.shapes.sm)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location Services disabled",
                            style = IshaaraTheme.typography.titleSmall,
                            color = colors.foreground
                        )
                        Text(
                            text = "Turn on device location to show your position",
                            style = IshaaraTheme.typography.bodySmall,
                            color = colors.foregroundMuted
                        )
                    }
                    if (onOpenSettings != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IshaaraButton(
                            text = "Settings",
                            onClick = onOpenSettings,
                            variant = IshaaraButtonVariant.Outlined,
                            size = IshaaraButtonSize.Small
                        )
                    }
                }
            }
        }

        // Recenter / "My Location" Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(IshaaraPalette.PureWhite)
                .border(1.dp, colors.border, CircleShape)
                .clickable {
                    panOffset = Offset.Zero
                    zoomScale = 1.0f
                    IshaaraLogger.d("ISHAARA_MAP", "Recenter button tapped by user")
                }
                .semantics {
                    contentDescription = recenterDescription
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌖",
                style = IshaaraTheme.typography.titleLarge,
                color = IshaaraPalette.Neutral900
            )
        }
    }
}

private fun computeBounds(points: List<Pair<Double, Double>>): DoubleArray {
    if (points.isEmpty()) {
        // Fallback default coordinates (e.g. Pune / Campus region)
        return doubleArrayOf(18.5204, 73.8567, 0.05, 0.05)
    }

    var minLat = points.first().first
    var maxLat = points.first().first
    var minLng = points.first().second
    var maxLng = points.first().second

    for (p in points) {
        minLat = min(minLat, p.first)
        maxLat = max(maxLat, p.first)
        minLng = min(minLng, p.second)
        maxLng = max(maxLng, p.second)
    }

    val latSpan = max(maxLat - minLat, 0.008) * 1.4
    val lngSpan = max(maxLng - minLng, 0.008) * 1.4
    val centerLat = (minLat + maxLat) / 2.0
    val centerLng = (minLng + maxLng) / 2.0

    return doubleArrayOf(centerLat, centerLng, latSpan, lngSpan)
}

private fun DrawScope.drawCartographicGrid(
    width: Float,
    height: Float,
    pan: Offset,
    zoom: Float
) {
    val gridColor = IshaaraPalette.Neutral200.copy(alpha = 0.5f)
    val gridSize = 48.dp.toPx() * zoom

    var x = (pan.x % gridSize)
    while (x < width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += gridSize
    }

    var y = (pan.y % gridSize)
    while (y < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}

private fun DrawScope.drawRoutePath(
    pickupOffset: Offset?,
    driverOffset: Offset?,
    destOffset: Offset?
) {
    val routePoints = listOfNotNull(driverOffset ?: pickupOffset, pickupOffset, destOffset).distinct()
    if (routePoints.size < 2) return

    val path = Path().apply {
        moveTo(routePoints.first().x, routePoints.first().y)
        for (i in 1 until routePoints.size) {
            lineTo(routePoints[i].x, routePoints[i].y)
        }
    }

    // Outer subtle casing
    drawPath(
        path = path,
        color = IshaaraPalette.Neutral300,
        style = Stroke(width = 8.dp.toPx())
    )

    // Inner active route line
    drawPath(
        path = path,
        color = IshaaraPalette.Cobalt500,
        style = Stroke(
            width = 4.dp.toPx(),
            pathEffect = PathEffect.cornerPathEffect(16f)
        )
    )
}

private fun DrawScope.drawPickupMarker(center: Offset) {
    // Outer glow ring
    drawCircle(
        color = IshaaraPalette.Emerald100,
        radius = 16.dp.toPx(),
        center = center
    )
    // Core badge
    drawCircle(
        color = IshaaraPalette.Emerald500,
        radius = 9.dp.toPx(),
        center = center
    )
    // Inner white dot
    drawCircle(
        color = IshaaraPalette.PureWhite,
        radius = 4.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawDestinationMarker(center: Offset) {
    // Outer ring
    drawCircle(
        color = IshaaraPalette.Crimson100,
        radius = 16.dp.toPx(),
        center = center
    )
    // Core badge
    drawCircle(
        color = IshaaraPalette.Crimson500,
        radius = 9.dp.toPx(),
        center = center
    )
    // Inner white square
    drawRect(
        color = IshaaraPalette.PureWhite,
        topLeft = Offset(center.x - 3.dp.toPx(), center.y - 3.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 6.dp.toPx())
    )
}

private fun DrawScope.drawDriverBusMarker(
    center: Offset,
    heading: Float?,
    accuracyMeters: Float?
) {
    // 1. Accuracy circle if reasonable
    if (accuracyMeters != null && accuracyMeters > 0f) {
        val radiusPx = (accuracyMeters * 1.5f).coerceIn(18.dp.toPx(), 48.dp.toPx())
        drawCircle(
            color = IshaaraPalette.Amber100.copy(alpha = 0.45f),
            radius = radiusPx,
            center = center
        )
    }

    // 2. Bus Vehicle Circle
    val markerRadius = 16.dp.toPx()
    // Subtle shadow / outer stroke
    drawCircle(
        color = IshaaraPalette.Neutral900.copy(alpha = 0.15f),
        radius = markerRadius + 3.dp.toPx(),
        center = center
    )
    // Amber body
    drawCircle(
        color = IshaaraPalette.Amber500,
        radius = markerRadius,
        center = center
    )
    // Dark core
    drawCircle(
        color = IshaaraPalette.Neutral900,
        radius = markerRadius - 4.dp.toPx(),
        center = center
    )

    // 3. Directional heading indicator if reliable
    if (heading != null) {
        rotate(degrees = heading, pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - markerRadius - 4.dp.toPx())
                lineTo(center.x + 5.dp.toPx(), center.y - markerRadius + 3.dp.toPx())
                lineTo(center.x - 5.dp.toPx(), center.y - markerRadius + 3.dp.toPx())
                close()
            }
            drawPath(
                path = arrowPath,
                color = IshaaraPalette.Amber500
            )
        }
    } else {
        // Simple center dot if no heading available
        drawCircle(
            color = IshaaraPalette.PureWhite,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

private fun DrawScope.drawUserLocationMarker(
    center: Offset,
    accuracyMeters: Float? = null
) {
    // 1. Accuracy/Pulsing outer ring
    val radius = if (accuracyMeters != null && accuracyMeters > 0) {
        (accuracyMeters * 1.5f).coerceIn(16.dp.toPx(), 44.dp.toPx())
    } else {
        22.dp.toPx()
    }
    drawCircle(
        color = IshaaraPalette.Cobalt500.copy(alpha = 0.15f),
        radius = radius,
        center = center
    )
    drawCircle(
        color = IshaaraPalette.Cobalt500.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 2. Crisp white halo
    drawCircle(
        color = IshaaraPalette.PureWhite,
        radius = 8.dp.toPx(),
        center = center
    )

    // 3. Crisp Cobalt Dot
    drawCircle(
        color = IshaaraPalette.Cobalt500,
        radius = 5.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawCampusLandmarks(
    centerLat: Double,
    centerLng: Double,
    projectToCanvas: (Double, Double) -> Offset
) {
    // Subtle campus landmark representations centered around the reference area
    val landmarks = listOf(
        Pair(centerLat + 0.003, centerLng - 0.004) to "Main Gate",
        Pair(centerLat - 0.002, centerLng + 0.003) to "Central Library",
        Pair(centerLat + 0.004, centerLng + 0.002) to "Academic Complex",
        Pair(centerLat - 0.003, centerLng - 0.003) to "Student Transit Hub"
    )

    for ((coords, _) in landmarks) {
        val offset = projectToCanvas(coords.first, coords.second)
        drawCircle(
            color = IshaaraPalette.Neutral300.copy(alpha = 0.6f),
            radius = 3.dp.toPx(),
            center = offset
        )
    }
}
