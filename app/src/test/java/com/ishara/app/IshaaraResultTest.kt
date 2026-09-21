package com.ishara.app

import com.ishara.app.core.network.ErrorNormalizer
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates domain result modeling and error normalization.
 */
class IshaaraResultTest {

    @Test
    fun result_success_mapsDataCorrectly() {
        val result: IshaaraResult<Int> = IshaaraResult.success(42)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is IshaaraResult.Success)
        assertEquals(84, (mapped as IshaaraResult.Success).data)
    }

    @Test
    fun result_failure_propagatesErrorWithoutExecutingMap() {
        val error = IshaaraError.Network("Offline")
        val result: IshaaraResult<Int> = IshaaraResult.failure(error)
        var transformed = false

        val mapped = result.map {
            transformed = true
            it * 2
        }

        assertTrue(mapped is IshaaraResult.Failure)
        assertEquals(false, transformed)
        assertEquals(error, (mapped as IshaaraResult.Failure).error)
    }

    @Test
    fun errorNormalizer_converts401ToAuthenticationError() {
        val error = ErrorNormalizer.normalize(401, "{\"message\":\"Token expired\"}")
        assertTrue(error is IshaaraError.Authentication)
        assertEquals("Token expired", error.message)
    }

    @Test
    fun errorNormalizer_converts429ToRateLimitedError() {
        val error = ErrorNormalizer.normalize(429, "{\"message\":\"Too many attempts\"}")
        assertTrue(error is IshaaraError.RateLimited)
        assertEquals("Too many attempts", error.message)
    }
}
