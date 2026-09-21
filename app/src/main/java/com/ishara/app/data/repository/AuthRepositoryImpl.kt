package com.ishara.app.data.repository

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.AuthMapper
import com.ishara.app.data.remote.datasource.AuthRemoteDataSource
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: SessionLocalDataSource
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSession> {
        return remoteDataSource.signInWithGoogle(idToken).map { dto ->
            val session = AuthMapper.toDomain(dto)
            localDataSource.saveSession(session)
            session
        }
    }

    override suspend fun completeOnboarding(role: UserRole): IshaaraResult<User> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.completeOnboarding(role.name, session.token).map { dto ->
            // Update session role in local store
            val updatedSession = session.copy(role = role)
            localDataSource.saveSession(updatedSession)
            AuthMapper.toDomain(dto)
        }
    }

    override suspend fun getCurrentUser(): IshaaraResult<User> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        return remoteDataSource.getCurrentUser(session.token).map { dto ->
            AuthMapper.toDomain(dto)
        }
    }

    override fun observeSession(): Flow<AuthSession?> {
        return localDataSource.observeSession()
    }

    override suspend fun signOut(): IshaaraResult<Unit> {
        val session = localDataSource.getSession()
        if (session != null) {
            remoteDataSource.signOut(session.token)
        }
        localDataSource.clearSession()
        return IshaaraResult.success(Unit)
    }
}
