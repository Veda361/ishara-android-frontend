package com.ishara.app.ui.screen.passenger.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.domain.repository.AuthRepository
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val navigationManager: NavigationManager
) : ViewModel() {

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            navigationManager.navigate("auth", popUpToRoute = "root", inclusive = true)
        }
    }
}
