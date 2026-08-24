package com.chareslm.shopping.auth.dto.response;

import java.util.List;

public record CreatedAdminUserResponse(
        Long userId,
        String username,
        String maskedEmail,
        String status,
        boolean mustChangePassword,
        String mailDeliveryStatus,
        List<RoleResponse> roles
) {
}
