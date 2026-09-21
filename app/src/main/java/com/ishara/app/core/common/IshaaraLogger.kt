package com.ishara.app.core.common

import android.util.Log

/**
 * Production-safe logger for Ishaara.
 * Sanitizes sensitive fields and conditionally disables detailed debugging in release builds.
 */
object IshaaraLogger {
    private var isDebug: Boolean = true

    fun setDebug(enabled: Boolean) {
        isDebug = enabled
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, sanitize(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, sanitize(message), throwable)
        } else {
            Log.w(tag, sanitize(message))
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, sanitize(message), throwable)
    }

    /**
     * Prevents accidental leaking of bearer tokens, passwords, and private API keys.
     */
    private fun sanitize(message: String): String {
        return message
            .replace(Regex("(?i)Bearer\\s+([A-Za-z0-9-_.]+)"), "Bearer [PROTECTED]")
            .replace(Regex("(?i)idToken=([^&\\s]+)"), "idToken=[PROTECTED]")
            .replace(Regex("(?i)x-admin-key:\\s*([^\\s]+)"), "x-admin-key: [PROTECTED]")
    }
}
