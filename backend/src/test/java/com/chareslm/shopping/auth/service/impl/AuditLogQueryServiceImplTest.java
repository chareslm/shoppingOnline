package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogQueryServiceImplTest {
    private final AuditLogMapper mapper = mock(AuditLogMapper.class);
    private final AuditLogQueryServiceImpl service = new AuditLogQueryServiceImpl(mapper, new ObjectMapper());

    @Test
    void queryNormalizesFiltersAndMasksSensitiveFields() {
        AuditLog log = new AuditLog();
        log.setId(9L);
        log.setActorUserId(3L);
        log.setActorUsername("root_admin");
        log.setModule("AUTH");
        log.setActionCode("PASSWORD_CHANGE");
        log.setSuccess(true);
        log.setClientIp("192.168.10.23");
        log.setUserAgent("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/151.0.0.0 Safari/537.36");
        log.setDetail("{\"roleIds\":[1,2],\"refreshToken\":\"secret-value\",\"profile\":{\"email\":\"a@example.com\"}}");
        log.setCreatedAt(LocalDateTime.of(2026, 8, 20, 10, 30));
        when(mapper.countAdminPage("root", "AUTH", "PASSWORD_CHANGE", true, null, null)).thenReturn(1L);
        when(mapper.selectAdminPage("root", "AUTH", "PASSWORD_CHANGE", true, null, null, 0, 20))
                .thenReturn(List.of(log));

        var result = service.listAuditLogs(" root ", "auth", "password_change", true,
                null, null, 1, 20);

        assertEquals(1L, result.total());
        var item = result.items().getFirst();
        assertEquals("192.168.*.*", item.maskedClientIp());
        assertEquals("Chrome 151.0.0.0 on Windows", item.client());
        Map<?, ?> detail = (Map<?, ?>) item.detail();
        assertEquals("***", detail.get("refreshToken"));
        assertEquals("***", ((Map<?, ?>) detail.get("profile")).get("email"));
        assertEquals(2, ((List<?>) detail.get("roleIds")).size());
        verify(mapper).selectAdminPage(eq("root"), eq("AUTH"), eq("PASSWORD_CHANGE"), eq(true),
                any(), any(), anyInt(), anyInt());
    }

    @Test
    void rejectsReversedTimeRange() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 21, 0, 0);
        LocalDateTime end = start.minusDays(1);

        assertThrows(BusinessException.class,
                () -> service.listAuditLogs(null, null, null, null, start, end, 1, 20));
    }
}
