package com.chareslm.shopping.message.dto.response;

public record SmtpSettingResponse(
        boolean enabled,
        String host,
        int port,
        String username,
        String fromAddress,
        boolean smtpAuth,
        boolean starttlsEnabled,
        boolean passwordConfigured,
        boolean usingEnvironmentFallback
) {
}
