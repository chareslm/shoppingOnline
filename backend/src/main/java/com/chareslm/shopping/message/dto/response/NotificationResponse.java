package com.chareslm.shopping.message.dto.response;

import java.time.LocalDateTime;

/**
 * 通知响应 DTO。
 */
public record NotificationResponse(
        Long id,
        Long templateId,
        String templateCode,
        String title,
        String content,
        Integer category,
        String categoryDesc,
        String bizType,
        String bizId,
        Integer isRead,
        LocalDateTime readTime,
        Integer pushStatus,
        LocalDateTime pushTime,
        LocalDateTime createdAt
) {
}
