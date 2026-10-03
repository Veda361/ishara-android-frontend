package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.SessionInvalidationCoordinator
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.AuthMapper
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of AuthRepository.
 * Manages the domain AuthState state machine, session restoration, secure storage,
 * and 401 session invalidation coordination.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: SessionLocalDataSource,
    private val sessionCoordinator: SessionInvalidationCoordinator? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    private val authMutex = Mutex()

    init {
        // Listen for centralized 401 invalidation events
        sessionCoordinator?.let { coordinator ->
            scope.launch {
                coordinator.invalidationEvents.collect { reason ->
                    invalidateSession(reason)
                }
            }
        }
    }

    override fun observeAuthState(): StateFlow<AuthState> = _authState.asStateFlow()

    override fun observeSession(): Flow<AuthSession?> = localDataSource.observeSession()

    override suspend fun getCurrentSession(): IshaaraResult<AuthSession?> {
        return IshaaraResult.success(localDataSource.getSession())
    }

    override suspend fun restoreSession(): IshaaraResult<AuthSession?> = authMutex.withLock {
        val storedSession = localDataSource.getSession()

        if (storedSession == null) {
            _authState.value = AuthState.Unauthenticated
            return IshaaraResult.success(null)
        }

        // Check local expiration if timestamp is present
        if (storedSession.isExpired) {
            IshaaraLogger.i(TAG, "Stored session has expired locally; clearing.")
            localDataSource.clearSession()
            _authState.value = AuthState.SessionExpired("Your session has expired. Please sign in again.")
            return IshaaraResult.success(null)
        }

        // Attempt online verification with fallback for offline mode
        val remoteUserResult = remoteDataSource.getCurrentUser(storedSession.token)
        return when (remoteUserResult) {
            is IshaaraResult.Success -> {
                val user = AuthMapper.toDomain(remoteUserResult.data)
                val finalSession = if (user.role != null && user.role != storedSession.role) {
                    val s = storedSession.copy(role = user.role)
                    localDataSource.saveSession(s)
                    s
                } else {
                    storedSession
                }
                _authState.value = AuthState.Authenticated(finalSession, user)
                sessionCoordinator?.reset()
                IshaaraResult.success(finalSession)
            }
            is IshaaraResult.Failure -> {
                when (val error = remoteUserResult.error) {
                    is IshaaraError.Authentication -> {
                        IshaaraLogger.w(TAG, "Session verification rejected by server (401/403).")
                        localDataSource.clearSession()
                        _authState.value = AuthState.SessionExpired(error.message)
                        IshaaraResult.success(null)
                    }
                    is IshaaraError.Network -> {
                        // Offline resilience: Preserve valid local session when network is unreachable
                        IshaaraLogger.i(TAG, "Network unavailable during startup; preserving valid local session.")
                        _authState.value = AuthState.Authenticated(storedSession, null)
                        IshaaraResult.success(storedSession)
                    }
                    else -> {
                        // For transient server errors, keep valid local session
                        IshaaraLogger.w(TAG, "Server error during startup check: ${error.message}; keeping local session.")
                        _authState.value = AuthState.Authenticated(storedSession, null)
                        IshaaraResult.success(storedSession)
                    }
                }
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSession> = authMutex.withLock {
        val remoteResult = remoteDataSource.signInWithGoogle(idToken)
        return when (remoteResult) {
            is IshaaraResult.Success -> {
                var session = AuthMapper.toDomain(remoteResult.data)
                localDataSource.saveSession(session)
                sessionCoordinator?.reset()

                // Fetch authoritative user profile from /api/v1/users/me
                val userResult = remoteDataSource.getCurrentUser(session.token)
                val user = if (userResult is IshaaraResult.Success) {
                    val u = AuthMapper.toDomain(userResult.data)
                    if (u.role != null) {
                        session = session.copy(role = u.role)
                        localDataSource.saveSession(session)
                    }
                    u
                } else null

                _authState.value = AuthState.Authenticated(session, user)
                IshaaraLogger.i(TAG, "Sign-in successful for user ${session.userId} with role ${session.role}")
                IshaaraResult.success(session)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(TAG, "Google sign-in failed: ${remoteResult.error.message}")
                _authState.value = AuthState.Error(remoteResult.error)
                IshaaraResult.failure(remoteResult.error)
            }
        }
    }

    override suspend fun sendEmailOtp(email: String): IshaaraResult<Unit> {
        return remoteDataSource.sendVerificationOtp(email)
    }

    override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSession> = authMutex.withLock {
        val remoteResult = remoteDataSource.signInWithEmailOtp(email, otp)
        return when (remoteResult) {
            is IshaaraResult.Success -> {
                var session = AuthMapper.toDomain(remoteResult.data)
                localDataSource.saveSession(session)
                sessionCoordinator?.reset()

                // Verify session via GET /api/auth/get-session
                val sessionResult = remoteDataSource.getCurrentSession(session.token)
                if (sessionResult is IshaaraResult.Success) {
                    val sDto = sessionResult.data
                    val sessionRole = UserRole.fromBackendString(sDto.role)
                    if (sessionRole != null) {
                        session = session.copy(role = sessionRole)
                        localDataSource.saveSession(session)
                    }
                }

                // Fetch authoritative user profile from /api/v1/users/me
                val userResult = remoteDataSource.getCurrentUser(session.token)
                val user = if (userResult is IshaaraResult.Success) {
                    val u = AuthMapper.toDomain(userResult.data)
                    if (u.role != null) {
                        session = session.copy(role = u.role)
                        localDataSource.saveSession(session)
                    }
                    u
                } else null

                _authState.value = AuthState.Authenticated(session, user)
                IshaaraLogger.i(TAG, "Email OTP sign-in successful for user ${session.userId} with role ${session.role}")
                IshaaraResult.success(session)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(TAG, "Email OTP sign-in failed: ${remoteResult.error.message}")
                _authState.value = AuthState.Error(remoteResult.error)
                IshaaraResult.failure(remoteResult.error)
            }
        }
    }

    override suspend fun completeOnboarding(role: UserRole): IshaaraResult<User> = authMutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val result = remoteDataSource.completeOnboarding(role.name, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val user = AuthMapper.toDomain(result.data)
                val updatedSession = session.copy(role = role)
                localDataSource.saveSession(updatedSession)
                _authState.value = AuthState.Authenticated(updatedSession, user)
                IshaaraResult.success(user)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun getCurrentUser(): IshaaraResult<User> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.getCurrentUser(session.token).map { dto ->
            AuthMapper.toDomain(dto)
        }
    }

    override suspend fun invalidateSession(reason: String) = authMutex.withLock {
        IshaaraLogger.w(TAG, "Invalidating session: $reason")
        localDataSource.clearSession()
        _authState.value = AuthState.SessionExpired(reason)
    }

    override suspend fun signOut(): IshaaraResult<Unit> = authMutex.withLock {
        try {
            val session = localDataSource.getSession()
            if (session != null) {
                // Best-effort remote sign out
                runCatching { remoteDataSource.signOut(session.token) }
            }
        } finally {
            localDataSource.clearSession()
            _authState.value = AuthState.Unauthenticated
            IshaaraLogger.i(TAG, "User signed out completely.")
        }
        return IshaaraResult.success(Unit)
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
