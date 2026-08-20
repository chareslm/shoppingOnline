package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

final class AuditRequestMetadata {
    private AuditRequestMetadata() {
    }

    static void populate(AuditLog log) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        log.setTraceId(limit(trimToNull(request.getHeader("X-Trace-Id")), 64));
        log.setRequestMethod(limit(trimToNull(request.getMethod()), 16));
        log.setRequestPath(limit(trimToNull(request.getRequestURI()), 255));
        log.setClientIp(limit(trimToNull(request.getRemoteAddr()), 45));
        log.setUserAgent(limit(trimToNull(request.getHeader("User-Agent")), 512));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
