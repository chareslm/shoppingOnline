package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.auth.service.AuthorizationManagementService;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuthorizationManagementServiceImpl implements AuthorizationManagementService {
    private static final Set<String> PLATFORM_ROLE_CODES = Set.of("USER", "ADMIN", "SUPER_ADMIN");

    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthorizationManagementServiceImpl(UserAccountMapper userAccountMapper, RoleMapper roleMapper,
                                              UserRoleMapper userRoleMapper, RefreshTokenMapper refreshTokenMapper,
                                              AuditService auditService, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void replaceUserRoles(Long operatorUserId, Long targetUserId, AssignUserRolesRequest request) {
        UserAccount operator = userAccountMapper.selectById(operatorUserId);
        UserAccount target = userAccountMapper.selectById(targetUserId);
        if (operator == null || target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.currentPassword(), operator.getPasswordHash())) {
            writeAudit(operatorUserId, targetUserId, false);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        List<Role> roles = roleMapper.selectByIds(request.roleIds());
        if (roles.size() != request.roleIds().size() || roles.stream().anyMatch(role -> !"ACTIVE".equals(role.getStatus()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Set<String> operatorRoles = Set.copyOf(roleMapper.selectCodesByUserId(operatorUserId));
        if (!operatorRoles.contains("SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (roles.stream().anyMatch(role -> !PLATFORM_ROLE_CODES.contains(role.getCode()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Set<String> targetRoles = Set.copyOf(roleMapper.selectCodesByUserId(targetUserId));
        boolean assignsSuperAdmin = roles.stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()));
        boolean removesSuperAdmin = targetRoles.contains("SUPER_ADMIN") && !assignsSuperAdmin;
        if (removesSuperAdmin && roleMapper.countUsersByRoleCode("SUPER_ADMIN") <= 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        userRoleMapper.deletePlatformRolesByUserId(targetUserId);
        for (Role role : roles) {
            UserRole relation = new UserRole();
            relation.setUserId(targetUserId);
            relation.setRoleId(role.getId());
            userRoleMapper.insert(relation);
        }
        refreshTokenMapper.revokeActiveByUserId(targetUserId, "ROLE_CHANGED");
        writeAudit(operatorUserId, targetUserId, true);
    }

    private void writeAudit(Long operatorUserId, Long targetUserId, boolean success) {
        auditService.record(operatorUserId, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER",
                targetUserId.toString(), success);
    }
}
