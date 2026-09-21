package com.ishara.app.domain.model

/**
 * The two supported Android roles in the Ishaara platform.
 * Note: AGENCY_OWNER belongs exclusively to the future separate web dashboard and is excluded here.
 */
enum class UserRole {
    /** Passenger / Student */
    USER,

    /** Bus Driver / Conductor */
    DRIVER_CONDUCTOR;

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.trim()?.uppercase()) {
                "DRIVER_CONDUCTOR", "DRIVER" -> DRIVER_CONDUCTOR
                else -> USER
            }
        }
    }
}
