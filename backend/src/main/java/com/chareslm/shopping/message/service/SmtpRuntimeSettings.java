package com.chareslm.shopping.message.service;

/**
 * Resolves the SMTP credentials actually used for outbound mail.
 *
 * <p>Database settings saved by a super administrator take precedence. When the
 * stored host is blank, environment / Spring Mail values remain the fallback.</p>
 */
public interface SmtpRuntimeSettings {
    ResolvedSmtp settings();

    record ResolvedSmtp(
            String host,
            int port,
            String username,
            String password,
            String fromAddress,
            boolean smtpAuth,
            boolean starttlsEnabled,
            boolean fromDatabase
    ) {
        public boolean ready() {
            return hasText(host) && hasText(fromAddress);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
