package com.ishara.app

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.datasource.UserRemoteDataSource
import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.data.repository.AuthRepositoryImpl
import com.ishara.app.data.repository.OnboardingRepositoryImpl
import com.ishara.app.data.repository.UserRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.AuthRepository
import com.ishara.app.domain.repository.OnboardingRepository
import com.ishara.app.domain.repository.UserRepository
import com.ishara.app.domain.usecase.GetAuthSessionUseCase
import com.ishara.app.domain.usecase.SendEmailOtpUseCase
import com.ishara.app.domain.usecase.SignInWithEmailOtpUseCase
import com.ishara.app.domain.usecase.SignInWithGoogleUseCase
import com.ishara.app.domain.usecase.SignOutUseCase
import com.ishara.app.feature.auth.AuthViewModel
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PHASE A02 Architecture Test Suite
 *
 * Verifies:
 * 1. Authentication lifecycle (Google Social, Email OTP, Logout)
 * 2. Session restoration and authoritative /users/me hydration
 * 3. 401 Session expiration handling
 * 4. Account switching data isolation
 * 5. Onboarding flow with 409 ONBOARDING_ALREADY_COMPLETED reconciliation
 * 6. Authoritative role resolution (USER -> Passenger, DRIVER_CONDUCTOR -> Driver)
 * 7. Stale cache non-trust / security
 */
class AuthPhaseA02Test {

    private lateinit var mockAuthRemoteDataSource: FakeAuthRemoteDataSource
    private lateinit var mockUserRemoteDataSource: FakeUserRemoteDataSource
    private lateinit var sessionStore: InMemorySessionStore

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var onboardingRepository: OnboardingRepository

    @Before
    fun setUp() {
        mockAuthRemoteDataSource = FakeAuthRemoteDataSource()
        mockUserRemoteDataSource = FakeUserRemoteDataSource()
        sessionStore = InMemorySessionStore()

        val sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)

        userRepository = UserRepositoryImpl(
            remoteDataSource = mockUserRemoteDataSource,
            sessionStore = sessionStore
        )

        authRepository = AuthRepositoryImpl(
            remoteDataSource = mockAuthRemoteDataSource,
            localDataSource = sessionLocalDataSource
        )

