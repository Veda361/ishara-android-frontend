package com.ishara.app.domain.model

/**
 * The two supported Android roles in the Ishaara platform.
 * Note: AGENCY_OWNER belongs exclusively to the future separate web dashboard and is excluded here.
 */
enum class UserRole {
    /** Passenger / Student */
    USER,

    /** Bus Driver / Conductor */
    DRIVER_CONDUCTOR,

    /** Platform Administrator */
    ADMIN;

    companion object {
        /**
         * Safely parses backend role strings.
         * Returns null if the value is unrecognized or null, preventing dangerous default role assumptions.
         */
        fun fromBackendString(value: String?): UserRole? {
            return when (value?.trim()?.uppercase()) {
                "USER" -> USER
                "DRIVER_CONDUCTOR", "DRIVER" -> DRIVER_CONDUCTOR
                "ADMIN" -> ADMIN
                else -> null
            }
        }

        @Deprecated("Use fromBackendString to avoid guessing roles", ReplaceWith("fromBackendString(value)"))
        fun fromString(value: String?): UserRole {
            return fromBackendString(value) ?: USER
        }
    }
}
