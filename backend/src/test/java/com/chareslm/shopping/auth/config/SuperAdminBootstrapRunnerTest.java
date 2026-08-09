package com.chareslm.shopping.auth.config;

import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperAdminBootstrapRunnerTest {

    @Test
    void createsTheFirstSuperAdminWithoutLoggingThePassword() {
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        UserPreferenceMapper userPreferenceMapper = mock(UserPreferenceMapper.class);
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        Role superAdminRole = new Role();
        superAdminRole.setId(9L);
        when(roleMapper.countUsersByRoleCode("SUPER_ADMIN")).thenReturn(0L);
        when(userAccountMapper.selectByLoginIdentifier("admin_local")).thenReturn(null);
        when(roleMapper.selectActiveByCode("SUPER_ADMIN")).thenReturn(superAdminRole);
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserAccount.class).setId(99L);
            return 1;
        });
        SuperAdminBootstrapRunner runner = new SuperAdminBootstrapRunner(
                new SuperAdminBootstrapProperties(true, "admin_local", "Password123!"), userAccountMapper,
                roleMapper, userRoleMapper, userProfileMapper, userPreferenceMapper, auditLogMapper,
                new BCryptPasswordEncoder());

        runner.run(null);

        verify(userAccountMapper).insert(any(UserAccount.class));
        verify(userRoleMapper).insert(any(com.chareslm.shopping.auth.entity.UserRole.class));
        verify(auditLogMapper).insert(any(com.chareslm.shopping.auth.entity.AuditLog.class));
    }

    @Test
    void doesNotModifyAnyAccountWhenASuperAdminAlreadyExists() {
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        when(roleMapper.countUsersByRoleCode("SUPER_ADMIN")).thenReturn(1L);
        SuperAdminBootstrapRunner runner = new SuperAdminBootstrapRunner(
                new SuperAdminBootstrapProperties(true, "admin_local", "Password123!"), userAccountMapper,
                roleMapper, mock(UserRoleMapper.class), mock(UserProfileMapper.class), mock(UserPreferenceMapper.class),
                mock(AuditLogMapper.class), new BCryptPasswordEncoder());

        runner.run(null);

        verify(userAccountMapper, never()).insert(any(UserAccount.class));
    }
}
