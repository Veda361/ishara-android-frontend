package com.ishara.app.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ishara.app.feature.auth.LoginViewModel
import com.ishara.app.ui.screen.splash.SplashViewModel
import com.ishara.app.ui.screen.passenger.profile.ProfileViewModel

class ViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    signInWithGoogleUseCase = appContainer.signInWithGoogleUseCase,
                    authRepository = appContainer.authRepository,
                    navigationManager = appContainer.navigationManager,
                    googleSignInManager = appContainer.googleSignInManager
                ) as T
            }
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(
                    authRepository = appContainer.authRepository,
                    navigationManager = appContainer.navigationManager
                ) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(
                    authRepository = appContainer.authRepository,
                    navigationManager = appContainer.navigationManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
