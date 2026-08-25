package com.chareslm.shopping.security.context;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.Set;

public record LoginUser(Long userId, String username, Set<String> roles, Set<String> permissions,
                        boolean mustChangePassword, Long deviceId) {

    public Collection<? extends GrantedAuthority> authorities() {
        return Stream.concat(
                        permissions.stream(),
                        roles.stream().map(role -> "ROLE_" + role))
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
