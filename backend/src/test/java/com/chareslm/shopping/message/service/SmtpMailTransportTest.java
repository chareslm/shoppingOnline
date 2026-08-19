package com.chareslm.shopping.message.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpMailTransportTest {
    @Test
    void port465UsesImplicitSslAndDisablesStartTls() {
        var smtp = new SmtpRuntimeSettings.ResolvedSmtp(
                "smtp.163.com", 465, "name@163.com", "auth-code", "other@163.com", true, true, true, true);

        var properties = SmtpMailTransport.mailProperties(smtp);

        assertEquals("true", properties.get("mail.smtps.ssl.enable"));
        assertEquals("false", properties.get("mail.smtp.starttls.enable"));
        assertEquals("smtps", SmtpMailTransport.mailProtocol(465));
        assertEquals(null, properties.get("mail.smtp.socketFactory.class"));
        assertEquals("name@163.com", SmtpMailTransport.envelopeFrom(smtp));
    }

    @Test
    void port587KeepsStartTls() {
        var smtp = new SmtpRuntimeSettings.ResolvedSmtp(
                "smtp.example.com", 587, "mailer@example.com", "secret", "mailer@example.com", true, true, true, true);

        var properties = SmtpMailTransport.mailProperties(smtp);

        assertEquals("false", properties.get("mail.smtp.ssl.enable"));
        assertEquals("true", properties.get("mail.smtp.starttls.enable"));
        assertTrue(SmtpMailTransport.implicitSsl(465));
        assertFalse(SmtpMailTransport.implicitSsl(587));
    }

    @Test
    void completesNeteaseLocalPart() {
        assertEquals("name@163.com", SmtpMailTransport.normalizeUsername("smtp.163.com", "name"));
        assertEquals("name@163.com", SmtpMailTransport.normalizeUsername("smtp.163.com", "name@163.com"));
    }

    @Test
    void neteasePrefersPort994Over465() {
        var smtp = new SmtpRuntimeSettings.ResolvedSmtp(
                "smtp.163.com", 465, "name@163.com", "auth-code", "name@163.com", true, false, true, true);

        var profiles = SmtpMailTransport.deliveryProfiles(smtp);

        assertEquals(994, profiles.getFirst().port());
        assertEquals(465, profiles.get(1).port());
        assertEquals("smtps", SmtpMailTransport.mailProtocol(994));
    }

    @Test
    void explainFailureDoesNotExposeProviderInternals() {
        assertEquals("无法与邮件服务器建立安全连接，请检查端口和加密方式。",
                SmtpMailTransport.explainFailure(new IllegalStateException("SSL peer shut down incorrectly on Docker 465")));
        assertEquals("SMTP 认证失败，请核对账号和密码。",
                SmtpMailTransport.explainFailure(new IllegalStateException("javax.mail.AuthenticationFailedException: 535 163.com")));
        assertEquals("邮件发送失败，请检查 SMTP 配置。",
                SmtpMailTransport.explainFailure(new IllegalStateException("unknown vendor stack at SmtpMailTransport.java:54")));
    }
}
