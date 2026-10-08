package com.ishara.app.domain.model

import kotlinx.serialization.Serializable

/**
 * The two supported Android roles in the Ishaara platform.
 * Note: AGENCY_OWNER belongs exclusively to the future separate web dashboard and is excluded here.
 */
@Serializable
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
