package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.request.PasswordLoginRequest;
import com.chareslm.shopping.auth.dto.request.RefreshTokenRequest;
import com.chareslm.shopping.auth.dto.request.RegisterRequest;
import com.chareslm.shopping.auth.dto.response.LoginResponse;
import com.chareslm.shopping.auth.dto.response.RegisteredUserResponse;
import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.RefreshToken;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserDevice;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.auth.mapper.PermissionMapper;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserDeviceMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.auth.service.AuthService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.security.context.LoginUser;
import com.chareslm.shopping.security.jwt.JwtProperties;
import com.chareslm.shopping.security.jwt.JwtTokenService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserDeviceMapper userDeviceMapper;
    private final AuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(UserAccountMapper userAccountMapper, RoleMapper roleMapper,
                           PermissionMapper permissionMapper, RefreshTokenMapper refreshTokenMapper,
                           UserRoleMapper userRoleMapper,
                           UserProfileMapper userProfileMapper, UserPreferenceMapper userPreferenceMapper,
                           UserDeviceMapper userDeviceMapper, AuditLogMapper auditLogMapper,
                           PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                           JwtProperties jwtProperties) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userRoleMapper = userRoleMapper;
        this.userProfileMapper = userProfileMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.userDeviceMapper = userDeviceMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public RegisteredUserResponse register(RegisterRequest request) {
        String username = trimToNull(request.username());
        String email = normalizeEmail(request.email());
        String phone = trimToNull(request.phone());
        validatePassword(request.password());

        if (userAccountMapper.selectByLoginIdentifier(firstIdentifier(username, email, phone)) != null
                || (username != null && userAccountMapper.selectByLoginIdentifier(username) != null)
                || (email != null && userAccountMapper.selectByLoginIdentifier(email) != null)
                || (phone != null && userAccountMapper.selectByLoginIdentifier(phone) != null)) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");

        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        Role userRole = roleMapper.selectActiveByCode("USER");
        if (userRole == null) {
            throw new IllegalStateException("The USER role is missing; apply V1__identity_and_user.sql first");
        }
        UserRole relation = new UserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        userRoleMapper.insert(relation);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(username != null ? username : firstIdentifier(email, phone, null));
        userProfileMapper.insert(profile);

        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        userPreferenceMapper.insert(preference);
        writeAudit(user.getId(), "REGISTER", true);

        return new RegisteredUserResponse(user.getId(), user.getUsername(), user.getStatus());
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse loginWithPassword(PasswordLoginRequest request, String clientIp) {
        String identifier = trimToNull(request.identifier());
        UserAccount user = identifier == null ? null : userAccountMapper.selectByLoginIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user != null) {
                userAccountMapper.incrementFailedLoginCount(user.getId());
                writeAudit(user.getId(), "PASSWORD_LOGIN", false);
            } else {
                writeAudit(null, "PASSWORD_LOGIN", false);
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            writeAudit(user.getId(), "PASSWORD_LOGIN", false);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
            writeAudit(user.getId(), "PASSWORD_LOGIN", false);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        Long deviceId = upsertDevice(user.getId(), request, clientIp);
        userAccountMapper.markLoginSucceeded(user.getId());
        Set<String> roles = Set.copyOf(roleMapper.selectCodesByUserId(user.getId()));
        Set<String> permissions = Set.copyOf(permissionMapper.selectCodesByUserId(user.getId()));
        LoginResponse response = issueTokens(user, deviceId, roles, permissions);
        writeAudit(user.getId(), "PASSWORD_LOGIN", true);
        return response;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse refresh(RefreshTokenRequest request) {
        JwtTokenService.RefreshTokenClaims claims;
        try {
            claims = jwtTokenService.parseRefreshToken(request.refreshToken());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        RefreshToken storedToken = refreshTokenMapper.selectByTokenId(claims.tokenId());
        if (storedToken == null || !storedToken.getUserId().equals(claims.userId())
                || !storedToken.getDeviceId().equals(claims.deviceId())
                || storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(LocalDateTime.now())
                || !MessageDigest.isEqual(tokenHash(request.refreshToken()).getBytes(StandardCharsets.UTF_8),
                storedToken.getTokenHash().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        UserAccount user = userAccountMapper.selectById(claims.userId());
        UserDevice device = userDeviceMapper.selectById(claims.deviceId());
        if (user == null || device == null || !user.getId().equals(device.getUserId()) || !"ACTIVE".equals(user.getStatus())
                || !"ACTIVE".equals(device.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        Set<String> roles = Set.copyOf(roleMapper.selectCodesByUserId(user.getId()));
        Set<String> permissions = Set.copyOf(permissionMapper.selectCodesByUserId(user.getId()));
        JwtTokenService.IssuedRefreshToken next = jwtTokenService.createRefreshToken(user.getId(), device.getId());
        if (refreshTokenMapper.revokeIfActive(claims.tokenId(), "ROTATED", next.tokenId()) != 1) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        saveRefreshToken(user.getId(), device.getId(), next);
        writeAudit(user.getId(), "TOKEN_REFRESH", true);
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), roles, permissions);
        return new LoginResponse(user.getId(), user.getUsername(), jwtTokenService.createAccessToken(loginUser),
                next.token(), jwtProperties.accessTokenTtl().toSeconds(), roles, permissions);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void logout(Long userId, String deviceId) {
        UserDevice device = userDeviceMapper.selectByUserAndDeviceId(userId, deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        refreshTokenMapper.revokeActiveByUserAndDevice(userId, device.getId(), "LOGOUT");
        writeAudit(userId, "LOGOUT", true);
    }

    private Long upsertDevice(Long userId, PasswordLoginRequest request, String clientIp) {
        UserDevice existing = userDeviceMapper.selectByUserAndDeviceId(userId, request.deviceId());
        if (existing != null) {
            userDeviceMapper.markActive(existing.getId(), request.deviceType().name(), request.deviceName(),
                    request.appVersion(), clientIp);
            return existing.getId();
        }
        UserDevice device = new UserDevice();
        device.setUserId(userId);
        device.setDeviceId(request.deviceId());
        device.setDeviceType(request.deviceType().name());
        device.setDeviceName(trimToNull(request.deviceName()));
        device.setAppVersion(trimToNull(request.appVersion()));
        device.setLastIp(clientIp);
        device.setStatus("ACTIVE");
        userDeviceMapper.insert(device);
        return device.getId();
    }

    private LoginResponse issueTokens(UserAccount user, Long deviceId, Set<String> roles, Set<String> permissions) {
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), roles, permissions);
        JwtTokenService.IssuedRefreshToken refreshToken = jwtTokenService.createRefreshToken(user.getId(), deviceId);
        saveRefreshToken(user.getId(), deviceId, refreshToken);
        return new LoginResponse(user.getId(), user.getUsername(), jwtTokenService.createAccessToken(loginUser),
                refreshToken.token(), jwtProperties.accessTokenTtl().toSeconds(), roles, permissions);
    }

    private void saveRefreshToken(Long userId, Long deviceId, JwtTokenService.IssuedRefreshToken issuedToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setDeviceId(deviceId);
        refreshToken.setTokenId(issuedToken.tokenId());
        refreshToken.setTokenHash(tokenHash(issuedToken.token()));
        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.ofInstant(issuedToken.expiresAt(), ZoneId.systemDefault()));
        refreshTokenMapper.insert(refreshToken);
    }

    private void writeAudit(Long actorUserId, String actionCode, boolean success) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setModule("AUTH");
        log.setActionCode(actionCode);
        log.setTargetType("USER");
        log.setTargetId(actorUserId == null ? null : actorUserId.toString());
        log.setSuccess(success);
        auditLogMapper.insert(log);
    }

    private static void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static String tokenHash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstIdentifier(String first, String second, String third) {
        return first != null ? first : second != null ? second : third;
    }
}
