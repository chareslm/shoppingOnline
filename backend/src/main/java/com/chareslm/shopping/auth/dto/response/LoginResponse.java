package com.chareslm.shopping.auth.dto.response;

import java.util.Set;

public record LoginResponse(Long userId, String username, String accessToken, String refreshToken, long expiresInSeconds,
                            Set<String> roles, Set<String> permissions, boolean mustChangePassword) {
}
