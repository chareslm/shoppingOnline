package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.response.PermissionResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;
import com.chareslm.shopping.auth.mapper.PermissionMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.service.AuthorizationQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationQueryServiceImpl implements AuthorizationQueryService {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public AuthorizationQueryServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
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
}
