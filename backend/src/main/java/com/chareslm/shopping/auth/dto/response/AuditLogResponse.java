package com.chareslm.shopping.auth.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        String module,
        String actionCode,
        String targetType,
        String targetId,
        boolean success,
        String traceId,
        String requestMethod,
        String requestPath,
        String maskedClientIp,
        String client,
        Object detail,
        LocalDateTime createdAt
) {
}
