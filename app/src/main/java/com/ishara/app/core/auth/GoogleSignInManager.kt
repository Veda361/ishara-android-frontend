package com.ishara.app.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ishara.app.BuildConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult

class GoogleSignInManager(private val appContext: Context) {

    private val credentialManager = CredentialManager.create(appContext)

    suspend fun getGoogleIdToken(context: Context): IshaaraResult<String> {
        Log.d("AUTH_DEBUG", "GoogleSignInManager: getGoogleIdToken() started")
        
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        Log.d("AUTH_DEBUG", "GoogleSignInManager: Using Server Client ID: $serverClientId")

        if (serverClientId.isBlank()) {
            Log.e("AUTH_DEBUG", "GoogleSignInManager: GOOGLE_WEB_CLIENT_ID is BLANK! Check local.properties and app/build.gradle.kts")
            return IshaaraResult.failure(IshaaraError.Validation("serverClientId", "Google Server Client ID is missing in build config"))
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            Log.d("AUTH_DEBUG", "GoogleSignInManager: Launching CredentialManager.getCredential")
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            Log.d("AUTH_DEBUG", "GoogleSignInManager: Credential received: ${credential::class.java.simpleName}")

            when {
                credential is GoogleIdTokenCredential -> {
                    val idToken = credential.idToken
                    Log.d("AUTH_DEBUG", "GoogleSignInManager: GoogleIdTokenCredential parsed. Token length: ${idToken.length}")
                    IshaaraResult.success(idToken)
                }
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Log.d("AUTH_DEBUG", "GoogleSignInManager: CustomCredential (GoogleIdToken) parsed. Token length: ${idToken.length}")
                        IshaaraResult.success(idToken)
                    } catch (e: Exception) {
                        Log.e("AUTH_DEBUG", "GoogleSignInManager: Failed to parse Google ID Token from CustomCredential", e)
                        IshaaraResult.failure(IshaaraError.Authentication(message = "Failed to parse Google ID Token credential"))
                    }
                }
                else -> {
                    Log.e("AUTH_DEBUG", "GoogleSignInManager: Unexpected credential type: ${credential.type}")
                    IshaaraResult.failure(IshaaraError.Authentication(message = "Unexpected credential type: ${credential.type}"))
                }
            }
        } catch (e: GetCredentialException) {
            Log.e("AUTH_DEBUG", "GoogleSignInManager: GetCredentialException: ${e.type} - ${e.message}")
            IshaaraResult.failure(IshaaraError.Authentication(message = e.message ?: "Google Sign-In failed"))
        } catch (e: Exception) {
            Log.e("AUTH_DEBUG", "GoogleSignInManager: Unknown Exception in GoogleSignInManager", e)
            IshaaraResult.failure(IshaaraError.Unknown(message = e.message ?: "An unknown error occurred during Google Sign-In", cause = e))
        }
    }
}
