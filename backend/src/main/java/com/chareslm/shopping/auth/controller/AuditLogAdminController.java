package com.chareslm.shopping.auth.controller;

import com.chareslm.shopping.auth.dto.response.AuditLogResponse;
import com.chareslm.shopping.auth.service.AuditLogQueryService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Validated
public class AuditLogAdminController {
    private final AuditLogQueryService auditLogQueryService;

    public AuditLogAdminController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:audit:view')")
    public ApiResponse<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) @Size(max = 64) String actorKeyword,
            @RequestParam(required = false) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,63}") String module,
            @RequestParam(required = false) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,127}") String actionCode,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.success(auditLogQueryService.listAuditLogs(actorKeyword, module, actionCode,
                success, startAt, endAt, page, pageSize));
    }
}
