package com.chareslm.shopping.chat.service;

import com.chareslm.shopping.chat.dto.request.CreateSessionRequest;
import com.chareslm.shopping.chat.dto.response.SessionResponse;

import java.util.List;

/**
 * 客服会话服务接口。
 */
public interface ChatSessionService {

    /**
     * 创建会话。
     */
    SessionResponse createSession(Long userId, CreateSessionRequest request);

    /**
     * 列表用户的会话。
     */
    List<SessionResponse> listUserSessions(Long userId);

    /**
     * 列表客服的会话。
     */
    List<SessionResponse> listCsSessions(Long csUserId);

    /**
     * 获取会话详情。
     */
    SessionResponse getSession(Long userId, Long sessionId);

    /**
     * 客服分配会话。
     */
    SessionResponse assignSession(Long csUserId, Long sessionId);

    /**
     * 关闭会话。
     */
    void closeSession(Long userId, Long sessionId);

    /**
     * 获取未读数。
     */
    int getUnreadCount(Long sessionId, Long userId);
}
