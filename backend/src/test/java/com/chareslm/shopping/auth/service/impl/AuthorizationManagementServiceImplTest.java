package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.dto.request.CreateAdminUserRequest;
import com.chareslm.shopping.auth.dto.response.CreatedAdminUserResponse;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationManagementServiceImplTest {
    private UserAccountMapper userAccountMapper;
    private RoleMapper roleMapper;
    private UserRoleMapper userRoleMapper;
    private RefreshTokenMapper refreshTokenMapper;
    private UserProfileMapper userProfileMapper;
    private UserPreferenceMapper userPreferenceMapper;
    private AuditService auditService;
    private MailService mailService;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthorizationManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        userAccountMapper = mock(UserAccountMapper.class);
        roleMapper = mock(RoleMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        userProfileMapper = mock(UserProfileMapper.class);
        userPreferenceMapper = mock(UserPreferenceMapper.class);
        auditService = mock(AuditService.class);
        mailService = mock(MailService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
        service = new AuthorizationManagementServiceImpl(userAccountMapper, roleMapper, userRoleMapper,
                refreshTokenMapper, userProfileMapper, userPreferenceMapper, auditService, passwordEncoder,
                mailService, transactions);
    }

    @Test
    void confirmsOperatorPasswordThenReplacesRolesAndRevokesSessions() {
        when(userAccountMapper.selectById(1L)).thenReturn(operator());
        when(userAccountMapper.selectById(2L)).thenReturn(target());
        when(roleMapper.selectByIds(Set.of(5L))).thenReturn(List.of(adminRole()));
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(roleMapper.selectCodesByUserId(2L)).thenReturn(List.of("USER"));

        service.replaceUserRoles(1L, 2L, new AssignUserRolesRequest(Set.of(5L), "Password123!"));

        verify(userRoleMapper).deletePlatformRolesByUserId(2L);
        verify(userRoleMapper).insert(any(UserRole.class));
        verify(refreshTokenMapper).revokeActiveByUserId(2L, "ROLE_CHANGED");
        verify(auditService).record(1L, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER", "2", true);
    }

    @Test
    void retainsAnAuditRecordWhenPasswordConfirmationFails() {
        when(userAccountMapper.selectById(1L)).thenReturn(operator());
        when(userAccountMapper.selectById(2L)).thenReturn(target());

        assertThrows(RuntimeException.class,
                () -> service.replaceUserRoles(1L, 2L, new AssignUserRolesRequest(Set.of(5L), "wrong-password")));

        verify(auditService).record(1L, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER", "2", false);
    }

    @Test
    void createUserIssuesTemporaryPasswordMarksForcedChangeAndEmailsTheAddress() {
        when(userAccountMapper.selectById(1L)).thenReturn(operator());
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(userAccountMapper.selectByLoginIdentifier(anyString())).thenReturn(null);
        when(roleMapper.selectByIds(Set.of(2L))).thenReturn(List.of(userRole()));
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setId(88L);
            assertTrue(account.getMustChangePassword());
            assertEquals("staff@example.com", account.getEmail());
            return 1;
        });

        CreatedAdminUserResponse response = service.createUser(1L,
                new CreateAdminUserRequest("staff_one", "staff@example.com", null, Set.of(2L), "Password123!"));

        assertEquals(88L, response.userId());
        assertEquals("SENT", response.mailDeliveryStatus());
        assertTrue(response.mustChangePassword());
        verify(userProfileMapper).insert(any(UserProfile.class));
        verify(userPreferenceMapper).insert(any(UserPreference.class));
        verify(mailService).sendAccountCredential(eq("staff@example.com"), eq("staff_one"), anyString());
        verify(auditService).record(1L, "AUTHORIZATION", "USER_CREATE", "USER", "88", true);
    }

    @Test
    void createUserKeepsAccountWhenSmtpFails() {
        when(userAccountMapper.selectById(1L)).thenReturn(operator());
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(userAccountMapper.selectByLoginIdentifier(anyString())).thenReturn(null);
        when(roleMapper.selectByIds(Set.of(2L))).thenReturn(List.of(userRole()));
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserAccount.class).setId(89L);
            return 1;
        });
        doThrow(new IllegalStateException("smtp unavailable")).when(mailService)
                .sendAccountCredential(eq("ops@example.com"), anyString(), anyString());

        CreatedAdminUserResponse response = service.createUser(1L,
                new CreateAdminUserRequest(null, "ops@example.com", null, Set.of(2L), "Password123!"));

        assertEquals("MAIL_FAILED", response.mailDeliveryStatus());
        verify(userAccountMapper).insert(any(UserAccount.class));
    }

    @Test
    void retryCredentialEmailRotatesTemporaryPassword() {
        UserAccount pending = target();
        pending.setEmail("ops@example.com");
        pending.setMustChangePassword(true);
        when(userAccountMapper.selectById(1L)).thenReturn(operator());
        when(userAccountMapper.selectById(2L)).thenReturn(pending);
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(roleMapper.selectByUserId(2L)).thenReturn(List.of(userRole()));

        CreatedAdminUserResponse response = service.retryCredentialEmail(1L, 2L);

        assertEquals("SENT", response.mailDeliveryStatus());
        verify(userAccountMapper).updateTemporaryPassword(eq(2L), anyString());
        verify(refreshTokenMapper).revokeActiveByUserId(2L, "TEMPORARY_PASSWORD_ROTATED");
        verify(mailService).sendAccountCredential(eq("ops@example.com"), anyString(), anyString());
        verify(auditService).record(1L, "AUTHORIZATION", "USER_CREDENTIAL_EMAIL_RETRY", "USER", "2", true);
    }

    @Test
    void retryIsRejectedAfterTheUserHasChangedTheTemporaryPassword() {
        UserAccount settled = target();
        settled.setEmail("ops@example.com");
        settled.setMustChangePassword(false);
        when(userAccountMapper.selectById(1L)).thenReturn(operator());
        when(userAccountMapper.selectById(2L)).thenReturn(settled);
        when(roleMapper.selectCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));

        assertThrows(BusinessException.class, () -> service.retryCredentialEmail(1L, 2L));
        verify(mailService, never()).sendAccountCredential(anyString(), anyString(), anyString());
    }

    private UserAccount operator() {
        UserAccount operator = new UserAccount();
        operator.setId(1L);
        operator.setPasswordHash(passwordEncoder.encode("Password123!"));
        return operator;
    }

    private UserAccount target() {
        UserAccount target = new UserAccount();
        target.setId(2L);
        return target;
    }

    private Role adminRole() {
        Role role = new Role();
        role.setId(5L);
        role.setCode("ADMIN");
        role.setName("平台管理员");
        role.setStatus("ACTIVE");
        return role;
    }

    private Role userRole() {
        Role role = new Role();
        role.setId(2L);
        role.setCode("USER");
        role.setName("普通用户");
        role.setStatus("ACTIVE");
        role.setBuiltIn(true);
        return role;
    }
}
