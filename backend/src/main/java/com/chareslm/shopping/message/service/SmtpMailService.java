package com.chareslm.shopping.message.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring {@link JavaMailSender} 的同步 SMTP 邮件实现。
 *
 * <p>该类不吞掉配置或投递异常；调用方据此记录失败状态并提供业务重试。</p>
 */
@Service
public class SmtpMailService implements MailService {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailService(JavaMailSender mailSender, @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
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

    private void send(String to, String subject, String text) {
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
