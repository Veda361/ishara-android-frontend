package com.ishara.app.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.ishara.app.BuildConfig
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError

/**
 * Result of a Google Sign-In attempt.
 */
sealed interface GoogleAuthResult {
    data class Success(val idToken: String) : GoogleAuthResult
    object Cancelled : GoogleAuthResult
    object NoCredentialsAvailable : GoogleAuthResult
    data class ConfigurationMissing(val message: String) : GoogleAuthResult
    data class Failure(val error: IshaaraError) : GoogleAuthResult
}

/**
 * Clean architectural abstraction for Google Authentication on Android.
 * Isolates Google Play Services / Credential Manager implementation details from domain and presentation logic.
 */
interface GoogleAuthClient {
    /**
     * Checks whether the Google OAuth Web Client ID is configured in the application environment.
     */
    fun isConfigured(): Boolean

    /**
     * Initiates Google Sign-In and returns a GoogleAuthResult.
     */
    suspend fun signIn(context: Context): GoogleAuthResult
}

/**
 * Standard implementation of GoogleAuthClient using AndroidX Credential Manager
 * and Google Identity Services (GetGoogleIdOption and GoogleIdTokenCredential).
 *
 * Checks for a configured Google Web Client ID (serverClientId) used as the ID-token audience.
 * If unconfigured, returns ConfigurationMissing without crashing or using fake tokens.
 */
class DefaultGoogleAuthClient(
    private val serverClientId: String? = null
) : GoogleAuthClient {

    override fun isConfigured(): Boolean {
        return !serverClientId.isNullOrBlank() && serverClientId != "UNCONFIGURED"
    }

    override suspend fun signIn(context: Context): GoogleAuthResult {
        if (!isConfigured()) {
            IshaaraLogger.w(TAG, "Google Sign-In attempted without configured Google Server Client ID.")
            return GoogleAuthResult.ConfigurationMissing(
                "Google Sign-In is not configured. Please set the Google OAuth Server Client ID."
            )
        }

        val audienceClientId = serverClientId!!
        IshaaraLogger.i(TAG, "Google sign-in started")

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(audienceClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            IshaaraLogger.d(TAG, "Credential Manager invoked")

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                IshaaraLogger.d(TAG, "Google credential received")
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    IshaaraLogger.d(TAG, "ID token obtained")
                    GoogleAuthResult.Success(idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    IshaaraLogger.e(TAG, "Failed to parse Google ID token credential: ${e.message}", e)
                    GoogleAuthResult.Failure(
                        IshaaraError.Authentication(
                            code = 400,
                            message = "Failed to parse Google ID token credential.",
                            cause = e
                        )
                    )
                }
            } else {
                IshaaraLogger.w(TAG, "Unexpected credential type received: ${credential.type}")
                GoogleAuthResult.Failure(
                    IshaaraError.Authentication(
                        code = 400,
                        message = "Unexpected credential type received from Credential Manager."
                    )
                )
            }
        } catch (e: GetCredentialCancellationException) {
            IshaaraLogger.i(TAG, "Google sign-in cancelled by user.")
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            IshaaraLogger.w(TAG, "No Google credentials available on device.")
            GoogleAuthResult.NoCredentialsAvailable
        } catch (e: GetCredentialException) {
            IshaaraLogger.e(TAG, "Credential Manager error: ${e.type} - ${e.message}", e)
            GoogleAuthResult.Failure(
                IshaaraError.Authentication(
                    code = 401,
                    message = e.message?.takeIf { it.isNotBlank() } ?: "Google sign-in failed.",
                    cause = e
                )
            )
        } catch (e: Exception) {
            IshaaraLogger.e(TAG, "Unexpected error during Google sign-in: ${e.message}", e)
            GoogleAuthResult.Failure(
                IshaaraError.Authentication(
                    code = 500,
                    message = e.localizedMessage ?: "An unexpected error occurred during Google sign-in.",
                    cause = e
                )
            )
        }
    }

    companion object {
        private const val TAG = "GoogleAuthClient"
    }
}
