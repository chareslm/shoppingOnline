package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServiceImplTest {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordCapturesBoundedRequestMetadata() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/admin/authorization/users/7/roles");
        request.setRemoteAddr("10.20.30.40");
        request.addHeader("X-Trace-Id", "trace-123");
        request.addHeader("User-Agent", "Admin Browser");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        new AuditServiceImpl(mapper).record(1L, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER", "7", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(mapper).insert(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("trace-123", log.getTraceId());
        assertEquals("PUT", log.getRequestMethod());
        assertEquals("/api/admin/authorization/users/7/roles", log.getRequestPath());
        assertEquals("10.20.30.40", log.getClientIp());
        assertEquals("Admin Browser", log.getUserAgent());
    }
}
