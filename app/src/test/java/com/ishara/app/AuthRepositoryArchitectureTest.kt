package com.ishara.app

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.data.repository.AuthRepositoryImpl
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the repository and session storage interaction for authentication.
 */
class AuthRepositoryArchitectureTest {

    private val fakeRemoteDataSource = object : AuthRemoteDataSource {
        override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto> {
            return IshaaraResult.success(
                AuthSessionResponseDto(
                    token = "session_token_xyz",
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
    fun signInWithGoogle_savesSessionInLocalDataSource() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val repository = AuthRepositoryImpl(fakeRemoteDataSource, localDataSource)

        val result = repository.signInWithGoogle("valid_id_token")

        assertTrue(result.isSuccess)
        val savedSession = localDataSource.getSession()
        assertNotNull(savedSession)
        assertEquals("session_token_xyz", savedSession?.token)
        assertEquals(UserRole.USER, savedSession?.role)
    }

    @Test
    fun completeOnboarding_updatesRoleInSessionStore() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val repository = AuthRepositoryImpl(fakeRemoteDataSource, localDataSource)

        // First sign in
        repository.signInWithGoogle("valid_id_token")

        // Then onboard as Driver
        val onboardResult = repository.completeOnboarding(UserRole.DRIVER_CONDUCTOR)

        assertTrue(onboardResult.isSuccess)
        val updatedSession = localDataSource.getSession()
        assertEquals(UserRole.DRIVER_CONDUCTOR, updatedSession?.role)
    }

    @Test
    fun signOut_clearsSessionStore() = runBlocking {
        val sessionStore = InMemorySessionStore()
        val localDataSource = SessionLocalDataSourceImpl(sessionStore)
        val repository = AuthRepositoryImpl(fakeRemoteDataSource, localDataSource)

        repository.signInWithGoogle("valid_id_token")
        assertNotNull(localDataSource.getSession())

        repository.signOut()
        assertNull(localDataSource.getSession())
    }
}
