package com.chareslm.shopping.security.context;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginUserTest {
    @Test
    void authoritiesContainPermissionsAndPrefixedRoles() {
        LoginUser user = new LoginUser(1L, "staff", Set.of("CUSTOMER_SERVICE"),
                Set.of("chat:view"), false, 2L);

        assertEquals(Set.of("chat:view", "ROLE_CUSTOMER_SERVICE"),
                user.authorities().stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()));
    }
}
