package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationManagementServiceImplTest {

    @Test
    void confirmsOperatorPasswordThenReplacesRolesAndRevokesSessions() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserAccount operator = new UserAccount();
        operator.setId(1L);
        operator.setPasswordHash(passwordEncoder.encode("Password123!"));
        UserAccount target = new UserAccount();
        target.setId(2L);
        Role adminRole = new Role();
        adminRole.setId(5L);
        adminRole.setCode("ADMIN");
        adminRole.setStatus("ACTIVE");

        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RefreshTokenMapper refreshTokenMapper = mock(RefreshTokenMapper.class);
        AuditService auditService = mock(AuditService.class);
        when(userAccountMapper.selectById(1L)).thenReturn(operator);
        when(userAccountMapper.selectById(2L)).thenReturn(target);
        when(roleMapper.selectByIds(Set.of(5L))).thenReturn(List.of(adminRole));
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(roleMapper.selectCodesByUserId(2L)).thenReturn(List.of("USER"));
        AuthorizationManagementServiceImpl service = new AuthorizationManagementServiceImpl(userAccountMapper, roleMapper,
                userRoleMapper, refreshTokenMapper, auditService, passwordEncoder);

        service.replaceUserRoles(1L, 2L, new AssignUserRolesRequest(Set.of(5L), "Password123!"));

        verify(userRoleMapper).deletePlatformRolesByUserId(2L);
        verify(userRoleMapper).insert(any(UserRole.class));
        verify(refreshTokenMapper).revokeActiveByUserId(2L, "ROLE_CHANGED");
        verify(auditService).record(1L, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER", "2", true);
    }

    @Test
    void retainsAnAuditRecordWhenPasswordConfirmationFails() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserAccount operator = new UserAccount();
        operator.setId(1L);
        operator.setPasswordHash(passwordEncoder.encode("Password123!"));
        UserAccount target = new UserAccount();
        target.setId(2L);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        AuditService auditService = mock(AuditService.class);
        when(userAccountMapper.selectById(1L)).thenReturn(operator);
        when(userAccountMapper.selectById(2L)).thenReturn(target);
        AuthorizationManagementServiceImpl service = new AuthorizationManagementServiceImpl(userAccountMapper,
                mock(RoleMapper.class), mock(UserRoleMapper.class), mock(RefreshTokenMapper.class), auditService,
                passwordEncoder);

        assertThrows(RuntimeException.class,
                () -> service.replaceUserRoles(1L, 2L, new AssignUserRolesRequest(Set.of(5L), "wrong-password")));

        verify(auditService).record(1L, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER", "2", false);
    }
}
