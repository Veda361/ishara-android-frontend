package com.ishara.app.ui.map

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun IshaaraMap() {

    val vit = LatLng(18.4575, 73.8508)

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vit, 16f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        properties = MapProperties(
            isMyLocationEnabled = false
        ),
        onMapLoaded = {
            Log.d("MAP_TEST", "MAP LOADED SUCCESSFULLY")
        }
    ) {

        val markerState = remember {
            MarkerState(position = vit)
        }

        Marker(
            state = markerState,
            title = "VIT Pune"
        )

    }
}