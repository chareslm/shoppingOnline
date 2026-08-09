package com.chareslm.shopping.auth.service;

public interface AuditService {
    void record(Long actorUserId, String module, String actionCode, String targetType, String targetId, boolean success);
}
