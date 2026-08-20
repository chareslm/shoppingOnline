package com.chareslm.shopping.auth.dto.response;

import java.time.LocalDateTime;

public record DeviceSessionResponse(
        Long id,
        String deviceType,
        String deviceName,
        String appVersion,
        String maskedIp,
        LocalDateTime lastActiveAt,
        LocalDateTime createdAt,
        String status,
        boolean current,
        LocalDateTime sessionExpiresAt
) {
}
