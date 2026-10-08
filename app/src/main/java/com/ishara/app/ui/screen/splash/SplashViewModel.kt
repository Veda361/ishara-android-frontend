package com.ishara.app.ui.screen.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.domain.repository.AuthRepository
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val navigationManager: NavigationManager
) : ViewModel() {

    fun checkSession() {
        Log.d("AUTH_DEBUG", "SplashViewModel: checkSession() called")
        viewModelScope.launch {
            // Minimum splash delay for branding
            val splashJob = launch { delay(2000) }

            Log.d("AUTH_DEBUG", "SplashViewModel: Validating session...")
            val result = authRepository.validateSession()
            
            splashJob.join()

            if (result.isSuccess) {
                val session = result.getOrNull()
                if (session != null) {
                    Log.d("AUTH_DEBUG", "SplashViewModel: Session valid, navigating for role: \${session.role}")
                    navigationManager.navigateForRole(session.role)
                } else {
                    Log.d("AUTH_DEBUG", "SplashViewModel: Session null, navigating to auth")
                    navigationManager.navigate("auth", popUpToRoute = "root", inclusive = true)
                }
            } else {
                Log.d("AUTH_DEBUG", "SplashViewModel: Session invalid, navigating to auth")
                navigationManager.navigate("auth", popUpToRoute = "root", inclusive = true)
            }
        }
    }
}
