package com.chareslm.shopping.message.service.impl;

import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.dto.request.UpdateSmtpSettingRequest;
import com.chareslm.shopping.message.dto.response.SmtpSettingResponse;
import com.chareslm.shopping.message.entity.SmtpSetting;
import com.chareslm.shopping.message.mapper.SmtpSettingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpSettingServiceImplTest {
    private SmtpSettingMapper smtpSettingMapper;
    private UserAccountMapper userAccountMapper;
    private AuditService auditService;
    private BCryptPasswordEncoder passwordEncoder;
    private SmtpSettingServiceImpl service;

    @BeforeEach
    void setUp() {
        smtpSettingMapper = mock(SmtpSettingMapper.class);
        userAccountMapper = mock(UserAccountMapper.class);
        auditService = mock(AuditService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new SmtpSettingServiceImpl(smtpSettingMapper, userAccountMapper, auditService, passwordEncoder,
                "smtp.example.com", 587, "noreply@example.com", "env-secret", "noreply@example.com", true, true);
    }

    @Test
    void fallsBackToEnvironmentWhenDatabaseHostIsEmpty() {
        SmtpSetting stored = new SmtpSetting();
        stored.setId(1);
        stored.setPort(465);
        when(smtpSettingMapper.selectById(1)).thenReturn(stored);

        SmtpSettingResponse response = service.current();

        assertEquals("smtp.example.com", response.host());
        assertTrue(response.usingEnvironmentFallback());
        assertTrue(response.passwordConfigured());
        assertTrue(service.settings().ready());
    }

    @Test
    void databaseHostTakesPrecedenceOverEnvironment() {
        SmtpSetting stored = new SmtpSetting();
        stored.setId(1);
        stored.setHost("smtp.admin.local");
        stored.setPort(465);
        stored.setUsername("admin-mail");
        stored.setPassword("db-secret");
        stored.setFromAddress("ops@admin.local");
        stored.setSmtpAuth(true);
        stored.setStarttlsEnabled(false);
        when(smtpSettingMapper.selectById(1)).thenReturn(stored);

        SmtpSettingResponse response = service.current();

        assertEquals("smtp.admin.local", response.host());
        assertEquals(465, response.port());
        assertEquals("ops@admin.local", response.fromAddress());
        assertFalse(response.usingEnvironmentFallback());
        assertTrue(response.passwordConfigured());
        assertEquals("db-secret", service.settings().password());
    }

    @Test
    void updateKeepsExistingPasswordWhenTheRequestOmitsIt() {
        UserAccount operator = new UserAccount();
        operator.setId(1L);
        operator.setPasswordHash(passwordEncoder.encode("Password123!"));
        SmtpSetting stored = new SmtpSetting();
        stored.setId(1);
        stored.setPassword("keep-me");
        when(userAccountMapper.selectById(1L)).thenReturn(operator);
        when(smtpSettingMapper.selectById(1)).thenReturn(stored);

        service.update(1L, new UpdateSmtpSettingRequest("smtp.saved.local", 587, "mailer", "  ",
                "Shop Notifications", true, true, true, "Password123!"));

        assertEquals("keep-me", stored.getPassword());
        assertEquals("smtp.saved.local", stored.getHost());
        assertEquals("Shop Notifications", stored.getFromAddress());
        verify(smtpSettingMapper).updateById(any(SmtpSetting.class));
        verify(auditService).record(1L, "SYSTEM", "SMTP_SETTING_UPDATE", "SMTP_SETTING", "1", true);
    }

    @Test
    void updateDisablesStartTlsForImplicitSslPorts() {
        UserAccount operator = new UserAccount();
        operator.setId(1L);
        operator.setPasswordHash(passwordEncoder.encode("Password123!"));
        SmtpSetting stored = new SmtpSetting();
        stored.setId(1);
        when(userAccountMapper.selectById(1L)).thenReturn(operator);
        when(smtpSettingMapper.selectById(1)).thenReturn(stored);

        service.update(1L, new UpdateSmtpSettingRequest("smtp.163.com", 465, "name@163.com", "auth-code",
                null, true, true, true, "Password123!"));

        assertEquals(465, stored.getPort());
        assertEquals("name@163.com", stored.getFromAddress());
        assertFalse(stored.getStarttlsEnabled());
    }

    @Test
    void updateRejectsAnIncorrectOperatorPassword() {
        UserAccount operator = new UserAccount();
        operator.setId(1L);
        operator.setPasswordHash(passwordEncoder.encode("Password123!"));
        when(userAccountMapper.selectById(1L)).thenReturn(operator);

        assertThrows(BusinessException.class, () -> service.update(1L,
                new UpdateSmtpSettingRequest("smtp.saved.local", 587, null, null, null, true, true, true, "wrong")));

        verify(auditService).record(1L, "SYSTEM", "SMTP_SETTING_UPDATE", "SMTP_SETTING", "1", false);
    }

    @Test
    void disabledSmtpDoesNotFallBackToEnvironment() {
        SmtpSetting stored = new SmtpSetting();
        stored.setId(1);
        stored.setEnabled(false);
        stored.setHost("smtp.admin.local");
        stored.setPort(465);
        stored.setFromAddress("ops@admin.local");
        when(smtpSettingMapper.selectById(1)).thenReturn(stored);

        SmtpSettingResponse response = service.current();

        assertFalse(response.enabled());
        assertFalse(response.usingEnvironmentFallback());
        assertFalse(service.settings().ready());
        assertFalse(service.settings().enabled());
    }
}
