package com.chareslm.shopping.auth.dto.response;

import java.util.Set;

public record CurrentUserResponse(Long userId, String username, Set<String> roles, Set<String> permissions,
                                  boolean mustChangePassword) {
}
