package com.ishara.app.feature.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of AuthRepository.
 * Note: Per project requirements, the backend is the primary source of truth.
 * This implementation is kept for Firebase-specific logic if needed by the presentation layer,
 * but AuthRepositoryImpl is the primary repository used in AppContainer.
 */
class FirebaseAuthRepository : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    override suspend fun signInWithGoogle(
        idToken: String
    ): IshaaraResult<AuthSession> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user!!
            val token = firebaseUser.getIdToken(false).await().token ?: ""

            IshaaraResult.success(
                AuthSession(
                    token = token,
                    userId = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    role = UserRole.USER
                )
            )
        } catch (e: Exception) {
            IshaaraResult.failure(
                IshaaraError.Unknown(
                    message = e.message ?: "Unknown Firebase error",
                    cause = e
                )
            )
        }
    }

    override suspend fun sendEmailOtp(email: String): IshaaraResult<Unit> {
        return IshaaraResult.failure(IshaaraError.Unknown("Not implemented in Firebase repository. Use AuthRepositoryImpl."))
    }

    override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSession> {
        return IshaaraResult.failure(IshaaraError.Unknown("Not implemented in Firebase repository. Use AuthRepositoryImpl."))
    }

    override suspend fun completeOnboarding(
        role: UserRole,
        name: String,
        phoneNumber: String
    ): IshaaraResult<User> {
        return IshaaraResult.failure(IshaaraError.Unknown("Not implemented in Firebase repository. Use AuthRepositoryImpl."))
    }

    override suspend fun getCurrentUser(): IshaaraResult<User> {
        return IshaaraResult.failure(IshaaraError.Unknown("Not implemented in Firebase repository. Use AuthRepositoryImpl."))
    }

    override suspend fun updateProfile(
        name: String?,
        phoneNumber: String?,
        image: String?
    ): IshaaraResult<User> {
        return IshaaraResult.failure(IshaaraError.Unknown("Not implemented in Firebase repository. Use AuthRepositoryImpl."))
    }

    override fun observeSession(): Flow<AuthSession?> =
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener {
                val user = auth.currentUser
                if (user == null) {
                    trySend(null)
                } else {
                    // Note: session token is not reactively available here without an async call
                    trySend(
                        AuthSession(
                            token = "", // Placeholder
                            userId = user.uid,
                            name = user.displayName ?: "",
                            email = user.email ?: "",
                            role = UserRole.USER
                        )
                    )
                }
            }
            auth.addAuthStateListener(listener)
            awaitClose {
                auth.removeAuthStateListener(listener)
            }
        }

    override suspend fun signOut(): IshaaraResult<Unit> {
        auth.signOut()
        return IshaaraResult.success(Unit)
    }
}
