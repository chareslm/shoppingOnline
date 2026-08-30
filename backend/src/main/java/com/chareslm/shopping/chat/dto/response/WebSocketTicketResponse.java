package com.chareslm.shopping.chat.dto.response;

import java.time.Instant;

public record WebSocketTicketResponse(String ticket, Instant expiresAt) {
}
