package com.chareslm.shopping.message.service;

import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Locale;
import java.util.Properties;

/**
 * Builds a JavaMail sender for the current SMTP settings.
 *
 * <p>163/126/yeah 使用 465/994 时必须走隐式 SSL，不能再叠加 STARTTLS；
 * 发件人必须与授权账号一致。Java 21 默认 TLS 1.3，网易 SMTP 通常只稳定支持 TLS 1.2。</p>
 */
public final class SmtpMailTransport {
    private SmtpMailTransport() {
    }

    public static JavaMailSenderImpl mailSender(SmtpRuntimeSettings.ResolvedSmtp smtp) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtp.host());
        mailSender.setPort(smtp.port());
        mailSender.setDefaultEncoding("UTF-8");
        mailSender.setProtocol(mailProtocol(smtp.port()));
        String username = normalizeUsername(smtp.host(), smtp.username());
        if (hasText(username)) {
            mailSender.setUsername(username);
        }
        if (hasText(smtp.password())) {
            mailSender.setPassword(smtp.password().trim());
        }
        mailSender.getJavaMailProperties().putAll(mailProperties(smtp));
        return mailSender;
    }

    public static String mailProtocol(int port) {
        return implicitSsl(port) ? "smtps" : "smtp";
    }

    public static Properties mailProperties(SmtpRuntimeSettings.ResolvedSmtp smtp) {
        boolean implicitSsl = implicitSsl(smtp.port());
        boolean starttls = !implicitSsl && smtp.starttlsEnabled();
        String prefix = implicitSsl ? "mail.smtps" : "mail.smtp";
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", mailProtocol(smtp.port()));
        properties.put(prefix + ".auth", Boolean.toString(smtp.smtpAuth()));
        properties.put(prefix + ".connectiontimeout", "15000");
        properties.put(prefix + ".timeout", "15000");
        properties.put(prefix + ".writetimeout", "15000");
        properties.put(prefix + ".ssl.protocols", "TLSv1.2");
        properties.put(prefix + ".ssl.trust", "*");
        properties.put(prefix + ".ssl.checkserveridentity", "false");
        if (implicitSsl) {
            // Use the smtps protocol for implicit SSL. Do not also install SSLSocketFactory:
            // wrapping the socket twice makes 163 close the handshake ("SSL peer shut down incorrectly").
            properties.put("mail.smtps.ssl.enable", "true");
            properties.put("mail.smtp.ssl.enable", "false");
            properties.put("mail.smtp.starttls.enable", "false");
            properties.put("mail.smtp.starttls.required", "false");
        } else {
            properties.put("mail.smtp.ssl.enable", "false");
            properties.put("mail.smtp.starttls.enable", Boolean.toString(starttls));
            properties.put("mail.smtp.starttls.required", Boolean.toString(starttls));
        }
        return properties;
    }

    public static String envelopeFrom(SmtpRuntimeSettings.ResolvedSmtp smtp) {
        String username = normalizeUsername(smtp.host(), smtp.username());
        String from = trimToEmpty(smtp.fromAddress());
        if (isNetease(smtp.host()) && looksLikeEmail(username)) {
            return username;
        }
        if (looksLikeEmail(from)) {
            return from;
        }
        if (looksLikeEmail(username)) {
            return username;
        }
        return from;
    }

    public static boolean implicitSsl(int port) {
        return port == 465 || port == 994;
    }

    /**
     * 163/126 在部分网络（含 Docker）上 465 会在握手时被对端断开，994 才能完成 SSL；
     * 25 常被运营商拦截并返回 EOF greeting。网易账号发送时优先尝试 994。
     */
    public static java.util.List<SmtpRuntimeSettings.ResolvedSmtp> deliveryProfiles(
            SmtpRuntimeSettings.ResolvedSmtp smtp) {
        if (!isNetease(smtp.host())) {
            return java.util.List.of(smtp);
        }
        SmtpRuntimeSettings.ResolvedSmtp ssl994 = withPort(smtp, 994);
        SmtpRuntimeSettings.ResolvedSmtp ssl465 = withPort(smtp, 465);
        if (smtp.port() == 994) {
            return java.util.List.of(ssl994);
        }
        return java.util.List.of(ssl994, ssl465);
    }

    private static SmtpRuntimeSettings.ResolvedSmtp withPort(SmtpRuntimeSettings.ResolvedSmtp smtp, int port) {
        return new SmtpRuntimeSettings.ResolvedSmtp(
                smtp.host(), port, smtp.username(), smtp.password(), smtp.fromAddress(),
                smtp.smtpAuth(), false, smtp.fromDatabase(), smtp.enabled());
    }

    public static String explainFailure(Throwable exception) {
        String detail = rootMessage(exception);
        String lower = detail.toLowerCase(Locale.ROOT);
        if (lower.contains("authenticationfailed") || lower.contains("535") || lower.contains("auth")) {
            return "SMTP 认证失败，请核对账号和密码。";
        }
        if (lower.contains("553") || lower.contains("sender") && lower.contains("not")) {
            return "发件人与 SMTP 账号不一致，请改为同一邮箱后再试。";
        }
        if (lower.contains("peer shut down") || lower.contains("received fatal alert")
                || lower.contains("sslhandshake") || lower.contains("bad greeting") || lower.contains("[eof]")) {
            return "无法与邮件服务器建立安全连接，请检查端口和加密方式。";
        }
        if (lower.contains("timed out") || lower.contains("connection") || lower.contains("connect")) {
            return "无法连接 SMTP 服务器，请检查主机和端口。";
        }
        return "邮件发送失败，请检查 SMTP 配置。";
    }

    public static String normalizeUsername(String host, String username) {
        String value = trimToEmpty(username);
        if (value.isEmpty() || value.contains("@") || !hasText(host)) {
            return value;
        }
        String lowered = host.toLowerCase(Locale.ROOT);
        if (lowered.contains("163.com")) {
            return value + "@163.com";
        }
        if (lowered.contains("126.com")) {
            return value + "@126.com";
        }
        if (lowered.contains("yeah.net")) {
            return value + "@yeah.net";
        }
        return value;
    }

    public static boolean isNetease(String host) {
        if (!hasText(host)) {
            return false;
        }
        String lowered = host.toLowerCase(Locale.ROOT);
        return lowered.contains("163.com") || lowered.contains("126.com") || lowered.contains("yeah.net");
    }

    private static boolean looksLikeEmail(String value) {
        return hasText(value) && value.contains("@");
    }

    private static String rootMessage(Throwable exception) {
        Throwable current = exception;
        String last = exception == null ? "" : String.valueOf(exception.getMessage());
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                last = current.getMessage();
            }
            current = current.getCause();
        }
        return last == null ? "" : last;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
