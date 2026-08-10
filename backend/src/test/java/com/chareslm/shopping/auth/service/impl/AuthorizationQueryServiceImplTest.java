package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.entity.Permission;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.mapper.PermissionMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
        AuthorizationQueryServiceImpl service = new AuthorizationQueryServiceImpl(roleMapper, permissionMapper,
                mock(UserAccountMapper.class));

        assertEquals("SUPER_ADMIN", service.listRoles().getFirst().code());
        assertEquals("system:role:view", service.listPermissions().getFirst().code());
    }

    @Test
    void listsUsersWithMaskedContactInformationAndAssignedRoles() {
        UserAccount user = new UserAccount();
        user.setId(8L);
        user.setUsername("customer_demo");
        user.setEmail("customer@example.com");
        user.setPhone("13812345678");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.of(2026, 8, 10, 9, 0));

        Role role = new Role();
        role.setId(6L);
        role.setCode("USER");
        role.setName("普通用户");
        role.setDataScope("SELF");
        role.setBuiltIn(true);

        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        when(userAccountMapper.countAdminPage("customer", "ACTIVE")).thenReturn(1L);
        when(userAccountMapper.selectAdminPage("customer", "ACTIVE", 0, 20)).thenReturn(List.of(user));
        when(roleMapper.selectByUserId(8L)).thenReturn(List.of(role));
        AuthorizationQueryServiceImpl service = new AuthorizationQueryServiceImpl(roleMapper,
                mock(PermissionMapper.class), userAccountMapper);

        var page = service.listUsers(" customer ", "ACTIVE", 1, 20);

        assertEquals(1L, page.total());
        assertEquals("c***@example.com", page.items().getFirst().maskedEmail());
        assertEquals("138****5678", page.items().getFirst().maskedPhone());
        assertEquals("USER", page.items().getFirst().roles().getFirst().code());
    }
}
