package com.ishara.app.core.common

import android.util.Log

/**
 * Production-safe logger for Ishaara.
 * Sanitizes sensitive fields, conditionally disables detailed debugging in release builds,
 * and gracefully falls back in JVM unit-test environments where android.util.Log is unmocked.
 */
object IshaaraLogger {
    private var isDebug: Boolean = true

    fun setDebug(enabled: Boolean) {
        isDebug = enabled
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            val sanitized = sanitize(message)
            try {
                Log.d(tag, sanitized)
            } catch (e: RuntimeException) {
                // JVM test fallback
                println("DEBUG: [$tag] $sanitized")
            }
        }
    }

    fun i(tag: String, message: String) {
        val sanitized = sanitize(message)
        try {
            Log.i(tag, sanitized)
        } catch (e: RuntimeException) {
            println("INFO: [$tag] $sanitized")
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        try {
            if (throwable != null) {
                Log.w(tag, sanitized, throwable)
            } else {
                Log.w(tag, sanitized)
            }
        } catch (e: RuntimeException) {
            println("WARN: [$tag] $sanitized")
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        try {
            Log.e(tag, sanitized, throwable)
        } catch (e: RuntimeException) {
            println("ERROR: [$tag] $sanitized")
        }
    }

    /**
     * Prevents accidental leaking of bearer tokens, passwords, and private API keys.
     */
    fun sanitize(message: String): String {
        return message
            .replace(Regex("(?i)Bearer\\s+([A-Za-z0-9-_.]+)"), "Bearer [PROTECTED]")
            .replace(Regex("(?i)Authorization:\\s*([^\\s,]+)"), "Authorization: [REDACTED]")
            .replace(Regex("(?i)idToken=([^&\\s]+)"), "idToken=[PROTECTED]")
            .replace(Regex("(?i)([?&]token=)([^&\\s]+)"), "$1[PROTECTED]")
            .replace(Regex("(?i)(\"token\"\\s*:\\s*\")([^\"]+)(\")"), "$1[PROTECTED]$3")
            .replace(Regex("(?i)(\"otp\"\\s*:\\s*\")([^\"]+)(\")"), "$1[PROTECTED]$3")
            .replace(Regex("(?i)(otp=)([^&\\s]+)"), "$1[PROTECTED]")
            .replace(Regex("(?i)x-admin-key:\\s*([^\\s]+)"), "x-admin-key: [PROTECTED]")
    }

    /**
     * Safe coordinate log formatter: coarsens precision to 2 decimal places in debug,
     * completely redacts in production.
     */
    fun formatCoordinatesForLog(latitude: Double, longitude: Double): String {
        return if (isDebug) {
            String.format(java.util.Locale.US, "%.2f***, %.2f***", latitude, longitude)
        } else {
            "[REDACTED]"
        }
    }
}
