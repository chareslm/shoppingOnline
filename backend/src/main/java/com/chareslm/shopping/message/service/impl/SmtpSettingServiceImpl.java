package com.chareslm.shopping.message.service.impl;

import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.dto.request.UpdateSmtpSettingRequest;
import com.chareslm.shopping.message.dto.response.SmtpSettingResponse;
import com.chareslm.shopping.message.entity.SmtpSetting;
import com.chareslm.shopping.message.mapper.SmtpSettingMapper;
import com.chareslm.shopping.message.service.SmtpMailTransport;
import com.chareslm.shopping.message.service.SmtpRuntimeSettings;
import com.chareslm.shopping.message.service.SmtpSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class SmtpSettingServiceImpl implements SmtpSettingService, SmtpRuntimeSettings {
    private static final int SETTING_ID = 1;

    private final SmtpSettingMapper smtpSettingMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final String envHost;
    private final int envPort;
    private final String envUsername;
    private final String envPassword;
    private final String envFrom;
    private final boolean envAuth;
    private final boolean envStarttls;

    public SmtpSettingServiceImpl(
            SmtpSettingMapper smtpSettingMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            @Value("${spring.mail.host:}") String envHost,
            @Value("${spring.mail.port:587}") int envPort,
            @Value("${spring.mail.username:}") String envUsername,
            @Value("${spring.mail.password:}") String envPassword,
            @Value("${app.mail.from:}") String envFrom,
            @Value("${MAIL_SMTP_AUTH:true}") boolean envAuth,
            @Value("${MAIL_SMTP_STARTTLS_ENABLED:true}") boolean envStarttls) {
        this.smtpSettingMapper = smtpSettingMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.envHost = envHost;
        this.envPort = envPort;
        this.envUsername = envUsername;
        this.envPassword = envPassword;
        this.envFrom = envFrom;
        this.envAuth = envAuth;
        this.envStarttls = envStarttls;
    }

    @Override
    public SmtpSettingResponse current() {
        ResolvedSmtp resolved = settings();
        boolean usingFallback = resolved.enabled() && !resolved.fromDatabase() && hasText(resolved.host());
        SmtpSetting stored = smtpSettingMapper.selectById(SETTING_ID);
        boolean editingStored = stored != null && hasText(stored.getHost());
        return new SmtpSettingResponse(
                mailEnabled(stored),
                blankToNull(editingStored ? stored.getHost() : resolved.host()),
                editingStored && stored.getPort() != null ? stored.getPort() : resolved.port(),
                blankToNull(editingStored ? stored.getUsername() : resolved.username()),
                blankToNull(editingStored ? stored.getFromAddress() : resolved.fromAddress()),
                editingStored
                        ? stored.getSmtpAuth() == null || stored.getSmtpAuth()
                        : resolved.smtpAuth(),
                editingStored
                        ? stored.getStarttlsEnabled() == null || stored.getStarttlsEnabled()
                        : resolved.starttlsEnabled(),
                hasText(editingStored ? stored.getPassword() : resolved.password()),
                usingFallback);
    }

    @Override
    @Transactional
    public SmtpSettingResponse update(Long operatorUserId, UpdateSmtpSettingRequest request) {
        UserAccount operator = userAccountMapper.selectById(operatorUserId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.currentPassword(), operator.getPasswordHash())) {
            auditService.record(operatorUserId, "SYSTEM", "SMTP_SETTING_UPDATE", "SMTP_SETTING", "1", false);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        SmtpSetting stored = requireRow();
        stored.setHost(trimToNull(request.host()));
        int port = request.port() == null ? 587 : request.port();
        stored.setPort(port);
        stored.setUsername(trimToNull(request.username()));
        if (hasText(request.password())) {
            stored.setPassword(request.password().trim());
        }
        String fromAddress = trimToNull(request.fromAddress());
        if (fromAddress == null) {
            fromAddress = trimToNull(request.username());
        }
        stored.setFromAddress(fromAddress);
        stored.setSmtpAuth(request.smtpAuth() == null || request.smtpAuth());
        stored.setStarttlsEnabled(SmtpMailTransport.implicitSsl(port)
                ? Boolean.FALSE
                : request.starttlsEnabled() == null || request.starttlsEnabled());
        stored.setEnabled(request.enabled() == null || request.enabled());
        stored.setUpdatedBy(operatorUserId);
        smtpSettingMapper.updateById(stored);
        auditService.record(operatorUserId, "SYSTEM", "SMTP_SETTING_UPDATE", "SMTP_SETTING", "1", true);
        return current();
    }

    @Override
    public void requireOperatorPassword(Long operatorUserId, String currentPassword) {
        UserAccount operator = userAccountMapper.selectById(operatorUserId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(currentPassword, operator.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    public ResolvedSmtp settings() {
        SmtpSetting stored = smtpSettingMapper.selectById(SETTING_ID);
        boolean mailEnabled = mailEnabled(stored);
        if (!mailEnabled) {
            return new ResolvedSmtp(
                    stored != null ? nullToEmpty(stored.getHost()) : "",
                    stored != null && stored.getPort() != null ? stored.getPort() : 587,
                    stored != null ? nullToEmpty(stored.getUsername()) : "",
                    stored != null ? nullToEmpty(stored.getPassword()) : "",
                    stored != null ? firstNonBlank(stored.getFromAddress(), stored.getUsername()) : "",
                    stored == null || stored.getSmtpAuth() == null || stored.getSmtpAuth(),
                    stored == null || stored.getStarttlsEnabled() == null || stored.getStarttlsEnabled(),
                    stored != null && hasText(stored.getHost()),
                    false);
        }
        if (stored != null && hasText(stored.getHost())) {
            return new ResolvedSmtp(
                    stored.getHost().trim(),
                    stored.getPort() == null ? 587 : stored.getPort(),
                    nullToEmpty(stored.getUsername()),
                    nullToEmpty(stored.getPassword()),
                    firstNonBlank(stored.getFromAddress(), stored.getUsername(), envFrom),
                    stored.getSmtpAuth() == null || stored.getSmtpAuth(),
                    stored.getStarttlsEnabled() == null || stored.getStarttlsEnabled(),
                    true,
                    true);
        }
        return new ResolvedSmtp(
                nullToEmpty(envHost),
                envPort <= 0 ? 587 : envPort,
                nullToEmpty(envUsername),
                nullToEmpty(envPassword),
                nullToEmpty(envFrom),
                envAuth,
                envStarttls,
                false,
                true);
    }

    private static boolean mailEnabled(SmtpSetting stored) {
        return stored == null || stored.getEnabled() == null || stored.getEnabled();
    }

    private SmtpSetting requireRow() {
        SmtpSetting stored = smtpSettingMapper.selectById(SETTING_ID);
        if (stored == null) {
            stored = new SmtpSetting();
            stored.setId(SETTING_ID);
            stored.setPort(587);
            stored.setSmtpAuth(true);
            stored.setStarttlsEnabled(true);
            stored.setEnabled(true);
            smtpSettingMapper.insert(stored);
        }
        return stored;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value : null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
