package com.ishara.app.feature.auth

import com.ishara.app.core.common.UiState
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.UserRole

data class AuthUiState(
    val operationState: UiState<Unit> = UiState.Idle,
    val session: AuthSession? = null,
    val role: UserRole? = null
) {
    val isAuthenticated: Boolean
        get() = session != null && !session.isExpired
}
