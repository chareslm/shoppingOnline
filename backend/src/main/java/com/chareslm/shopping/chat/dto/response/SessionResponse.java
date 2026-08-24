package com.chareslm.shopping.chat.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 会话响应DTO。
 */
@Getter
@Setter
public class SessionResponse {

    private Long sessionId;
    private Long userId;
    private Long shopId;
    private Long csUserId;
    private String subject;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer status;
    private Integer priority;
    private Integer unreadCount;
    private LocalDateTime createdAt;
}
