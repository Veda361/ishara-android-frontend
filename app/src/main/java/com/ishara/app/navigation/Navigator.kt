package com.ishara.app.navigation

import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface NavigationCommand {
    data class NavigateTo(val route: String, val popUpToRoute: String? = null, val inclusive: Boolean = false) : NavigationCommand
    object NavigateUp : NavigationCommand
    data class SwitchToRole(val role: UserRole) : NavigationCommand
}

/**
 * Decoupled navigation coordinator.
 * ViewModels emit navigation commands without holding direct Activity or NavController references.
 */
class NavigationManager {
    private val _commands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 1)
    val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    fun navigate(route: String, popUpToRoute: String? = null, inclusive: Boolean = false) {
        _commands.tryEmit(NavigationCommand.NavigateTo(route, popUpToRoute, inclusive))
    }

    fun navigateUp() {
        _commands.tryEmit(NavigationCommand.NavigateUp)
    }

    fun navigateForRole(role: UserRole) {
        _commands.tryEmit(NavigationCommand.SwitchToRole(role))
    }
}
