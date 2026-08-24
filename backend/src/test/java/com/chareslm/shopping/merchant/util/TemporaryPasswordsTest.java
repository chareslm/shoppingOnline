package com.chareslm.shopping.merchant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TemporaryPasswordsTest {
    @Test
    void offlinePasswordIsFixedWhenMailDisabled() {
        assertEquals("123456QWERqwer!@", TemporaryPasswords.issue(false));
        assertNotEquals(TemporaryPasswords.OFFLINE_DEFAULT, TemporaryPasswords.issue(true));
    }
}
