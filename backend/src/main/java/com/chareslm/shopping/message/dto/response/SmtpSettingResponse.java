package com.chareslm.shopping.message.dto.response;

public record SmtpSettingResponse(
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
