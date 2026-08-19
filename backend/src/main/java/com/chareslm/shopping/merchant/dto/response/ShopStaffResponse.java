package com.chareslm.shopping.merchant.dto.response;

import java.time.LocalDateTime;

public record ShopStaffResponse(
        Long id,
        Long shopId,
        String shopName,
        Long userId,
        String displayName,
        String maskedEmail,
        String username,
        String status,
        String auditRemark,
        String emailDeliveryStatus,
        boolean mustChangePassword,
        LocalDateTime createdAt
) {
}
