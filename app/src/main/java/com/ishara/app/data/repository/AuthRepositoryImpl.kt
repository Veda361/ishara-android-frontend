package com.ishara.app.data.repository

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.data.remote.dto.OnboardingRequestDto
import com.ishara.app.data.remote.dto.UpdateUserRequestDto
import com.ishara.app.data.remote.dto.UserDto
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
        return remoteDataSource.signInWithSocial("google", idToken).map { response ->
            val session = AuthSession(
                token = response.token,
                userId = response.user.id,
                email = response.user.email,
                role = UserRole.fromString(response.user.role),
                name = response.user.name
            )
            sessionStore.saveSession(session)
            session
        }
    }

    override suspend fun sendEmailOtp(email: String): IshaaraResult<Unit> {
        return remoteDataSource.sendEmailOtp(email)
    }

    override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSession> {
        return remoteDataSource.signInWithEmailOtp(email, otp).map { response ->
            val session = AuthSession(
                token = response.token,
                userId = response.user.id,
                email = response.user.email,
                role = UserRole.fromString(response.user.role),
                name = response.user.name
            )
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
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        val request = OnboardingRequestDto(
            role = role.name,
            name = name,
            phoneNumber = phoneNumber
        )

        return remoteDataSource.completeOnboarding(token, request).map { it.toDomain() }
    }

    override suspend fun getCurrentUser(): IshaaraResult<User> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        return remoteDataSource.getCurrentUser(token).map { response ->
            response.data?.toDomain() ?: throw IllegalStateException("Empty user data")
        }
    }

    override suspend fun updateProfile(
        name: String?,
        phoneNumber: String?,
        image: String?
    ): IshaaraResult<User> {
        val token = sessionStore.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication())

        val request = UpdateUserRequestDto(name, phoneNumber, image)
        return remoteDataSource.updateProfile(token, request).map { it.toDomain() }
    }

    override fun observeSession(): Flow<AuthSession?> {
        return sessionStore.observeSession()
    }

    override suspend fun signOut(): IshaaraResult<Unit> {
        val token = sessionStore.getSession()?.token
        if (token != null) {
            remoteDataSource.signOut(token)
        }
        sessionStore.clearSession()
        return IshaaraResult.success(Unit)
    }

    private fun UserDto.toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        phoneNumber = phoneNumber,
        role = UserRole.fromString(role),
        profileImageUrl = image,
        isOnboarded = isOnboarded
    )
}
