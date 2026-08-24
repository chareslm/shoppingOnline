package com.chareslm.shopping.chat.converter;

import com.chareslm.shopping.chat.dto.response.SessionResponse;
import com.chareslm.shopping.chat.entity.ChatSession;

/**
 * 会话实体 ↔ DTO 转换。
 */
public final class ChatSessionConverter {

    private ChatSessionConverter() {
    }

    /**
     * Entity → Response DTO。
     */
    public static SessionResponse toResponse(ChatSession session, int unreadCount) {
        SessionResponse resp = new SessionResponse();
        resp.setSessionId(session.getId());
        resp.setUserId(session.getUserId());
        resp.setShopId(session.getShopId());
        resp.setCsUserId(session.getCsUserId());
        resp.setSubject(session.getSubject());
        resp.setLastMessage(session.getLastMessage());
        resp.setLastMessageTime(session.getLastMessageTime());
        resp.setStatus(session.getStatus());
        resp.setPriority(session.getPriority());
        resp.setCreatedAt(session.getCreatedAt());
        resp.setUnreadCount(unreadCount);
        return resp;
    }
}
