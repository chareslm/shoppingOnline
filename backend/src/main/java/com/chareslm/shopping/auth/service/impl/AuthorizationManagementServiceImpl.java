package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.dto.request.CreateAdminUserRequest;
import com.chareslm.shopping.auth.dto.response.CreatedAdminUserResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;
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
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthorizationManagementServiceImpl implements AuthorizationManagementService {
    private static final Set<String> PLATFORM_ROLE_CODES = Set.of("USER", "ADMIN", "SUPER_ADMIN");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SPECIAL = "!@#$%^&*".toCharArray();

    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TransactionTemplate transactionTemplate;

    public AuthorizationManagementServiceImpl(UserAccountMapper userAccountMapper, RoleMapper roleMapper,
                                              UserRoleMapper userRoleMapper, RefreshTokenMapper refreshTokenMapper,
                                              UserProfileMapper userProfileMapper, UserPreferenceMapper userPreferenceMapper,
                                              AuditService auditService, PasswordEncoder passwordEncoder,
                                              MailService mailService, TransactionTemplate transactionTemplate) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userProfileMapper = userProfileMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.transactionTemplate = transactionTemplate;
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
            writeRoleAudit(operatorUserId, targetUserId, false);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        List<Role> roles = requirePlatformRoles(request.roleIds());
        Set<String> operatorRoles = Set.copyOf(roleMapper.selectCodesByUserId(operatorUserId));
        if (!operatorRoles.contains("SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Set<String> targetRoles = Set.copyOf(roleMapper.selectCodesByUserId(targetUserId));
        boolean assignsSuperAdmin = roles.stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()));
        boolean removesSuperAdmin = targetRoles.contains("SUPER_ADMIN") && !assignsSuperAdmin;
        if (removesSuperAdmin && roleMapper.countUsersByRoleCode("SUPER_ADMIN") <= 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        userRoleMapper.deletePlatformRolesByUserId(targetUserId);
        grantPlatformRoles(targetUserId, roles, operatorUserId);
        refreshTokenMapper.revokeActiveByUserId(targetUserId, "ROLE_CHANGED");
        writeRoleAudit(operatorUserId, targetUserId, true);
    }

    @Override
    public CreatedAdminUserResponse createUser(Long operatorUserId, CreateAdminUserRequest request) {
        requireOperator(operatorUserId, request.currentPassword(), "USER_CREATE", null);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = trimToNull(request.username());
        String phone = trimToNull(request.phone());
        if (identifierTaken(username, email, phone)) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        List<Role> roles = requirePlatformRoles(request.roleIds());

        String temporaryPassword = generatePassword();
        UserAccount created = transactionTemplate.execute(status -> persistCreatedUser(
                operatorUserId, username, email, phone, temporaryPassword, roles));
        if (created == null) {
            throw new IllegalStateException("Failed to persist the new account");
        }
        String mailStatus = deliverCredential(created.getEmail(), loginHint(created), temporaryPassword);
        auditService.record(operatorUserId, "AUTHORIZATION", "USER_CREATE", "USER", created.getId().toString(), true);
        return toCreatedResponse(created, roles, mailStatus);
    }

    @Override
    public CreatedAdminUserResponse retryCredentialEmail(Long operatorUserId, Long targetUserId) {
        UserAccount operator = userAccountMapper.selectById(operatorUserId);
        UserAccount target = userAccountMapper.selectById(targetUserId);
        if (operator == null || target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!roleMapper.selectCodesByUserId(operatorUserId).contains("SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (target.getEmail() == null || target.getEmail().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (!Boolean.TRUE.equals(target.getMustChangePassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String temporaryPassword = generatePassword();
        userAccountMapper.updateTemporaryPassword(target.getId(), passwordEncoder.encode(temporaryPassword));
        refreshTokenMapper.revokeActiveByUserId(target.getId(), "TEMPORARY_PASSWORD_ROTATED");
        String mailStatus = deliverCredential(target.getEmail(), loginHint(target), temporaryPassword);
        auditService.record(operatorUserId, "AUTHORIZATION", "USER_CREDENTIAL_EMAIL_RETRY", "USER",
                targetUserId.toString(), true);
        return toCreatedResponse(target, roleMapper.selectByUserId(target.getId()), mailStatus);
    }

    private UserAccount persistCreatedUser(Long operatorUserId, String username, String email, String phone,
                                           String temporaryPassword, List<Role> roles) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setStatus("ACTIVE");
        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(username != null ? username : email);
        userProfileMapper.insert(profile);
        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        userPreferenceMapper.insert(preference);
        grantPlatformRoles(user.getId(), roles, operatorUserId);
        return user;
    }

    private UserAccount requireOperator(Long operatorUserId, String currentPassword, String actionCode, Long targetId) {
        UserAccount operator = userAccountMapper.selectById(operatorUserId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(currentPassword, operator.getPasswordHash())) {
            auditService.record(operatorUserId, "AUTHORIZATION", actionCode, "USER",
                    targetId == null ? "" : targetId.toString(), false);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!roleMapper.selectCodesByUserId(operatorUserId).contains("SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return operator;
    }

    private List<Role> requirePlatformRoles(Set<Long> roleIds) {
        List<Role> roles = roleMapper.selectByIds(roleIds);
        if (roles.size() != roleIds.size() || roles.stream().anyMatch(role -> !"ACTIVE".equals(role.getStatus()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (roles.stream().anyMatch(role -> !PLATFORM_ROLE_CODES.contains(role.getCode()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return roles;
    }

    private void grantPlatformRoles(Long userId, List<Role> roles, Long operatorUserId) {
        for (Role role : roles) {
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            relation.setGrantedBy(operatorUserId);
            userRoleMapper.insert(relation);
        }
    }

    private boolean identifierTaken(String username, String email, String phone) {
        return userAccountMapper.selectByLoginIdentifier(email) != null
                || (username != null && userAccountMapper.selectByLoginIdentifier(username) != null)
                || (phone != null && userAccountMapper.selectByLoginIdentifier(phone) != null);
    }

    private String deliverCredential(String email, String loginHint, String temporaryPassword) {
        try {
            mailService.sendAccountCredential(email, loginHint, temporaryPassword);
            return "SENT";
        } catch (RuntimeException exception) {
            return "MAIL_FAILED";
        }
    }

    private CreatedAdminUserResponse toCreatedResponse(UserAccount user, List<Role> roles, String mailStatus) {
        List<RoleResponse> roleResponses = roles.stream()
                .map(role -> new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDataScope(),
                        Boolean.TRUE.equals(role.getBuiltIn())))
                .toList();
        return new CreatedAdminUserResponse(user.getId(), user.getUsername(), maskEmail(user.getEmail()),
                user.getStatus(), true, mailStatus, roleResponses);
    }

    private static String loginHint(UserAccount user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private static String generatePassword() {
        List<Character> chars = new ArrayList<>();
        chars.add(random(LOWER));
        chars.add(random(UPPER));
        chars.add(random(DIGITS));
        chars.add(random(SPECIAL));
        char[] all = (new String(LOWER) + new String(UPPER) + new String(DIGITS) + new String(SPECIAL)).toCharArray();
        while (chars.size() < 20) {
            chars.add(random(all));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder result = new StringBuilder(chars.size());
        chars.forEach(result::append);
        return result.toString();
    }

    private static char random(char[] source) {
        return source[RANDOM.nextInt(source.length)];
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int separator = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(separator);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void writeRoleAudit(Long operatorUserId, Long targetUserId, boolean success) {
        auditService.record(operatorUserId, "AUTHORIZATION", "USER_ROLE_REPLACE", "USER",
                targetUserId.toString(), success);
    }
}
