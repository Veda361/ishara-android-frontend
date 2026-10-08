package com.ishara.app.core.network

import android.util.Log
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

class KtorHttpClient(private val sessionStore: SessionStore) : IshaaraHttpClient {

    private val client = HttpClientProvider.client

    override suspend fun execute(
        request: HttpRequest
    ): IshaaraResult<HttpResponse> {
        val tag = "AUTH_DEBUG"
        return try {
            val session = sessionStore.getSession()
            
            Log.d(tag, "KtorHttpClient: Executing ${request.method} to ${request.url}")
            
            val response = client.request(request.url) {
                method = when (request.method) {
                    HttpMethod.GET -> io.ktor.http.HttpMethod.Get
                    HttpMethod.POST -> io.ktor.http.HttpMethod.Post
                    HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
                    HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
                    HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
                }

                // Add existing headers
                request.headers.forEach { (key, value) ->
                    header(key, value)
                }

                // Inject Authorization header if not present, session exists, and it's not an auth endpoint
                val isAuthEndpoint = request.url.contains("/api/auth/")
                if (!request.headers.containsKey("Authorization") && session != null && !isAuthEndpoint) {
                    header("Authorization", "Bearer ${session.token}")
                    Log.d(tag, "KtorHttpClient: Injected Authorization header")
                }

                request.queryParams.forEach { (key, value) ->
                    parameter(key, value)
                }

                request.body?.let {
                    setBody(it)
                }
            }

            val body = response.bodyAsText()
            val statusCode = response.status.value

            Log.d(tag, "KtorHttpClient: Response Code: $statusCode")
            Log.d(tag, "KtorHttpClient: Response Body: $body")

            if (statusCode in 200..299) {
                IshaaraResult.Success(
                    HttpResponse(
                        statusCode = statusCode,
                        headers = response.headers.entries().associate {
                            it.key to it.value.joinToString()
                        },
                        body = body
                    )
                )
            } else {
                Log.e(tag, "KtorHttpClient: HTTP Error $statusCode: $body")
                IshaaraResult.Failure(
                    ErrorNormalizer.normalize(statusCode, body)
                )
            }

        } catch (e: Exception) {
            Log.e(tag, "KtorHttpClient: Execution failed", e)
            IshaaraResult.Failure(
                ErrorNormalizer.normalize(500, null, e)
            )
        }
    }
}
