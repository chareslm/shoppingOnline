package com.chareslm.shopping.chat.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Issues short-lived, single-use tickets so access JWTs never appear in WebSocket URLs. */
@Service
public class ChatWebSocketTicketService {
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public IssuedTicket issue(Long userId) {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(TICKET_TTL);
        tickets.put(value, new Ticket(userId, expiresAt));
        return new IssuedTicket(value, expiresAt);
    }

    public Optional<Long> consume(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Ticket ticket = tickets.remove(value);
        if (ticket == null || !ticket.expiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(ticket.userId());
    }

    private record Ticket(Long userId, Instant expiresAt) {
    }

    public record IssuedTicket(String ticket, Instant expiresAt) {
    }
}
