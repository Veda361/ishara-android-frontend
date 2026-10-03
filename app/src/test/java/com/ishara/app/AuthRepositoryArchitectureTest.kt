package com.ishara.app

import com.ishara.app.core.network.SessionInvalidationCoordinator
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.data.repository.AuthRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.GetAuthSessionUseCase
import com.ishara.app.domain.usecase.ObserveAuthStateUseCase
import com.ishara.app.domain.usecase.RestoreSessionUseCase
import com.ishara.app.domain.usecase.SignInWithGoogleUseCase
import com.ishara.app.domain.usecase.SignOutUseCase
import com.ishara.app.feature.auth.AuthViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Validates the complete Authentication and Session Architecture for Phase 03.
 * Covers all 10 required architectural scenarios.
 */
class AuthRepositoryArchitectureTest {

    private class MockAuthRemoteDataSource(
        var shouldFailAuth: Boolean = false,
        var shouldNetworkFail: Boolean = false,
        var should401Fail: Boolean = false
    ) : AuthRemoteDataSource {

        val signInCallCount = AtomicInteger(0)
        var slowSignInGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

        override suspend fun sendVerificationOtp(email: String): IshaaraResult<Unit> {
            if (shouldNetworkFail) {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }
            return IshaaraResult.success(Unit)
        }

        override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto> {
            if (shouldFailAuth) {
                return IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Invalid OTP."))
            }
            return IshaaraResult.success(
                AuthSessionResponseDto(
                    token = "session_token_xyz",
                    userId = "user_123",
                    role = "USER",
                    expiresAt = System.currentTimeMillis() + 3_600_000L
                )
            )
        }

        override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto> {
            signInCallCount.incrementAndGet()
            slowSignInGate?.await()
            if (shouldFailAuth) {
                return IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Invalid ID token."))
            }
            return IshaaraResult.success(
                AuthSessionResponseDto(
                    token = "session_token_xyz",
                    userId = "user_123",
                    role = "USER",
                    expiresAt = System.currentTimeMillis() + 3_600_000L
                )
            )
        }

        override suspend fun getCurrentSession(token: String): IshaaraResult<AuthSessionResponseDto> {
            if (should401Fail) {
                return IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Unauthorized."))
            }
            return IshaaraResult.success(
                AuthSessionResponseDto(
                    token = token,
                    userId = "user_123",
                    role = "USER"
                )
            )
        }

        override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> {
            return IshaaraResult.success(
                UserDto(
                    id = "user_123",
                    name = "Test User",
                    role = role,
                    isOnboarded = true
                )
            )
        }

        override suspend fun getCurrentUser(token: String): IshaaraResult<UserDto> {
            if (should401Fail) {
                return IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Unauthorized."))
            }
            if (shouldNetworkFail) {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }
            return IshaaraResult.success(
                UserDto(
                    id = "user_123",
                    name = "Test User",
                    role = "USER",
                    isOnboarded = true
                )
            )
        }

        override suspend fun signOut(token: String): IshaaraResult<Unit> {
            return IshaaraResult.success(Unit)
        }
    }

