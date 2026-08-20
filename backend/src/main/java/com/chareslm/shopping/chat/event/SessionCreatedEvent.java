package com.chareslm.shopping.chat.event;

import java.time.LocalDateTime;

/**
 * 会话创建事件。
 * 用于异步通知、日志审计、Redis 在线状态缓存预留等。
 */
public record SessionCreatedEvent(
        Long sessionId,
        Long userId,
        Long shopId,
        Long csUserId,
        LocalDateTime createdAt
) {
}
