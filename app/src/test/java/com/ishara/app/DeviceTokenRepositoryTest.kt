package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.DeviceTokenStore
import com.ishara.app.core.storage.InMemoryDeviceTokenStore
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.remote.datasource.DeviceTokenRemoteDataSource
import com.ishara.app.data.remote.dto.RegisterPushTokenRequestDto
import com.ishara.app.data.remote.dto.RegisterPushTokenResponseDto
import com.ishara.app.data.remote.dto.RemovePushTokenRequestDto
import com.ishara.app.data.remote.dto.RemovePushTokenResponseDto
import com.ishara.app.data.repository.DeviceTokenRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DeviceTokenRepositoryImpl.
 * Verifies validation, session authentication, persistence, and remote contract delegation.
 */
class DeviceTokenRepositoryTest {

    private class TestDispatcherProvider(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private class FakeDeviceTokenRemoteDataSource : DeviceTokenRemoteDataSource {
        var lastRegisterRequest: RegisterPushTokenRequestDto? = null
        var lastRegisterAuthToken: String? = null
        var registerResult: IshaaraResult<RegisterPushTokenResponseDto> = IshaaraResult.success(
            RegisterPushTokenResponseDto(
                tokenMasked = "fcm_masked...",
                platform = "android",
                isActive = true,
                lastSeenAt = "2026-10-01T12:00:00Z",
                success = true,
                registered = true
            )
        )

        var lastRemoveRequest: RemovePushTokenRequestDto? = null
        var lastRemoveAuthToken: String? = null
        var removeResult: IshaaraResult<RemovePushTokenResponseDto> = IshaaraResult.success(
            RemovePushTokenResponseDto(success = true, removed = true)
        )

        override suspend fun registerPushToken(
            request: RegisterPushTokenRequestDto,
            token: String?
        ): IshaaraResult<RegisterPushTokenResponseDto> {
            lastRegisterRequest = request
            lastRegisterAuthToken = token
            return registerResult
        }

        override suspend fun removePushToken(
            request: RemovePushTokenRequestDto,
            token: String?
        ): IshaaraResult<RemovePushTokenResponseDto> {
            lastRemoveRequest = request
            lastRemoveAuthToken = token
            return removeResult
        }
    }

    private val testDispatchers = TestDispatcherProvider()

    @Test
    fun `registerDeviceToken fails with validation error if token is blank`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource()
        val tokenStore = InMemoryDeviceTokenStore()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token", userId = "u1", role = UserRole.USER)
        )
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.registerDeviceToken("   ")

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertEquals("token", (error as IshaaraError.Validation).field)
    }

    @Test
    fun `registerDeviceToken fails with authentication error if session is missing`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource()
        val tokenStore = InMemoryDeviceTokenStore()
        val sessionStore = InMemorySessionStore(null)
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.registerDeviceToken("valid_fcm_token_123")

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Authentication)
    }

    @Test
    fun `registerDeviceToken passes android platform and saves token on success`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource()
        val tokenStore = InMemoryDeviceTokenStore()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token_xyz", userId = "u1", role = UserRole.USER)
        )
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.registerDeviceToken(
            token = "fcm_real_token_123",
            platform = "android",
            deviceId = "pixel8",
            appVersion = "1.0.0"
        )

        assertTrue(result is IshaaraResult.Success)
        assertEquals("session_token_xyz", fakeDataSource.lastRegisterAuthToken)
        assertEquals("fcm_real_token_123", fakeDataSource.lastRegisterRequest?.token)
        assertEquals("android", fakeDataSource.lastRegisterRequest?.platform)
        assertEquals("pixel8", fakeDataSource.lastRegisterRequest?.deviceId)

        // Verify token saved in tokenStore
        assertEquals("fcm_real_token_123", tokenStore.getToken())
    }

    @Test
    fun `registerDeviceToken propagates remote errors`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource().apply {
            registerResult = IshaaraResult.failure(IshaaraError.Network("Offline"))
        }
        val tokenStore = InMemoryDeviceTokenStore()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token", userId = "u1", role = UserRole.USER)
        )
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.registerDeviceToken("fcm_token")

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Network)
    }

    @Test
    fun `removeDeviceToken fails with validation error if token is blank`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource()
        val tokenStore = InMemoryDeviceTokenStore()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token", userId = "u1", role = UserRole.USER)
        )
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.removeDeviceToken("")

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Validation)
    }

    @Test
    fun `removeDeviceToken fails with authentication error if session is missing`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource()
        val tokenStore = InMemoryDeviceTokenStore()
        val sessionStore = InMemorySessionStore(null)
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.removeDeviceToken("fcm_token_to_remove")

        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    @Test
    fun `removeDeviceToken clears tokenStore if token matches`() = runBlocking {
        val fakeDataSource = FakeDeviceTokenRemoteDataSource()
        val tokenStore = InMemoryDeviceTokenStore("fcm_matching_token")
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "session_token", userId = "u1", role = UserRole.USER)
        )
        val repository = DeviceTokenRepositoryImpl(fakeDataSource, tokenStore, sessionStore, testDispatchers)

        val result = repository.removeDeviceToken("fcm_matching_token")

        assertTrue(result is IshaaraResult.Success)
        assertTrue((result as IshaaraResult.Success).data)
        assertNull(tokenStore.getToken())
    }

    @Test
    fun `token caching operations work as expected`() = runBlocking {
        val tokenStore = InMemoryDeviceTokenStore()
        val repository = DeviceTokenRepositoryImpl(
            FakeDeviceTokenRemoteDataSource(),
            tokenStore,
            InMemorySessionStore(null),
            testDispatchers
        )

        assertNull(repository.getCachedToken())
        repository.saveCachedToken("token_abc")
        assertEquals("token_abc", repository.getCachedToken())
        repository.clearCachedToken()
        assertNull(repository.getCachedToken())
    }

    @Test
    fun `account switching isolation clears cached token`() = runBlocking {
        val tokenStore = InMemoryDeviceTokenStore("user_a_token")
        val repository = DeviceTokenRepositoryImpl(
            FakeDeviceTokenRemoteDataSource(),
            tokenStore,
            InMemorySessionStore(null),
            testDispatchers
        )

        // User A logout
        repository.clearCachedToken()
        assertNull(repository.getCachedToken())

        // User B login
        repository.saveCachedToken("user_b_token")
        assertEquals("user_b_token", repository.getCachedToken())
    }
}
