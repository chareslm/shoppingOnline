package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.response.AuditLogResponse;
import com.chareslm.shopping.common.api.PageResponse;

import java.time.LocalDateTime;

public interface AuditLogQueryService {
    PageResponse<AuditLogResponse> listAuditLogs(String actorKeyword, String module, String actionCode,
                                                 Boolean success, LocalDateTime startAt, LocalDateTime endAt,
                                                 int page, int pageSize);
}
