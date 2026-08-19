package com.chareslm.shopping.message.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSmtpSettingRequest(
        @Size(max = 255) String host,
        @Min(1) @Max(65535) Integer port,
        @Size(max = 254) String username,
        @Size(max = 512) String password,
        @Size(max = 254) String fromAddress,
        Boolean smtpAuth,
        Boolean starttlsEnabled,
        Boolean enabled,
        @NotBlank @Size(max = 64) String currentPassword
) {
}
