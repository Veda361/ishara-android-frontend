package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.RegisterPushTokenResponseDto
import com.ishara.app.domain.model.DeviceTokenRegistration

object DeviceTokenMapper {

    fun toDomain(dto: RegisterPushTokenResponseDto): DeviceTokenRegistration {
        return DeviceTokenRegistration(
            tokenMasked = dto.tokenMasked,
            platform = dto.platform,
            isActive = dto.isActive,
            lastSeenAt = dto.lastSeenAt,
            success = dto.success,
            registered = dto.registered
        )
    }
}
