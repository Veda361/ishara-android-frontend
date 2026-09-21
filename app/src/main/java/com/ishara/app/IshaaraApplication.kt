package com.ishara.app

import android.app.Application
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.di.AppContainer
import com.ishara.app.core.di.DefaultAppContainer

/**
 * Custom Application class for Ishaara.
 * Initializes the Dependency Injection AppContainer and process-level infrastructure.
 */
class IshaaraApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        IshaaraLogger.i(TAG, "Initializing Ishaara application...")
        appContainer = DefaultAppContainer(this)
        IshaaraLogger.i(TAG, "Ishaara application initialized successfully.")
    }

    companion object {
        private const val TAG = "IshaaraApp"
    }
}
