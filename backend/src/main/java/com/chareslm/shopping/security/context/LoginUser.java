package com.chareslm.shopping.security.context;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;

public record LoginUser(Long userId, String username, Set<String> roles, Set<String> permissions) {

    public Collection<? extends GrantedAuthority> authorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }
}