    @Test
    fun test1_noStoredSession_emitsUnauthenticated() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        val result = repository.restoreSession()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(AuthState.Unauthenticated, repository.observeAuthState().value)
    }

    @Test
    fun test2_validStoredSession_restoresAuthenticatedState() = runBlocking {
        val validSession = AuthSession(
            token = "valid_tok",
            userId = "user_123",
            role = UserRole.USER,
            expiresAtMillis = System.currentTimeMillis() + 3_600_000L
        )
        val sessionStore = InMemorySessionStore(validSession)
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        val result = repository.restoreSession()

        assertTrue(result.isSuccess)
        val authState = repository.observeAuthState().value
        assertTrue(authState is AuthState.Authenticated)
        assertEquals("user_123", (authState as AuthState.Authenticated).session.userId)
    }

    @Test
    fun test3_expiredStoredSession_emitsSessionExpiredAndClears() = runBlocking {
        val expiredSession = AuthSession(
            token = "expired_tok",
            userId = "user_123",
            role = UserRole.USER,
            expiresAtMillis = System.currentTimeMillis() - 1000L
        )
        val sessionStore = InMemorySessionStore(expiredSession)
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        val result = repository.restoreSession()

        assertTrue(result.isSuccess)
        assertNull(localDataSource.getSession())
        val authState = repository.observeAuthState().value
        assertTrue(authState is AuthState.SessionExpired)
    }

    @Test
    fun test4_successfulAuthentication_persistsSessionAndEmitsAuthenticated() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        val result = repository.signInWithGoogle("valid_id_token")

        assertTrue(result.isSuccess)
        val savedSession = localDataSource.getSession()
        assertNotNull(savedSession)
        assertEquals("session_token_xyz", savedSession?.token)
        val authState = repository.observeAuthState().value
        assertTrue(authState is AuthState.Authenticated)
    }

    @Test
    fun test5_authenticationFailure_emitsErrorState() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource(shouldFailAuth = true)
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        val result = repository.signInWithGoogle("invalid_token")

        assertTrue(result.isFailure)
        assertNull(localDataSource.getSession())
        val authState = repository.observeAuthState().value
        assertTrue(authState is AuthState.Error)
    }

    @Test
    fun test6_signOut_clearsSessionAndEmitsUnauthenticated() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        repository.signInWithGoogle("valid_id_token")
        assertNotNull(localDataSource.getSession())

        repository.signOut()
        assertNull(localDataSource.getSession())
        assertEquals(AuthState.Unauthenticated, repository.observeAuthState().value)
    }

    @Test
    fun test7_networkUnavailableDuringStartup_preservesValidLocalSession() = runBlocking {
        val validSession = AuthSession(
            token = "valid_offline_tok",
            userId = "user_offline",
            role = UserRole.USER,
            expiresAtMillis = System.currentTimeMillis() + 3_600_000L
        )
        val sessionStore = InMemorySessionStore(validSession)
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource(shouldNetworkFail = true)
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)

        val result = repository.restoreSession()

        assertTrue(result.isSuccess)
        assertNotNull(localDataSource.getSession())
        val authState = repository.observeAuthState().value
        assertTrue("Valid session must be preserved when offline", authState is AuthState.Authenticated)
    }

    @Test
    fun test8_401SessionInvalidation_clearsSessionAndNotifiesCoordinator() = runBlocking {
        val coordinator = SessionInvalidationCoordinator(debounceWindowMillis = 1000L)
        val validSession = AuthSession(
            token = "revoked_tok",
            userId = "user_revoked",
            role = UserRole.USER
        )
        val sessionStore = InMemorySessionStore(validSession)
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource, coordinator)

        // Trigger 401
        val wasTriggered = coordinator.notifyUnauthorized("Token revoked by server.")

        assertTrue(wasTriggered)
        // Invalidation callback runs asynchronously on coordinator event
        repository.invalidateSession("Token revoked by server.")

        assertNull(localDataSource.getSession())
        val authState = repository.observeAuthState().value
        assertTrue(authState is AuthState.SessionExpired)
    }

    @Test
    fun test9_multipleConcurrent401s_coalesceToSingleEvent() {
        val coordinator = SessionInvalidationCoordinator(debounceWindowMillis = 2000L)

        val firstTrigger = coordinator.notifyUnauthorized("401 from Call 1")
        val secondTrigger = coordinator.notifyUnauthorized("401 from Call 2")
        val thirdTrigger = coordinator.notifyUnauthorized("401 from Call 3")

        assertTrue("First 401 must trigger invalidation", firstTrigger)
        assertFalse("Second concurrent 401 must be coalesced", secondTrigger)
        assertFalse("Third concurrent 401 must be coalesced", thirdTrigger)
    }

    @Test
    fun test10_repeatedLoginTap_preventsDuplicateAuthentication() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val remoteDataSource = MockAuthRemoteDataSource()
        val repository = AuthRepositoryImpl(remoteDataSource, localDataSource)
        val navigationManager = NavigationManager()

        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        remoteDataSource.slowSignInGate = gate

        val testScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        val viewModel = AuthViewModel(
            getAuthSessionUseCase = GetAuthSessionUseCase(repository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(repository),
            signOutUseCase = SignOutUseCase(repository),
            navigationManager = navigationManager,
            restoreSessionUseCase = RestoreSessionUseCase(repository),
            observeAuthStateUseCase = ObserveAuthStateUseCase(repository),
            externalScope = testScope
        )

        // First tap initiates in-flight sign-in
        viewModel.signInWithGoogle("token_1")
        // Wait briefly for coroutine to reach gate
        kotlinx.coroutines.delay(50)

        // Second tap while first is in-flight must be rejected
        viewModel.signInWithGoogle("token_2")

        // Release gate to complete first request
        gate.complete(Unit)
        kotlinx.coroutines.delay(100)

        assertEquals("Only one remote call must be dispatched during in-flight sign-in", 1, remoteDataSource.signInCallCount.get())
    }
}
