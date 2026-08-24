package com.chareslm.shopping.message.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 基于当前有效 SMTP 配置的同步邮件实现。
 *
 * <p>系统管理员在管理端保存的配置优先于环境变量。该类不吞掉配置或投递异常；
 * 调用方据此记录失败状态并提供业务重试。</p>
 */
@Slf4j
@Service
public class SmtpMailService implements MailService {
    private final SmtpRuntimeSettings smtpRuntimeSettings;

    public SmtpMailService(SmtpRuntimeSettings smtpRuntimeSettings) {
        this.smtpRuntimeSettings = smtpRuntimeSettings;
    }

    @Override
    public boolean isEnabled() {
        return smtpRuntimeSettings.settings().enabled();
    }

    @Override
    public boolean isConfigured() {
        SmtpRuntimeSettings.ResolvedSmtp smtp = smtpRuntimeSettings.settings();
        return smtp.enabled() && smtp.ready() && hasText(SmtpMailTransport.envelopeFrom(smtp));
    }

    @Override
    public void sendMerchantCredential(String email, String shopName, String temporaryPassword) {
        send(email, "商家账号已开通",
                "您的店铺“" + shopName + "”已通过审核。\n临时密码：" + temporaryPassword
                        + "\n请登录后立即修改密码。");
    }

    @Override
    public void sendMerchantEnabledNotice(String email, String shopName) {
        send(email, "商家权限已开通", "您的现有账号已获得店铺“" + shopName + "”的商家主账号权限。");
    }

    @Override
    public void sendMerchantRevokedNotice(String email, String shopName) {
        send(email, "商家权限已撤销", "您的店铺“" + shopName + "”经营权限已被平台撤销。如有疑问请联系平台客服。");
    }

    @Override
    public void sendMerchantRestoredNotice(String email, String shopName) {
        send(email, "商家权限已恢复", "您的店铺“" + shopName + "”经营权限已重新授予，可继续使用原账号登录商家工作台。");
    }

    @Override
    public void sendAccountCredential(String email, String loginHint, String temporaryPassword) {
        send(email, "平台账号已开通",
                "系统管理员已为你开通平台账号。\n登录标识：" + loginHint
                        + "\n临时密码：" + temporaryPassword
                        + "\n首次登录后必须立即修改密码。");
    }

    @Override
    public void sendCustomerServiceCredential(String email, String shopName, String loginHint, String temporaryPassword) {
        send(email, "客服账号已开通",
                "店铺“" + shopName + "”已为你开通客服账号。\n请在用户 Web 选择商家身份登录。\n登录标识：" + loginHint
                        + "\n临时密码：" + temporaryPassword
                        + "\n首次登录后必须立即修改密码。客服仅可使用用户沟通页面。");
    }

    @Override
    public void sendTestMessage(String email) {
        send(email, "SMTP 配置测试", "这是一封来自综合电商平台的测试邮件。如果能看到它，说明运行时 SMTP 已可用。");
    }

    private void send(String to, String subject, String text) {
        SmtpRuntimeSettings.ResolvedSmtp smtp = smtpRuntimeSettings.settings();
        if (!smtp.enabled()) {
            throw new IllegalStateException("SMTP 已关闭，不会发送邮件。新建账号初始密码为 123456QWERqwer!@");
        }
        String from = SmtpMailTransport.envelopeFrom(smtp);
        if (!smtp.ready() || !hasText(from)) {
            throw new IllegalStateException("SMTP 未配置完整：需要主机，以及发件人或完整的 SMTP 账号邮箱");
        }
        Exception lastFailure = null;
        for (SmtpRuntimeSettings.ResolvedSmtp attempt : SmtpMailTransport.deliveryProfiles(smtp)) {
            try {
                deliver(SmtpMailTransport.mailSender(attempt), from, to, subject, text);
                if (attempt.port() != smtp.port()) {
                    log.info("SMTP delivered via {}:{} after configured port {} failed",
                            attempt.host(), attempt.port(), smtp.port());
                }
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                log.warn("SMTP send failed host={} port={} to={}: {}",
                        attempt.host(), attempt.port(), to, exception.toString());
            }
        }
        throw new IllegalStateException(SmtpMailTransport.explainFailure(lastFailure), lastFailure);
    }

    private static void deliver(JavaMailSenderImpl mailSender, String from, String to, String subject, String text)
            throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);
        mailSender.send(message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
