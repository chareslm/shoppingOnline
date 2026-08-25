package com.chareslm.shopping.chat.controller;

import com.chareslm.shopping.chat.service.ChatWebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID_ATTRIBUTE = "chatUserId";

    private final ChatWebSocketTicketService ticketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("ticket");
        Optional<Long> userId = ticketService.consume(ticket);
        if (userId.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(USER_ID_ATTRIBUTE, userId.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake action is required.
    }
}