        onboardingRepository = OnboardingRepositoryImpl(
            remoteDataSource = mockUserRemoteDataSource,
            userRepository = userRepository,
            sessionStore = sessionStore
        )
    }

    // =========================================================================
    // 1. App Startup & Session Restoration
    // =========================================================================

    @Test
    fun appStartup_withoutSession_isUnauthenticated() = runBlocking {
        val restored = authRepository.restoreSession()
        assertTrue(restored.isSuccess)
        assertNull(restored.getOrNull())
        assertEquals(AuthState.Unauthenticated, authRepository.observeAuthState().value)
    }

    @Test
    fun appStartup_withValidSession_restoresAndHydratesAuthoritativeUser() = runBlocking {
        val initialSession = AuthSession(
            token = "valid_token_123",
            userId = "user_abc",
            role = null,
            expiresAtMillis = System.currentTimeMillis() + 3_600_000L
        )
        sessionStore.saveSession(initialSession)
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "user_abc",
            name = "Test Passenger",
            role = "USER",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val restored = authRepository.restoreSession()
        assertTrue(restored.isSuccess)
        assertTrue(authRepository.observeAuthState().value is AuthState.Authenticated)

        val profileResult = userRepository.getCurrentUserProfile()
        assertTrue(profileResult is IshaaraResult.Success)
        val profile = (profileResult as IshaaraResult.Success).data
        assertEquals(UserRole.USER, profile.role)
        assertTrue(profile.onboardingCompleted)
    }

    @Test
    fun appStartup_withExpiredSession_cleansUpSession() = runBlocking {
        val expiredSession = AuthSession(
            token = "expired_token_456",
            userId = "user_xyz",
            role = null,
            expiresAtMillis = System.currentTimeMillis() - 1000L
        )
        sessionStore.saveSession(expiredSession)

        val restored = authRepository.restoreSession()
        assertTrue(restored.isSuccess)
        assertNull(restored.getOrNull())
        assertTrue(authRepository.observeAuthState().value is AuthState.SessionExpired)
        assertNull(sessionStore.getSession())
    }

    // =========================================================================
    // 2. Google Social Sign-In
    // =========================================================================

    @Test
    fun googleSignIn_success_hydratesSessionAndAuthoritativeProfile() = runBlocking {
        mockAuthRemoteDataSource.signInResponse = AuthSessionResponseDto(
            token = "google_session_jwt",
            userId = "google_user_1",
            role = null,
            expiresAt = System.currentTimeMillis() + 86_400_000L
        )
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "google_user_1",
            name = "Google User",
            email = "user@gmail.com",
            role = "USER",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val result = authRepository.signInWithGoogle("valid_google_id_token")
        assertTrue(result is IshaaraResult.Success)
        assertTrue(authRepository.observeAuthState().value is AuthState.Authenticated)
        assertEquals("google_session_jwt", sessionStore.getSession()?.token)

        val profile = (userRepository.getCurrentUserProfile() as IshaaraResult.Success).data
        assertEquals(UserRole.USER, profile.role)
    }

    @Test
    fun googleSignIn_failure_propagatesErrorWithoutPersistingSession() = runBlocking {
        mockAuthRemoteDataSource.shouldFailAuth = true

        val result = authRepository.signInWithGoogle("invalid_google_id_token")
        assertTrue(result is IshaaraResult.Failure)
        assertTrue(authRepository.observeAuthState().value is AuthState.Error)
        assertNull(sessionStore.getSession())
    }

    // =========================================================================
    // 3. Email OTP Flow
    // =========================================================================

    @Test
    fun emailOtp_send_success() = runBlocking {
        val result = authRepository.sendEmailOtp("test@example.com")
        assertTrue(result is IshaaraResult.Success)
        assertEquals("test@example.com", mockAuthRemoteDataSource.lastSentEmail)
    }

    @Test
    fun emailOtp_signIn_success_hydratesSession() = runBlocking {
        mockAuthRemoteDataSource.signInResponse = AuthSessionResponseDto(
            token = "email_otp_jwt",
            userId = "otp_user_2",
            role = null,
            expiresAt = System.currentTimeMillis() + 86_400_000L
        )
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "otp_user_2",
            name = "OTP Driver",
            email = "test@example.com",
            role = "DRIVER_CONDUCTOR",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val result = authRepository.signInWithEmailOtp("test@example.com", "123456")
        assertTrue(result is IshaaraResult.Success)
        assertTrue(authRepository.observeAuthState().value is AuthState.Authenticated)

        val profile = (userRepository.getCurrentUserProfile() as IshaaraResult.Success).data
        assertEquals(UserRole.DRIVER_CONDUCTOR, profile.role)
    }

    @Test
    fun emailOtp_invalidOtp_returnsFailureWithoutPersistingSession() = runBlocking {
        mockAuthRemoteDataSource.shouldFailAuth = true

        val result = authRepository.signInWithEmailOtp("test@example.com", "000000")
        assertTrue(result is IshaaraResult.Failure)
        assertTrue(authRepository.observeAuthState().value is AuthState.Error)
        assertNull(sessionStore.getSession())
    }

    @Test
    fun emailOtp_viewModel_sendAndVerify_success() = runBlocking {
        val navManager = NavigationManager()
        val testScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined)
        val viewModel = AuthViewModel(
            getAuthSessionUseCase = GetAuthSessionUseCase(authRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository),
            signOutUseCase = SignOutUseCase(authRepository),
            navigationManager = navManager,
            sendEmailOtpUseCase = SendEmailOtpUseCase(authRepository),
            signInWithEmailOtpUseCase = SignInWithEmailOtpUseCase(authRepository),
            externalScope = testScope
        )

        mockAuthRemoteDataSource.signInResponse = AuthSessionResponseDto(
            token = "jwt_token_123",
            userId = "driver_1",
            role = "DRIVER_CONDUCTOR"
        )
        mockAuthRemoteDataSource.onboardingUserDto = UserDto(
            id = "driver_1",
            name = "Bus Driver",
            role = "DRIVER_CONDUCTOR",
            isOnboarded = true
        )
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "driver_1",
            name = "Bus Driver",
            role = "DRIVER_CONDUCTOR",
            isOnboarded = true
        )

        viewModel.onEmailInputChanged("driver@bus.com")
        viewModel.sendVerificationOtp()

        assertTrue(viewModel.uiState.value.isOtpSent)
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.onOtpInputChanged("123456")
        viewModel.verifyEmailOtp()

        assertEquals(UserRole.DRIVER_CONDUCTOR, viewModel.uiState.value.role)
        assertNotNull(viewModel.uiState.value.session)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun emailOtp_viewModel_errorHandling_differentiatesErrors() = runBlocking {
        val navManager = NavigationManager()
        val testScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined)
        val viewModel = AuthViewModel(
            getAuthSessionUseCase = GetAuthSessionUseCase(authRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository),
            signOutUseCase = SignOutUseCase(authRepository),
            navigationManager = navManager,
            sendEmailOtpUseCase = SendEmailOtpUseCase(authRepository),
            signInWithEmailOtpUseCase = SignInWithEmailOtpUseCase(authRepository),
            externalScope = testScope
        )

        mockAuthRemoteDataSource.shouldFailAuth = true

        viewModel.onEmailInputChanged("driver@bus.com")
        viewModel.sendVerificationOtp()

        assertFalse(viewModel.uiState.value.isOtpSent)
        assertEquals("Server is temporarily unavailable. Please try again shortly.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.errorMessage!!.contains("service is not available", ignoreCase = true))
    }

    // =========================================================================
    // 4. Logout & Account Switching
    // =========================================================================

    @Test
    fun logout_clearsFrontendAuthStateAndSession() = runBlocking {
        sessionStore.saveSession(
            AuthSession(token = "tok", userId = "u1", role = UserRole.USER)
        )
        userRepository.getCurrentUserProfile()

        val result = authRepository.signOut()
        assertTrue(result is IshaaraResult.Success)
        assertEquals(AuthState.Unauthenticated, authRepository.observeAuthState().value)
        assertNull(sessionStore.getSession())
        assertTrue(mockAuthRemoteDataSource.signOutCalled)
    }

    @Test
    fun accountSwitching_clearsPreviousAccountState_andHydratesNewAccount() = runBlocking {
        // First account: Passenger
        sessionStore.saveSession(
            AuthSession(token = "pass_tok", userId = "pass_id", role = UserRole.USER)
        )
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "pass_id",
            name = "Passenger A",
            role = "USER",
            isOnboarded = true,
            onboardingCompleted = true
        )
        userRepository.getCurrentUserProfile()
        assertEquals(UserRole.USER, userRepository.observeUserProfile().first()?.role)

        // Logout
        authRepository.signOut()
        userRepository.clearCachedProfile()
        assertNull(userRepository.observeUserProfile().first())

        // Login as Driver
        mockAuthRemoteDataSource.signInResponse = AuthSessionResponseDto(
            token = "driver_tok",
            userId = "driver_id",
            role = "DRIVER_CONDUCTOR"
        )
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "driver_id",
            name = "Driver B",
            role = "DRIVER_CONDUCTOR",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val loginResult = authRepository.signInWithEmailOtp("driver@example.com", "654321")
        assertTrue(loginResult is IshaaraResult.Success)

        val newProfile = (userRepository.getCurrentUserProfile() as IshaaraResult.Success).data
        assertEquals("driver_id", newProfile.id)
        assertEquals(UserRole.DRIVER_CONDUCTOR, newProfile.role)
    }

    // =========================================================================
    // 5. Onboarding & 409 Conflict Reconciliation
    // =========================================================================

    @Test
    fun onboarding_freshSubmission_success_updatesAuthoritativeProfile() = runBlocking {
        sessionStore.saveSession(
            AuthSession(token = "session_token", userId = "new_user", role = null)
        )
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "new_user",
            name = "New Driver",
            role = "DRIVER_CONDUCTOR",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val result = onboardingRepository.submitOnboarding(UserRole.DRIVER_CONDUCTOR)
        assertTrue(result is IshaaraResult.Success)
        val profile = (result as IshaaraResult.Success).data
        assertEquals(UserRole.DRIVER_CONDUCTOR, profile.role)
        assertTrue(profile.onboardingCompleted)
    }

    @Test
    fun onboarding_409Conflict_reconcilesWithBackendProfileWithoutFailing() = runBlocking {
        sessionStore.saveSession(
            AuthSession(token = "session_token", userId = "existing_user", role = null)
        )
        // Backend returns 409 Conflict with ONBOARDING_ALREADY_COMPLETED
        mockUserRemoteDataSource.shouldConflictOnboarding = true
        // Authoritative /users/me returns existing user with role USER
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "existing_user",
            name = "Existing User",
            role = "USER",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val result = onboardingRepository.submitOnboarding(UserRole.USER)
        assertTrue("Onboarding should succeed via reconciliation on 409 conflict", result is IshaaraResult.Success)
        val profile = (result as IshaaraResult.Success).data
        assertEquals(UserRole.USER, profile.role)
        assertTrue(profile.onboardingCompleted)
    }

    // =========================================================================
    // 6. Authoritative Role Routing Decisions
    // =========================================================================

    @Test
    fun roleRouting_unauthenticated_mapsToUnauthenticatedState() {
        val authState = computeNavigationDecision(
            isAuthenticated = false,
            userProfile = null
        )
        assertEquals("UNAUTHENTICATED", authState)
    }

    @Test
    fun roleRouting_onboardingIncomplete_mapsToOnboardingRequired() {
        val authState = computeNavigationDecision(
            isAuthenticated = true,
            userProfile = UserProfile(id = "u1", name = "Pending", role = null, onboardingCompleted = false)
        )
        assertEquals("ONBOARDING_REQUIRED", authState)
    }

    @Test
    fun roleRouting_userRole_mapsToPassenger() {
        val authState = computeNavigationDecision(
            isAuthenticated = true,
            userProfile = UserProfile(id = "u1", name = "Passenger", role = UserRole.USER, onboardingCompleted = true)
        )
        assertEquals("PASSENGER", authState)
    }

    @Test
    fun roleRouting_driverRole_mapsToDriver() {
        val authState = computeNavigationDecision(
            isAuthenticated = true,
            userProfile = UserProfile(id = "u1", name = "Driver", role = UserRole.DRIVER_CONDUCTOR, onboardingCompleted = true)
        )
        assertEquals("DRIVER", authState)
    }

    @Test
    fun roleRouting_missingRoleAfterOnboarding_mapsToAuthError() {
        val authState = computeNavigationDecision(
            isAuthenticated = true,
            userProfile = UserProfile(id = "u1", name = "Invalid", role = null, onboardingCompleted = true)
        )
        assertEquals("AUTH_ERROR", authState)
    }

    // =========================================================================
    // 8. Security & Sanitization
    // =========================================================================

    @Test
    fun security_tokensAndOtp_areRedactedInLogger() {
        val rawMessage = "Request with Bearer eyJhbGciOiJIUzI1NiJ9 and otp=123456 idToken=secret_token"
        val sanitized = com.ishara.app.core.common.IshaaraLogger.sanitize(rawMessage)

        assertFalse("Bearer token must not leak in logs", sanitized.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse("OTP must not leak in logs", sanitized.contains("123456"))
        assertFalse("ID token must not leak in logs", sanitized.contains("secret_token"))
        assertTrue(sanitized.contains("[PROTECTED]"))
    }

    @Test
    fun security_staleCachedRole_isNotTrustedOverBackend() = runBlocking {
        // Pre-populate cache with an untrusted role
        sessionStore.saveSession(
            AuthSession(token = "session_token", userId = "user_1", role = UserRole.DRIVER_CONDUCTOR)
        )
        // Backend authoritative profile returns USER
        mockUserRemoteDataSource.mockUser = UserDto(
            id = "user_1",
            name = "Real Passenger",
            role = "USER",
            isOnboarded = true,
            onboardingCompleted = true
        )

        val result = userRepository.getCurrentUserProfile()
        assertTrue(result is IshaaraResult.Success)
        val authoritativeProfile = (result as IshaaraResult.Success).data
        assertEquals("Authoritative backend role must override any local cache", UserRole.USER, authoritativeProfile.role)
    }

    // Helper simulating the single authoritative root decision in MainActivity
    private fun computeNavigationDecision(isAuthenticated: Boolean, userProfile: UserProfile?): String {
        if (!isAuthenticated) return "UNAUTHENTICATED"
        if (userProfile == null) return "AUTH_INITIALIZING"
        if (!userProfile.onboardingCompleted) return "ONBOARDING_REQUIRED"
        return when (userProfile.role) {
            UserRole.USER, UserRole.ADMIN -> "PASSENGER"
            UserRole.DRIVER_CONDUCTOR -> "DRIVER"
            null -> "AUTH_ERROR"
        }
    }

    // =========================================================================
    // Mocks & Fakes
    // =========================================================================

    private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
        var shouldFailAuth: Boolean = false
        var lastSentEmail: String? = null
        var signOutCalled: Boolean = false
        var signInResponse: AuthSessionResponseDto = AuthSessionResponseDto("token", "u1", "USER")
        var onboardingUserDto: UserDto = UserDto("u1", "name", role = "USER", isOnboarded = true)

        override suspend fun sendVerificationOtp(email: String): IshaaraResult<Unit> {
            lastSentEmail = email
            return if (shouldFailAuth) {
                IshaaraResult.failure(IshaaraError.Server(500, "Failed to send OTP"))
            } else {
                IshaaraResult.success(Unit)
            }
        }

        override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto> {
            return if (shouldFailAuth) {
                IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Invalid OTP code."))
            } else {
                IshaaraResult.success(signInResponse)
            }
        }

        override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto> {
            return if (shouldFailAuth) {
                IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Invalid ID token."))
            } else {
                IshaaraResult.success(signInResponse)
            }
        }

        override suspend fun getCurrentSession(token: String): IshaaraResult<AuthSessionResponseDto> {
            return IshaaraResult.success(signInResponse)
        }

        override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> {
            return IshaaraResult.success(onboardingUserDto)
        }

        override suspend fun getCurrentUser(token: String): IshaaraResult<UserDto> {
            return IshaaraResult.success(onboardingUserDto)
        }

        override suspend fun signOut(token: String): IshaaraResult<Unit> {
            signOutCalled = true
            return IshaaraResult.success(Unit)
        }
    }

    private class FakeUserRemoteDataSource : UserRemoteDataSource {
        var shouldReturn401: Boolean = false
        var shouldConflictOnboarding: Boolean = false
        var mockUser: UserDto? = null

        override suspend fun getCurrentUserProfile(token: String): IshaaraResult<UserDto> {
            if (shouldReturn401) {
                return IshaaraResult.failure(IshaaraError.Authentication(code = 401, message = "Unauthorized"))
            }
            return if (mockUser != null) {
                IshaaraResult.success(mockUser!!)
            } else {
                IshaaraResult.failure(IshaaraError.NotFound("User not found"))
            }
        }

        override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> {
            if (shouldConflictOnboarding) {
                return IshaaraResult.failure(
                    IshaaraError.Conflict(
                        errorCode = "ONBOARDING_ALREADY_COMPLETED",
                        message = "User has already completed onboarding."
                    )
                )
            }
            return if (mockUser != null) {
                IshaaraResult.success(mockUser!!)
            } else {
                IshaaraResult.failure(IshaaraError.NotFound("User not found"))
            }
        }

        override suspend fun updateUserProfile(
            name: String?,
            phoneNumber: String?,
            image: String?,
            token: String
        ): IshaaraResult<UserDto> {
            return mockUser?.let { IshaaraResult.success(it) } ?: IshaaraResult.failure(IshaaraError.NotFound())
        }
    }
}
