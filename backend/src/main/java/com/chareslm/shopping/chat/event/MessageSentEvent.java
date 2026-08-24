package com.chareslm.shopping.chat.event;

import java.time.LocalDateTime;

/**
 * 消息发送事件。
 * 用于 WebSocket 推送、日志审计、Redis 未读数缓存预留等。
 */
public record MessageSentEvent(
        Long messageId,
        Long sessionId,
        Long senderId,
        Long receiverId,
        Integer msgType,
        String content,
        LocalDateTime sentAt
) {
}
