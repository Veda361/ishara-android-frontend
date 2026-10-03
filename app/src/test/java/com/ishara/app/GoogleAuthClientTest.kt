package com.ishara.app

import com.ishara.app.core.auth.DefaultGoogleAuthClient
import com.ishara.app.core.auth.GoogleAuthClient
import com.ishara.app.core.auth.GoogleAuthResult
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.datasource.AuthRemoteDataSourceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthClientTest {

    @Test
    fun test_unconfiguredClient_returnsFalseForIsConfigured() {
        val clientNull = DefaultGoogleAuthClient(null)
        assertFalse(clientNull.isConfigured())

        val clientEmpty = DefaultGoogleAuthClient("")
        assertFalse(clientEmpty.isConfigured())

        val clientBlank = DefaultGoogleAuthClient("   ")
        assertFalse(clientBlank.isConfigured())

        val clientPlaceholder = DefaultGoogleAuthClient("UNCONFIGURED")
        assertFalse(clientPlaceholder.isConfigured())
    }

    @Test
    fun test_configuredClient_returnsTrueForIsConfigured() {
        val client = DefaultGoogleAuthClient("123456789-abcdef.apps.googleusercontent.com")
        assertTrue(client.isConfigured())
    }

    @Test
    fun test_buildConfig_hasConfiguredWebClientId() {
        assertTrue(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank())
        assertEquals("879711366748-vte2ip1qrc5vf2jd13hcvgisprraqoc9.apps.googleusercontent.com", BuildConfig.GOOGLE_WEB_CLIENT_ID)
        val client = DefaultGoogleAuthClient(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        assertTrue(client.isConfigured())
    }

    @Test
    fun test_unconfiguredClient_signInReturnsConfigurationMissing() = runBlocking {
        val client = DefaultGoogleAuthClient(null)
        // Pass dummy / null context because unconfigured check happens before CredentialManager.create
        val dummyContext = android.content.ContextWrapper(null)
        val result = client.signIn(dummyContext)

        assertTrue(result is GoogleAuthResult.ConfigurationMissing)
        val configMissing = result as GoogleAuthResult.ConfigurationMissing
        assertTrue(configMissing.message.contains("Google OAuth Server Client ID"))
    }

    @Test
    fun test_betterAuthRequest_containsNestedIdToken() = runBlocking {
        var capturedRequest: HttpRequest? = null
        val mockHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                capturedRequest = request
                return IshaaraResult.success(
                    HttpResponse(
                        statusCode = 200,
                        headers = emptyMap(),
                        body = """{"token":"test_token_123","userId":"u1","role":"USER","expiresAt":1727280000000}"""
                    )
                )
            }
        }

        val remoteDataSource = AuthRemoteDataSourceImpl(mockHttpClient, NetworkConfig())
        val result = remoteDataSource.signInWithGoogle("sample_google_jwt_token")

        assertTrue(result.isSuccess)
        assertNotNull(capturedRequest)
        assertEquals(HttpMethod.POST, capturedRequest?.method)
        assertTrue(capturedRequest?.url?.endsWith("/api/auth/sign-in/social") == true)

        val expectedBody = """{"provider":"google","idToken":{"token":"sample_google_jwt_token"}}"""
        assertEquals(expectedBody, capturedRequest?.body)
    }

    @Test
    fun test_betterAuthResponse_parsesNestedSessionAndIsoTimestamp() = runBlocking {
        val mockHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                val jsonResponse = """
                {
                    "session": {
                        "token": "session_tok_nested",
                        "userId": "user_nested_id",
                        "expiresAt": "2026-10-01T12:00:00.000Z"
                    },
                    "user": {
                        "id": "user_nested_id",
                        "name": "Alex",
                        "role": "DRIVER_CONDUCTOR"
                    }
                }
                """.trimIndent()
                return IshaaraResult.success(HttpResponse(200, jsonResponse))
            }
        }

        val remoteDataSource = AuthRemoteDataSourceImpl(mockHttpClient, NetworkConfig())
        val result = remoteDataSource.signInWithGoogle("token_sample")

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("session_tok_nested", session?.token)
        assertEquals("user_nested_id", session?.userId)
        assertEquals("DRIVER_CONDUCTOR", session?.role)
        assertNotNull(session?.expiresAt)
    }

    @Test
    fun test_googleAuthResult_sealedVariants() {
        val success = GoogleAuthResult.Success("tok_abc")
        assertEquals("tok_abc", success.idToken)

        val cancelled = GoogleAuthResult.Cancelled
        assertTrue(cancelled is GoogleAuthResult)

        val noCreds = GoogleAuthResult.NoCredentialsAvailable
        assertTrue(noCreds is GoogleAuthResult)

        val missing = GoogleAuthResult.ConfigurationMissing("missing")
        assertEquals("missing", missing.message)

        val failure = GoogleAuthResult.Failure(IshaaraError.Authentication(message = "fail"))
        assertEquals("fail", failure.error.message)
    }
}
