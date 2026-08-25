package com.chareslm.shopping.chat.controller;

import com.chareslm.shopping.chat.dto.response.WebSocketTicketResponse;
import com.chareslm.shopping.chat.service.ChatWebSocketTicketService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/websocket-ticket")
@RequiredArgsConstructor
public class ChatWebSocketTicketController {
    private final ChatWebSocketTicketService ticketService;

    @PostMapping
    public ApiResponse<WebSocketTicketResponse> issue() {
        ChatWebSocketTicketService.IssuedTicket issued = ticketService.issue(CurrentUser.require().userId());
        return ApiResponse.success(new WebSocketTicketResponse(issued.ticket(), issued.expiresAt()));
    }
}
