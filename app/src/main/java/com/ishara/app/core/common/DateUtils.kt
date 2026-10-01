package com.ishara.app.core.common

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val isoFormatNoMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseIso8601(dateString: String?): Long? {
        if (dateString == null) return null
        return try {
            isoFormat.parse(dateString)?.time
        } catch (e: Exception) {
            try {
                isoFormatNoMillis.parse(dateString)?.time
            } catch (e2: Exception) {
                dateString.toLongOrNull()
            }
        }
    }
}
