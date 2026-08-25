package com.chareslm.shopping.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服聊天 WebSocket 处理器。
 * <p>
 * 负责实时消息推送，不做持久化。
 * 消息持久化由 Service 层负责。
 * <p>
 * 客户端先通过认证 HTTP 接口获取一次性票据，再连接 ws://host/ws/chat?ticket={ticket}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** userId → WebSocketSession */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        userSessions.put(userId, session);
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT_WS");
        MDC.put("action", "connected");
        log.info("WebSocket connected: userId={}", userId);
        MDC.clear();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // WebSocket 仅负责实时消息推送，不处理业务逻辑
        // 业务逻辑（持久化、通知）由 HTTP 接口 → Service 层完成
        // 客户端仅通过 WebSocket 接收推送
        log.debug("Ignoring client WebSocket frame with {} bytes; chat writes use the authenticated HTTP API",
                message.getPayloadLength());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId, session);
            MDC.put("userId", String.valueOf(userId));
            MDC.put("module", "CHAT_WS");
            MDC.put("action", "disconnected");
            log.info("WebSocket disconnected: userId={}, status={}", userId, status);
            MDC.clear();
        }
    }

    /**
     * 向指定用户推送消息。
     */
    public void sendToUser(Long userId, Object data) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(data);
                session.sendMessage(new TextMessage(json));
                log.debug("Pushed message to userId={}", userId);
            } catch (IOException e) {
                log.warn("Failed to push message to userId={}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * 向指定会话的另一方推送消息。
     */
    public void pushMessage(Long receiverId, Object messageData) {
        sendToUser(receiverId, messageData);
    }

    /**
     * 用户ID仅由握手拦截器消费一次性票据后写入，不能由客户端直接提交。
     */
    private Long extractUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(ChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return userId instanceof Long value ? value : null;
    }
}
