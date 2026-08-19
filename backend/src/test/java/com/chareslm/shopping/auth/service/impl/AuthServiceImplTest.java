package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.request.ChangePasswordRequest;
import com.chareslm.shopping.auth.dto.request.PasswordLoginRequest;
import com.chareslm.shopping.auth.dto.request.RegisterRequest;
import com.chareslm.shopping.auth.dto.response.LoginResponse;
import com.chareslm.shopping.auth.dto.response.RegisteredUserResponse;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.entity.RefreshToken;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserDevice;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.enums.DeviceType;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.auth.mapper.PermissionMapper;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserDeviceMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.security.jwt.JwtProperties;
import com.chareslm.shopping.security.jwt.JwtTokenService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {
    private UserAccountMapper userAccountMapper;
    private RoleMapper roleMapper;
    private PermissionMapper permissionMapper;
    private RefreshTokenMapper refreshTokenMapper;
    private UserRoleMapper userRoleMapper;
    private UserProfileMapper userProfileMapper;
    private UserPreferenceMapper userPreferenceMapper;
    private UserDeviceMapper userDeviceMapper;
    private AuditLogMapper auditLogMapper;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userAccountMapper = mock(UserAccountMapper.class);
        roleMapper = mock(RoleMapper.class);
        permissionMapper = mock(PermissionMapper.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        userProfileMapper = mock(UserProfileMapper.class);
        userPreferenceMapper = mock(UserPreferenceMapper.class);
        userDeviceMapper = mock(UserDeviceMapper.class);
        auditLogMapper = mock(AuditLogMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties jwtProperties = new JwtProperties("test-secret-with-at-least-32-bytes-long", Duration.ofMinutes(30), Duration.ofDays(7));
        authService = new AuthServiceImpl(userAccountMapper, roleMapper, permissionMapper, refreshTokenMapper, userRoleMapper,
                userProfileMapper, userPreferenceMapper, userDeviceMapper, auditLogMapper, passwordEncoder,
                new JwtTokenService(jwtProperties), jwtProperties);
    }

    @Test
    void registerCreatesAccountProfilePreferenceAndDefaultRole() {
        Role role = new Role();
        role.setId(2L);
        when(userAccountMapper.selectByLoginIdentifier(anyString())).thenReturn(null);
        when(roleMapper.selectActiveByCode("USER")).thenReturn(role);
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserAccount.class).setId(101L);
            return 1;
        });

        RegisteredUserResponse response = authService.register(new RegisterRequest("alice_1", null, null, "Password123!"));

        assertEquals(101L, response.userId());
        assertEquals("alice_1", response.username());
        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountMapper).insert(accountCaptor.capture());
        assertFalse(accountCaptor.getValue().getPasswordHash().contains("Password123!"));
        verify(userRoleMapper).insert(any(UserRole.class));
        verify(userProfileMapper).insert(any(UserProfile.class));
        verify(userPreferenceMapper).insert(any(UserPreference.class));
        verify(auditLogMapper).insert(any(AuditLog.class));
    }

    @Test
    void passwordLoginCreatesDeviceAndSignsAccessToken() {
        UserAccount user = new UserAccount();
        user.setId(101L);
        user.setUsername("alice_1");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setStatus("ACTIVE");
        when(userAccountMapper.selectByLoginIdentifier("alice_1")).thenReturn(user);
        when(roleMapper.selectCodesByUserId(101L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectCodesByUserId(101L)).thenReturn(List.of());
        when(userDeviceMapper.selectByUserAndDeviceId(101L, "browser-1")).thenReturn(null);
        when(userDeviceMapper.insert(any(UserDevice.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserDevice.class).setId(201L);
            return 1;
        });

        LoginResponse response = authService.loginWithPassword(
                new PasswordLoginRequest("alice_1", "Password123!", "browser-1", DeviceType.WEB, null, null),
                "127.0.0.1");

        assertEquals(101L, response.userId());
        assertEquals("alice_1", response.username());
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(1800L, response.expiresInSeconds());
        assertEquals(List.of("USER"), response.roles().stream().toList());
        verify(userDeviceMapper).insert(any(UserDevice.class));
        verify(userAccountMapper).markLoginSucceeded(101L);
        verify(auditLogMapper).insert(any(AuditLog.class));
    }

    @Test
    void adminWebLoginIsRejectedForCustomerService() {
        UserAccount user = new UserAccount();
        user.setId(101L);
        user.setUsername("cs_one");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setStatus("ACTIVE");
        when(userAccountMapper.selectByLoginIdentifier("cs_one")).thenReturn(user);
        when(roleMapper.selectCodesByUserId(101L)).thenReturn(List.of("CUSTOMER_SERVICE"));

        assertThrows(BusinessException.class, () -> authService.loginWithPassword(
                new PasswordLoginRequest("cs_one", "Password123!", "admin-1", DeviceType.ADMIN_WEB, null, null),
                "127.0.0.1"));
        verify(userAccountMapper, never()).markLoginSucceeded(101L);
    }

    @Test
    void refreshRotatesRefreshTokenAndSignsNewAccessToken() {
        UserAccount user = new UserAccount();
        user.setId(101L);
        user.setUsername("alice_1");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setStatus("ACTIVE");
        when(userAccountMapper.selectByLoginIdentifier("alice_1")).thenReturn(user);
        when(userAccountMapper.selectById(101L)).thenReturn(user);
        when(roleMapper.selectCodesByUserId(101L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectCodesByUserId(101L)).thenReturn(List.of());
        when(userDeviceMapper.selectByUserAndDeviceId(101L, "browser-1")).thenReturn(null);
        when(userDeviceMapper.insert(any(UserDevice.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, UserDevice.class).setId(201L);
            return 1;
        });
        UserDevice device = new UserDevice();
        device.setId(201L);
        device.setUserId(101L);
        device.setStatus("ACTIVE");
        when(userDeviceMapper.selectById(201L)).thenReturn(device);

        LoginResponse firstLogin = authService.loginWithPassword(
                new PasswordLoginRequest("alice_1", "Password123!", "browser-1", DeviceType.WEB, null, null),
                "127.0.0.1");
        ArgumentCaptor<RefreshToken> refreshCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(refreshCaptor.capture());
        when(refreshTokenMapper.selectByTokenId(refreshCaptor.getValue().getTokenId())).thenReturn(refreshCaptor.getValue());
        when(refreshTokenMapper.revokeIfActive(anyString(), anyString(), anyString())).thenReturn(1);

        LoginResponse refreshed = authService.refresh(new com.chareslm.shopping.auth.dto.request.RefreshTokenRequest(firstLogin.refreshToken()));

        assertNotNull(refreshed.accessToken());
        assertNotNull(refreshed.refreshToken());
        assertFalse(firstLogin.refreshToken().equals(refreshed.refreshToken()));
        verify(refreshTokenMapper).revokeIfActive(anyString(), anyString(), anyString());
    }

    @Test
    void changePasswordReplacesHashAndRevokesEveryRefreshToken() {
        UserAccount user = new UserAccount();
        user.setId(101L);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        when(userAccountMapper.selectById(101L)).thenReturn(user);

        authService.changePassword(101L, new ChangePasswordRequest("Password123!", "NewPassword456!"));

        ArgumentCaptor<String> passwordHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userAccountMapper).updatePasswordHash(eq(101L), passwordHashCaptor.capture());
        assertTrue(passwordEncoder.matches("NewPassword456!", passwordHashCaptor.getValue()));
        verify(refreshTokenMapper).revokeActiveByUserId(101L, "PASSWORD_CHANGED");
        verify(auditLogMapper).insert(any(AuditLog.class));
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordWithoutUpdatingAccount() {
        UserAccount user = new UserAccount();
        user.setId(101L);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        when(userAccountMapper.selectById(101L)).thenReturn(user);

        assertThrows(BusinessException.class,
                () -> authService.changePassword(101L,
                        new ChangePasswordRequest("WrongPassword1!", "NewPassword456!")));

        verify(userAccountMapper, never()).updatePasswordHash(anyLong(), anyString());
        verify(refreshTokenMapper, never()).revokeActiveByUserId(anyLong(), anyString());
        verify(auditLogMapper).insert(any(AuditLog.class));
    }
}
