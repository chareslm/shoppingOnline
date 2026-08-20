package com.chareslm.shopping.message.event;

import java.time.LocalDateTime;

/**
 * 通知发送事件。
 * 用于异步推送、日志审计、Redis 未读数缓存预留等。
 */
public record NotificationSentEvent(
        Long notificationId,
        Long userId,
        String templateCode,
        Integer category,
        String title,
        LocalDateTime sentAt
) {
}
