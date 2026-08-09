package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.entity.Permission;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.mapper.PermissionMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationQueryServiceImplTest {

    @Test
    void listsActiveRolesAndPermissionsAsApiResponses() {
        Role role = new Role();
        role.setId(1L);
        role.setCode("SUPER_ADMIN");
        role.setName("超级管理员");
        role.setDataScope("ALL");
        role.setBuiltIn(true);
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setCode("system:role:view");
        permission.setName("查看角色");
        permission.setResource("system:role");
        permission.setActionCode("view");

        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        when(roleMapper.selectAllActive()).thenReturn(List.of(role));
        when(permissionMapper.selectAllActive()).thenReturn(List.of(permission));
        AuthorizationQueryServiceImpl service = new AuthorizationQueryServiceImpl(roleMapper, permissionMapper);

        assertEquals("SUPER_ADMIN", service.listRoles().getFirst().code());
        assertEquals("system:role:view", service.listPermissions().getFirst().code());
    }
}
