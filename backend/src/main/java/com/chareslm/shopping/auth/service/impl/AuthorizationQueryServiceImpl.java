package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.response.PermissionResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;
import com.chareslm.shopping.auth.dto.response.AdminUserResponse;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.mapper.PermissionMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.service.AuthorizationQueryService;
import com.chareslm.shopping.common.api.PageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationQueryServiceImpl implements AuthorizationQueryService {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserAccountMapper userAccountMapper;

    public AuthorizationQueryServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper,
                                         UserAccountMapper userAccountMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public List<RoleResponse> listRoles() {
        return roleMapper.selectAllActive().stream()
                .map(role -> new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDataScope(),
                        Boolean.TRUE.equals(role.getBuiltIn())))
                .toList();
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.selectAllActive().stream()
                .map(permission -> new PermissionResponse(permission.getId(), permission.getCode(), permission.getName(),
                        permission.getResource(), permission.getActionCode(), permission.getDescription()))
                .toList();
    }

    @Override
    public PageResponse<AdminUserResponse> listUsers(String keyword, String status, int page, int pageSize) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = trimToNull(status);
        long total = userAccountMapper.countAdminPage(normalizedKeyword, normalizedStatus);
        List<AdminUserResponse> items = userAccountMapper
                .selectAdminPage(normalizedKeyword, normalizedStatus, (page - 1) * pageSize, pageSize)
                .stream()
                .map(this::toAdminUserResponse)
                .toList();
        return new PageResponse<>(items, total, page, pageSize);
    }

    private AdminUserResponse toAdminUserResponse(UserAccount user) {
        List<RoleResponse> roles = roleMapper.selectByUserId(user.getId()).stream()
                .map(this::toRoleResponse)
                .toList();
        return new AdminUserResponse(user.getId(), user.getUsername(), maskEmail(user.getEmail()),
                maskPhone(user.getPhone()), user.getStatus(), Boolean.TRUE.equals(user.getMustChangePassword()),
                roles, user.getCreatedAt(), user.getLastLoginAt());
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDataScope(),
                Boolean.TRUE.equals(role.getBuiltIn()));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int separator = email.indexOf('@');
        String local = email.substring(0, separator);
        return local.substring(0, 1) + "***" + email.substring(separator);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
