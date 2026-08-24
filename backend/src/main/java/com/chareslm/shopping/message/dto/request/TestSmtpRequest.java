package com.chareslm.shopping.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestSmtpRequest(
        @Size(max = 254) String to,
        @NotBlank @Size(max = 64) String currentPassword
) {
}
