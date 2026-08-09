package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.auth.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditLogMapper auditLogMapper;

    public AuditServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorUserId, String module, String actionCode, String targetType, String targetId,
                       boolean success) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setModule(module);
        log.setActionCode(actionCode);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setSuccess(success);
        auditLogMapper.insert(log);
    }
}
