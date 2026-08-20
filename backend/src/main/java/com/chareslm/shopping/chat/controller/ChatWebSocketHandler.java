package com.chareslm.shopping.chat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chareslm.shopping.security.context.LoginUser;
import com.chareslm.shopping.security.jwt.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服聊天 WebSocket 处理器。
 * <p>
 * 负责实时消息推送，不做持久化。
 * 消息持久化由 Service 层负责。
 * <p>
 * 客户端连接时需在 URL 携带 JWT Token：ws://host/ws/chat?token={accessToken}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** userId → WebSocketSession */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final JwtTokenService jwtTokenService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            MDC.put("userId", String.valueOf(userId));
            MDC.put("module", "CHAT_WS");
            MDC.put("action", "connected");
            log.info("WebSocket connected: userId={}", userId);
            MDC.clear();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // WebSocket 仅负责实时消息推送，不处理业务逻辑
        // 业务逻辑（持久化、通知）由 HTTP 接口 → Service 层完成
        // 客户端仅通过 WebSocket 接收推送
        JsonNode payload = objectMapper.readTree(message.getPayload());
        log.debug("Received WebSocket message: {}", payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
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
     * 从 WebSocket 连接的 URL 中通过 JWT Token 解析用户ID。
     * 客户端连接时需携带 access token：ws://host/ws/chat?token={accessToken}
     */
    private Long extractUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                try {
                    LoginUser loginUser = jwtTokenService.parseAccessToken(kv[1]);
                    return loginUser.userId();
                } catch (Exception e) {
                    log.warn("Invalid JWT token in WebSocket connection: {}", e.getMessage());
                    return null;
                }
            }
        }
        log.warn("WebSocket connection missing token parameter");
        return null;
    }
}
