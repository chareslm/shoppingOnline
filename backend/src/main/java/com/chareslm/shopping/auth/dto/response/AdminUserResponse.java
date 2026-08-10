package com.chareslm.shopping.auth.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
        Long userId,
        String username,
        String maskedEmail,
        String maskedPhone,
        String status,
        List<RoleResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
