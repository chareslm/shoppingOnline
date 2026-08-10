package com.chareslm.shopping.auth.config;

import com.chareslm.shopping.auth.entity.AuditLog;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.mapper.AuditLogMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "security.bootstrap-super-admin", name = "enabled", havingValue = "true")
public class SuperAdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);

    private final SuperAdminBootstrapProperties properties;
    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final AuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminBootstrapRunner(SuperAdminBootstrapProperties properties, UserAccountMapper userAccountMapper,
                                     RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                                     UserProfileMapper userProfileMapper, UserPreferenceMapper userPreferenceMapper,
                                     AuditLogMapper auditLogMapper, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userProfileMapper = userProfileMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleMapper.countUsersByRoleCode("SUPER_ADMIN") > 0) {
            log.info("Super-admin bootstrap skipped because a super administrator already exists");
            return;
        }
        validateConfiguration();
        if (userAccountMapper.selectByLoginIdentifier(properties.username()) != null) {
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN_USERNAME already belongs to an existing account");
        }
        Role superAdminRole = roleMapper.selectActiveByCode("SUPER_ADMIN");
        if (superAdminRole == null) {
            throw new IllegalStateException("The SUPER_ADMIN role is missing; apply database migrations first");
        }

        UserAccount account = new UserAccount();
        account.setUsername(properties.username());
        account.setPasswordHash(passwordEncoder.encode(properties.password()));
        account.setStatus("ACTIVE");
        userAccountMapper.insert(account);

        UserRole relation = new UserRole();
        relation.setUserId(account.getId());
        relation.setRoleId(superAdminRole.getId());
        userRoleMapper.insert(relation);

        UserProfile profile = new UserProfile();
        profile.setUserId(account.getId());
        profile.setNickname(properties.username());
        userProfileMapper.insert(profile);
        UserPreference preference = new UserPreference();
        preference.setUserId(account.getId());
        userPreferenceMapper.insert(preference);

        AuditLog auditLog = new AuditLog();
        auditLog.setModule("AUTHORIZATION");
        auditLog.setActionCode("BOOTSTRAP_SUPER_ADMIN");
        auditLog.setTargetType("USER");
        auditLog.setTargetId(account.getId().toString());
        auditLog.setSuccess(true);
        auditLogMapper.insert(auditLog);
        log.warn("A super administrator was created by the explicitly enabled bootstrap configuration; disable it now");
    }

    private void validateConfiguration() {
        if (properties.username() == null || !properties.username().matches("^[A-Za-z][A-Za-z0-9_]{2,63}$")) {
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN_USERNAME must be a valid username");
        }
        if (properties.password() == null || properties.password().length() < 12
                || properties.password().length() > 64
                || properties.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !properties.password().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$")) {
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN_PASSWORD must satisfy the strong password policy");
        }
    }
}
