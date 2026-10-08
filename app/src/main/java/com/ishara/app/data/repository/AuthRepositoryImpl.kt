package com.ishara.app.data.repository

import android.util.Log
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.AuthMapper
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.dto.OnboardingRequestDto
import com.ishara.app.data.remote.dto.UpdateUserRequestDto
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionStore: SessionStore
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSession> {
        Log.d("AUTH_DEBUG", "AuthRepository: signInWithGoogle() called")
        return remoteDataSource.signInWithSocial("google", idToken).map { response ->
            Log.d("AUTH_DEBUG", "AuthRepository: Mapping social sign-in response")
            val session = AuthMapper.toAuthSession(response)
            Log.d("AUTH_DEBUG", "AuthRepository: Saving session to store. User ID: ${session.userId}")
            sessionStore.saveSession(session)
            session
        }.onFailure { error ->
            Log.e("AUTH_DEBUG", "AuthRepository: signInWithGoogle failed: ${error.message}")
        }
    }

    override suspend fun sendEmailOtp(email: String): IshaaraResult<Unit> {
        Log.d("AUTH_DEBUG", "AuthRepository: sendEmailOtp() for $email")
        return remoteDataSource.sendEmailOtp(email)
    }

    override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSession> {
        Log.d("AUTH_DEBUG", "AuthRepository: signInWithEmailOtp() called")
        return remoteDataSource.signInWithEmailOtp(email, otp).map { response ->
            val session = AuthMapper.toAuthSession(response)
            sessionStore.saveSession(session)
            session
        }
    }

    override suspend fun completeOnboarding(
        role: UserRole,
        name: String,
        phoneNumber: String
    ): IshaaraResult<User> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Session missing"))

        val request = OnboardingRequestDto(
            role = role.name,
            name = name,
            phoneNumber = phoneNumber
        )

        return remoteDataSource.completeOnboarding(token, request).map { AuthMapper.toDomain(it.data!!) }
    }

    override suspend fun getCurrentUser(): IshaaraResult<User> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Session missing"))

        return remoteDataSource.getCurrentUser(token).map { response ->
            AuthMapper.toDomain(response.data ?: throw IllegalStateException("Empty user data"))
        }
    }

    override suspend fun updateProfile(
        name: String?,
        phoneNumber: String?,
        image: String?
    ): IshaaraResult<User> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Session missing"))

        val request = UpdateUserRequestDto(name, phoneNumber, image)
        return remoteDataSource.updateProfile(token, request).map { AuthMapper.toDomain(it.data!!) }
    }

    override fun observeSession(): Flow<AuthSession?> {
        return sessionStore.observeSession()
    }

    override suspend fun validateSession(): IshaaraResult<AuthSession> {
        Log.d("AUTH_DEBUG", "AuthRepository: validateSession() called")
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "No local session found")).also {
                Log.d("AUTH_DEBUG", "AuthRepository: No local session found during validation")
            }
        
        return remoteDataSource.getSession(token).map { response ->
            Log.d("AUTH_DEBUG", "AuthRepository: Remote session validated successfully")
            val session = AuthMapper.toAuthSession(response, token)
            sessionStore.saveSession(session)
            session
        }.onFailure { error ->
            Log.e("AUTH_DEBUG", "AuthRepository: Remote session validation failed: ${error.message}")
        }
    }

    override suspend fun checkHealth(): IshaaraResult<Unit> {
        return remoteDataSource.checkHealth()
    }

    override suspend fun signOut(): IshaaraResult<Unit> {
        Log.d("AUTH_DEBUG", "AuthRepository: signOut() called")
        val token = sessionStore.getSession()?.token
        if (token != null) {
            remoteDataSource.signOut(token)
        }
        sessionStore.clearSession()
        return IshaaraResult.success(Unit)
    }
}
