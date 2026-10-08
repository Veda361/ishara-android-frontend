package com.ishara.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.ishara.app.ui.theme.IsharaTheme

class MainActivity : ComponentActivity(), OnMapsSdkInitializedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("AUTH_DEBUG", "MainActivity: onCreate")
        Log.d("AUTH_DEBUG", "BuildConfig.BASE_URL: ${BuildConfig.BASE_URL}")
        Log.d("AUTH_DEBUG", "BuildConfig.GOOGLE_WEB_CLIENT_ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")

        // Initialize Google Maps SDK with the legacy renderer to ensure stability on older devices
        MapsInitializer.initialize(
            applicationContext,
            Renderer.LEGACY,
            this
        )

        enableEdgeToEdge()

        setContent {
            IsharaTheme {
                IsharaApp()
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: Renderer) {
        Log.d("MAP_TEST", "Maps SDK initialized with renderer: $renderer")
    }
}
