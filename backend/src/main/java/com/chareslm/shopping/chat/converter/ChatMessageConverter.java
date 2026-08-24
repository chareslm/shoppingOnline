package com.chareslm.shopping.chat.converter;

import com.chareslm.shopping.chat.dto.response.MessageResponse;
import com.chareslm.shopping.chat.entity.ChatMessage;

/**
 * 聊天消息实体 ↔ DTO 转换。
 */
public final class ChatMessageConverter {

    private ChatMessageConverter() {
    }

    /**
     * Entity → Response DTO。
     */
    public static MessageResponse toResponse(ChatMessage msg) {
        MessageResponse resp = new MessageResponse();
        resp.setId(msg.getId());
        resp.setSessionId(msg.getSessionId());
        resp.setSenderId(msg.getSenderId());
        resp.setSenderType(msg.getSenderType());
        resp.setContent(msg.getContent());
        resp.setMsgType(msg.getMsgType());
        resp.setExtraData(msg.getExtraData());
        resp.setIsRead(msg.getIsRead());
        resp.setReadTime(msg.getReadTime());
        resp.setStatus(msg.getStatus());
        resp.setCreatedAt(msg.getCreatedAt());
        return resp;
    }
}
