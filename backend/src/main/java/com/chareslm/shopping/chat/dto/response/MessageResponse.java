package com.chareslm.shopping.chat.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息响应DTO。
 */
@Getter
@Setter
public class MessageResponse {

    private Long id;
    private Long sessionId;
    private Long senderId;
    private Integer senderType;
    private String senderName;
    private String senderAvatar;
    private String content;
    private Integer msgType;
    private String extraData;
    private Integer isRead;
    private LocalDateTime readTime;
    private Integer status;
    private LocalDateTime createdAt;
}
