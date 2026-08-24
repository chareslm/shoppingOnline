package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.response.AuditLogResponse;
import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.auth.service.AuditLogQueryService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuditLogQueryServiceImpl implements AuditLogQueryService {
    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "password", "token", "secret", "authorization", "cookie", "credential", "privatekey",
            "phone", "email", "realname", "address");
    private static final Pattern EDGE = Pattern.compile("Edg/([\\d.]+)");
    private static final Pattern CHROME = Pattern.compile("Chrome/([\\d.]+)");
    private static final Pattern FIREFOX = Pattern.compile("Firefox/([\\d.]+)");
    private static final Pattern SAFARI = Pattern.compile("Version/([\\d.]+).+Safari/");

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogQueryServiceImpl(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResponse<AuditLogResponse> listAuditLogs(String actorKeyword, String module, String actionCode,
                                                        Boolean success, LocalDateTime startAt, LocalDateTime endAt,
                                                        int page, int pageSize) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String normalizedActor = trimToNull(actorKeyword);
        String normalizedModule = upperToNull(module);
        String normalizedAction = upperToNull(actionCode);
        long total = auditLogMapper.countAdminPage(normalizedActor, normalizedModule, normalizedAction,
                success, startAt, endAt);
        var items = auditLogMapper.selectAdminPage(normalizedActor, normalizedModule, normalizedAction,
                        success, startAt, endAt, (page - 1) * pageSize, pageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(items, total, page, pageSize);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorUserId(), log.getActorUsername(), log.getModule(),
                log.getActionCode(), log.getTargetType(), log.getTargetId(), Boolean.TRUE.equals(log.getSuccess()),
                log.getTraceId(), log.getRequestMethod(), log.getRequestPath(), maskIp(log.getClientIp()),
                summarizeClient(log.getUserAgent()), sanitizeDetail(log.getDetail()), log.getCreatedAt());
    }

    private Object sanitizeDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(detail);
            sanitizeNode(node);
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception ignored) {
            return "[内容不可解析]";
        }
    }

    private void sanitizeNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveKey(field.getKey())) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(field.getKey(), "***");
                } else {
                    sanitizeNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::sanitizeNode);
        }
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

    static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String[] ipv4 = ip.split("\\.", -1);
        if (ipv4.length == 4) {
            return ipv4[0] + "." + ipv4[1] + ".*.*";
        }
        int separator = ip.indexOf(':');
        return separator < 0 ? "***" : ip.substring(0, separator) + ":****";
    }

    static String summarizeClient(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String browser = match(EDGE, userAgent, "Microsoft Edge");
        if (browser == null) browser = match(CHROME, userAgent, "Chrome");
        if (browser == null) browser = match(FIREFOX, userAgent, "Firefox");
        if (browser == null) browser = match(SAFARI, userAgent, "Safari");
        if (browser == null) browser = "Unknown client";
        String platform = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Android") ? "Android"
                : userAgent.matches(".*(iPhone|iPad).*") ? "iOS"
                : userAgent.contains("Macintosh") ? "macOS"
                : userAgent.contains("Linux") ? "Linux" : "Unknown OS";
        return browser + " on " + platform;
    }

    private static String match(Pattern pattern, String value, String label) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? label + " " + matcher.group(1) : null;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
