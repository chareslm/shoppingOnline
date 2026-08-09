package com.chareslm.shopping.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestSecurityHandlerTest {

    @Test
    void authenticationEntryPointReturnsUnifiedUnauthorizedJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(new ObjectMapper()).commence(request, response,
                new InsufficientAuthenticationException("missing token"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("40101"));
        assertTrue(response.getContentAsString().contains("trace-1"));
    }

    @Test
    void accessDeniedHandlerReturnsUnifiedForbiddenJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(new ObjectMapper()).handle(request, response, new AccessDeniedException("forbidden"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("40301"));
    }
}
