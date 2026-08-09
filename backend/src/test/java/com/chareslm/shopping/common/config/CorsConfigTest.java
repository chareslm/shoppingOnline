package com.chareslm.shopping.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void onlyAllowsConfiguredOriginsWithoutCrossOriginCredentials() {
        CorsConfig config = new CorsConfig(new CorsProperties(List.of("http://localhost:5173")));
        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/auth/login/password"));

        assertEquals(List.of("http://localhost:5173"), cors.getAllowedOrigins());
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertFalse(cors.getAllowCredentials());
    }
}
