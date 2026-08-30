package com.chareslm.shopping.chat.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatWebSocketTicketServiceTest {
    @Test
    void ticketCanOnlyBeConsumedOnce() {
        ChatWebSocketTicketService service = new ChatWebSocketTicketService();
        ChatWebSocketTicketService.IssuedTicket issued = service.issue(42L);

        assertEquals(42L, service.consume(issued.ticket()).orElseThrow());
        assertTrue(service.consume(issued.ticket()).isEmpty());
    }

    @Test
    void missingTicketIsRejected() {
        ChatWebSocketTicketService service = new ChatWebSocketTicketService();

        assertTrue(service.consume(null).isEmpty());
        assertTrue(service.consume("unknown").isEmpty());
    }
}
